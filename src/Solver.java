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
                "CMT1X", "CMT1Y"
        };

        String[] instanceSets = { "AVRPSPD", "GVRPSPD", "HVRPSPD" };
        ModelFactory[] models = { AVRPSPDModel.factory(), GVRPSPDModel.factory(), HVRPSPDModel.factory() };

        int subprobTimeLimit = 10;
        int lambda = 10;

        int numberOfExecutions = 10;
        int solverTimeLimit = 1800;

        for (String filename : filenames) {
            int exec = 2;
            if (filename.equals("CMT1X"))
                exec = 10;
            for (; exec <= numberOfExecutions; exec++) {

                Instance instance = new Instance(filename, "AVRPSPD");

                new VariableDepthNeighborhoodSearch(instance, AVRPSPDModel.factory(), "AVRPSPD",
                        solverStartTime,
                        solverTimeLimit, subprobTimeLimit, lambda, exec);

            }

        }

    }

}
