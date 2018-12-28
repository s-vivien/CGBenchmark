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
    private static final String positionsOutputFormat = "%25s    %-12s %-12s %-12s %-12s";
    private static final Log LOG = LogFactory.getLog(ResultWrapper.class);
    private static final NumberFormat formatter = new DecimalFormat("#0.00");

    private StringBuilder reportBuilder = new StringBuilder();
    private StringBuilder detailBuilder = new StringBuilder();
    private List<TestOutput> results = Collections.synchronizedList(new ArrayList<>());
    private Map<Integer, String> nickPerAgentId = new HashMap<>();
    private Map<Integer, Dominance> dominances = new LinkedHashMap<>();
    private double gwinrate;
    private List<Consumer> accountConsumerList;
    private int totalTestNumber;
    private int maxEnemies;
    private int crashes = 0;

    /**
     * first dimension : player number
     * second dimension : 0=total, 1 2 3 4=rank
     */
    private int[][] positions = new int[5][5];

    public ResultWrapper(CodeConfiguration codeCfg, List<Consumer> accountConsumerList, int totalTestNumber, int maxEnemies) {
        this.maxEnemies = maxEnemies;
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

            if (to.isCrash()) crashes++;
        }

        // Print temporary results every 6 matches
        if (results.size() % 6 == 0) {
            LOG.info("Temporary results " + getTimeLeftEstimationDetails() + ":" + getWinrateDetails());
            detailBuilder.append("Temporary results " + getTimeLeftEstimationDetails() + ":" + getWinrateDetails() + System.lineSeparator());
        }
    }

    public void finishReport() {
        // Sorting results by ID to keep the same order regardless of the consumers order/speed, for easier comparison between code results
        reportBuilder.append("End : " + (new Date()) + System.lineSeparator());
        reportBuilder.append(getWinrateDetails() + System.lineSeparator() + System.lineSeparator() + System.lineSeparator());
        reportBuilder.append(detailBuilder);
    }

    public String getShortFilenameWinrate() {
        return formatter.format(gwinrate).replace(",", ".");
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
        int gtotal = 0, gwin = 0, gdraw = 0, glose = 0;
        String winrateString = "";

        for (Map.Entry<Integer, Dominance> entry : dominances.entrySet()) {
            Dominance dom = entry.getValue();
            gtotal += dom.total;
            gwin += dom.win;
            gdraw += dom.draw;
            glose += dom.lose;
            winrate = dom.win != 0 ? (100.0 * ((double) dom.win) / dom.total) : 0;
            lossrate = dom.lose != 0 ? (100.0 * ((double) dom.lose) / dom.total) : 0;
            drawrate = dom.draw != 0 ? (100.0 * ((double) dom.draw) / dom.total) : 0;
            winrateString += System.lineSeparator() + String.format(winrateOutputFormat, nickPerAgentId.get(entry.getKey()),
                    "GW=" + formatter.format(winrate + 0.5 * drawrate) + "%", "[ W=" + formatter.format(winrate) + "%", "L=" + formatter.format(lossrate) + "%", "D=" + formatter.format(drawrate) + "%", "] [" + dom.total + "]");
        }

        winrate = gwin != 0 ? (100.0 * ((double) gwin) / gtotal) : 0;
        lossrate = glose != 0 ? (100.0 * ((double) glose) / gtotal) : 0;
        drawrate = gdraw != 0 ? (100.0 * ((double) gdraw) / gtotal) : 0;
        gwinrate = winrate + 0.5 * drawrate;

        winrateString += System.lineSeparator() + String.format(winrateOutputFormat, "-- TOTAL --",
                "GW=" + formatter.format(gwinrate) + "%",
                "[ W=" + formatter.format(winrate) + "%", "L=" + formatter.format(lossrate) + "%", "D=" + formatter.format(drawrate) + "%", "] [" + gtotal + " games]" + (crashes > 0 ? " [" + crashes + " crash(es)]" : ""));

        if (maxEnemies > 1) {
            String[] winrates = new String[4];
            Arrays.fill(winrates, "");
            for (int npl = 2; npl <= 4; npl++) {
                if (positions[npl][0] > 0) {
                    for (int i = 1; i <= npl; i++) {
                        double rate = positions[npl][i] != 0 ? 100.0 * (((double) positions[npl][i]) / positions[npl][0]) : 0;
                        winrates[i - 1] = i + ": " + formatter.format(rate) + "%";
                    }
                    winrateString += System.lineSeparator() + String.format(positionsOutputFormat, "Positions (" + npl + " players)", winrates[0], winrates[1], winrates[2], winrates[3]);
                }
            }
        }

        return winrateString;
    }

    public StringBuilder getReportBuilder() {
        return reportBuilder;
    }

    public StringBuilder getDetailBuilder() {
        return detailBuilder;
    }
}
