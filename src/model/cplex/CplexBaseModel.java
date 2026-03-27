package model.cplex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import data.Solution;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.MIPEmphasis;

public abstract class CplexBaseModel implements AutoCloseable {
    protected IloCplex cplex;

    protected TeeOutputStream teeOutputStream;

    protected double startTime;
    protected double modelCreationTime;
    protected double modelSolvingTime;

    protected static class TeeOutputStream extends OutputStream {
        protected final OutputStream consoleStream;
        protected final OutputStream variableStream;

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
        public void write(byte[] b, int off, int len) throws IOException {
            consoleStream.write(b, off, len);
            variableStream.write(b, off, len);
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

    public CplexBaseModel(double timeLimit) throws IloException {
        this.cplex = new IloCplex();

        this.teeOutputStream = new TeeOutputStream(new ByteArrayOutputStream());
        this.cplex.setOut(new PrintStream(teeOutputStream));

        this.cplex.setParam(IloCplex.Param.MIP.Display, 2);
        this.cplex.setParam(IloCplex.Param.TimeLimit, timeLimit);
        this.cplex.setParam(IloCplex.Param.Emphasis.MIP, MIPEmphasis.Heuristic);
    }

    public List<Solution> solve() throws IloException {
        buildModel();

        startTime = System.currentTimeMillis();
        if (cplex.solve()) {
            modelSolvingTime = (System.currentTimeMillis() - startTime) / 1000;
            return buildSolution(teeOutputStream.variableStream.toString());
        }

        modelSolvingTime = (System.currentTimeMillis() - startTime) / 1000;
        return new ArrayList<>();
    }

    protected void buildModel() throws IloException {
        startTime = System.currentTimeMillis();
        createVariables();
        buildObjective();
        buildConstraints();
        modelCreationTime = (System.currentTimeMillis() - startTime) / 1000;
    }

    protected abstract void createVariables() throws IloException;

    protected abstract void buildObjective() throws IloException;

    protected abstract void buildConstraints() throws IloException;

    protected abstract List<Solution> buildSolution(String log) throws IloException;

    @Override
    public void close() throws Exception {
        cplex.close();
        teeOutputStream.close();
    }
}
