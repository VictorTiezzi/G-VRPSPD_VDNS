import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import data.Instance;
import model.cplex.vrpspd.*;

class Main {
    public static void main(String[] args) {
        new Solver();
    }
}

public class Solver {

    Solver() {
        String solverStartTime = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").format(LocalDateTime.now());
        String[] filenames = { "instance214" };

        int subprobTimeLimit = 30;
        int numberOfExecutions = 1;
        int solverTimeLimit = 300;

        for (String filename : filenames) {
            Instance instance = new Instance(filename, "AVCI");

            for (int exec = 1; exec <= numberOfExecutions; exec++) {
                new VariableDepthNeighborhoodSearch(instance, HVRPSPDModel.factory(), solverStartTime, solverTimeLimit,
                        subprobTimeLimit, exec);
            }

        }

    }

}
