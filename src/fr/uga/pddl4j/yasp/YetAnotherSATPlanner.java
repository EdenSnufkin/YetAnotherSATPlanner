package fr.uga.pddl4j.yasp;

import fr.uga.pddl4j.heuristics.state.FastForward;
import fr.uga.pddl4j.parser.DefaultParsedProblem;
import fr.uga.pddl4j.parser.ErrorManager;
import fr.uga.pddl4j.parser.Message;
import fr.uga.pddl4j.parser.Parser;
import fr.uga.pddl4j.problem.DefaultProblem;
import fr.uga.pddl4j.problem.Problem;
import fr.uga.pddl4j.problem.State;
import fr.uga.pddl4j.planners.statespace.AbstractStateSpacePlanner;
import fr.uga.pddl4j.planners.statespace.HSP;
import fr.uga.pddl4j.planners.LogLevel;
import fr.uga.pddl4j.plan.Plan;

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.IVecInt;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.*;

/**
 * The class shows how to use PDDL4J + SAT4J libraries to create a SAT planner.
 *
 * @version 0.1 - 29.03.2024
 */
public class YetAnotherSATPlanner extends AbstractStateSpacePlanner {

    /**
     * The main method the class. The first argument must be the path to the PDDL domain description and the second
     * argument the path to the PDDL problem description.
     *
     * @param args the command line arguments.
     */

    static final int MAXSTEPS = 50;
    // SAT solver max number of var
    static final int MAXVAR = 1000000;
    // SAT solver max number of clauses
    static final int NBCLAUSES = 500000;
    // SAT solver timeout
    static final int TIMEOUT = 3600;

    static final boolean DEBUG = false;

    /**
     * Instantiates the planning problem from a parsed problem.
     *
     * @param problem the problem to instantiate.
     * @return the instantiated planning problem or null if the problem cannot be instantiated.
     */
    @Override
    public Problem instantiate(DefaultParsedProblem problem) {
        final Problem pb = new DefaultProblem(problem);
        pb.instantiate();
        return pb;
    }

    /**
     * Solves the planning problem and returns the first solution found.
     *
     * @param problem the problem to be solved.
     * @return a solution search or null if it does not exist.
     */
    @Override
    public Plan solve(final Problem problem) {

        int stepmax = MAXSTEPS;
        Plan plan = null;

        // Compute a heuristic lower bound for plan steps
        final FastForward ff = new FastForward(problem);
        final int hlb = ff.estimate(new State(problem.getInitialState()), problem.getGoal());
        if (hlb > MAXSTEPS) {
            System.out.println("Problem has no solution in " + MAXSTEPS + " steps!");
            System.out.println("At least " + hlb + " steps are necessary.");
            System.exit(0);
        } else {

            // Intial number of steps of the SAT encoding
            int steps = hlb;

            // Create the SAT encoding
            SATEncoding sat = new SATEncoding(problem, steps);

            // Create the SAT solver()
            final ISolver solver = SolverFactory.newDefault();
            solver.setTimeout(TIMEOUT);
            // Prepare the solver to accept MAXVAR variables. MANDATORY for MAXSAT solving
            solver.newVar(MAXVAR);
            solver.setExpectedNumberOfClauses(NBCLAUSES);
            IProblem ip = solver;
            
            boolean verbose = true;

            if(verbose){
                System.out.println("nb of actions : " + problem.getActions().size() + " / nb of fluents : " + problem.getFluents().size());
            }





            // Search starts here!
            boolean doSearch = true;
            List<List<Integer>> currentEncoding = new ArrayList<List<Integer>>();
            Vec<IVecInt> VecEncoding;
            VecInt vecIntEnco;
            PrintWriter systemWriter = new PrintWriter(System.out);
            

            while (doSearch && !(steps > stepmax)) {
                currentEncoding = Stream.concat(sat.currentDimacs.stream(), sat.currentGoal.stream())
                  .collect(Collectors.toList());
                solver.reset();
                try{
                    VecEncoding = new Vec<IVecInt>();
                    for (List<Integer> clause:currentEncoding){
                        vecIntEnco = new VecInt(clause.stream().mapToInt(i->i).toArray());
                        VecEncoding.push(vecIntEnco);
                    }
                    
                    if(!VecEncoding.isEmpty()){solver.addAllClauses(VecEncoding);}
                    //System.out.println(solver.toString());
                    //if(verbose){solver.printInfos(systemWriter);}
                    doSearch = !ip.isSatisfiable();
                }
                catch(Exception e){
                    System.out.println(e.getMessage());
                }
                
                // doSearch = false only when solution is found
                if (!doSearch){
                    System.out.println("Problem is Satisfiable !");
                    final List<Integer> solution = Arrays.stream(solver.model()).boxed().collect(Collectors.toList()); 
                    plan = sat.extractPlan(solution,problem);
                    plan.remove(plan.actions().size()-1);
                } else {
                    System.out.println("Problem isn't Satisfiable :()");
                    steps++;
                    //Maybe do binary search on steps => possible amélioration 
                    sat.next();
                }
            }
            systemWriter.close();
        }
        return plan;
    }

    public static void main(final String[] args) {

        // Checks the number of arguments from the command line
        if (args.length != 2) {
            System.out.println("Invalid command line");
            return;
        }

        try {
            // Creates an instance of the PDDL parser
            final Parser parser = new Parser();
            parser.setLogLevel(LogLevel.OFF);
            // Parses the domain and the problem files.
            final DefaultParsedProblem parsedProblem = parser.parse(args[0], args[1]);
            // Gets the error manager of the parser
            final ErrorManager errorManager = parser.getErrorManager();
            // Checks if the error manager contains errors
            if (!errorManager.isEmpty()) {
                // Prints the errors
                for (Message m : errorManager.getMessages()) {
                    System.out.println(m.toString());
                }
            } else {
                
                // Creates an instance of the SAT planner
                final YetAnotherSATPlanner planner = new YetAnotherSATPlanner();

                // Prints that the domain and the problem were successfully parsed
                System.out.print("\nparsing domain file \"" + args[0] + "\" done successfully");
                System.out.print("\nparsing problem file \"" + args[1] + "\" done successfully\n\n");
                
                // Create a problem
                final Problem problem = planner.instantiate(parsedProblem);

                // Check if the goal is trivially unsatisfiable
                if (!problem.isSolvable()) {
                    System.out.println("Goal can be simplified to FALSE. No search will solve it");
                    System.exit(0);
                } else {
                    
                    Plan plan = planner.solve(problem);
                        
                        if (plan != null) {
                            System.out.println("YetAnotherSATPlanner Plan:");
                            System.out.println(problem.toString(plan));
                            final AbstractStateSpacePlanner solverHSP = new HSP();
                            try{
                                plan = solverHSP.solve(problem);
                                System.out.println("HSP Plan :");
                                System.out.println(problem.toString(plan));
                            } catch (Exception e) {e.printStackTrace();} 
                        } else {
                            System.out.println("No solution found!");
                        }
                }
            }
            // This exception could happen if the domain or the problem does not exist
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}