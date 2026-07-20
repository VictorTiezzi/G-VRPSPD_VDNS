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

        Instance instance = new Instance("instance101", "HVRPSPD");

        new VariableDepthNeighborhoodSearch(instance, HVRPSPDModel.factory(), "HVRPSPD",
            solverStartTime,
            solverTimeLimit, subprobTimeLimit, lambda, 1);

    }

}
