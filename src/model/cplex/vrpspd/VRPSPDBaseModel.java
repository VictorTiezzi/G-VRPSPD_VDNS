package model.cplex.vrpspd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import data.*;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import model.cplex.CplexBaseModel;

public abstract class VRPSPDBaseModel extends CplexBaseModel {

    protected LinkManager linkManager;

    protected Instance instance;

    protected int numberOfNodes;
    protected Node depotNode;
    protected List<Node> clientNodes;

    protected double totalDelivery;
    protected double totalPickup;

    protected Map<Link, IloNumVar> deliveryVars = new HashMap<>();
    protected Map<Link, IloNumVar> pickupVars = new HashMap<>();

    protected VRPSPDBaseModel(Instance instance, Set<Link> links, double timeLimit) throws IloException {
        super(timeLimit);

        this.instance = instance;

        this.numberOfNodes = instance.numberOfNodes();
        this.depotNode = instance.depotNode();
        this.clientNodes = instance.clientNodes();

        this.totalDelivery = instance.totalDelivery();
        this.totalPickup = instance.totalPickup();

        this.linkManager = new LinkManager(numberOfNodes, links);
    }

}
