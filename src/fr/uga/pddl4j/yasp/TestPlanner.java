package fr.uga.pddl4j.yasp;

import fr.uga.pddl4j.parser.DefaultParsedProblem;
import fr.uga.pddl4j.parser.ErrorManager;
import fr.uga.pddl4j.parser.Message;
import fr.uga.pddl4j.parser.Parser;
import fr.uga.pddl4j.problem.Problem;
import fr.uga.pddl4j.planners.LogLevel;
import fr.uga.pddl4j.plan.Plan;

import java.io.File;
import java.io.FileWriter;

public class TestPlanner{
    public static void main(final String args[]){

        // Checks the number of arguments from the command line
        if (args.length != 2) {
            System.out.println("Invalid command line");
            return;
        }

        String domain = args[0];
        File problems = new File(args[1]);
        String result="";
        //Create Logger
        try{
            File logger = new File("logger.txt");        
            if(logger.createNewFile()){System.out.println("Logger file created");}        
            FileWriter loggerWriter = new FileWriter(logger,true);
        
            // Creates an instance of the PDDL parser
            final Parser parser = new Parser();
            parser.setLogLevel(LogLevel.OFF);
            loggerWriter.write("\n\nBeginning Test with domain :" + domain + "\n");
            for(File unparsedproblem: problems.listFiles()) {
                // Parses the domain and the problem files.
                final DefaultParsedProblem parsedProblem = parser.parse(domain, unparsedproblem.toString());
                // Gets the error manager of the parser
                final ErrorManager errorManager = parser.getErrorManager();
                final String problemName = unparsedproblem.getName();
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
                    System.out.print("\nparsing domain file \"" + domain + "\" done successfully");
                    System.out.print("\nparsing problem file \"" + problemName + "\" done successfully\n\n");
                    
                    // Create a problem
                    final Problem problem = planner.instantiate(parsedProblem);

                    // Check if the goal is trivially unsatisfiable
                    if (!problem.isSolvable()) {
                        System.out.println("Goal can be simplified to FALSE. No search will solve it");
                        result = "Goal is False\n";
                        System.exit(0);
                    } else {
                        
                        Plan plan = planner.solve(problem);
                            
                            if (plan != null) {
                                result = "YetAnotherSATPlanner plan : \n" + problem.toString(plan) +"\n";
                                System.out.println(result);
                                //add validator
                                
                            } else {
                                System.out.println("No solution found!");
                                result= "No Solution found\n";
                            }
                    }
                }
                loggerWriter.write("Problem :" + problemName +" / " + result + "\n") ;
            } 
            loggerWriter.close();
        }catch(Exception e){e.printStackTrace();}
    }
}