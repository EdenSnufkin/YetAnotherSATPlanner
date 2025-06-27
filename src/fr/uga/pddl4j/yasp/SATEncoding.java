package fr.uga.pddl4j.yasp;

import fr.uga.pddl4j.plan.Plan;
import fr.uga.pddl4j.plan.SequentialPlan;
import fr.uga.pddl4j.problem.Fluent;
import fr.uga.pddl4j.problem.Problem;
import fr.uga.pddl4j.problem.operator.Action;
import fr.uga.pddl4j.problem.operator.ConditionalEffect;
import fr.uga.pddl4j.util.BitVector;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

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
    private List<List<Integer>> actionPreconditionList = new ArrayList<List<Integer>>();
    private List<List<Integer>> actionEffectPosList = new ArrayList<List<Integer>>();
    private List<List<Integer>> actionEffectNegList = new ArrayList<List<Integer>>();
    private int nb_actions;

    /*
     * State transistions
     */
    /*
    private HashMap<Integer, List<Integer>> addList = new HashMap<Integer, List<Integer>>();
    private HashMap<Integer, List<Integer>> delList = new HashMap<Integer, List<Integer>>();
    private List<List<Integer>> stateTransitionList = new ArrayList<List<Integer>>();
    */

    /*
     * Action disjunctions
     */
    /*
    private List<List<Integer>> actionDisjunctionList = new ArrayList<List<Integer>>();
    */

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
        final List<Action> actions = problem.getActions();
        this.nb_actions = actions.size();
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

        //action precondition and effect
        BitVector actionPrecond;
        BitVector actionEffectPos = new BitVector(nb_fluents);
        BitVector actionEffectNeg = new BitVector(nb_fluents);

        for(int i=0;i<this.nb_actions;i++){
            actionPrecond = actions.get(i).getPrecondition().getPositiveFluents();

            actionEffectNeg.clear();
            actionEffectPos.clear();
            for (ConditionalEffect condition: actions.get(i).getConditionalEffects()){
                actionEffectPos.or(condition.getEffect().getPositiveFluents()); 
                actionEffectNeg.or(condition.getEffect().getNegativeFluents());}

            this.actionPreconditionList.add(new ArrayList<Integer>());
            this.actionEffectPosList.add(new ArrayList<Integer>());
            this.actionEffectNegList.add(new ArrayList<Integer>());
            
            for(int j=0;j<this.nb_fluents;j++){
                if(actionPrecond.get(j)){this.actionPreconditionList.get(i).add(j);}
                if(actionEffectPos.get(j)){this.actionEffectPosList.get(i).add(j);}
                if(actionEffectNeg.get(j)){this.actionEffectNegList.get(i).add(j);}
            }
        }

        //copy initList into dimacs
        

        
        encode(1, steps);
        
    }

    private void encode(int from, int to) {
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

 
        
        int action_value;
        List<Integer> clause;

        for(int curr_step=from;curr_step<to;curr_step++){
            //an action, when applicable, has some effects
            //(no ai or AND pi ) AND (no ai or AND ei+1)
            for(int i=0;i<nb_actions;i++){
                //no ai or AND pi
                action_value= pair(i+ nb_fluents,curr_step);
                for(Integer fluent:this.actionPreconditionList.get(i)){
                    clause = new ArrayList<Integer>();
                    clause.add(pair(fluent,curr_step));
                    clause.add(-action_value);
                    this.currentDimacs.add(clause);
                }
                //no ai or AND ei+1
                if (curr_step!=to-1){
                    for(Integer fluent:this.actionEffectPosList.get(i)){
                        clause = new ArrayList<Integer>();
                        clause.add(-action_value);
                        clause.add(pair(fluent,curr_step+1));
                        this.currentDimacs.add(clause);
                    }
                    for(Integer fluent:this.actionEffectNegList.get(i)){
                        clause = new ArrayList<Integer>();
                        clause.add(-action_value);
                        clause.add(pair(fluent,curr_step+1));
                        this.currentDimacs.add(clause);
                    }
                }

                //no 2 actions in same step
                //no ai or no bi
                for(int j=0;j<this.nb_actions;j++)
                {
                    if(i!=j){
                        clause = new ArrayList<Integer>();
                        clause.add(-action_value);
                        clause.add(-pair(j+nb_fluents,curr_step));
                        this.currentDimacs.add(clause);
                    }
                }
            }

            //explanatory frame axiom
            for(int i=0;i<nb_fluents;i++){
                //Fi OR no Fi+1 OR (OR Ai with fi in effect+ ai) AND
                clause = new ArrayList<Integer>();
                clause.add(pair(i,curr_step));
                clause.add(-pair(i,curr_step+1));
                for(int action=0;action<nb_actions;action++){
                    if(this.actionEffectPosList.get(action).contains(i)){ 
                        clause.add(pair(action+nb_fluents,curr_step));}
                }
                this.currentDimacs.add(clause);
                
                //no Fi OR Fi+1 OR (OR Ai with fi in effect- ai)
                clause = new ArrayList<Integer>();
                clause.add(-pair(i,curr_step));
                clause.add(pair(i,curr_step+1));
                for(int action=0;action<nb_actions;action++){
                    if(this.actionEffectNegList.get(action).contains(i)){ 
                        clause.add(pair(action+nb_fluents,curr_step));}
                }
                this.currentDimacs.add(clause);
            }
            
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