package fr.svivien.cgbenchmark.model.test;

import fr.svivien.cgbenchmark.model.config.CodeConfiguration;
import fr.svivien.cgbenchmark.model.config.EnemyConfiguration;
import fr.svivien.cgbenchmark.producerconsumer.Consumer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/**
 * Wraps test results for a single code
 */
public class ResultWrapper {

    private class Dominance {
        int win, lose, draw, total;
    }

    private static final String winrateOutputFormat = "%20s    %-12s %-12s %-10s %-9s %s";
    private static final String positionsOutputFormat = "%25s    %-12s %-12s %-12s %-12s %s";

    private static final Log LOG = LogFactory.getLog(ResultWrapper.class);
    private static final NumberFormat formatter = new DecimalFormat("#0.00");

    private StringBuilder reportBuilder = new StringBuilder();
    private List<TestOutput> results = Collections.synchronizedList(new ArrayList<>());
    private Map<Integer, String> nickPerAgentId = new HashMap<>();
    private Map<Integer, Dominance> dominances = new HashMap<>();
    private List<Consumer> accountConsumerList;
    private int totalTestNumber;

    /**
     * first dimension : player number
     * second dimension : 0=total, 1 2 3 4=rank, 5=crash number
     */
    private int[][] positions = new int[5][6];

    public ResultWrapper(CodeConfiguration codeCfg, List<Consumer> accountConsumerList, int totalTestNumber) {
        this.totalTestNumber = totalTestNumber;
        this.accountConsumerList = accountConsumerList;
        for (Consumer consumer : this.accountConsumerList) {
            consumer.setResultWrapper(this);
        }
        for (EnemyConfiguration ec : codeCfg.getEnemies()) {
            nickPerAgentId.put(ec.getAgentId(), ec.getName());
        }
        for (EnemyConfiguration ec : codeCfg.getEnemies()) {
            dominances.put(ec.getAgentId(), new Dominance());
        }
        String logStr = "Testing " + codeCfg.getSourcePath() + " against ";
        for (EnemyConfiguration ec : codeCfg.getEnemies()) {
            logStr += " " + ec.getName() + "_" + ec.getAgentId();
        }
        reportBuilder.append(logStr + System.lineSeparator());
        reportBuilder.append("Start : " + (new Date()) + System.lineSeparator());
    }

    public void addTestResult(TestOutput to) {
        results.add(to);
        int playersNumber = to.getRankPerAgentId().size();

        if (!to.isError()) {
            int myRank = to.getRankPerAgentId().get(-1);
            positions[playersNumber][myRank]++;
            positions[playersNumber][0]++; // total

            for (Map.Entry<Integer, Dominance> entry : dominances.entrySet()) {
                if (to.getRankPerAgentId().get(entry.getKey()) == null) continue;
                int diff = to.getRankPerAgentId().get(-1) - to.getRankPerAgentId().get(entry.getKey());
                if (diff < 0) { // i'm ahead
                    entry.getValue().win++;
                } else if (diff > 0) { // i'm behind
                    entry.getValue().lose++;
                } else { // draw
                    entry.getValue().draw++;
                }
                entry.getValue().total++;
            }

            if (to.isCrash()) positions[playersNumber][5]++;
        }

        // Print temporary results every 6 matches
        if (results.size() % 6 == 0) {
            LOG.info("Temporary results " + getTimeLeftEstimationDetails() + ":" + getWinrateDetails());
        }
    }

    public void finishReport() {
        // Sorting results by ID to keep the same order regardless of the consumers order/speed, for easier comparison between code results
        results.sort((a, b) -> a.getResultString().compareTo(b.getResultString()));
        reportBuilder.append("End : " + (new Date()) + System.lineSeparator());
        reportBuilder.append(getWinrateDetails() + System.lineSeparator() + System.lineSeparator());
        for (TestOutput testOutput : results) {
            reportBuilder.append(testOutput.getResultString() + System.lineSeparator());
        }
    }

    public String getShortFilenameWinrate() {
        int wins = 0, total = 0;
        for (int i = 1; i <= 4; i++) {
            wins += positions[i][1];
            total += positions[i][0];
        }
        double globalWinrate = wins != 0 ? (100.0 * ((double) wins) / (double) total) : 0.;
        return formatter.format(globalWinrate).replace(",", ".");
    }

    private String getTimeLeftEstimationDetails() {
        double meanTestDuration = 0;
        for (Consumer consumer : this.accountConsumerList) {
            double consumerMeanTestDuration = consumer.getMeanTestDuration();
            if (consumerMeanTestDuration != -1) {
                meanTestDuration += consumerMeanTestDuration;
            } else {
                // Not enough stats to compute accurate stats
                return "";
            }
        }

        meanTestDuration /= this.accountConsumerList.size();
        double timeLeft = meanTestDuration * (totalTestNumber - results.size());
        timeLeft /= this.accountConsumerList.size();

        return "(time left estimation : " + ((int) (timeLeft / (1000 * 60))) + " minutes) ";
    }

    public String getWinrateDetails() {
        double winrate, lossrate, drawrate;
        String winrateString = "";

        for (Map.Entry<Integer, Dominance> entry : dominances.entrySet()) {
            Dominance dom = entry.getValue();
            winrate = dom.win != 0 ? (100.0 * ((double) dom.win) / dom.total) : 0;
            lossrate = dom.lose != 0 ? (100.0 * ((double) dom.lose) / dom.total) : 0;
            drawrate = dom.draw != 0 ? (100.0 * ((double) dom.draw) / dom.total) : 0;
            winrateString += System.lineSeparator()
                             + String.format(winrateOutputFormat, nickPerAgentId.get(entry.getKey()),
                    "GW=" + formatter.format(winrate + 0.5 * drawrate) + "%", "[ W=" + formatter.format(winrate) + "%", "L=" + formatter.format(lossrate) + "%", "D=" + formatter.format(drawrate) + "%", "] [" + dom.total + "]");
        }

        String[] winrates = new String[4];
        Arrays.fill(winrates, "");
        for (int npl = 2; npl <= 4; npl++) {
            if (positions[npl][0] > 0) {
                for (int i = 1; i <= npl; i++) {
                    double rate = positions[npl][i] != 0 ? 100.0 * (((double) positions[npl][i]) / positions[npl][0]) : 0;
                    winrates[i - 1] = i + ": " + formatter.format(rate) + "%";
                }
                winrateString +=
                        System.lineSeparator() + String.format(positionsOutputFormat, "Positions (" + npl + " players)", winrates[0], winrates[1], winrates[2], winrates[3], "(" + positions[npl][5] + " crash(es) on " + positions[npl][0] + " games)");
            }
        }

        return winrateString;
    }

    public StringBuilder getReportBuilder() {
        return reportBuilder;
    }
}
