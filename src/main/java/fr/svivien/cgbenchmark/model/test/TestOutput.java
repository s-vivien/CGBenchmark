package fr.svivien.cgbenchmark.model.test;

import com.google.gson.Gson;
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

    private static Gson gson = new Gson();
    private boolean error;
    private String resultString;
    private Map<Integer, AgentIdResult> resultPerAgentId = new HashMap<>();

    private static final String outputFormat = "[ %10s ][ %8s ] %s";

    private boolean containsTimeoutInfo(String msg, boolean checkForInvalidActions) {
        String lowerCaseMsg = msg.toLowerCase();
        return lowerCaseMsg.contains(Constants.TIMEOUT_INFORMATION_PART)
                || lowerCaseMsg.contains(Constants.TIMEOUT_BIS_INFORMATION_PART)
                || (checkForInvalidActions && lowerCaseMsg.contains(Constants.INVALID_INFORMATION_PART))
                || (checkForInvalidActions && lowerCaseMsg.contains(Constants.INVALID_BIS_INFORMATION_PART));
    }

    // Error constructor
    public TestOutput(TestInput test, String consumerName, String errorMessage) {
        this.error = true;
        this.resultString = String.format(outputFormat, consumerName, "SEED " + test.getSeedNumber(), "ERROR" + (errorMessage == null ? "" : (" " + errorMessage)));
    }

    // Success constructor
    public TestOutput(TestInput test, String consumerName, PlayResponse response) {
        String resultMessage;
        Map<Integer, String> nickPerAgentId = new HashMap<>();
        for (EnemyConfiguration ec : test.getPlayers()) {
            nickPerAgentId.put(ec.getAgentId(), ec.getName());
            resultPerAgentId.put(ec.getAgentId(), new AgentIdResult());
        }

        // Looks for crashes
        for (PlayResponse.Frame frame : response.frames) {
            if (containsTimeoutInfo(frame.gameInformation, false) || (frame.summary != null && containsTimeoutInfo(frame.summary, false))) {
                this.resultPerAgentId.get(test.getPlayers().get(frame.agentId).getAgentId()).setCrashed(true);
            }
        }
        if (response.tooltips != null) {
            for (String tooltip : response.tooltips) {
                if (containsTimeoutInfo(tooltip, true)) {
                    PlayResponse.Tooltip parsedTooltip = gson.fromJson(tooltip, PlayResponse.Tooltip.class);
                    this.resultPerAgentId.get(test.getPlayers().get(parsedTooltip.event).getAgentId()).setCrashed(true);
                }
            }
        }

        List<Integer> scores = response.scores;

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

        resultMessage = Constants.CG_HOST + "/replay/" + response.gameId + " ";
        int rank = 1;
        AgentIdResult agentIdResult = resultPerAgentId.get(orderedAgentIds.get(0));
        resultMessage += rank + ":" + nickPerAgentId.get(orderedAgentIds.get(0)) + (agentIdResult.isCrashed() ? "(C)" : "");
        agentIdResult.setRank(rank);
        for (int i = 1; i < test.getPlayers().size(); i++) {
            if (scorePerAgentId.get(orderedAgentIds.get(i)) < scorePerAgentId.get(orderedAgentIds.get(i - 1))) {
                rank++;
            }
            agentIdResult = resultPerAgentId.get(orderedAgentIds.get(i));
            resultMessage += " " + rank + ":" + nickPerAgentId.get(orderedAgentIds.get(i)) + (agentIdResult.isCrashed() ? "(C)" : "");
            agentIdResult.setRank(rank);
        }

        this.resultString = String.format(outputFormat, consumerName, "SEED " + test.getSeedNumber(), resultMessage);
    }
}
