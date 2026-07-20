import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import data.Instance;
import model.ModelFactory;
import model.cplex.vrpspd.*;

class Main {
    public static void main(String[] args) {
        new Solver();
    }
}

public class Solver {

    Solver() {
        String solverStartTime = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").format(LocalDateTime.now());

        String[] filenames = {
                "SCA8-2", "SCA8-7"
        };

        String[] instanceSets = { "AVRPSPD", "GVRPSPD", "HVRPSPD" };
        ModelFactory[] models = { AVRPSPDModel.factory(), GVRPSPDModel.factory(), HVRPSPDModel.factory() };

        int subprobTimeLimit = 10;
        int lambda = 10;

        int numberOfExecutions = 10;
        int solverTimeLimit = 1800;

        for (String filename : filenames) {
            Instance instance = new Instance(filename, "AVRPSPD");

            for (int exec = 1; exec <= numberOfExecutions; exec++) {
                new VariableDepthNeighborhoodSearch(instance, AVRPSPDModel.factory(), "AVRPSPD",
                        solverStartTime,
                        solverTimeLimit, subprobTimeLimit, lambda, exec);
            }

        }

    }

}
