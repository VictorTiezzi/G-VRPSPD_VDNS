package model.cplex.vrpspd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import data.*;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import model.ModelFactory;

public class VRPSPDModel extends VRPSPDBaseModel {

    public static ModelFactory factory() {
        return new ModelFactory() {
            @Override
            public void setCostFunction(Instance instance) {
                instance.setCostFunction(route -> route.links.stream().mapToDouble(l -> l.distance()).sum());
            }

            @Override
            public model.cplex.CplexBaseModel create(Instance instance, Set<Link> links, double timeLimit)
                    throws IloException {
                return new VRPSPDModel(instance, links, timeLimit);
            }
        };
    }
    // graph G = (N, A)
    // nodes N = {0, 1, ..., n}
    // 1 depot node {0}
    // client grup N_c = N \ {0}
    // for each client i, there is a pickup demand P_i and a delivery demand D_i
    // 1 vehicle type V with capacity Q

    protected Veichle veichle;

    protected Map<Link, IloIntVar> pathVars = new HashMap<>();

    public VRPSPDModel(Instance instance, Set<Link> links, double timeLimit) throws IloException {
        super(instance, links, timeLimit);

        this.veichle = instance.veichles().getFirst();
    }

    @Override
    protected void createVariables() throws IloException {
        for (Link link : linkManager.getAll()) {
            pathVars.put(link, cplex.boolVar(
                    "path" + "(" + link.origin().id() + "," + link.destiny().id() + ")"));
            deliveryVars.put(link, cplex.numVar(0.0, veichle.capacity(),
                    "delivery" + "(" + link.origin().id() + "," + link.destiny().id() + ")"));
            pickupVars.put(link, cplex.numVar(0.0, veichle.capacity(),
                    "pickup" + "(" + link.origin().id() + "," + link.destiny().id() + ")"));
        }
    }

    @Override
    protected void buildObjective() throws IloException {
        IloLinearNumExpr minExp = cplex.linearNumExpr();

        for (Link link : linkManager.getAll())
            minExp.addTerm(link.distance(), pathVars.get(link));

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
        expression13();
        expression14();
    }

    protected List<Solution> buildSolution(String log) throws IloException {
        List<Solution> solutionsOut = new ArrayList<>();

        for (int sol = 0; sol < cplex.getSolnPoolNsolns(); sol++) {
            List<Route> routes = new ArrayList<>();

            for (Link link0 : linkManager.getAllOutbound(0)) {
                if (cplex.getValue(pathVars.get(link0), sol) > 0.9999) {
                    List<Node> nodes = new ArrayList<>();
                    int destination = link0.destiny().id();
                    while (destination != 0) {
                        for (Link link : linkManager.getAllOutbound(destination)) {
                            if (cplex.getValue(pathVars.get(link), sol) > 0.9999) {
                                nodes.add(link.origin());
                                destination = link.destiny().id();
                                break;
                            }
                        }
                    }
                    routes.add(new Route(nodes, instance));
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

    public void warmStart(List<Solution> solutionsIn) throws IloException {
        if (solutionsIn.isEmpty())
            return;

        List<Link> links = linkManager.getAll();
        int numLinks = links.size();

        IloNumVar[] allVars = new IloNumVar[numLinks * 3];
        Map<Link, Integer> linkIndexMap = new HashMap<>(numLinks);

        for (int index = 0; index < numLinks; index++) {
            Link link = links.get(index);
            allVars[index] = pathVars.get(link);
            allVars[numLinks + index] = pickupVars.get(link);
            allVars[numLinks * 2 + index] = deliveryVars.get(link);
            linkIndexMap.put(link, index);
        }

        int solNumber = 1;
        for (Solution solution : solutionsIn) {
            double[] allValues = new double[numLinks * 3];

            for (Route route : solution.routes) {
                for (int i = 0; i < route.links.size(); i++) {
                    int index = linkIndexMap.get(route.links.get(i));
                    allValues[index] = 1;
                    allValues[numLinks + index] = route.pickupCourse.get(i);
                    allValues[numLinks * 2 + index] = route.deliveryCourse.get(i);
                }
            }

            cplex.addMIPStart(allVars, allValues, "MIPStart_" + solNumber);
            solNumber++;
        }
    }

    private void expression02() throws IloException {
        for (Node nodeI : clientNodes) {
            IloLinearIntExpr exp = cplex.linearIntExpr();
            for (Link link : linkManager.getAllInbound(nodeI.id())) {
                exp.addTerm(1, pathVars.get(link));
            }
            cplex.addEq(exp, 1, "expression02(" + nodeI.id() + ")");
        }
    }

    private void expression03() throws IloException {
        for (Node nodeI : clientNodes) {
            IloLinearIntExpr exp = cplex.linearIntExpr();
            for (Link link : linkManager.getAllOutbound(nodeI.id())) {
                exp.addTerm(1, pathVars.get(link));
            }
            cplex.addEq(exp, 1, "expression03(" + nodeI.id() + ")");
        }
    }

    private void expression04() throws IloException {
        for (Link link : linkManager.getAll()) {
            IloLinearNumExpr expLeft = cplex.linearNumExpr();

            expLeft.addTerm(1, deliveryVars.get(link));
            expLeft.addTerm(1, pickupVars.get(link));

            Node nodeI = link.origin();
            Node nodeJ = link.destiny();
            double M4_ij = veichle.capacity()
                    - Math.max(0, Math.max(nodeI.delivery() - nodeI.pickup(), nodeJ.pickup() - nodeJ.delivery()));

            IloLinearNumExpr expRight = cplex.linearNumExpr();
            expRight.addTerm(M4_ij, pathVars.get(link));

            cplex.addLe(expLeft, expRight, "expression04(" + nodeI.id() + "-" + nodeJ.id() + ")");
        }
    }

    private void expression05() throws IloException {
        IloLinearNumExpr exp = cplex.linearNumExpr();
        for (Link link : linkManager.getAllOutbound(depotNode.id())) {
            exp.addTerm(1, deliveryVars.get(link));
        }
        cplex.addEq(exp, totalDelivery, "expression05");
    }

    private void expression06() throws IloException {
        IloLinearNumExpr exp = cplex.linearNumExpr();
        for (Link link : linkManager.getAllInbound(depotNode.id())) {
            exp.addTerm(1, deliveryVars.get(link));
        }
        cplex.addEq(exp, 0, "expression06");
    }

    private void expression07() throws IloException {
        IloLinearNumExpr exp = cplex.linearNumExpr();
        for (Link link : linkManager.getAllInbound(depotNode.id())) {
            exp.addTerm(1, pickupVars.get(link));
        }
        cplex.addEq(exp, totalPickup, "expression07");
    }

    private void expression08() throws IloException {
        IloLinearNumExpr exp = cplex.linearNumExpr();
        for (Link link : linkManager.getAllOutbound(depotNode.id())) {
            exp.addTerm(1, pickupVars.get(link));
        }
        cplex.addEq(0, exp, "expression08");
    }

    private void expression09() throws IloException {
        for (Node nodeI : clientNodes) {
            for (Link link : linkManager.getAllOutbound(nodeI.id())) {
                IloLinearNumExpr exp = cplex.linearNumExpr();
                exp.addTerm(veichle.capacity() - link.origin().delivery(), pathVars.get(link));
                cplex.addLe(deliveryVars.get(link), exp,
                        "expression09(" + link.origin().id() + "-" + link.destiny().id() + ")");
            }
        }
    }

    private void expression10() throws IloException {
        for (Node nodeI : clientNodes) {
            for (Link link : linkManager.getAllOutbound(nodeI.id())) {
                if (link.destiny() == depotNode)
                    continue;
                IloLinearNumExpr exp = cplex.linearNumExpr();
                exp.addTerm(veichle.capacity() - link.destiny().pickup(), pathVars.get(link));
                cplex.addLe(pickupVars.get(link), exp,
                        "expression10(" + link.origin().id() + "-" + link.destiny().id() + ")");
            }
        }
    }

    private void expression11() throws IloException {
        for (Node nodeI : clientNodes) {
            for (Link link : linkManager.getAllOutbound(nodeI.id())) {
                if (link.destiny() == depotNode)
                    continue;
                IloLinearNumExpr exp = cplex.linearNumExpr();
                exp.addTerm(link.destiny().delivery(), pathVars.get(link));
                cplex.addGe(deliveryVars.get(link), exp,
                        "expression11(" + link.origin().id() + "-" + link.destiny().id() + ")");
            }
        }
    }

    private void expression12() throws IloException {
        for (Node nodeI : clientNodes) {
            for (Link link : linkManager.getAllOutbound(nodeI.id())) {
                IloLinearNumExpr exp = cplex.linearNumExpr();
                exp.addTerm(link.origin().pickup(), pathVars.get(link));
                cplex.addGe(pickupVars.get(link), exp,
                        "expression12(" + link.origin().id() + "-" + link.destiny().id() + ")");
            }
        }
    }

    private void expression13() throws IloException {
        for (Node nodeJ : clientNodes) {
            IloLinearNumExpr exp = cplex.linearNumExpr();
            for (Link linkIJ : linkManager.getAllInbound(nodeJ.id())) {
                exp.addTerm(-1, pickupVars.get(linkIJ));
            }
            for (Link linkJI : linkManager.getAllOutbound(nodeJ.id())) {
                exp.addTerm(1, pickupVars.get(linkJI));
            }
            cplex.addEq(exp, nodeJ.pickup(), "expression13(" + nodeJ.id() + ")");
        }
    }

    private void expression14() throws IloException {
        for (Node nodeJ : clientNodes) {
            IloLinearNumExpr exp = cplex.linearNumExpr();
            for (Link linkIJ : linkManager.getAllInbound(nodeJ.id())) {
                exp.addTerm(1, deliveryVars.get(linkIJ));
            }
            for (Link linkJI : linkManager.getAllOutbound(nodeJ.id())) {
                exp.addTerm(-1, deliveryVars.get(linkJI));
            }
            cplex.addEq(exp, nodeJ.delivery(), "expression14(" + nodeJ.id() + ")");
        }
    }

}
