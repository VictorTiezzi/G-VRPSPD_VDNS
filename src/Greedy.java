import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import data.Instance;
import data.Node;
import data.Route;
import data.Solution;

public class Greedy {
    private final double NEAREST_NEIGHBOR_PROBABILITY = 0.99;
    private final Instance instance;
    private final LocalSearch localSearch;
    private final Random random = new Random();

    public Greedy(Instance instance, LocalSearch localSearch) {
        this.instance = instance;
        this.localSearch = localSearch;
    }

    public Solution run() {
        double startTime = System.currentTimeMillis();

        List<Route> routes = new ArrayList<>();
        List<Node> freeNodes = new ArrayList<>(instance.clientNodes());

        while (!freeNodes.isEmpty()) {
            Route route = createRoute(freeNodes);
            routes.add(route);
        }

        double creationTime = (System.currentTimeMillis() - startTime) / 1000.0;

        Solution solution = localSearch.run(new Solution(routes, "Greedy", creationTime, 0.0));

        solution.status = "Greedy";
        solution.creationTime = creationTime;

        return solution;
    }

    private Route createRoute(List<Node> freeNodes) {
        List<Node> feasibleNodes = new ArrayList<>(freeNodes);

        double totalRouteDelivery = 0.0;
        double vehicleCapacity = instance.veichles().getLast().capacity();

        List<Node> trialRoute = new ArrayList<>();
        Node currentEndNode = instance.depotNode();

        while (!feasibleNodes.isEmpty()) {
            Node trialNode = null;

            if (currentEndNode != instance.depotNode() && random.nextDouble() < NEAREST_NEIGHBOR_PROBABILITY) {
                double minDistance = Double.MAX_VALUE;

                for (Node candidate : feasibleNodes) {
                    double distance = instance.linkManager()
                            .get(currentEndNode.id(), candidate.id())
                            .distance();

                    if (distance < minDistance) {
                        minDistance = distance;
                        trialNode = candidate;
                    }
                }
            } else {
                trialNode = feasibleNodes.get(random.nextInt(feasibleNodes.size()));
            }

            if (isFeasibleRoute(trialRoute, trialNode, totalRouteDelivery + trialNode.delivery(),
                    vehicleCapacity)) {
                trialRoute.add(trialNode);
                currentEndNode = trialNode;
                totalRouteDelivery += trialNode.delivery();
            }
            feasibleNodes.remove(trialNode);
        }

        freeNodes.removeAll(trialRoute);
        return new Route(trialRoute, instance);
    }

    private Boolean isFeasibleRoute(List<Node> trialRoute, Node trialNode, double loadTrial, double capacity) {
        if (loadTrial > capacity)
            return false;

        for (Node next : trialRoute) {
            loadTrial += next.pickup() - next.delivery();
            if (loadTrial > capacity)
                return false;
        }

        return loadTrial + trialNode.pickup() - trialNode.delivery() <= capacity;
    }

}
