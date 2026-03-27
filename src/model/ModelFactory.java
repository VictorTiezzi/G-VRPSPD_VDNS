package model;

import java.util.Set;

import data.Instance;
import data.Link;
import model.cplex.CplexBaseModel;
import ilog.concert.IloException;

public interface ModelFactory {

    void setCostFunction(Instance instance);

    CplexBaseModel create(Instance instance, Set<Link> links, double timeLimit) throws IloException;

}
