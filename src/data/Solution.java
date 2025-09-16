package data;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Solution {

    public double lowerBound;
    public double gap;
    public String status;
    public double creationTime;
    public double solvingTime;
    public List<Route> routes;

    public String cplexLog;

    public Solution(List<Route> routes, String status, double creationTime, double solvingTime) {
        this.routes = routes;
        this.status = status;
        this.lowerBound = getTotalCost();
        this.gap = 0.0;
        this.creationTime = creationTime;
        this.solvingTime = solvingTime;
        this.cplexLog = "";
    }

    public Solution(List<Route> routes, double lowerBound,
            double gap, String status, double creationTime, double solvingTime, String cplexLog) {
        this.routes = routes;
        this.lowerBound = lowerBound;
        this.gap = gap;
        this.status = status;
        this.creationTime = creationTime;
        this.solvingTime = solvingTime;
        this.cplexLog = cplexLog;
    }

    public Solution(Solution solution) {
        this.routes = new ArrayList<>();
        for (Route route : solution.routes) {
            this.routes.add(new Route(route));
        }
        this.lowerBound = solution.lowerBound;
        this.gap = solution.gap;
        this.status = solution.status;
        this.creationTime = solution.creationTime;
        this.solvingTime = solution.solvingTime;
        this.cplexLog = solution.cplexLog;
    }

    public double getTotalCost() {
        return routes.stream().mapToDouble(route -> route.totalCost).sum();
    }

    public List<Link> getAllLinks() {
        return routes.stream()
                .flatMap(route -> route.links.stream())
                .collect(Collectors.toList());
    }

    public void exportSolution(String fileDirectory, double bestSolutionTotalCost, double processTime, int cliqueSize) {
        final int divide = Data.instanceName.contains("CON") || Data.instanceName.contains("SCA") ? 10000 : 1;
        final String FfileDirectory = fileDirectory;
        final double FbestSolutionTotalCost = bestSolutionTotalCost / divide;
        final double totalCost = getTotalCost() / divide;
        final double lowerBound = this.lowerBound / divide;
        final double gap = this.gap;
        final String status = this.status;
        final double creationTime = this.creationTime;
        final double solvingTime = this.solvingTime;
        final double FprocessTime = processTime;
        final int FcliqueSize = cliqueSize;

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> {
                try (PrintStream printer = new PrintStream(FfileDirectory)) {
                    printer.printf("%-15s%15.2f\n", "Best cost:", FbestSolutionTotalCost);
                    printer.printf("%-15s%15.2f\n", "Total cost:", totalCost);
                    printer.printf("%-15s%15.2f\n", "Lower bound:", lowerBound);
                    printer.printf("%-15s%15.4f\n", "Gap:", gap);
                    printer.printf("%-15s%15s\n", "Status:", status);
                    printer.printf("%-15s%15.2f\n", "Creation time:", creationTime);
                    printer.printf("%-15s%15.2f\n", "Solving time:", solvingTime);
                    printer.printf("%-15s%15.2f\n", "Process time:", FprocessTime);
                    printer.printf("%-15s%15d\n", "Clique size:", FcliqueSize);
                    printer.println("------------------------------");
                    int count = 0;
                    for (Route route : routes) {
                        printer.printf("%-15s", "Route " + (++count) + ": ");
                        printer.printf("%5d", 0);
                        for (int n = 0; n < route.nodes.size(); n++) {
                            Node node = route.nodes.get(n);
                            printer.printf("%5d", node.id());
                        }
                        printer.printf("%5d", 0);
                        printer.println();
                        printer.println("------------------------------");
                    }
                    printer.println();
                    printer.println("CPLEX LOG:");
                    printer.println(cplexLog);
                } catch (IOException e) {
                    Logger.getLogger(Solution.class.getName()).log(Level.SEVERE, "Exception during solution export", e);
                }
            });
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Solution other)) {
            return false;
        }

        if (this.routes.size() != other.routes.size()) {
            return false;
        }

        Set<Link> thisRouteLinks = new HashSet<>();
        Set<Link> otherRouteLinks = new HashSet<>();

        this.routes.forEach(route -> thisRouteLinks.addAll(new HashSet<>(route.links)));
        other.routes.forEach(route -> otherRouteLinks.addAll(new HashSet<>(route.links)));

        return thisRouteLinks.equals(otherRouteLinks);
    }

    @Override
    public int hashCode() {
        int result = 0;
        for (Route route : routes) {
            result += 31 * route.hashCode();
        }
        return result;
    }

}
