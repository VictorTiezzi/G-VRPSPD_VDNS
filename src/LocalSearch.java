import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import data.*;

public class LocalSearch {
    private final double COST_TOLERANCE = 0.001;
    private final Instance instance;
    // private Solution solution;

    public LocalSearch(Instance instance) {
        this.instance = instance;
    }

    public Solution run(Solution solutionIn) {
        double startTime = System.currentTimeMillis();
        Solution solution = solutionIn;

        List<Route> routes = new ArrayList<>();
        Double bestTotalCost = 0.0;
        for (Route route : solution.routes) {
            routes.add(new Route(route));
            bestTotalCost += route.totalCost;
        }

        boolean improved = true;
        while (improved) {
            improved = false;

            // Perform 2-opt optimization
            improved |= performTwoOpt(routes);

            // Perform crossover optimization
            improved |= performCrossover(routes);

            // Perform insertion optimization
            improved |= performInsertion(routes);

            // Perform interchange optimization
            improved |= performInterchange(routes);
            
        }

        return new Solution(routes, "LocalSearch", 0.0, (System.currentTimeMillis() - startTime) / 1000);
    }

    private boolean performTwoOpt(List<Route> routes) {
        boolean improved = false;
        for (int route_Index = 0; route_Index < routes.size(); route_Index++) {

            boolean routeImproved = true;
            while (routeImproved) {
                routeImproved = false;

                Route route = routes.get(route_Index);
                outer: for (int firstIndex = 0; firstIndex < route.nodes.size() - 1; firstIndex++) {
                    for (int lastIndex = firstIndex + 1; lastIndex < route.nodes.size(); lastIndex++) {

                        List<Node> candidateNodes = new ArrayList<>(route.nodes);
                        Collections.reverse(candidateNodes.subList(firstIndex, lastIndex + 1));

                        Route candidateRoute = new Route(candidateNodes, instance);
                        if (!candidateRoute.isFeasible)
                            continue;

                        if (candidateRoute.totalCost < route.totalCost - COST_TOLERANCE) {
                            routes.set(route_Index, candidateRoute);
                            improved = true;
                            routeImproved = true;
                            break outer;
                        }
                    }
                }

            }
        }
        return improved;
    }

    private boolean performInsertion(List<Route> routes) {
        boolean improved = false;
        double bestCost = routes.stream().mapToDouble(route -> route.totalCost).sum();

        List<Node> omega = new ArrayList<>(instance.clientNodes());
        Collections.shuffle(omega);

        while (!omega.isEmpty()) {
            Node nodeR = omega.removeFirst();

            List<List<Route>> results = new ArrayList<>();

            results.add(tryInsertionInSameRoute(nodeR, routes));
            results.add(tryInsertionInOtherRoutes(nodeR, routes));

            for (List<Route> resultRoute : results) {
                double resultTotalCost = resultRoute.stream().mapToDouble(route -> route.totalCost).sum();
                if (resultTotalCost < bestCost - COST_TOLERANCE) {
                    routes = resultRoute;
                    bestCost = resultTotalCost;
                }
            }
        }

        return improved;
    }

    private boolean performInterchange(List<Route> routes) {
        boolean improved = false;
        double bestCost = routes.stream().mapToDouble(route -> route.totalCost).sum();

        List<Node> omega = new ArrayList<>(instance.clientNodes());
        Collections.shuffle(omega);

        while (!omega.isEmpty()) {
            Node nodeR = omega.removeFirst();

            List<List<Route>> results = new ArrayList<>();

            results.add(tryInterchangeInSameRoute(nodeR, routes));
            results.add(tryInterchangeInOtherRoutes(nodeR, routes));

            for (List<Route> resultRoute : results) {
                double resultTotalCost = resultRoute.stream().mapToDouble(route -> route.totalCost).sum();
                if (resultTotalCost < bestCost - COST_TOLERANCE) {
                    routes = resultRoute;
                    bestCost = resultTotalCost;
                }
            }
        }

        return improved;
    }

    private boolean performCrossover(List<Route> routes) {
        boolean improved = false;

        boolean routeImproved = true;
        while (routeImproved) {
            routeImproved = false;

            outer: for (int route_IndexA = 0; route_IndexA < routes.size(); route_IndexA++) {
                Route routeA = routes.get(route_IndexA);

                for (int route_IndexB = route_IndexA + 1; route_IndexB < routes.size(); route_IndexB++) {
                    Route routeB = routes.get(route_IndexB);

                    double originalCost = routeA.totalCost + routeB.totalCost;
                    double bestDelta = 0;
                    Route bestRouteA = null, bestRouteB = null;

                    for (int cutA = 0; cutA <= routeA.nodes.size(); cutA++) {
                        List<Node> preCutA = new ArrayList<>(routeA.nodes.subList(0, cutA));
                        List<Node> posCutA = new ArrayList<>(routeA.nodes.subList(cutA, routeA.nodes.size()));

                        for (int cutB = 0; cutB <= routeB.nodes.size(); cutB++) {
                            List<Node> preCutB = new ArrayList<>(routeB.nodes.subList(0, cutB));
                            List<Node> posCutB = new ArrayList<>(routeB.nodes.subList(cutB, routeB.nodes.size()));

                            List<Node> newNodesA = new ArrayList<>();
                            newNodesA.addAll(preCutA);
                            newNodesA.addAll(posCutB);

                            Route candidateRouteA = new Route(newNodesA, instance);
                            if (!candidateRouteA.isFeasible)
                                continue;

                            List<Node> newNodesB = new ArrayList<>();
                            newNodesB.addAll(preCutB);
                            newNodesB.addAll(posCutA);

                            Route candidateRouteB = new Route(newNodesB, instance);
                            if (!candidateRouteB.isFeasible)
                                continue;

                            double newCost = candidateRouteA.totalCost + candidateRouteB.totalCost;
                            double delta = newCost - originalCost;

                            if (delta < bestDelta - COST_TOLERANCE) {
                                bestDelta = delta;
                                bestRouteA = candidateRouteA;
                                bestRouteB = candidateRouteB;

                            }
                        }
                    }

                    if (bestRouteA != null && bestRouteB != null) {
                        boolean routeAEmpty = bestRouteA.nodes.isEmpty();
                        boolean routeBEmpty = bestRouteB.nodes.isEmpty();
                        if (routeAEmpty || routeBEmpty) {
                            int emptyIndex = routeAEmpty ? route_IndexA : route_IndexB;
                            int nonEmptyIndex = routeAEmpty ? route_IndexB : route_IndexA;
                            Route nonEmptyRoute = routeAEmpty ? bestRouteB : bestRouteA;

                            routes.set(nonEmptyIndex, nonEmptyRoute);
                            routes.remove(emptyIndex);

                        } else {
                            routes.set(route_IndexA, bestRouteA);
                            routes.set(route_IndexB, bestRouteB);
                        }

                        improved = true;
                        routeImproved = true;
                        break outer;
                    }
                }
            }
        }
        return improved;
    }

    private boolean performCrossover(List<Route> routes, int zxc) {
        boolean improved = false;

        for (int route_IndexA = 0; route_IndexA < routes.size(); route_IndexA++) {
            for (int route_IndexB = route_IndexA + 1; route_IndexB < routes.size(); route_IndexB++) {
                Route routeA = routes.get(route_IndexA);
                Route routeB = routes.get(route_IndexB);
                double originalCost = routeA.totalCost + routeB.totalCost;
                double bestDelta = 0;
                Route bestRouteA = null, bestRouteB = null;

                for (int i = 0; i < routeA.nodes.size(); i++) {
                    for (int j = i + 2; j < routeA.nodes.size(); j++) {

                        for (int k = 0; k < routeB.nodes.size(); k++) {
                            for (int l = k + 2; l < routeB.nodes.size(); l++) {

                                List<Node> candidateNodeA1 = new ArrayList<>(routeA.nodes.subList(0, i));
                                List<Node> candidateNodeA2 = new ArrayList<>(routeA.nodes.subList(0, i));

                                List<Node> subPathB = new ArrayList<>(routeB.nodes.subList(k, l));

                                candidateNodeA1.addAll(subPathB);
                                candidateNodeA2.addAll(subPathB.reversed());

                                candidateNodeA1.addAll(routeA.nodes.subList(j, routeA.nodes.size()));
                                candidateNodeA2.addAll(routeA.nodes.subList(j, routeA.nodes.size()));

                                List<Node> candidateNodeB1 = new ArrayList<>(routeB.nodes.subList(0, k));
                                List<Node> candidateNodeB2 = new ArrayList<>(routeB.nodes.subList(0, k));

                                List<Node> subPathA = new ArrayList<>(routeA.nodes.subList(i, j));

                                candidateNodeB1.addAll(subPathA);
                                candidateNodeB2.addAll(subPathA.reversed());

                                candidateNodeB1.addAll(routeB.nodes.subList(l, routeB.nodes.size()));
                                candidateNodeB2.addAll(routeB.nodes.subList(l, routeB.nodes.size()));

                                List<Route> candidateRoutesA = new ArrayList<>();

                                Route candidateRouteA1 = new Route(candidateNodeA1, instance);
                                if (candidateRouteA1.isFeasible)
                                    candidateRoutesA.add(candidateRouteA1);

                                Route candidateRouteA2 = new Route(candidateNodeA2, instance);
                                if (candidateRouteA2.isFeasible)
                                    candidateRoutesA.add(candidateRouteA2);

                                List<Route> candidateRoutesB = new ArrayList<>();

                                Route candidateRouteB1 = new Route(candidateNodeB1, instance);
                                if (candidateRouteB1.isFeasible)
                                    candidateRoutesB.add(candidateRouteB1);

                                Route candidateRouteB2 = new Route(candidateNodeB2, instance);
                                if (candidateRouteB2.isFeasible)
                                    candidateRoutesB.add(candidateRouteB2);

                                for (Route candidateRouteA : candidateRoutesA) {
                                    for (Route candidateRouteB : candidateRoutesB) {
                                        double newCost = candidateRouteA.totalCost + candidateRouteB.totalCost;
                                        double delta = newCost - originalCost;

                                        if (delta < bestDelta - COST_TOLERANCE) {
                                            bestDelta = delta;
                                            bestRouteA = candidateRouteA;
                                            bestRouteB = candidateRouteB;
                                        }

                                    }
                                }

                            }
                        }
                    }
                }

                if (bestRouteA != null && bestRouteB != null) {
                    routes.set(route_IndexA, bestRouteA);
                    routes.set(route_IndexB, bestRouteB);
                    improved = true;
                }
            }
        }
        return improved;
    }

    private int[] findNodeAndRouteIndexContainingNode(Node nodeR, List<Route> routes) {
        for (int route_Index = 0; route_Index < routes.size(); route_Index++) {
            for (int node_Index = 0; node_Index < routes.get(route_Index).nodes.size(); node_Index++) {
                if (routes.get(route_Index).nodes.get(node_Index) == nodeR)
                    return new int[] { node_Index, route_Index };
            }
        }
        return new int[] { -1, -1 };
    }

    private List<Route> tryInsertionInSameRoute(Node nodeR, List<Route> routes) {
        boolean improved = false;

        List<Route> copyRoutes = new ArrayList<>();
        for (Route route : routes) {
            copyRoutes.add(new Route(route));
        }

        int[] temp = findNodeAndRouteIndexContainingNode(nodeR, copyRoutes);
        int nodeR_Index = temp[0];
        int routeR_Index = temp[1];

        Route routeR = copyRoutes.get(routeR_Index);

        List<Node> preCandidateNodesR = new ArrayList<>(routeR.nodes);
        preCandidateNodesR.remove(nodeR_Index);

        double bestCost = routeR.totalCost;
        Route bestCandidateRouteR = null;

        for (int positionK = 0; positionK < routeR.nodes.size(); positionK++) {
            if (positionK == nodeR_Index)
                continue;

            List<Node> candidateNodesR = new ArrayList<>(preCandidateNodesR);
            candidateNodesR.add(positionK, nodeR);

            Route candidateRouteR = new Route(candidateNodesR, instance);
            if (!candidateRouteR.isFeasible)
                continue;

            if (candidateRouteR.totalCost < bestCost - COST_TOLERANCE) {
                bestCost = candidateRouteR.totalCost;
                bestCandidateRouteR = candidateRouteR;
                improved = true;
            }
        }

        if (improved)
            copyRoutes.set(routeR_Index, bestCandidateRouteR);

        return copyRoutes;
    }

    private List<Route> tryInsertionInOtherRoutes(Node nodeR, List<Route> routes) {
        boolean improved = false;

        List<Route> copyRoutes = new ArrayList<>();
        for (Route route : routes) {
            copyRoutes.add(new Route(route));
        }

        int[] temp = findNodeAndRouteIndexContainingNode(nodeR, copyRoutes);
        int nodeR_Index = temp[0];
        int routeR_Index = temp[1];

        Route routeR = copyRoutes.get(routeR_Index);

        List<Node> candidateNodesR = new ArrayList<>(routeR.nodes);
        candidateNodesR.remove(nodeR_Index);

        Route candidateRouteR = new Route(candidateNodesR, instance);
        if (!candidateRouteR.isFeasible)
            return null;

        Route bestCandidateRouteR = candidateRouteR;
        Route bestCandidateRouteS = null;
        double bestImprovement = 0.0;
        int bestRouteS_Index = -1;

        for (int routeS_Index = 0; routeS_Index < copyRoutes.size(); routeS_Index++) {
            if (routeS_Index == routeR_Index)
                continue;

            Route routeS = copyRoutes.get(routeS_Index);

            double deliveryAux = routeS.deliveryCourse.getFirst() + nodeR.delivery();
            double pickupAux = routeS.pickupCourse.getLast() + nodeR.pickup();
            if (deliveryAux > instance.veichles().getLast().capacity()
                    || pickupAux > instance.veichles().getLast().capacity())
                continue;

            for (int k = 0; k <= routeS.nodes.size(); k++) {
                List<Node> candidateNodesS = new ArrayList<>(routeS.nodes);

                candidateNodesS.add(k, nodeR);

                Route candidateRouteS = new Route(candidateNodesS, instance);
                if (!candidateRouteS.isFeasible)
                    continue;

                double improvement = candidateRouteR.totalCost + candidateRouteS.totalCost
                        - routeR.totalCost - routeS.totalCost;
                if (improvement < bestImprovement - COST_TOLERANCE) {
                    bestImprovement = improvement;
                    bestCandidateRouteR = candidateRouteR;
                    bestCandidateRouteS = candidateRouteS;
                    bestRouteS_Index = routeS_Index;
                    improved = true;
                }
            }
        }

        if (candidateRouteR.nodes.size() > 1) {
            Route candidateRouteS = new Route(List.of(nodeR), instance);

            double improvement = candidateRouteR.totalCost + candidateRouteS.totalCost - routeR.totalCost;
            if (improvement < bestImprovement - COST_TOLERANCE) {
                bestCandidateRouteR = candidateRouteR;
                bestCandidateRouteS = candidateRouteS;
                bestRouteS_Index = copyRoutes.size();
                improved = true;
            }
        }

        if (improved) {
            if (bestRouteS_Index == copyRoutes.size())
                copyRoutes.add(bestCandidateRouteS);
            else
                copyRoutes.set(bestRouteS_Index, bestCandidateRouteS);

            if (candidateRouteR.nodes.isEmpty())
                copyRoutes.remove(routeR_Index);
            else
                copyRoutes.set(routeR_Index, bestCandidateRouteR);
        }

        return copyRoutes;
    }

    private List<Route> tryInterchangeInSameRoute(Node nodeR, List<Route> routes) {
        Boolean improved = false;

        List<Route> copyRoutes = new ArrayList<>();
        for (Route route : routes) {
            copyRoutes.add(new Route(route));
        }

        int[] temp = findNodeAndRouteIndexContainingNode(nodeR, copyRoutes);
        int nodeR_Index = temp[0];
        int routeR_Index = temp[1];

        Route routeR = copyRoutes.get(routeR_Index);

        double bestCost = routeR.totalCost;
        Route bestCandidateRouteR = null;

        for (int nodeK_Index = 0; nodeK_Index < routeR.nodes.size(); nodeK_Index++) {
            if (nodeK_Index == nodeR_Index)
                continue;

            List<Node> candidateNodesR = new ArrayList<>(routeR.nodes);
            Collections.swap(candidateNodesR, nodeR_Index, nodeK_Index);

            Route candidateRouteR = new Route(candidateNodesR, instance);
            if (!candidateRouteR.isFeasible)
                continue;

            if (candidateRouteR.totalCost < bestCost - COST_TOLERANCE) {
                bestCost = candidateRouteR.totalCost;
                bestCandidateRouteR = candidateRouteR;
                improved = true;
            }
        }

        if (improved)
            copyRoutes.set(routeR_Index, bestCandidateRouteR);

        return copyRoutes;
    }

    private List<Route> tryInterchangeInOtherRoutes(Node nodeR, List<Route> routes) {
        Boolean improved = false;

        List<Route> copyRoutes = new ArrayList<>();
        for (Route route : routes) {
            copyRoutes.add(new Route(route));
        }

        int[] temp = findNodeAndRouteIndexContainingNode(nodeR, copyRoutes);
        int nodeR_Index = temp[0];
        int routeR_Index = temp[1];

        Route routeR = copyRoutes.get(routeR_Index);

        double bestImprovement = 0.0;
        Route bestCandidateRouteR = null;
        Route bestCandidateRouteS = null;
        int bestRouteS_Index = -1;

        for (int routeS_Index = 0; routeS_Index < copyRoutes.size(); routeS_Index++) {
            if (routeS_Index == routeR_Index)
                continue;

            Route routeS = copyRoutes.get(routeS_Index);

            for (int nodeS_Index = 0; nodeS_Index < routeS.nodes.size(); nodeS_Index++) {
                Node nodeS = routeS.nodes.get(nodeS_Index);

                List<Node> candidateNodesR = new ArrayList<>(routeR.nodes);
                candidateNodesR.set(nodeR_Index, nodeS);

                List<Node> candidateNodesS = new ArrayList<>(routeS.nodes);
                candidateNodesS.set(nodeS_Index, nodeR);

                Route candidateRouteR = new Route(candidateNodesR, instance);
                if (!candidateRouteR.isFeasible)
                    continue;
                Route candidateRouteS = new Route(candidateNodesS, instance);
                if (!candidateRouteS.isFeasible)
                    continue;

                double improvement = candidateRouteR.totalCost + candidateRouteS.totalCost
                        - routeR.totalCost - routeS.totalCost;
                if (improvement < bestImprovement - COST_TOLERANCE) {
                    bestImprovement = improvement;
                    bestCandidateRouteR = candidateRouteR;
                    bestCandidateRouteS = candidateRouteS;
                    bestRouteS_Index = routeS_Index;
                    improved = true;
                }
            }
        }

        if (improved) {
            copyRoutes.set(routeR_Index, bestCandidateRouteR);
            copyRoutes.set(bestRouteS_Index, bestCandidateRouteS);
        }

        return copyRoutes;
    }
}