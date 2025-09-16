import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import data.Data;

class Main {
    public static void main(String[] args) {
        new Solver();
    }
}

public class Solver {

    Solver() {
        String solverStartTime = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").format(LocalDateTime.now());
        String[] filenames = { "CMT5X" };

        int subprobTimeLimit = 30;
        int numberOfExecutions = 10;
        int solverTimeLimit = 3600;

        for (String filename : filenames) {
            new Data(filename);

            for (int exec = 1; exec <= numberOfExecutions; exec++) {
                new VariableDepthNeighborhoodSearch(filename, solverStartTime, solverTimeLimit, subprobTimeLimit, exec);
            }

        }

    }

}
