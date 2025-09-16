package data;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Data {
    public static String instanceName;
    public static int numberOfNodes;
    public static int capacity;
    public static Node depot;
    public static List<Node> clientNodes;
    public static List<Node> allNodes;
    public static int totalDelivery;
    public static int totalPickup;
    public static LinkManager linkManager;
    public static double alfa;

    public static final int UNIT_FUEL_COST = 1;
    public static final int FCR_WITHOUT_LOAD = 1;
    public static final int FCR_FULLY_LOADED = 2;

    public Data(String filename) {

        String filePath = "";
        if (filename.contains("CMT")) {
            filePath = "./instances/SALHI/" + filename + ".vrpspd";
        } else if (filename.contains("CON") || filename.contains("SCA")) {
            filePath = "./instances/DETHLOFF/" + filename + ".vrpspd";
        }

        String instanceNameAux = "";
        int numberOfNodesAux = 0;
        int capacityAux = 0;
        double[][] coordinates = null;
        double[][] distanceMatrix = null;
        List<Node> nodesAux = new LinkedList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean readingCoordinates = false;
            boolean readingDistances = false;
            boolean readingPickupDelivery = false;

            int distanceRow = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                if (line.startsWith("NAME")) {
                    instanceNameAux = line.split(":")[1].trim();
                } else if (line.startsWith("DIMENSION")) {
                    numberOfNodesAux = Integer.parseInt(line.split(":")[1].trim());
                } else if (line.startsWith("CAPACITY")) {
                    capacityAux = Integer.parseInt(line.split(":")[1].trim());
                } else if (line.startsWith("NODE_COORD_SECTION")) {
                    coordinates = new double[numberOfNodesAux][2];
                    readingCoordinates = true;
                } else if (line.startsWith("EDGE_WEIGHT_SECTION")) {
                    distanceMatrix = new double[numberOfNodesAux][numberOfNodesAux];
                    readingDistances = true;
                    distanceRow = 0;
                } else if (line.startsWith("PICKUP_AND_DELIVERY_SECTION")) {
                    readingDistances = false;
                    readingCoordinates = false;
                    readingPickupDelivery = true;
                } else if (line.startsWith("DEPOT_SECTION")) {
                    break;
                } else if (readingDistances) {
                    String[] distances = line.split(" ");
                    for (int distanceCol = 0; distanceCol < distances.length; distanceCol++) {
                        distanceMatrix[distanceRow][distanceCol] = Double.parseDouble(distances[distanceCol]);
                    }
                    distanceRow++;
                } else if (readingCoordinates) {
                    String[] parts = line.split(" ");
                    int id = Integer.parseInt(parts[0]) - 1;
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);

                    coordinates[id][0] = x;
                    coordinates[id][1] = y;
                } else if (readingPickupDelivery) {
                    String[] parts = line.split(" ");
                    int id = Integer.parseInt(parts[0]) - 1;
                    int pickup = Integer.parseInt(parts[5]);
                    int delivery = Integer.parseInt(parts[6]);
                    nodesAux.add(new Node(id, pickup, delivery));
                }
            }
        } catch (FileNotFoundException e) {
            Logger.getLogger(Data.class.getName()).log(Level.SEVERE, "File not found: " + filePath, e);
        } catch (IOException e) {
            Logger.getLogger(Data.class.getName()).log(Level.SEVERE, "Error reading file: " + filePath, e);
        }

        Data.instanceName = instanceNameAux;
        Data.numberOfNodes = numberOfNodesAux;
        Data.capacity = capacityAux;
        Data.depot = nodesAux.getFirst();
        Data.clientNodes = new ArrayList<>(nodesAux.subList(1, nodesAux.size()));
        Data.allNodes = new ArrayList<>(nodesAux);
        Data.totalDelivery = Data.clientNodes.stream().mapToInt(Node::delivery).sum();
        Data.totalPickup = Data.clientNodes.stream().mapToInt(Node::pickup).sum();
        Data.alfa = (double) (FCR_FULLY_LOADED - FCR_WITHOUT_LOAD) / capacity;

        if (distanceMatrix == null) {
            distanceMatrix = new double[numberOfNodes][numberOfNodes];
            for (int i = 0; i < numberOfNodes; i++) {
                for (int j = i + 1; j < numberOfNodes; j++) {
                    if (i != j) {
                        assert coordinates != null;
                        double distance = calculateEuclideanDistance(
                                coordinates[i][0], coordinates[i][1],
                                coordinates[j][0], coordinates[j][1]);
                        distanceMatrix[i][j] = distance;
                        distanceMatrix[j][i] = distance;
                    }
                }
            }
        }

        LinkManager linkManagerAux = new LinkManager();

        for (int i = 0; i < nodesAux.size(); i++) {
            for (int j = i + 1; j < nodesAux.size(); j++) {
                linkManagerAux.set(new Link(nodesAux.get(i), nodesAux.get(j), distanceMatrix[i][j]));
                linkManagerAux.set(new Link(nodesAux.get(j), nodesAux.get(i), distanceMatrix[j][i]));
            }
        }

        Data.linkManager = linkManagerAux;

        Route.setAllStatic();
    }

    private double calculateEuclideanDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

}
