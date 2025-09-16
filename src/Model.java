import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.MIPEmphasis;

import data.*;

public class Model {

    public IloCplex cplex;

    private double startTime;
    private double modelCreationTime;
    private double modelSolvingTime;

    public List<Solution> solutionsOut;

    private LinkManager linkManager;

    public static class TeeOutputStream extends OutputStream {
        private final OutputStream consoleStream;
        private final OutputStream variableStream;

        public TeeOutputStream(OutputStream variableStream) {
            this.consoleStream = System.out;
            this.variableStream = variableStream;
        }

        @Override
        public void write(int b) throws IOException {
            consoleStream.write(b);
            variableStream.write(b);
        }

        @Override
        public void flush() throws IOException {
            consoleStream.flush();
            variableStream.flush();
        }

        @Override
        public void close() throws IOException {
            variableStream.close();
        }
    }

    public Model(double timeLimit, Set<Link> links) {
        this.linkManager = new LinkManager(links);

        try {
            this.cplex = new IloCplex();

            cplex.setParam(IloCplex.Param.MIP.Display, 2);
            cplex.setParam(IloCplex.Param.TimeLimit, timeLimit);
            cplex.setParam(IloCplex.Param.Emphasis.MIP, MIPEmphasis.Heuristic);
        } catch (IloException e) {
            Logger.getLogger(Model.class.getName()).log(Level.SEVERE, "Error initializing CPLEX", e);
        }

        buildCplexModel();
    }

    public void finalizeModel() {
        cplex.end();
    }

    public void buildCplexModel() {
        startTime = System.currentTimeMillis();

        try {

            for (Link link : linkManager.getAll()) {
                link.pathCplexVar = cplex.boolVar("path" + "(" + link.origin.id() + "," + link.destiny.id() + ")");
                link.deliveryCplexVar = cplex.intVar(0, Data.capacity,
                        "delivery" + "(" + link.origin.id() + "," + link.destiny.id() + ")");
                link.pickupCplexVar = cplex.intVar(0, Data.capacity,
                        "pickup" + "(" + link.origin.id() + "," + link.destiny.id() + ")");

            }

            expression01();
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

        } catch (IloException e) {
            Logger.getLogger(Model.class.getName()).log(Level.SEVERE, "Error building CPLEX model", e);
        }

        modelCreationTime = (System.currentTimeMillis() - startTime) / 1000;
    }

    public void warmStart(List<Solution> solutionsIn) {
        try {
            mipStart(solutionsIn);
        } catch (IloException e) {
            e.printStackTrace();
        }

    }

    private void expression01() throws IloException {
        IloLinearNumExpr minExp = cplex.linearNumExpr();
        double factor = Data.UNIT_FUEL_COST * Data.FCR_WITHOUT_LOAD;
        double alphaFactor = Data.UNIT_FUEL_COST * Data.alfa;

        for (Link link : linkManager.getAll()) {
            double weightedDistance = link.distance;
            minExp.addTerm(factor * weightedDistance, link.pathCplexVar);
            minExp.addTerm(alphaFactor * weightedDistance, link.deliveryCplexVar);
            minExp.addTerm(alphaFactor * weightedDistance, link.pickupCplexVar);
        }

        cplex.addMinimize(minExp, "expression01");
    }

    private void expression02() throws IloException {
        for (int i = 1; i < Data.numberOfNodes; i++) {
            IloLinearIntExpr exp = cplex.linearIntExpr();
            for (Link link : linkManager.getAllIncomingLinks(i)) {
                exp.addTerm(1, link.pathCplexVar);
            }
            cplex.addEq(exp, 1, "expression02(" + i + ")");
        }
    }

    private void expression03() throws IloException {
        for (int i = 1; i < Data.numberOfNodes; i++) {
            IloLinearIntExpr exp = cplex.linearIntExpr();
            for (Link link : linkManager.getAllOutgoingLinks(i)) {
                exp.addTerm(1, link.pathCplexVar);
            }
            cplex.addEq(exp, 1, "expression03(" + i + ")");
        }
    }

    private void expression04() throws IloException {
        for (Link link : linkManager.getAll()) {
            IloLinearIntExpr expLeft = cplex.linearIntExpr();

            expLeft.addTerm(1, link.deliveryCplexVar);
            expLeft.addTerm(1, link.pickupCplexVar);

            Node nodeI = Data.allNodes.get(link.origin.id());
            Node nodeJ = Data.allNodes.get(link.destiny.id());
            int M4_ij = Data.capacity
                    - Math.max(0, Math.max(nodeI.delivery() - nodeI.pickup(), nodeJ.pickup() - nodeJ.delivery()));

            IloLinearIntExpr expRight = cplex.linearIntExpr();
            expRight.addTerm(M4_ij, link.pathCplexVar);

            cplex.addLe(expLeft, expRight, "expression04(" + link.origin.id() + "-" + link.destiny.id() + ")");
        }
    }

    private void expression05() throws IloException {
        IloLinearIntExpr exp = cplex.linearIntExpr();
        for (Link link : linkManager.getAllOutgoingLinks(0)) {
            exp.addTerm(1, link.deliveryCplexVar);
        }
        cplex.addEq(exp, Data.totalDelivery, "expression05");
    }

    private void expression06() throws IloException {
        IloLinearIntExpr exp = cplex.linearIntExpr();
        for (Link link : linkManager.getAllIncomingLinks(0)) {
            exp.addTerm(1, link.deliveryCplexVar);
        }
        cplex.addEq(exp, 0, "expression06");
    }

    private void expression07() throws IloException {
        IloLinearIntExpr exp = cplex.linearIntExpr();
        for (Link link : linkManager.getAllIncomingLinks(0)) {
            exp.addTerm(1, link.pickupCplexVar);
        }
        cplex.addEq(exp, Data.totalPickup, "expression07");
    }

    private void expression08() throws IloException {
        IloLinearIntExpr exp = cplex.linearIntExpr();
        for (Link link : linkManager.getAllOutgoingLinks(0)) {
            exp.addTerm(1, link.pickupCplexVar);
        }
        cplex.addEq(0, exp, "expression08");
    }

    private void expression09() throws IloException {
        for (int i = 1; i < Data.numberOfNodes; i++) {
            for (Link link : linkManager.getAllOutgoingLinks(i)) {
                IloLinearIntExpr exp = cplex.linearIntExpr();
                exp.addTerm(Data.capacity - link.origin.delivery(), link.pathCplexVar);
                cplex.addLe(link.deliveryCplexVar, exp,
                        "expression09(" + link.origin.id() + "-" + link.destiny.id() + ")");
            }
        }

    }

    private void expression10() throws IloException {
        for (int i = 0; i < Data.numberOfNodes; i++) {
            for (Link link : linkManager.getAllOutgoingLinks(i)) {
                if (link.destiny.id() == 0)
                    continue;
                IloLinearIntExpr exp = cplex.linearIntExpr();
                exp.addTerm(Data.capacity - link.destiny.pickup(), link.pathCplexVar);
                cplex.addLe(link.pickupCplexVar, exp,
                        "expression10(" + link.origin.id() + "-" + link.destiny.id() + ")");
            }
        }
    }

    private void expression11() throws IloException {
        for (int i = 0; i < Data.numberOfNodes; i++) {
            for (Link link : linkManager.getAllOutgoingLinks(i)) {
                if (link.destiny.id() == 0)
                    continue;
                IloLinearIntExpr exp = cplex.linearIntExpr();
                exp.addTerm(link.destiny.delivery(), link.pathCplexVar);
                cplex.addGe(link.deliveryCplexVar, exp,
                        "expression11(" + link.origin.id() + "-" + link.destiny.id() + ")");
            }
        }
    }

    private void expression12() throws IloException {
        for (int i = 1; i < Data.numberOfNodes; i++) {
            for (Link link : linkManager.getAllOutgoingLinks(i)) {
                IloLinearIntExpr exp = cplex.linearIntExpr();
                exp.addTerm(link.origin.pickup(), link.pathCplexVar);
                cplex.addGe(link.pickupCplexVar, exp,
                        "expression12(" + link.origin.id() + "-" + link.destiny.id() + ")");
            }
        }
    }

    private void expression13() throws IloException {
        for (int j = 1; j < Data.numberOfNodes; j++) {
            IloLinearIntExpr exp = cplex.linearIntExpr();
            for (Link linkIJ : linkManager.getAllIncomingLinks(j)) {
                exp.addTerm(-1, linkIJ.pickupCplexVar);
            }
            for (Link linkJI : linkManager.getAllOutgoingLinks(j)) {
                exp.addTerm(1, linkJI.pickupCplexVar);
            }
            cplex.addEq(exp, Data.allNodes.get(j).pickup(), "expression13(" + j + ")");
        }
    }

    private void expression14() throws IloException {
        for (int j = 1; j < Data.numberOfNodes; j++) {
            IloLinearIntExpr exp = cplex.linearIntExpr();
            for (Link linkIJ : linkManager.getAllIncomingLinks(j)) {
                exp.addTerm(1, linkIJ.deliveryCplexVar);
            }
            for (Link linkJI : linkManager.getAllOutgoingLinks(j)) {
                exp.addTerm(-1, linkJI.deliveryCplexVar);
            }
            cplex.addEq(exp, Data.allNodes.get(j).delivery(), "expression13(" + j + ")");
        }
    }

    private void mipStart(List<Solution> solutionsIn) throws IloException {
        List<Link> links = linkManager.getAll();
        int numLinks = links.size();

        IloIntVar[] allVars = new IloIntVar[numLinks * 3];
        Map<Link, Integer> linkIndexMap = new HashMap<>(numLinks);

        for (int index = 0; index < numLinks; index++) {
            Link link = links.get(index);
            allVars[index] = link.pathCplexVar;
            allVars[numLinks + index] = link.pickupCplexVar;
            allVars[numLinks * 2 + index] = link.deliveryCplexVar;
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

    public boolean solveModel() {

        try (TeeOutputStream teeOutputStream = new TeeOutputStream(new ByteArrayOutputStream())) {
            cplex.setOut(new PrintStream(teeOutputStream));
            startTime = System.currentTimeMillis();

            if (cplex.solve()) {
                modelSolvingTime = (System.currentTimeMillis() - startTime) / 1000;
                buildSolution(teeOutputStream.variableStream.toString());
                return true;
            }

        } catch (IloException | IOException e) {
            Logger.getLogger(Model.class.getName()).log(Level.SEVERE, "Exception during model solving", e);
        }
        modelSolvingTime = (System.currentTimeMillis() - startTime) / 1000;
        return false;
    }

    private void buildSolution(String log) throws IloException {
        solutionsOut = new ArrayList<>();

        for (int sol = 0; sol < cplex.getSolnPoolNsolns(); sol++) {
            List<Route> routes = new ArrayList<>();

            for (Link link0 : linkManager.getAllOutgoingLinks(0)) {
                if (cplex.getValue(link0.pathCplexVar, sol) > 0.9999) {
                    List<Node> nodes = new ArrayList<>();
                    int destination = link0.destiny.id();
                    while (destination != 0) {
                        for (Link link : linkManager.getAllOutgoingLinks(destination)) {
                            if (cplex.getValue(link.pathCplexVar, sol) > 0.9999) {
                                nodes.add(link.origin);
                                destination = link.destiny.id();
                                break;
                            }
                        }
                    }
                    routes.add(new Route(nodes));
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

    }

}
