package fr.svivien.cgbenchmark.business.result;

import fr.svivien.cgbenchmark.utils.Constants;
import org.apache.commons.math3.distribution.BetaDistribution;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds statistics for a particular enemy, regardless of the matchup type (1v1, 1v2, 1v3)
 */
public class DominanceStats {

    private static final String winrateOutputFormat = "%20s    GW=%-7s [%-7s,%-7s] [W=%-7s L=%-7s D=%-7s] [%s]%s";
    private static final NumberFormat doubleFormatter = new DecimalFormat(Constants.DOUBLE_FORMAT);

    private final Map<Integer, String> nickPerAgentId = new HashMap<>();
    private final Map<Integer, DominanceStat> dominances = new LinkedHashMap<>();
    private final Map<Integer, Integer> crashes = new LinkedHashMap<>();
    private int gameNumber = 0;

    public DominanceStats() {
        dominances.put(-1, new DominanceStat());
        crashes.put(-1, 0);
    }

    double getGlobalMeanWinrate() {
        return dominances.get(-1).getMeanWinrate();
    }

    void addEnemy(int agentId, String nick) {
        dominances.put(agentId, new DominanceStat());
        crashes.put(agentId, 0);
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

    void incrementCrash(int agentId) {
        this.crashes.put(agentId, this.crashes.get(agentId) + 1);
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
            int crashes = this.crashes.get(entry.getKey());
            double[] winrateBounds = dom.getWinrateBounds();

            builder.append(String.format(
                    winrateOutputFormat,
                    nickPerAgentId.get(entry.getKey()),
                    doubleFormatter.format(dom.getMeanWinrate()) + "%",
                    doubleFormatter.format(winrateBounds[0]) + "%",
                    doubleFormatter.format(winrateBounds[1]) + "%",
                    doubleFormatter.format(dom.getWinrate()) + "%",
                    doubleFormatter.format(dom.getLoserate()) + "%",
                    doubleFormatter.format(dom.getDrawrate()) + "%",
                    dom.total,
                    crashes > 0 ? " [" + crashes + " crash" + (crashes > 1 ? "es" : "") + "]" : ""));
            builder.append(System.lineSeparator());
        }
        DominanceStat totalStat = dominances.get(-1);
        double[] overallWinrateBounds = totalStat.getWinrateBounds();
        int crashes = this.crashes.get(-1);
        builder.append(String.format(
                winrateOutputFormat,
                "-- EVERYONE --",
                doubleFormatter.format(totalStat.getMeanWinrate()) + "%",
                doubleFormatter.format(overallWinrateBounds[0]) + "%",
                doubleFormatter.format(overallWinrateBounds[1]) + "%",
                doubleFormatter.format(totalStat.getWinrate()) + "%",
                doubleFormatter.format(totalStat.getLoserate()) + "%",
                doubleFormatter.format(totalStat.getDrawrate()) + "%",
                gameNumber + " games",
                crashes > 0 ? " [ME : " + crashes + " crash" + (crashes > 1 ? "es" : "") + "]" : ""));

        return builder.toString();
    }

    private class DominanceStat {
        private int total;
        private double win, lose, draw;

        double getWinrate() {
            return win != 0 ? (100.0 * win / total) : 0;
        }

        double getLoserate() {
            return lose != 0 ? (100.0 * lose / total) : 0;
        }

        double getDrawrate() {
            return draw != 0 ? (100.0 * draw / total) : 0;
        }

        double getMeanWinrate() {
            return this.getWinrate() + 0.5 * this.getDrawrate();
        }

        double[] getWinrateBounds() {
            if (total <= 0) return new double[]{0, 100};
            double meanWins = win + 0.5 * draw;
            double alpha = 0.05;
            BetaDistribution betaDist = new BetaDistribution(total - meanWins + 1, meanWins + 1e-7);
            double lower_bound = 1 - betaDist.inverseCumulativeProbability(1 - alpha / 2d);
            betaDist = new BetaDistribution(total - meanWins + 1e-7, meanWins + 1);
            double upper_bound = 1 - betaDist.inverseCumulativeProbability(alpha / 2d);
            return new double[]{100 * lower_bound, 100 * upper_bound};
        }
    }
}
