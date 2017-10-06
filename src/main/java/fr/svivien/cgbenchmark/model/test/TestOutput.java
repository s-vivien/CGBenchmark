package fr.svivien.cgbenchmark.model.test;

import fr.svivien.cgbenchmark.Constants;
import fr.svivien.cgbenchmark.model.request.play.PlayResponse;

/**
 * Test result data
 */
public class TestOutput {

    private boolean crash;
    private boolean error;
    private boolean win;
    private String resultString;

    private static final String outputFormat = "[ %8s ][ %2s ] %s";

    public TestOutput(TestInput test, PlayResponse response) {

        // Checks if your AI crashed or not ..
        if (response != null && response.success != null) {
            int myAgentIndex = test.isReverse() ? 1 : 0;
            for (PlayResponse.Frame frame : response.success.frames) {
                if (frame.agentId == myAgentIndex && frame.gameInformation.contains(Constants.TIMEOUT_INFORMATION_PART)) {
                    this.crash = true;
                    break;
                }
            }
        }

        String resultMessage;
        if (response == null || response.error != null) {
            this.error = true;
            resultMessage = "ERROR" + (response == null ? "" : (" " + response.error.message));
        } else {
            this.win = response.success.scores.get(test.isReverse() ? 1 : 0) > response.success.scores.get(test.isReverse() ? 0 : 1);
            resultMessage = Constants.CG_HOST + "/replay/" + response.success.gameId + " " + (this.win ? "WIN !" : "LOSE..");
        }

        this.resultString = String.format(outputFormat, "SEED " + test.getSeedNumber(), test.isReverse() ? "J2" : "J1", resultMessage + (crash ? " (CRASH)" : ""));
    }

    public boolean isError() {
        return error;
    }

    public boolean isWin() {
        return win;
    }

    public boolean isCrash() {
        return crash;
    }

    public String getResultString() {
        return resultString;
    }
}
