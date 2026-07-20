package data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Instance {
    private final String instanceName;
    private final String instanceSet;

    // Nodes
    private final int numberOfNodes;
    private final List<Node> allNodes;
    private final Node depotNode;
    private final List<Node> clientNodes;

    // Vehicle
    private final int numberOfVehicles;
    private final List<Vehicle> vehicles;

    private final double totalPickup;
    private final double totalDelivery;

    // Links - "Distance Matrix"
    private final LinkManager linkManager;

    private Function<Route, Double> costFunction;

    public Instance(String instanceName, String instanceSet) {

        String instanceFilePath = "";

        switch (instanceSet) {
            case "GVRPSPD" -> instanceFilePath = "./instances/GVRPSPD/" + instanceName + ".vrpspd";
            case "AVRPSPD" -> instanceFilePath = "./instances/AVRPSPD/" + instanceName + ".vrpspd";
            case "HVRPSPD" -> instanceFilePath = "./instances/HVRPSPD/" + instanceName + ".dat";

            case "TRAINGVRPSPD" -> instanceFilePath = "./instances/TRAIN/GVRPSPD/" + instanceName + ".vrpspd";
            case "TRAINAVRPSPD" -> instanceFilePath = "./instances/TRAIN/AVRPSPD/" + instanceName + ".vrpspd";
            case "TRAINHVRPSPD" -> instanceFilePath = "./instances/TRAIN/HVRPSPD/" + instanceName + ".dat";
        }

        // Initialize final fields
        int numberOfNodes = 0;
        List<Node> allNodes = null;
        int numberOfVehicles = 0;
        List<Vehicle> vehicles = null;
        double totalPickup = 0;
        double totalDelivery = 0;
        LinkManager linkManager = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(instanceFilePath))) {

            if (instanceSet.equals("HVRPSPD") || instanceSet.equals("TRAINHVRPSPD")) {

                String line;
                // Read number of vehicles
                line = reader.readLine();
                numberOfVehicles = Integer.parseInt(line.trim());
                vehicles = new ArrayList<>();
                for (int i = 0; i < numberOfVehicles; i++) {
                    line = reader.readLine();
                    String[] parts = line.trim().split("\\s+");
                    int id = Integer.parseInt(parts[0]);
                    double capacity = Double.parseDouble(parts[1]);
                    double variableCost = Double.parseDouble(parts[2]);
                    double fixedCost = Double.parseDouble(parts[3]);
                    vehicles.add(new Vehicle(id, capacity, variableCost, fixedCost));
                }
                // Read number of nodes
                line = reader.readLine();
                numberOfNodes = Integer.parseInt(line.trim());
                allNodes = new ArrayList<>();
                totalPickup = 0;
                totalDelivery = 0;
                List<Node> nodeList = new ArrayList<>();
                for (int i = 0; i < numberOfNodes; i++) {
                    line = reader.readLine();
                    String[] parts = line.trim().split("\\s+");
                    int id = Integer.parseInt(parts[0]);
                    double pickup = Double.parseDouble(parts[2]);
                    double delivery = Double.parseDouble(parts[1]);
                    double x = Double.parseDouble(parts[3]);
                    double y = Double.parseDouble(parts[4]);
                    Node node = new Node(id, pickup, delivery, x, y);
                    allNodes.add(node);
                    nodeList.add(node);
                    totalPickup += pickup;
                    totalDelivery += delivery;
                }
                // Create LinkManager
                linkManager = new LinkManager(numberOfNodes);
                for (int i = 0; i < numberOfNodes; i++) {
                    for (int j = 0; j < numberOfNodes; j++) {
                        if (i != j) {
                            Node n1 = nodeList.get(i);
                            Node n2 = nodeList.get(j);
                            double dist = calculateEuclideanDistance(n1, n2);
                            Link link = new Link(n1, n2, dist);
                            linkManager.set(link);
                        }
                    }
                }

            } else {

                String line;
                line = reader.readLine();
                line = reader.readLine();
                String type = line.trim().split("\\s+")[2];
                line = reader.readLine();
                numberOfNodes = Integer.parseInt(line.split(":")[1].trim());
                line = reader.readLine();
                numberOfVehicles = Integer.parseInt(line.split(":")[1].trim());
                line = reader.readLine();
                double capacity = Double.parseDouble(line.split(":")[1].trim());
                vehicles = new ArrayList<>();
                vehicles.add(new Vehicle(1, capacity, 1.0, 1.0));
                double[][] distanceMatrix = new double[numberOfNodes][numberOfNodes];

                if (type.equals("MVRPB")) {
                    line = reader.readLine();
                    line = reader.readLine();

                    int[][] nodeCord = new int[numberOfNodes][2];
                    for (int i = 0; i < numberOfNodes; i++) {
                        line = reader.readLine();
                        String[] cords = line.trim().split("\\s+");
                        nodeCord[i][0] = Integer.parseInt(cords[1]);
                        nodeCord[i][1] = Integer.parseInt(cords[2]);
                    }

                    for (int i = 0; i < numberOfNodes; i++) {
                        for (int j = i + 1; j < numberOfNodes; j++) {
                            Double distance = calculateEuclideanDistance(nodeCord[i][0], nodeCord[i][1], nodeCord[j][0], nodeCord[j][1]);
                            distanceMatrix[i][j] = distance;
                            distanceMatrix[j][i] = distance;
                        }
                    }

                } else {
                    line = reader.readLine();
                    line = reader.readLine();
                    line = reader.readLine();

                    for (int i = 0; i < numberOfNodes; i++) {
                        line = reader.readLine();
                        String[] distances = line.trim().split("\\s+");
                        for (int j = 0; j < numberOfNodes; j++) {
                            distanceMatrix[i][j] = Double.parseDouble(distances[j]);
                        }
                    }
                }

                line = reader.readLine();

                allNodes = new ArrayList<>();
                totalPickup = 0;
                totalDelivery = 0;

                for (int i = 0; i < numberOfNodes; i++) {
                    line = reader.readLine();
                    line = line.trim();
                    String[] parts = line.split("\\s+");

                    double pickup = Double.parseDouble(parts[5]);
                    double delivery = Double.parseDouble(parts[6]);

                    Node node = new Node(i, pickup, delivery, 0.0, 0.0);
                    allNodes.add(node);
                    totalPickup += pickup;
                    totalDelivery += delivery;
                }

                linkManager = new LinkManager(numberOfNodes);
                for (int i = 0; i < numberOfNodes; i++) {
                    for (int j = 0; j < numberOfNodes; j++) {
                        if (i != j) {
                            Node n1 = allNodes.get(i);
                            Node n2 = allNodes.get(j);
                            double dist = distanceMatrix[i][j];
                            Link link = new Link(n1, n2, dist);
                            linkManager.set(link);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.instanceName = instanceName;
        this.instanceSet = instanceSet;
        this.numberOfNodes = numberOfNodes;
        this.allNodes = allNodes;
        this.depotNode = allNodes.getFirst();
        this.clientNodes = new ArrayList<>(allNodes.subList(1, allNodes.size()));
        this.numberOfVehicles = numberOfVehicles;
        this.vehicles = vehicles;
        this.totalPickup = totalPickup;
        this.totalDelivery = totalDelivery;
        this.linkManager = linkManager;
    }

    private double calculateEuclideanDistance(Node n1, Node n2) {
        return Math.sqrt(Math.pow(n1.x() - n2.x(), 2) + Math.pow(n1.y() - n2.y(), 2));
    }

    private double calculateEuclideanDistance(int n1x, int n1y, int n2x, int n2y) {
        return Math.sqrt(Math.pow(n1x - n2x, 2) + Math.pow(n1y - n2y, 2));
    }

    public String instanceName() {
        return instanceName;
    }

    public String instanceSet() {
        return instanceSet;
    }

    public int numberOfNodes() {
        return numberOfNodes;
    }

    public List<Node> allNodes() {
        return allNodes;
    }

    public Node depotNode() {
        return depotNode;
    }

    public List<Node> clientNodes() {
        return clientNodes;
    }

    public int numberOfVehicles() {
        return numberOfVehicles;
    }

    public List<Vehicle> vehicles() {
        return vehicles;
    }

    public double totalPickup() {
        return totalPickup;
    }

    public double totalDelivery() {
        return totalDelivery;
    }

    public LinkManager linkManager() {
        return linkManager;
    }

    public Function<Route, Double> costFunction() {
        return costFunction;
    }

    public void setCostFunction(Function<Route, Double> costFunction) {
        this.costFunction = costFunction;
    }

}
