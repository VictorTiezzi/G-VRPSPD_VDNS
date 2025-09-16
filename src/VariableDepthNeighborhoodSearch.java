import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import data.Data;
import data.Link;
import data.Node;
import data.Solution;

public class VariableDepthNeighborhoodSearch {
    private final int MIN_CLIQUE_SIZE = 4;
    private final int MAX_CLIQUE_SIZE = Data.numberOfNodes;
    private final double COST_TOLERANCE = 0.001;

    private final int MAX_SOLUTION_TO_BUILD_CLIQUE = 5;

    private final List<Node> nodes = List.copyOf(Data.clientNodes);
    private final Node depot = Data.depot;

    private final double startTime;
    private final String fileDirectory;
    private final String instanceName;

    private final LocalSearch localSearch;

    private Solution bestSolution = null;
    private int cliqueSize = MIN_CLIQUE_SIZE;

    private double timeToBest = 0.0;
    private int iterationCounter = 0;
    private int iterationToBest = 0;
    private int improvementCount = 0;

    public VariableDepthNeighborhoodSearch(String filename, String solverStartTime, int solverTimeLimit,
            int subproblemTimeLimit, int executionId) {

        startTime = System.currentTimeMillis();
        instanceName = filename;
        fileDirectory = String.format("./solution/%s/VDNS/%s/exec_%s/",
                solverStartTime, filename, executionId);
        createDirectories();

        localSearch = new LocalSearch();

        executeLNSAlgoritm(subproblemTimeLimit, solverTimeLimit);
    }

    public void executeLNSAlgoritm(int subproblemTimeLimit, int solverTimeLimit) {
        this.bestSolution = new Greedy().run();
        exportSolution(bestSolution, "", 0);
        iterationCounter++;

        TreeSet<Solution> allCurrentSolutions = new TreeSet<>(Comparator.comparingDouble(Solution::getTotalCost));
        allCurrentSolutions.add(bestSolution);

        List<Node> remainingNodes = new ArrayList<>(nodes);

        do {

            if (remainingNodes.size() < cliqueSize)
                remainingNodes = new ArrayList<>(nodes);

            List<Node> clique = buildClique(remainingNodes, cliqueSize);

            Set<Link> subMatrix = buildSubproblemLinks(clique, allCurrentSolutions);

            double timeLimit = getElapsedTime() + subproblemTimeLimit <= solverTimeLimit
                    ? subproblemTimeLimit
                    : solverTimeLimit - getElapsedTime();

            List<Solution> solutionsFromCplex = new LinkedList<>();
            Model model = new Model(timeLimit, subMatrix);
            if (model.solveModel())
                solutionsFromCplex.addAll(model.solutionsOut);
            if (solutionsFromCplex.isEmpty())
                solutionsFromCplex.add(bestSolution);
            model.finalizeModel();

            allCurrentSolutions.clear();
            allCurrentSolutions.add(bestSolution);

            Solution bestSolutionFromCplex = solutionsFromCplex.removeFirst();

            if (checkNewSolution(bestSolutionFromCplex)) {
                createAndCheckNewLocalSearchSolution(bestSolution);
                allCurrentSolutions.add(bestSolution);
            }

            cliqueSize = bestSolutionFromCplex.gap <= 0.01
                    ? Math.min(cliqueSize + 1, MAX_CLIQUE_SIZE)
                    : Math.max(cliqueSize - 1, MIN_CLIQUE_SIZE);

            solutionsFromCplex.removeAll(allCurrentSolutions);

            for (Solution solution : solutionsFromCplex) {
                createAndCheckNewLocalSearchSolution(solution);
                allCurrentSolutions.add(solution);
            }

            iterationCounter++;
        } while (getElapsedTime() <= solverTimeLimit);

        printCnt();
        exportSolution(bestSolution, "best", 0);
    }

    private boolean checkNewSolution(Solution solution) {
        exportSolution(solution, "", cliqueSize);
        if (solution.getTotalCost() < this.bestSolution.getTotalCost() - COST_TOLERANCE) {
            this.bestSolution = solution;
            improvementCount++;
            timeToBest = getElapsedTime();
            iterationToBest = iterationCounter;
            return true;
        }
        return false;
    }

    private boolean createAndCheckNewLocalSearchSolution(Solution solution) {
        solution = localSearch.run(solution);
        if (solution.getTotalCost() < bestSolution.getTotalCost() - COST_TOLERANCE) {
            bestSolution = solution;
            exportSolution(solution, "ls", 0);
            improvementCount++;
            timeToBest = getElapsedTime();
            iterationToBest = iterationCounter;
            return true;
        }
        return false;
    }

    private List<Node> buildClique(List<Node> remainingNodes, int cliqueSize) {
        List<Node> clique = new ArrayList<>();
        clique.add(depot);

        List<Node> nodesLeft = new ArrayList<>(nodes);

        Node baseNode = selectRandomNode(remainingNodes);
        remainingNodes.remove(baseNode);

        // Node baseNode = selectRandomNode(nodesLeft);

        nodesLeft.remove(baseNode);
        clique.add(baseNode);
        while (clique.size() < cliqueSize) {
            // Node nextNode = selectRandomNode(nodesLeft);
            Node nextNode = selectNodeByDistance(nodesLeft, baseNode);
            remainingNodes.remove(nextNode);
            nodesLeft.remove(nextNode);
            clique.add(nextNode);
        }
        return clique;
    }

    private Node selectRandomNode(List<Node> candidates) {
        return candidates.get(new Random().nextInt(candidates.size()));
    }

    private Node selectNodeByDistance(List<Node> candidates, Node baseNode) {
        double totalWeight = candidates.stream()
                .mapToDouble(node -> 1 / (Data.linkManager.get(baseNode.id(), node.id()).distance + 0.01))
                .sum();

        NavigableMap<Double, Node> probabilityMap = new TreeMap<>();
        double cumulative = 0;

        for (Node node : candidates) {
            double weight = 1 / (Data.linkManager.get(baseNode.id(), node.id()).distance + 0.01);
            cumulative += weight / totalWeight;
            probabilityMap.put(cumulative, node);
        }
        return probabilityMap.ceilingEntry(new Random().nextDouble()).getValue();
    }

    private Set<Link> buildSubproblemLinks(List<Node> clique, TreeSet<Solution> bestSolutions) {
        Set<Link> linkSet = new HashSet<>();

        Set<Node> incoming = new HashSet<>();
        Set<Node> outgoing = new HashSet<>();

        int maxSolutions = Math.min(bestSolutions.size(), MAX_SOLUTION_TO_BUILD_CLIQUE);

        for (int i = 0; i < maxSolutions; i++) {
            Solution solution = bestSolutions.pollFirst();

            linkSet.addAll(solution.getAllLinks());

            solution.routes.forEach(route -> {
                for (int n = 0; n < route.nodes.size(); n++) {
                    Node node = route.nodes.get(n);
                    if (clique.contains(node)) {
                        incoming.add(node.equals(route.nodes.getFirst()) ? depot : route.nodes.get(n - 1));
                        outgoing.add(node.equals(route.nodes.getLast()) ? depot : route.nodes.get(n + 1));
                    }
                }
            });
        }

        // Adiciona arcos de clique para clique
        for (int i = 0; i < clique.size(); i++) {
            for (int j = i + 1; j < clique.size(); j++) {
                linkSet.add(Data.linkManager.get(clique.get(i).id(), clique.get(j).id()));
                linkSet.add(Data.linkManager.get(clique.get(j).id(), clique.get(i).id()));
            }
        }

        // Adiciona arcos de incoming para clique
        incoming.forEach(node1 -> clique.forEach(node2 -> {
            if (node1.id() != node2.id()) {
                linkSet.add(Data.linkManager.get(node1.id(), node2.id()));
            }
        }));

        // Adiciona arcos de clique para outgoing
        clique.forEach(node1 -> outgoing.forEach(node2 -> {
            if (node1.id() != node2.id()) {
                linkSet.add(Data.linkManager.get(node1.id(), node2.id()));
            }
        }));

        // Adiciona arcos de incoming para outgoing
        incoming.forEach(node1 -> outgoing.forEach(node2 -> {
            if (node1.id() != node2.id()) {
                linkSet.add(Data.linkManager.get(node1.id(), node2.id()));
            }
        }));

        return linkSet;
    }

    private double getElapsedTime() {
        return (System.currentTimeMillis() - this.startTime) / 1000;
    }

    private void createDirectories() {
        try {
            Files.createDirectories(Paths.get(fileDirectory));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportSolution(Solution solution, String suffix, int cliqueSize) {
        solution.exportSolution(
                fileDirectory + instanceName + String.format("-%06d", iterationCounter) + suffix + ".sol",
                bestSolution.getTotalCost(), getElapsedTime(), cliqueSize);
    }

    private void printCnt() {
        try (PrintStream printer = new PrintStream(fileDirectory + instanceName + ".cnt")) {
            printer.printf("%-30s%8.2f%n", "BEST COST", bestSolution.getTotalCost());
            printer.printf("%-30s%8.2f%n", "PROCESS TIME", getElapsedTime());
            printer.printf("%-30s%8.2f%n", "TIME TO BEST", timeToBest);
            printer.printf("%-30s%8d%n", "ITERATIONS", iterationCounter);
            printer.printf("%-30s%8d%n", "ITERATIONS TO BEST", iterationToBest);
            printer.printf("%-30s%8d%n", "IMPROVEMENTS", improvementCount);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}
