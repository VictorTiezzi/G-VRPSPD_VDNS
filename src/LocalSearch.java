import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import data.*;

public class LocalSearch {
  private final double COST_TOLERANCE = 0.001;
  private Solution solution;

  public Solution run(Solution solutionIn) {
    double startTime = System.currentTimeMillis();
    solution = solutionIn;

    boolean improved = true;
    while (improved) {
      improved = false;

      // Perform 2-opt optimization
      improved |= performTwoOpt();

      // Perform crossover optimization
      improved |= performCrossover();

      // Perform insertion optimization
      improved |= performInsertion();

      // Perform interchange optimization
      improved |= performInterchange();

    }

    solution.solvingTime = (System.currentTimeMillis() - startTime) / 1000;
    solution.lowerBound = 0.0;
    solution.gap = 0.0;
    solution.status = "LocalSearch";
    solution.creationTime = 0.0;

    return solution;
  }

  private boolean performTwoOpt() {
    boolean improved = false;
    for (int route_Index = 0; route_Index < solution.routes.size(); route_Index++) {
      Route route = solution.routes.get(route_Index);

      for (int firstIndex = 0; firstIndex < route.nodes.size() - 1; firstIndex++) {
        for (int lastIndex = firstIndex + 1; lastIndex < route.nodes.size(); lastIndex++) {
          List<Node> candidateNodes = new ArrayList<>(route.nodes);
          Collections.reverse(candidateNodes.subList(firstIndex, lastIndex + 1));

          Route candidateRoute = new Route(candidateNodes);
          if (!candidateRoute.isFeasible)
            continue;

          if (candidateRoute.totalCost < route.totalCost - COST_TOLERANCE) {
            route = candidateRoute;
            solution.routes.set(route_Index, candidateRoute);
            lastIndex = firstIndex - 1 == 0 ? 1 : firstIndex - 1;
            firstIndex = 0;
            improved = true;
          }
        }
      }
    }
    return improved;
  }

  private boolean performInsertion() {
    boolean improved = false;

    List<Node> omega = new ArrayList<>(Data.clientNodes);
    Collections.shuffle(omega);

    while (!omega.isEmpty()) {
      Node nodeR = omega.removeFirst();

      improved |= tryInsertionInSameRoute(nodeR);
      improved |= tryInsertionInOtherRoutes(nodeR);
    }

    return improved;
  }

  private boolean performInterchange() {
    boolean improved = false;

    List<Node> omega = new ArrayList<>(Data.clientNodes);
    Collections.shuffle(omega);

    while (!omega.isEmpty()) {
      Node nodeR = omega.removeFirst();

      improved |= tryInterchangeInSameRoute(nodeR);
      improved |= tryInterchangeInOtherRoutes(nodeR);
    }

    return improved;
  }

  private boolean performCrossover() {
    boolean improved = false;
    List<Integer> omega = new ArrayList<>();
    for (int i = 0; i < solution.routes.size(); i++) {
      omega.add(i);
    }
    Collections.shuffle(omega);

    for (int indiceRouteA = 0; indiceRouteA < omega.size(); indiceRouteA++) {
      for (int indiceRouteB = indiceRouteA + 1; indiceRouteB < omega.size(); indiceRouteB++) {
        Route routeA = solution.routes.get(omega.get(indiceRouteA));
        Route routeB = solution.routes.get(omega.get(indiceRouteB));
        double originalCost = routeA.totalCost + routeB.totalCost;
        double bestDelta = 0;
        Route bestRouteA = null, bestRouteB = null;

        for (int cutA = 0; cutA <= routeA.nodes.size(); cutA++) {
          for (int cutB = 0; cutB <= routeB.nodes.size(); cutB++) {
            List<Node> newNodesA = new ArrayList<>(routeA.nodes.subList(0, cutA));
            newNodesA.addAll(routeB.nodes.subList(cutB, routeB.nodes.size()));
            Route candidateRouteA = new Route(newNodesA);
            if (!candidateRouteA.isFeasible)
              continue;

            List<Node> newNodesB = new ArrayList<>(routeB.nodes.subList(0, cutB));
            newNodesB.addAll(routeA.nodes.subList(cutA, routeA.nodes.size()));
            Route candidateRouteB = new Route(newNodesB);
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
            int emptyIndex = routeAEmpty ? indiceRouteA : indiceRouteB;
            int nonEmptyIndex = routeAEmpty ? indiceRouteB : indiceRouteA;
            Route nonEmptyRoute = routeAEmpty ? bestRouteB : bestRouteA;

            solution.routes.set(omega.get(nonEmptyIndex), nonEmptyRoute);
            solution.routes.remove((int) omega.get(emptyIndex));

            int removedIndice = omega.remove(emptyIndex);
            if (routeAEmpty)
              indiceRouteB--;
            omega.addFirst(omega.remove(routeAEmpty ? indiceRouteB : indiceRouteA));

            for (int i = 0; i < omega.size(); i++) {
              if (omega.get(i) > removedIndice) {
                omega.set(i, omega.get(i) - 1);
              }
            }
            indiceRouteA = 0;
            indiceRouteB = 0;
          } else {
            solution.routes.set(omega.get(indiceRouteA), bestRouteA);
            solution.routes.set(omega.get(indiceRouteB), bestRouteB);
            omega.addFirst(omega.remove(indiceRouteB));
            omega.addFirst(omega.remove(indiceRouteA + 1));
            indiceRouteA = 0;
            indiceRouteB = 1;
          }
          improved = true;
        }
      }
    }
    return improved;
  }

  private int findRouteIndexContainingNode(Node nodeR) {
    return solution.routes.stream()
        .filter(route -> route.nodes.contains(nodeR))
        .findFirst()
        .map(solution.routes::indexOf)
        .orElse(-1);
  }

  private Boolean tryInsertionInSameRoute(Node nodeR) {
    boolean improved = false;

    int routeR_Index = findRouteIndexContainingNode(nodeR);
    int nodeR_Index = solution.routes.get(routeR_Index).nodes.indexOf(nodeR);

    Route routeR = solution.routes.get(routeR_Index);

    List<Node> preCandidateNodesR = new ArrayList<>(routeR.nodes);
    preCandidateNodesR.remove(nodeR_Index);

    double bestCost = routeR.totalCost;
    Route bestCandidateRouteR = null;

    for (int positionK = 0; positionK < routeR.nodes.size(); positionK++) {
      if (positionK == nodeR_Index)
        continue;

      List<Node> candidateNodesR = new ArrayList<>(preCandidateNodesR);
      candidateNodesR.add(positionK, nodeR);

      Route candidateRouteR = new Route(candidateNodesR);
      if (!candidateRouteR.isFeasible)
        continue;

      if (candidateRouteR.totalCost < bestCost - COST_TOLERANCE) {
        bestCost = candidateRouteR.totalCost;
        bestCandidateRouteR = candidateRouteR;
        improved = true;
      }
    }

    if (improved)
      solution.routes.set(routeR_Index, bestCandidateRouteR);

    return improved;
  }

  private Boolean tryInsertionInOtherRoutes(Node nodeR) {
    boolean improved = false;

    int routeR_Index = findRouteIndexContainingNode(nodeR);
    int nodeR_Index = solution.routes.get(routeR_Index).nodes.indexOf(nodeR);

    Route routeR = solution.routes.get(routeR_Index);

    List<Node> candidateNodesR = new ArrayList<>(routeR.nodes);
    candidateNodesR.remove(nodeR_Index);

    Route candidateRouteR = new Route(candidateNodesR);
    if (!candidateRouteR.isFeasible)
      return null;

    Route bestCandidateRouteR = candidateRouteR;
    Route bestCandidateRouteS = null;
    double bestImprovement = 0.0;
    int bestRouteS_Index = -1;

    for (int routeS_Index = 0; routeS_Index < solution.routes.size(); routeS_Index++) {
      if (routeS_Index == routeR_Index)
        continue;

      Route routeS = solution.routes.get(routeS_Index);

      int deliveryAux = routeS.deliveryCourse.getFirst() + nodeR.delivery();
      int pickupAux = routeS.pickupCourse.getLast() + nodeR.pickup();
      if (deliveryAux > Data.capacity || pickupAux > Data.capacity)
        continue;

      for (int k = 0; k <= routeS.nodes.size(); k++) {
        List<Node> candidateNodesS = new ArrayList<>(routeS.nodes);

        candidateNodesS.add(k, nodeR);

        Route candidateRouteS = new Route(candidateNodesS);
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
      Route candidateRouteS = new Route(List.of(nodeR));

      double improvement = candidateRouteR.totalCost + candidateRouteS.totalCost - routeR.totalCost;
      if (improvement < bestImprovement - COST_TOLERANCE) {
        bestCandidateRouteR = candidateRouteR;
        bestCandidateRouteS = candidateRouteS;
        bestRouteS_Index = solution.routes.size();
        improved = true;
      }
    }

    if (improved) {
      if (bestRouteS_Index == solution.routes.size()) {
        solution.routes.add(bestCandidateRouteS);
      } else {
        solution.routes.set(bestRouteS_Index, bestCandidateRouteS);
      }
      if (candidateRouteR.nodes.isEmpty()) {
        solution.routes.remove(routeR_Index);
      } else {
        solution.routes.set(routeR_Index, bestCandidateRouteR);
      }
    }
    return improved;
  }

  private Boolean tryInterchangeInSameRoute(Node nodeR) {
    Boolean improved = false;

    int routeR_Index = findRouteIndexContainingNode(nodeR);
    int nodeR_Index = solution.routes.get(routeR_Index).nodes.indexOf(nodeR);

    Route routeR = solution.routes.get(routeR_Index);

    double bestCost = routeR.totalCost;
    Route bestCandidateRouteR = null;

    for (int nodeK_Index = 0; nodeK_Index < routeR.nodes.size(); nodeK_Index++) {
      if (nodeK_Index == nodeR_Index)
        continue;

      List<Node> candidateNodesR = new ArrayList<>(routeR.nodes);
      Collections.swap(candidateNodesR, nodeR_Index, nodeK_Index);

      Route candidateRouteR = new Route(candidateNodesR);
      if (!candidateRouteR.isFeasible)
        continue;

      if (candidateRouteR.totalCost < bestCost - COST_TOLERANCE) {
        bestCost = candidateRouteR.totalCost;
        bestCandidateRouteR = candidateRouteR;
        improved = true;
      }
    }

    if (improved)
      solution.routes.set(routeR_Index, bestCandidateRouteR);

    return improved;
  }

  private Boolean tryInterchangeInOtherRoutes(Node nodeR) {
    Boolean improved = false;

    int routeR_Index = findRouteIndexContainingNode(nodeR);
    int nodeR_Index = solution.routes.get(routeR_Index).nodes.indexOf(nodeR);

    Route routeR = solution.routes.get(routeR_Index);

    double bestImprovement = 0.0;
    Route bestCandidateRouteR = null;
    Route bestCandidateRouteS = null;
    int bestRouteS_Index = -1;

    for (int routeS_Index = 0; routeS_Index < solution.routes.size(); routeS_Index++) {
      if (routeS_Index == routeR_Index)
        continue;

      Route routeS = solution.routes.get(routeS_Index);

      for (int nodeS_Index = 0; nodeS_Index < routeS.nodes.size(); nodeS_Index++) {
        Node nodeS = routeS.nodes.get(nodeS_Index);

        List<Node> candidateNodesR = new ArrayList<>(routeR.nodes);
        candidateNodesR.set(nodeR_Index, nodeS);

        List<Node> candidateNodesS = new ArrayList<>(routeS.nodes);
        candidateNodesS.set(nodeS_Index, nodeR);

        Route candidateRouteR = new Route(candidateNodesR);
        if (!candidateRouteR.isFeasible)
          continue;
        Route candidateRouteS = new Route(candidateNodesS);
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
      solution.routes.set(routeR_Index, bestCandidateRouteR);
      solution.routes.set(bestRouteS_Index, bestCandidateRouteS);
    }

    return improved;
  }
}