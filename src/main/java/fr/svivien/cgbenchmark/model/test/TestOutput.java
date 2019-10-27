package fr.svivien.cgbenchmark.model.test;

import fr.svivien.cgbenchmark.model.config.EnemyConfiguration;
import fr.svivien.cgbenchmark.model.request.play.PlayResponse;
import fr.svivien.cgbenchmark.utils.Constants;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test result data
 */
@Data
public class TestOutput {

    private boolean crash;
    private boolean error;
    private String resultString;
    private Map<Integer, Integer> rankPerAgentId = new HashMap<>();

    private static final String outputFormat = "[ %10s ][ %8s ] %s%s";

    public TestOutput(TestInput test, String consumerName, PlayResponse response) {
        // Checks if your AI crashed or not ..
        if (response != null && response.success != null) {
            for (PlayResponse.Frame frame : response.success.frames) {
                if (frame.gameInformation.toLowerCase().contains(Constants.TIMEOUT_INFORMATION_PART) || (frame.summary != null && (frame.summary.toLowerCase().contains(Constants.TIMEOUT_INFORMATION_PART)))) {
                    this.crash = true;
                    break;
                }
            }
            if (response.success.tooltips != null) {
                for (String tooltip : response.success.tooltips) {
                    if (tooltip.toLowerCase().contains(Constants.TIMEOUT_INFORMATION_PART)) {
                        this.crash = true;
                        break;
                    }
                }
            }
        }

        String resultMessage;
        if (response == null || response.error != null) {
            this.error = true;
            resultMessage = "ERROR" + (response == null ? "" : (" " + response.error.message));
        } else {
            Map<Integer, String> nickPerAgentId = new HashMap<>();
            for (EnemyConfiguration ec : test.getPlayers()) {
                nickPerAgentId.put(ec.getAgentId(), ec.getName());
            }

            List<Integer> scores = response.success.scores;

            Map<Integer, Integer> scorePerAgentId = new HashMap<>();
            List<Integer> orderedAgentIds = new ArrayList<>();
            for (int i = 0; i < test.getPlayers().size(); i++) {
                scorePerAgentId.put(test.getPlayers().get(i).getAgentId(), scores.get(i));
                orderedAgentIds.add(test.getPlayers().get(i).getAgentId());
            }
            orderedAgentIds.sort((a, b) -> {
                int diff = scorePerAgentId.get(b) - scorePerAgentId.get(a);
                if (diff != 0) {
                    return diff;
                } else {
                    return a - b;
                }
            });

            resultMessage = Constants.CG_HOST + "/replay/" + response.success.gameId + " ";
            int rank = 1;
            resultMessage += rank + ":" + nickPerAgentId.get(orderedAgentIds.get(0));
            rankPerAgentId.put(orderedAgentIds.get(0), rank);
            for (int i = 1; i < test.getPlayers().size(); i++) {
                if (scorePerAgentId.get(orderedAgentIds.get(i)) < scorePerAgentId.get(orderedAgentIds.get(i - 1))) {
                    rank++;
                }
                resultMessage += " " + rank + ":" + nickPerAgentId.get(orderedAgentIds.get(i));
                rankPerAgentId.put(orderedAgentIds.get(i), rank);
            }
        }

        this.resultString = String.format(outputFormat, consumerName, "SEED " + test.getSeedNumber(), resultMessage, (crash ? " (CRASH)" : ""));
    }
}
