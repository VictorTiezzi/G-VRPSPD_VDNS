package data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Route {
    private static LinkManager linkManager;
    private static Node depot;
    private static int capacity;
    private static double alfa;

    private static int UNIT_FUEL_COST;
    private static int FCR_WITHOUT_LOAD;

    public static void setAllStatic() {
        Route.linkManager = Data.linkManager;
        Route.depot = Data.depot;
        Route.capacity = Data.capacity;
        Route.UNIT_FUEL_COST = Data.UNIT_FUEL_COST;
        Route.FCR_WITHOUT_LOAD = Data.FCR_WITHOUT_LOAD;
        Route.alfa = Data.alfa;
    }

    public List<Node> nodes;
    public List<Link> links;
    public List<Integer> deliveryCourse;
    public List<Integer> pickupCourse;

    public boolean isFeasible;
    public double totalCost;

    public Route(List<Node> nodes) {
        this.nodes = nodes;

        links = new ArrayList<>(nodes.size());
        deliveryCourse = new ArrayList<>(nodes.size());
        pickupCourse = new ArrayList<>(nodes.size());

        isFeasible = buildAndCheckFeasiability();
    }

    public Route(Route route) {
        this.nodes = new ArrayList<>(route.nodes);

        links = new ArrayList<>(route.links);
        deliveryCourse = new ArrayList<>(route.deliveryCourse);
        pickupCourse = new ArrayList<>(route.pickupCourse);

        isFeasible = route.isFeasible;
        totalCost = route.totalCost;
    }

    private boolean buildAndCheckFeasiability() {
        if (nodes.isEmpty())
            return true;
        deliveryCourse.add(nodes.stream().mapToInt(Node::delivery).sum());
        if (deliveryCourse.getFirst() > capacity)
            return false;
        if (nodes.stream().mapToInt(Node::pickup).sum() > capacity)
            return false;
        pickupCourse.addFirst(0);

        links.add(linkManager.get(depot.id(), nodes.getFirst().id()));
        for (int i = 0; i < nodes.size() - 1; i++) {
            links.add(linkManager.get(nodes.get(i).id(), nodes.get(i + 1).id()));
        }
        links.add(linkManager.get(nodes.getLast().id(), depot.id()));

        totalCost += UNIT_FUEL_COST * links.getFirst().distance
                * (FCR_WITHOUT_LOAD + alfa * (deliveryCourse.getLast() + pickupCourse.getLast()));

        for (int i = 1; i < links.size(); i++) {
            deliveryCourse.add(deliveryCourse.getLast() - links.get(i).origin.delivery());
            pickupCourse.add(pickupCourse.getLast() + links.get(i).origin.pickup());
            if (deliveryCourse.getLast() + pickupCourse.getLast() > capacity)
                return false;
            totalCost += UNIT_FUEL_COST * links.get(i).distance
                    * (FCR_WITHOUT_LOAD + alfa * (deliveryCourse.getLast() + pickupCourse.getLast()));
        }

        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Route other)) {
            return false;
        }
        return Objects.equals(this.nodes, other.nodes);
    }

    @Override
    public int hashCode() {
        return (int) (Objects.hash(nodes) + totalCost);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("Route [nodes= 0, ");

        for (int index = 0; index < nodes.size(); index++) {
            result.append(nodes.get(index).id()).append(", ");
        }
        return result + "0 ]";
    }
}
