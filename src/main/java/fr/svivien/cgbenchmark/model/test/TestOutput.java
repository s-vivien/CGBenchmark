package fr.svivien.cgbenchmark.model.test;

import fr.svivien.cgbenchmark.Constants;
import fr.svivien.cgbenchmark.model.request.play.PlayResponse;

/**
 * Test result data
 */
public class TestOutput {

    private boolean error;
    private boolean win;
    private String resultString;

    private static final String outputFormat = "[ %8s ][ %2s ] %s";

    public TestOutput(TestInput test, PlayResponse response) {
        if (response == null) {
            this.error = true;
            this.resultString = String.format(outputFormat, "SEED " + test.getSeedNumber(), test.isReverse() ? "J2" : "J1", "ERROR");
        } else if (response.error != null) {
            this.error = true;
            this.resultString = String.format(outputFormat, "SEED " + test.getSeedNumber(), test.isReverse() ? "J2" : "J1", "ERROR " + response.error.message);
        } else {
            this.win = response.success.scores.get(test.isReverse() ? 1 : 0) > response.success.scores.get(test.isReverse() ? 0 : 1);
            this.resultString = String.format(outputFormat, "SEED " + test.getSeedNumber(), test.isReverse() ? "J2" : "J1", Constants.CG_HOST + "/replay/" + response.success.gameId) + " " + (this.win ? "WIN !" : "LOSE..");
        }
    }

    public boolean isError() {
        return error;
    }

    public boolean isWin() {
        return win;
    }

    public String getResultString() {
        return resultString;
    }
}
