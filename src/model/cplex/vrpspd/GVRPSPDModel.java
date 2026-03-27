package model.cplex.vrpspd;

import java.util.Set;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;

import data.*;
import model.ModelFactory;

public class GVRPSPDModel extends VRPSPDModel {
    private static final int UNIT_FUEL_COST = 1;
    private static final int FCR_WITHOUT_LOAD = 1;
    private static final int FCR_FULLY_LOADED = 2;
    private final double alfa;

    public static ModelFactory factory() {
        return new ModelFactory() {
            @Override
            public void setCostFunction(Instance instance) {
                instance.setCostFunction(route -> {
                    double alfa = (FCR_FULLY_LOADED - FCR_WITHOUT_LOAD) / route.veichle.capacity();
                    return route.links.stream().mapToDouble(l -> UNIT_FUEL_COST * l.distance() * (FCR_WITHOUT_LOAD
                            + alfa * (route.deliveryCourse.get(route.links.indexOf(l))
                                    + route.pickupCourse.get(route.links.indexOf(l)))))
                            .sum();
                });
            }

            @Override
            public model.cplex.CplexBaseModel create(Instance instance, Set<Link> links, double timeLimit)
                    throws IloException {
                return new GVRPSPDModel(instance, links, timeLimit);
            }
        };
    }

    public GVRPSPDModel(Instance instance, Set<Link> links, double timeLimit) throws IloException {
        super(instance, links, timeLimit);

        this.alfa = (FCR_FULLY_LOADED - FCR_WITHOUT_LOAD) / this.veichle.capacity();
    }

    @Override
    protected void buildObjective() throws IloException {
        IloLinearNumExpr minExp = cplex.linearNumExpr();

        double factor = UNIT_FUEL_COST * FCR_WITHOUT_LOAD;
        double alphaFactor = UNIT_FUEL_COST * alfa;

        for (Link link : linkManager.getAll()) {
            double weightedDistance = link.distance();
            minExp.addTerm(factor * weightedDistance, pathVars.get(link));
            minExp.addTerm(alphaFactor * weightedDistance, deliveryVars.get(link));
            minExp.addTerm(alphaFactor * weightedDistance, pickupVars.get(link));
        }

        cplex.addMinimize(minExp, "expression01");
    }

}
