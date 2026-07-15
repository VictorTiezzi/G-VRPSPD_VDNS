package data;

import java.util.ArrayList;
import java.util.List;

public class Route {

    public Vehicle vehicle = null;
    public List<Node> nodes = new ArrayList<>();
    public List<Link> links = new ArrayList<>();
    public List<Double> pickupCourse = new ArrayList<>();
    public List<Double> deliveryCourse = new ArrayList<>();
    
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

        pickupCourse.addFirst(0.0);

        deliveryCourse.add(nodes.stream().mapToDouble(Node::delivery).sum());
        if (deliveryCourse.getLast() > instance.vehicles().getLast().capacity())
            return;

        double biggestLoad = deliveryCourse.getLast();

        for (int i = 0; i < nodes.size() - 1; i++) {
            links.add(instance.linkManager().get(nodes.get(i).id(), nodes.get(i + 1).id()));
            pickupCourse.add(pickupCourse.getLast() + links.getLast().origin().pickup());
            deliveryCourse.add(deliveryCourse.getLast() - links.getLast().origin().delivery());
            
            if (pickupCourse.getLast() + deliveryCourse.getLast() > instance.vehicles().getLast().capacity())
                return;

            if (pickupCourse.getLast() + deliveryCourse.getLast() > biggestLoad)
                biggestLoad = pickupCourse.getLast() + deliveryCourse.getLast();
        }

        links.add(instance.linkManager().get(nodes.getLast().id(), instance.depotNode().id()));
        pickupCourse.add(pickupCourse.getLast() + links.getLast().origin().pickup());
        deliveryCourse.add(deliveryCourse.getLast() - links.getLast().origin().delivery());
    
        if (pickupCourse.getLast() + deliveryCourse.getLast() > instance.vehicles().getLast().capacity())
            return;

        if (pickupCourse.getLast() + deliveryCourse.getLast() > biggestLoad)
            biggestLoad = pickupCourse.getLast() + deliveryCourse.getLast();

        for (Vehicle vehicle : instance.vehicles()) {
            if (biggestLoad <= vehicle.capacity()) {
                this.vehicle = vehicle;
                break;
            }
        }

        this.totalCost = instance.costFunction().apply(this);
        this.isFeasible = true;
    }

    public Route(Route route) {
        this.vehicle = route.vehicle;
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
