import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import data.Data;
import data.Link;
import data.Solution;

public class Cplex {

    public void solveWithCplex() throws IOException {

        String timeNow = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss").format(LocalDateTime.now());
        String[] filenames = new String[] { "CMT1X" };
        int timeLimit = 3600;

        for (String filename : filenames) {
            new Data(filename);

            String fileDirectory = String.format("./solution/cplex/%s/%s", filename, timeNow);
            Files.createDirectories(Paths.get(fileDirectory));

            double startTime = System.currentTimeMillis();

            double timeToBest = 0;

            int iterToBest = 0;
            int iterCounter = 0;
            int imprCounter = 0;

            Solution bestSolution = new Greedy().run();
            double processTime = (System.currentTimeMillis() - startTime) / 1000;

            bestSolution.exportSolution(
                    String.format("%s/%s-%06d.sol", fileDirectory, filename, ++iterCounter),
                    bestSolution.getTotalCost(),
                    processTime, 0);

            Set<Link> links = new HashSet<>(Data.linkManager.getAll());

            Model nextModel = new Model(timeLimit, links);
            nextModel.buildCplexModel();
            nextModel.solveModel();

            processTime = (System.currentTimeMillis() - startTime) / 1000;

            nextModel.solutionsOut.getFirst().exportSolution(
                    String.format("%s/%s-%06d.sol", fileDirectory, filename, ++iterCounter),
                    bestSolution.getTotalCost(),
                    processTime, 0);
            Solution firstSolution = nextModel.solutionsOut.getFirst();
            nextModel.finalizeModel();

            if (firstSolution.getTotalCost() < bestSolution.getTotalCost() - 0.0001) {
                bestSolution = nextModel.solutionsOut.getFirst();
                imprCounter++;
                timeToBest = (System.currentTimeMillis() - startTime) / 1000;
                iterToBest = iterCounter;
            }

            nextModel.finalizeModel();

            PrintStream printer = new PrintStream(fileDirectory + "/" + filename + ".cnt");
            printer.printf("%-30s%8.2f%n", "BEST COST", bestSolution.getTotalCost());
            printer.printf("%-30s%8.2f%n", "PROCESS TIME", processTime);
            printer.printf("%-30s%8.2f%n", "TIME TO BEST", timeToBest);
            printer.printf("%-30s%8d%n", "ITERATIONS", iterCounter);
            printer.printf("%-30s%8d%n", "ITERATIONS TO BEST", iterToBest);
            printer.printf("%-30s%8d%n", "IMPROVEMENTS", imprCounter);
            printer.close();
        }
    }
}
