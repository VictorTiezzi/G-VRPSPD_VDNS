package model.cplex.vrpspd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;

import data.*;
import model.ModelFactory;

public class HVRPSPDModel extends VRPSPDBaseModel {

    public static ModelFactory factory() {
        return new ModelFactory() {
            @Override
            public void setCostFunction(Instance instance) {
                instance.setCostFunction(route -> route.veichle.fixedCost()
                        + route.links.stream().mapToDouble(l -> route.veichle.variableCost() * l.distance()).sum());
            }

            @Override
            public model.cplex.CplexBaseModel create(Instance instance, Set<Link> links, double timeLimit)
                    throws IloException {
                return new HVRPSPDModel(instance, links, timeLimit);
            }
        };
    }

    private int numberOfVeichles;
    private List<Veichle> veichles;

    private Map<Link, IloIntVar[]> pathVars = new HashMap<>();

    public HVRPSPDModel(Instance instance, Set<Link> links, double timeLimit) throws IloException {
        super(instance, links, timeLimit);

        this.numberOfVeichles = instance.numberOfVeichles();
        this.veichles = instance.veichles();
    }

    @Override
    protected void createVariables() throws IloException {
        for (Link link : linkManager.getAll()) {
            IloIntVar[] vars = new IloIntVar[numberOfVeichles];
            for (int t = 0; t < numberOfVeichles; t++)
                vars[t] = cplex.boolVar("path" + "(" + link.origin().id() + "," + link.destiny().id() + "," + t + ")");
            pathVars.put(link, vars);
            deliveryVars.put(link, cplex.numVar(0.0, veichles.getLast().capacity(),
                    "delivery" + "(" + link.origin().id() + "," + link.destiny().id() + ")"));
            pickupVars.put(link, cplex.numVar(0.0, veichles.getLast().capacity(),
                    "pickup" + "(" + link.origin().id() + "," + link.destiny().id() + ")"));
        }
    }

    @Override
    protected void buildObjective() throws IloException {
        IloLinearNumExpr minExp = cplex.linearNumExpr();
        for (Link link : linkManager.getAll()) {
            for (int t = 0; t < numberOfVeichles; t++) {
                if (link.origin() == depotNode) {
                    minExp.addTerm(veichles.get(t).fixedCost(), pathVars.get(link)[t]);
                }
                minExp.addTerm(veichles.get(t).variableCost() * link.distance(), pathVars.get(link)[t]);
            }
        }
        cplex.addMinimize(minExp, "expression01");
    }

    @Override
    protected void buildConstraints() throws IloException {
        expression02();
        expression03();
        expression04();
        expression05();
        expression06();
        expression07();
        expression08();
        expression09();
        expression10();
        expression11();
        expression12();
    }

    protected List<Solution> buildSolution(String log) throws IloException {
        List<Solution> solutionsOut = new ArrayList<>();

        for (int sol = 0; sol < cplex.getSolnPoolNsolns(); sol++) {
            List<Route> routes = new ArrayList<>();

            for (Link link0 : linkManager.getAllOutbound(depotNode.id())) {
                for (int i = 0; i < numberOfVeichles; i++) {
                    if (cplex.getValue(pathVars.get(link0)[i], sol) > 0.9999) {
                        List<Node> nodes = new ArrayList<>();
                        int destination = link0.destiny().id();
                        while (destination != 0) {
                            for (Link link : linkManager.getAllOutbound(destination)) {
                                if (cplex.getValue(pathVars.get(link)[i], sol) > 0.9999) {
                                    nodes.add(link.origin());
                                    destination = link.destiny().id();
                                    break;
                                }
                            }
                        }
                        routes.add(new Route(nodes, instance));
                    }
                }
            }

            solutionsOut.add(new Solution(
                    routes,
                    cplex.getBestObjValue(),
                    Math.abs(cplex.getObjValue() - cplex.getBestObjValue())
                            / ((1E-10) + Math.abs(cplex.getObjValue())),
                    cplex.getStatus().toString(),
                    modelCreationTime,
                    modelSolvingTime,
                    log));
        }

        return solutionsOut;
    }

    public void warmStart(List<Solution> solutionsIn) {
        try {
            mipStart(solutionsIn);
        } catch (IloException e) {
            e.printStackTrace();
        }
    }

    public void warmStart(Solution solutionIn) {
        try {
            mipStart(new ArrayList<>(Arrays.asList(solutionIn)));
        } catch (IloException e) {
            e.printStackTrace();
        }
    }

    private void expression02() throws IloException {
        for (Node node : clientNodes) {
            IloLinearNumExpr exp = cplex.linearNumExpr();
            for (Link link : linkManager.getAllInbound(node.id())) {
                for (int t = 0; t < numberOfVeichles; t++) {
                    exp.addTerm(1, pathVars.get(link)[t]);
                }
            }
            cplex.addEq(exp, 1, "expression02(" + node.id() + ")");
        }
    }

    private void expression03() throws IloException {
        for (int i = 1; i < numberOfNodes; i++) {
            for (int t = 0; t < numberOfVeichles; t++) {
                IloLinearNumExpr exp = cplex.linearNumExpr();
                for (Link link : linkManager.getAllInbound(i))
                    exp.addTerm(1, pathVars.get(link)[t]);

                for (Link link : linkManager.getAllOutbound(i))
                    exp.addTerm(-1, pathVars.get(link)[t]);

                cplex.addEq(exp, 0, "expression03(" + i + "," + t + ")");
            }
        }

        for (Node node : clientNodes) {
            for (int t = 0; t < numberOfVeichles; t++) {
                IloLinearNumExpr exp = cplex.linearNumExpr();
                for (Link link : linkManager.getAllInbound(node.id())) {
                    exp.addTerm(1, pathVars.get(link)[t]);
                }
                for (Link link : linkManager.getAllOutbound(node.id())) {
                    exp.addTerm(-1, pathVars.get(link)[t]);
                }
                cplex.addEq(exp, 0, "expression03(" + node.id() + "," + t + ")");
            }
        }
    }

    private void expression04() throws IloException {
        for (Node node : clientNodes) {
            IloLinearNumExpr exp = cplex.linearNumExpr();
            for (Link link : linkManager.getAllInbound(node.id())) {
                exp.addTerm(1, deliveryVars.get(link));
            }
            for (Link link : linkManager.getAllOutbound(node.id())) {
                exp.addTerm(-1, deliveryVars.get(link));
            }
            cplex.addEq(exp, node.delivery(), "expression04(" + node.id() + ")");
        }
    }

    private void expression05() throws IloException {
        for (Node node : clientNodes) {
            IloLinearNumExpr exp = cplex.linearNumExpr();
            for (Link link : linkManager.getAllOutbound(node.id())) {
                exp.addTerm(1, pickupVars.get(link));
            }
            for (Link link : linkManager.getAllInbound(node.id())) {
                exp.addTerm(-1, pickupVars.get(link));
            }

            cplex.addEq(exp, node.pickup(), "expression05(" + node.id() + ")");
        }
    }

    private void expression06() throws IloException {
        for (Link link : linkManager.getAll()) {
            IloLinearNumExpr exp = cplex.linearNumExpr();

            exp.addTerm(1, deliveryVars.get(link));
            exp.addTerm(1, pickupVars.get(link));
            for (int t = 0; t < numberOfVeichles; t++) {
                exp.addTerm(
                        -(veichles.get(t).capacity()
                                - Math.max(0,
                                        Math.max(link.origin().delivery() - link.origin().pickup(),
                                                link.destiny().pickup() - link.destiny().delivery()))),
                        pathVars.get(link)[t]);
            }
            cplex.addLe(exp, 0, "expression06(" + link.origin().id() + "," + link.destiny().id() + ")");
        }
    }

    private void expression07() throws IloException {
        for (Link link : linkManager.getAll()) {
            IloLinearNumExpr exp = cplex.linearNumExpr();
            exp.addTerm(1, deliveryVars.get(link));
            for (int t = 0; t < numberOfVeichles; t++) {
                exp.addTerm(-link.destiny().delivery(), pathVars.get(link)[t]);
            }
            cplex.addGe(exp, 0, "expression07(" + link.origin().id() + "," + link.destiny().id() + ")");
        }
    }

    private void expression08() throws IloException {
        for (Link link : linkManager.getAll()) {
            IloLinearNumExpr exp = cplex.linearNumExpr();
            exp.addTerm(1, pickupVars.get(link));
            for (int t = 0; t < numberOfVeichles; t++) {
                exp.addTerm(-link.origin().pickup(), pathVars.get(link)[t]);
            }
            cplex.addGe(exp, 0, "expression08(" + link.origin().id() + "," + link.destiny().id() + ")");
        }
    }

    private void expression09() throws IloException {
        for (Link link : linkManager.getAll()) {
            IloLinearNumExpr exp = cplex.linearNumExpr();
            exp.addTerm(1, deliveryVars.get(link));
            for (int t = 0; t < numberOfVeichles; t++) {
                exp.addTerm(-(veichles.get(t).capacity() - link.origin().delivery()), pathVars.get(link)[t]);
            }
            cplex.addLe(exp, 0, "expression09(" + link.origin().id() + "," + link.destiny().id() + ")");
        }
    }

    private void expression10() throws IloException {
        for (Link link : linkManager.getAll()) {
            IloLinearNumExpr exp = cplex.linearNumExpr();
            exp.addTerm(1, pickupVars.get(link));
            for (int t = 0; t < numberOfVeichles; t++) {
                exp.addTerm(-(veichles.get(t).capacity() - link.destiny().pickup()), pathVars.get(link)[t]);
            }
            cplex.addLe(exp, 0, "expression10(" + link.origin().id() + "," + link.destiny().id() + ")");
        }
    }

    private void expression11() throws IloException {
        for (Node node : clientNodes) {
            IloLinearNumExpr exp = cplex.linearNumExpr();
            for (Link link : linkManager.getAllInbound(node.id())) {
                exp.addTerm(1, deliveryVars.get(link));
            }
            cplex.addGe(exp, node.delivery(), "expression11(" + node.id() + ")");
        }
    }

    private void expression12() throws IloException {
        for (Node node : clientNodes) {
            IloLinearNumExpr exp = cplex.linearNumExpr();
            for (Link link : linkManager.getAllOutbound(node.id())) {
                exp.addTerm(1, pickupVars.get(link));
            }
            cplex.addGe(exp, node.pickup(), "expression12(" + node.id() + ")");
        }
    }

    private void mipStart(List<Solution> solutionsIn) throws IloException {
        List<Link> links = linkManager.getAll();
        int numLinks = links.size();

        IloNumVar[] allPathCplexVars = new IloNumVar[numLinks * numberOfVeichles];
        IloNumVar[] allDeliveryCplexVars = new IloNumVar[numLinks];
        IloNumVar[] allPickupCplexVars = new IloNumVar[numLinks];

        Map<Link, Integer> linkIndexMap = new HashMap<>(numLinks);

        for (int index = 0; index < numLinks; index++) {
            Link link = links.get(index);
            for (int t = 0; t < numberOfVeichles; t++) {
                allPathCplexVars[numLinks * t + index] = pathVars.get(link)[t];
            }
            allDeliveryCplexVars[index] = deliveryVars.get(link);
            allPickupCplexVars[index] = pickupVars.get(link);
            linkIndexMap.put(link, index);
        }

        int solNumber = 1;
        for (Solution solution : solutionsIn) {
            double[] allPathCplexValues = new double[numLinks * numberOfVeichles];
            double[] allDeliveryCplexValues = new double[numLinks];
            double[] allPickupCplexValues = new double[numLinks];

            for (Route route : solution.routes) {
                for (int i = 0; i < route.links.size(); i++) {
                    int index = linkIndexMap.get(route.links.get(i));
                    allPathCplexValues[numLinks * (route.veichle.id() - 1) + index] = 1;
                    allDeliveryCplexValues[index] = route.deliveryCourse.get(i);
                    allPickupCplexValues[index] = route.pickupCourse.get(i);
                }
            }

            IloNumVar[] allVars = new IloNumVar[(numLinks * numberOfVeichles) + (numLinks * 2)];
            System.arraycopy(allPathCplexVars, 0, allVars, 0, allPathCplexVars.length);
            System.arraycopy(allDeliveryCplexVars, 0, allVars, allPathCplexVars.length, allDeliveryCplexVars.length);
            System.arraycopy(allPickupCplexVars, 0, allVars, allPathCplexVars.length + allDeliveryCplexVars.length,
                    allPickupCplexVars.length);

            double[] allValues = new double[(numLinks * numberOfVeichles) + (numLinks * 2)];
            System.arraycopy(allPathCplexValues, 0, allValues, 0, allPathCplexValues.length);
            System.arraycopy(allDeliveryCplexValues, 0, allValues, allPathCplexValues.length,
                    allDeliveryCplexValues.length);
            System.arraycopy(allPickupCplexValues, 0, allValues,
                    allPathCplexValues.length + allDeliveryCplexValues.length, allPickupCplexValues.length);

            cplex.addMIPStart(allVars, allValues, "MIPStart_" + solNumber);
            solNumber++;
        }
    }

}
