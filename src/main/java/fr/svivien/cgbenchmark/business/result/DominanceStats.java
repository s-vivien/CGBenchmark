package fr.svivien.cgbenchmark.business.result;

import fr.svivien.cgbenchmark.utils.Constants;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds statistics for a particular enemy, regardless of the matchup type (1v1, 1v2, 1v3)
 */
public class DominanceStats {

    private class DominanceStat {
        private int total;
        private double win, lose, draw;
    }

    private static final String winrateOutputFormat = "%20s    GW=%-7s   [ W=%-7s  L=%-7s  D=%-7s ] [%s]%s";
    private static final NumberFormat doubleFormatter = new DecimalFormat(Constants.DOUBLE_FORMAT);

    private final Map<Integer, String> nickPerAgentId = new HashMap<>();
    private final Map<Integer, DominanceStat> dominances = new LinkedHashMap<>();
    private int gameNumber = 0;
    private int crashNumber = 0;

    public DominanceStats() {
        dominances.put(-1, new DominanceStat());
    }

    void addEnemy(int agentId, String nick) {
        dominances.put(agentId, new DominanceStat());
        nickPerAgentId.put(agentId, nick);
    }

    void addStat(int agentId, int rankDiff) {
        DominanceStat stat = dominances.get(agentId);
        DominanceStat totalStat = dominances.get(-1);
        stat.total++;
        totalStat.total++;
        if (rankDiff < 0) { // i'm ahead
            stat.win++;
            totalStat.win++;
        } else if (rankDiff > 0) { // i'm behind
            stat.lose++;
            totalStat.lose++;
        } else { // draw
            stat.draw++;
            totalStat.draw++;
        }
    }

    private double getWinrate(int agentId) {
        DominanceStat stat = dominances.get(agentId);
        return stat.win != 0 ? (100.0 * stat.win / stat.total) : 0;
    }

    private double getLoserate(int agentId) {
        DominanceStat stat = dominances.get(agentId);
        return stat.lose != 0 ? (100.0 * stat.lose / stat.total) : 0;
    }

    private double getDrawrate(int agentId) {
        DominanceStat stat = dominances.get(agentId);
        return stat.draw != 0 ? (100.0 * stat.draw / stat.total) : 0;
    }

    double getMeanWinrate(int agentId) {
        return getWinrate(agentId) + 0.5 * getDrawrate(agentId);
    }

    void incrementCrash() {
        this.crashNumber++;
    }

    void incrementGameNumber() {
        this.gameNumber++;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (Map.Entry<Integer, DominanceStat> entry : dominances.entrySet()) {
            if (entry.getKey() == -1) continue;
            DominanceStat dom = entry.getValue();

            builder.append(String.format(
                    winrateOutputFormat,
                    nickPerAgentId.get(entry.getKey()),
                    doubleFormatter.format(getMeanWinrate(entry.getKey())) + "%",
                    doubleFormatter.format(getWinrate(entry.getKey())) + "%",
                    doubleFormatter.format(getLoserate(entry.getKey())) + "%",
                    doubleFormatter.format(getDrawrate(entry.getKey())) + "%",
                    dom.total,
                    ""));
            builder.append(System.lineSeparator());
        }

        builder.append(String.format(
                winrateOutputFormat,
                "-- TOTAL --",
                doubleFormatter.format(getMeanWinrate(-1)) + "%",
                doubleFormatter.format(getWinrate(-1)) + "%",
                doubleFormatter.format(getLoserate(-1)) + "%",
                doubleFormatter.format(getDrawrate(-1)) + "%",
                gameNumber + " games",
                crashNumber > 0 ? " [" + crashNumber + " crash(es)]" : ""));

        return builder.toString();
    }
}
