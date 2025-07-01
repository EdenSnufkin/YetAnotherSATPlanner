package fr.uga.pddl4j.yasp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import fr.uga.pddl4j.plan.Plan;
import fr.uga.pddl4j.plan.SequentialPlan;
import fr.uga.pddl4j.problem.Fluent;
import fr.uga.pddl4j.problem.Problem;
import fr.uga.pddl4j.problem.operator.Action;
import fr.uga.pddl4j.problem.operator.Condition;
import fr.uga.pddl4j.problem.operator.Effect;
import fr.uga.pddl4j.util.BitVector;

/**
 * This class implements a planning problem/domain encoding into DIMACS
 *
 * @author H. Fiorino
 * @version 0.1 - 30.03.2024
 */
public final class SATEncoding {
    /*
     * A SAT problem in dimacs format is a list of int list a.k.a clauses
     */
    private List<List<Integer>> initList = new ArrayList<List<Integer>>();
    private int nb_fluents;

    /*
     * Goal
     */
    private List<Integer> goalList = new ArrayList<Integer>();

    /*
     * Actions
     */
    private List<Action> actions = new ArrayList<Action>();
    private List<List<Integer>> actionPreconditionList = new ArrayList<List<Integer>>();
    private List<List<Integer>> actionEffectList = new ArrayList<List<Integer>>();
    private int nb_actions;

    /*
     * State transistions
     */
    private HashMap<Integer, List<Integer>> addList = new HashMap<Integer, List<Integer>>();
    private HashMap<Integer, List<Integer>> delList = new HashMap<Integer, List<Integer>>();
    private List<List<Integer>> stateTransitionList = new ArrayList<List<Integer>>();
    

    /*
     * Action disjunctions
     */
    
    private List<List<Integer>> actionDisjunctionList = new ArrayList<List<Integer>>();
    

    /*
     * Current DIMACS encoding of the planning domain and problem for #steps steps
     * Contains the initial state, actions and action disjunction
     * Goal is no there!
     */
    public List<List<Integer>> currentDimacs = new ArrayList<List<Integer>>();

    /*
     * Current goal encoding
     */
    public List<List<Integer>> currentGoal = new ArrayList<List<Integer>>();

    /*
     * Current number of steps of the SAT encoding
     */
    private int steps;

    public SATEncoding(Problem problem, int steps) {

        this.steps = steps;

        // Encoding of init
        // Each fact is a unit clause
        // Init state step is 1
        // We get the initial state from the planning problem
        // State is a bit vector where the ith bit at 1 corresponds to the ith fluent being true
        this.nb_fluents = problem.getFluents().size();
        //System.out.println(" fluents = " + nb_fluents );
        final BitVector init = problem.getInitialState().getPositiveFluents();
        this.actions = problem.getActions();
        this.nb_actions = this.actions.size();
        List<Integer> clause;

        // InitList => format Dimacs
        for(Integer i=0;i<this.nb_fluents;i++){
            clause = new ArrayList<Integer>();
            if(init.get(i))
            {
                clause.add(pair(i,1));
            }
            else
            {
                clause.add(-pair(i,1));
            }
            this.initList.add(clause);
        }

        //goalList

        BitVector goalBitSet = problem.getGoal().getPositiveFluents();
        goalBitSet.or(problem.getGoal().getNegativeFluents());

        for(Integer i=0;i<this.nb_fluents;i++){
            if(goalBitSet.get(i)) {this.goalList.add(i);}
        }

        for(Integer fluent:this.goalList){
            this.currentGoal.add(new ArrayList<Integer>(){{add(pair(fluent,1));}});
        }

        //check if goal is in initList => TODO !!!


        
        encode(1, steps);
        
    }

    private void encode(int from, int to){

        this.currentGoal.clear();
        this.currentDimacs.clear();
        if(from==1){
            for( List<Integer> sublist : this.initList){
                this.currentDimacs.add(new ArrayList<>(sublist));
            }
        }
        final int nb_fluents = this.nb_fluents;

        //copy goal with right "final step"
        for(Integer fluent: this.goalList){
            this.currentGoal.add(new ArrayList<Integer>(){{
                        add(pair(fluent,to));}});
        }

        for(int t=from;t<=to;t++){
            //reset list
            this.actionDisjunctionList.clear();
            this.actionPreconditionList.clear();
            this.actionEffectList.clear();
            this.stateTransitionList.clear();
            this.addList.clear();
            this.delList.clear();

            //action clauses
            for(int a=0;a<this.nb_actions;a++){
                Action action = this.actions.get(a);
                int actionVal = pair(nb_fluents + a, t);

                //precond & effect
                Condition pre = action.getPrecondition();
                BitVector posPre = pre.getPositiveFluents();
                BitVector posNeg = pre.getNegativeFluents();

                Effect eff = action.getUnconditionalEffect();
                BitVector effPos = eff.getPositiveFluents();
                BitVector effNeg = eff.getNegativeFluents();
                for(int fluent=0;fluent<nb_fluents;fluent++){
                    if(posPre.get(fluent)) { this.actionPreconditionList.add(Arrays.asList(-actionVal,pair(fluent,t)));}
                    if(posNeg.get(fluent)) { this.actionPreconditionList.add(Arrays.asList(-actionVal,-pair(fluent,t)));}
                    if(effPos.get(fluent)) { this.actionEffectList.add(Arrays.asList(-actionVal,pair(fluent,t+1)));
                                             this.addList.computeIfAbsent(pair(fluent,t), k-> new ArrayList<>()).add(actionVal);}
                    if(effNeg.get(fluent)) { this.actionEffectList.add(Arrays.asList(-actionVal,-pair(fluent,t+1)));
                                             this.delList.computeIfAbsent(pair(fluent,t), k-> new ArrayList<>()).add(actionVal);}
                }
                
                //No 2 action at same time
                for(int b = a+1;b<this.nb_actions;b++){
                    int actionValB = pair(nb_fluents + b,t);
                    this.actionDisjunctionList.add(Arrays.asList(-actionVal,-actionValB));
                }
            }

            //frameAxiom
            if(t < to){
                for(int fluent=0;fluent<this.nb_fluents;fluent++){
                    int fi = pair(fluent,t);
                    int fip1 = pair(fluent,t+1);

                    List<Integer> adds = this.addList.getOrDefault(fi, new ArrayList<>());
                    List<Integer> dels = this.delList.getOrDefault(fi, new ArrayList<>());

                    List<Integer> clause = new ArrayList<>();
                    clause.add(fi);
                    clause.add(-fip1);
                    clause.addAll(adds);
                    stateTransitionList.add(clause);

                    clause = new ArrayList<>();
                    clause.add(-fi);
                    clause.add(fip1);
                    clause.addAll(dels);
                    stateTransitionList.add(clause);
                }
            }
            
            //add all clauses
            this.currentDimacs.addAll(this.actionPreconditionList);
            this.currentDimacs.addAll(this.actionEffectList);
            this.currentDimacs.addAll(this.actionDisjunctionList);
            this.currentDimacs.addAll(this.stateTransitionList);
        }

        System.out.println("Encoding : successfully done (" + (this.currentDimacs.size()
                + this.currentGoal.size()) + " clauses, " + to + " steps)");
    }


    
    /*
     * SAT encoding for next step
     */
    public void next() {
        this.steps++;
        encode(this.steps-1, this.steps);
    }

    public String toString(final List<Integer> clause, final Problem problem) {
        final int nb_fluents = problem.getFluents().size();
        List<Integer> dejavu = new ArrayList<Integer>();
        String t = "[";
        String u = "";
        int tmp = 1;
        int [] couple;
        int bitnum;
        int step;
        for (Integer x : clause) {
            if (x > 0) {
                couple = unpair(x);
                bitnum = couple[0];
                step = couple[1];
            } else {
                couple = unpair(- x);
                bitnum = - couple[0];
                step = couple[1];
            }
            t = t + "(" + bitnum + ", " + step + ")";
            t = (tmp == clause.size()) ? t + "]\n" : t + " + ";
            tmp++;
            final int b = Math.abs(bitnum);
            if (!dejavu.contains(b)) {
                dejavu.add(b);
                u = u + b + " >> ";
                if (nb_fluents >= b) {
                    Fluent fluent = problem.getFluents().get(b - 1);
                    u = u + problem.toString(fluent)  + "\n";
                } else {
                    u = u + problem.toShortString(problem.getActions().get(b - nb_fluents)) + "\n";
                }
            }
        }
        return t + u;
    }

    public Plan extractPlan(final List<Integer> solution, final Problem problem) {
        Plan plan = new SequentialPlan();
        HashMap<Integer, Action> sequence = new HashMap<Integer, Action>();
        final int nb_fluents = problem.getFluents().size();
        int[] couple;
        int bitnum;
        int step;
        for (Integer x : solution) {
            if (x > 0) {
                couple = unpair(x);
                bitnum = couple[0];
            } else {
                couple = unpair(-x);
                bitnum = -couple[0];
            }
            step = couple[1];
            // This is a positive (asserted) action
            if (bitnum > nb_fluents) {
                final Action action = problem.getActions().get(bitnum - nb_fluents);
                sequence.put(step, action);
            }
        }

        for (int s = sequence.keySet().size(); s > 0 ; s--) {
            plan.add(0, sequence.get(s));
        }
        return plan;
    }
    
    // Cantor paring function generates unique numbers
    private static int pair(int num, int step) {
        return (int) (0.5 * (num + step) * (num + step + 1) + step);
    }

    private static int[] unpair(int z) {
        /*
        Cantor unpair function is the reverse of the pairing function. It takes a single input
        and returns the two corespoding values.
        */
        int t = (int) (Math.floor((Math.sqrt(8 * z + 1) - 1) / 2));
        int bitnum = t * (t + 3) / 2 - z;
        int step = z - t * (t + 1) / 2;
        return new int[]{bitnum, step}; //Returning an array containing the two numbers
    }

}