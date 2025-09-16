import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import data.Data;
import data.Node;
import data.Route;
import data.Solution;

public class Greedy {
    private static final double NEAREST_NEIGHBOR_PROBABILITY = 0.99;
    private static final Random random = new Random();

    public Solution run() {
        double startTime = System.currentTimeMillis();

        List<Route> routes = new ArrayList<>();
        List<Node> freeNodes = new ArrayList<>(Data.clientNodes);

        Collections.shuffle(freeNodes);

        while (!freeNodes.isEmpty()) {
            Route route = createRoute(freeNodes, NEAREST_NEIGHBOR_PROBABILITY);
            routes.add(route);
        }

        double creationTime = (System.currentTimeMillis() - startTime) / 1000.0;

        Solution solution = new LocalSearch().run(new Solution(routes, "Greedy", creationTime, 0.0));

        solution.status = "Greedy";
        solution.creationTime = creationTime;

        return solution;
    }

    private Route createRoute(List<Node> freeNodes, double nearestNeighborProbability) {
        List<Node> feasibleNodes = new ArrayList<>(freeNodes);
        double totalDelivery = 0;

        List<Node> nodes = new ArrayList<>();
        Node end = Data.depot;

        while (!feasibleNodes.isEmpty()) {
            Node trialNode = null;

            if (end != Data.depot && random.nextDouble() < nearestNeighborProbability) {
                final Node currentEnd = end;
                trialNode = feasibleNodes.stream()
                        .min(Comparator.comparing(n -> Data.linkManager.get(currentEnd.id(), n.id()).distance))
                        .orElse(null);
            } else {
                trialNode = feasibleNodes.get(random.nextInt(feasibleNodes.size()));
            }

            if (trialNode != null) {
                if (isFeasibleRoute(trialNode, nodes, totalDelivery + trialNode.delivery(), Data.capacity)) {
                    nodes.add(trialNode);
                    end = trialNode;
                    totalDelivery += trialNode.delivery();
                }
                feasibleNodes.remove(trialNode);
            }
        }
        freeNodes.removeAll(nodes);
        return new Route(nodes);
    }

    private Boolean isFeasibleRoute(Node trialNode, List<Node> nodes, double loadTrial, double capacity) {

        if (loadTrial <= capacity) {
            for (Node next : nodes) {
                loadTrial += next.pickup() - next.delivery();
                if (loadTrial > capacity) {
                    return false;
                }
            }
            return loadTrial + trialNode.pickup() - trialNode.delivery() < capacity;
        }
        return false;
    }

}
