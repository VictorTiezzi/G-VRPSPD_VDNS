package data;

import java.util.ArrayList;
import java.util.List;

public class Route {

    public Veichle veichle = null;
    public List<Node> nodes = new ArrayList<>();
    public List<Link> links = new ArrayList<>();
    public List<Double> deliveryCourse = new ArrayList<>();
    public List<Double> pickupCourse = new ArrayList<>();

    public boolean isFeasible = false;
    public double totalCost = Double.MAX_VALUE;

    public Route(List<Node> nodes, Instance instance) {
        this.nodes.addAll(nodes);

        if (nodes.isEmpty()) {
            isFeasible = true;
            totalCost = 0.0;
            return;
        }

        links.add(instance.linkManager().get(instance.depotNode().id(), nodes.getFirst().id()));
        deliveryCourse.add(nodes.stream().mapToDouble(Node::delivery).sum());
        pickupCourse.addFirst(0.0);

        if (deliveryCourse.getLast() > instance.veichles().getLast().capacity())
            return;

        double biggestLoad = deliveryCourse.getLast();

        for (int i = 0; i < nodes.size() - 1; i++) {
            links.add(instance.linkManager().get(nodes.get(i).id(), nodes.get(i + 1).id()));
            deliveryCourse.add(deliveryCourse.getLast() - links.getLast().origin().delivery());
            pickupCourse.add(pickupCourse.getLast() + links.getLast().origin().pickup());
            if (deliveryCourse.getLast() + pickupCourse.getLast() > instance.veichles().getLast().capacity())
                return;
            if (deliveryCourse.getLast() + pickupCourse.getLast() > biggestLoad)
                biggestLoad = deliveryCourse.getLast() + pickupCourse.getLast();
        }

        links.add(instance.linkManager().get(nodes.getLast().id(), instance.depotNode().id()));
        deliveryCourse.add(deliveryCourse.getLast() - links.getLast().origin().delivery());
        pickupCourse.add(pickupCourse.getLast() + links.getLast().origin().pickup());
        if (pickupCourse.getLast() > instance.veichles().getLast().capacity())
            return;
        if (pickupCourse.getLast() > biggestLoad)
            biggestLoad = pickupCourse.getLast();

        for (Veichle veichle : instance.veichles()) {
            if (veichle.capacity() >= biggestLoad) {
                this.veichle = veichle;
                break;
            }
        }

        this.totalCost = instance.costFunction().apply(this);
        this.isFeasible = true;
    }

    public Route(Route route) {
        this.veichle = route.veichle;
        this.nodes = new ArrayList<>(route.nodes);
        this.links.addAll(route.links);
        this.deliveryCourse.addAll(route.deliveryCourse);
        this.pickupCourse.addAll(route.pickupCourse);

        this.isFeasible = route.isFeasible;
        this.totalCost = route.totalCost;
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
