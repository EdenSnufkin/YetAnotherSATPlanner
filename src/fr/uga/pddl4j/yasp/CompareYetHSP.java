package fr.uga.pddl4j.yasp;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.uga.pddl4j.parser.DefaultParsedProblem;
import fr.uga.pddl4j.plan.Plan;
import fr.uga.pddl4j.planners.Statistics;
import fr.uga.pddl4j.planners.statespace.HSP;
import fr.uga.pddl4j.problem.Problem;
import fr.uga.pddl4j.planners.LogLevel;

import java.util.Arrays;
import java.util.ArrayList;

public class CompareYetHSP{

    public String escapeSpecialCharacters(String data) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }
        String escapedData = data.replaceAll("\\R", " ");
        if (escapedData.contains(",") || escapedData.contains("\"") || escapedData.contains("'")) {
            escapedData = escapedData.replace("\"", "\"\"");
            escapedData = "\"" + escapedData + "\"";
        }
        return escapedData;
    }

    public String convertToCSV(String[] data) {
        return Stream.of(data)
        .map(this::escapeSpecialCharacters)
        .collect(Collectors.joining(","));
    }

    public void givenDataArray_whenConvertToCSV_thenOutputCreated(String CSV_FILE_NAME, List<String[]> dataLines) throws IOException {
        File csvOutputFile = new File(CSV_FILE_NAME);
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            dataLines.stream()
            .map(this::convertToCSV)
            .forEach(pw::println);
        }
        assertTrue(csvOutputFile.exists());
    }
    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Invalid command line");
            return;
        }

        File test_dir = new File(args[1]);
        List<File> test_files = Arrays.asList(test_dir.listFiles());
        String domain = args[0];

        List<String[]> dataset = new ArrayList<String[]>();

        System.out.println(domain);


        for(File test_file:test_files){
            String problem_name = test_file.toString();
            if(problem_name != domain){
                System.out.println(problem_name);   

                try{
                    final YetAnotherSATPlanner plannerYet = new YetAnotherSATPlanner();
                    final DefaultParsedProblem problemParsedYet = plannerYet.parse(domain,problem_name);
                    final Problem problemYet = plannerYet.instantiate(problemParsedYet);
                    final Plan planYet = plannerYet.solve(problemYet);
                    final Statistics statsYet = plannerYet.getStatistics();

                    final HSP plannerHSP = new HSP();
                    plannerHSP.setLogLevel(LogLevel.ERROR);
                    final DefaultParsedProblem problemParsedHSP = plannerHSP.parse(domain,problem_name);
                    final Problem problemHSP = plannerHSP.instantiate(problemParsedHSP);
                    final Plan planHSP =  plannerHSP.solve(problemHSP);
                    final Statistics statsHSP = plannerHSP.getStatistics();

                    dataset.add(new String[]{
                        problem_name,
                        String.valueOf(statsYet.getNumberOfActions()),
                        String.valueOf(statsYet.getTimeToEncode()),
                        String.valueOf(statsYet.getTimeToSearch()),
                        String.valueOf(statsHSP.getTimeToEncode()),
                        String.valueOf(statsHSP.getTimeToSearch())});

                    System.out.println("YET plan :\n");
                    System.out.println(problemYet.toString(planYet));
                    System.out.println("HSP plan :\n");
                    System.out.println(problemHSP.toString(planHSP));


                } catch (Exception e){
                    System.out.println(e.getMessage());
                }
            }   
        }
        try{
            System.out.println("Writing data to csv File");
            CompareYetHSP compare = new CompareYetHSP();
            compare.givenDataArray_whenConvertToCSV_thenOutputCreated("CSV_ComparedYETHSP.csv",dataset);

        }catch (Exception e ){System.out.println(e.getMessage());}
    }
}