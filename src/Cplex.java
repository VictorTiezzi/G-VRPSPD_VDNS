import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import data.Instance;
import data.Link;
import data.Solution;
import ilog.concert.IloException;
import model.ModelFactory;
import model.cplex.CplexBaseModel;
import model.cplex.vrpspd.GVRPSPDModel;

public class Cplex {

    public void solveWithCplex() throws IloException, Exception {

        String timeNow = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss").format(LocalDateTime.now());
        String[] filenames = new String[] { "CMT1X" };
        ModelFactory modelFactory = GVRPSPDModel.factory();
        int timeLimit = 3600;

        for (String filename : filenames) {
            Instance instance = new Instance(filename, "SALHI");
            modelFactory.setCostFunction(instance);

            String fileDirectory = String.format("./solution/cplex/%s/%s", filename, timeNow);
            Files.createDirectories(Paths.get(fileDirectory));

            double startTime = System.currentTimeMillis();

            double timeToBest = 0;

            int iterToBest = 0;
            int iterCounter = 0;
            int imprCounter = 0;

            Solution bestSolution = new Greedy(instance, new LocalSearch(instance)).run();
            double processTime = (System.currentTimeMillis() - startTime) / 1000;

            bestSolution.exportSolution(instance.instanceName(),
                    String.format("%s/%s-%06d.sol", fileDirectory, filename, iterCounter++),
                    bestSolution.getTotalCost(),
                    processTime, 0);

            Set<Link> links = new HashSet<>(instance.linkManager().getAll());

            List<Solution> solutionsFromCplex = new LinkedList<>();

            try (CplexBaseModel model = modelFactory.create(instance, links, timeLimit)) {
                solutionsFromCplex.addAll(model.solve());
                processTime = (System.currentTimeMillis() - startTime) / 1000;
                if (solutionsFromCplex.isEmpty())
                    solutionsFromCplex.add(bestSolution);
            }

            Solution firstSolution = solutionsFromCplex.getFirst();

            firstSolution.exportSolution(instance.instanceName(),
                    String.format("%s/%s-%06d.sol", fileDirectory, filename, iterCounter),
                    bestSolution.getTotalCost(),
                    processTime, 0);

            if (firstSolution.getTotalCost() < bestSolution.getTotalCost() - 0.0001) {
                bestSolution = firstSolution;
                imprCounter++;
                timeToBest = (System.currentTimeMillis() - startTime) / 1000;
                iterToBest = iterCounter;
            }

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
