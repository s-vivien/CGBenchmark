package fr.svivien.cgbenchmark.model.test;

import fr.svivien.cgbenchmark.model.config.CodeConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Wraps test results for a single code
 */
public class ResultWrapper {

    private static final String[] REPORT_PREFIXES = {"As P1", "As P2", "Overall"};
    private static final String outputFormat = "%-8s    %-10s %-10s %-10s %-11s %s";
    private static final Log LOG = LogFactory.getLog(ResultWrapper.class);
    private static final NumberFormat formatter = new DecimalFormat("#0.00");

    private StringBuilder reportBuilder = new StringBuilder();
    private List<TestOutput> results = Collections.synchronizedList(new ArrayList<>());
    private int[] W = new int[3], L = new int[3], D = new int[3], T = new int[3], C = new int[3];

    public ResultWrapper(CodeConfiguration codeCfg) {
        reportBuilder.append("Testing " + codeCfg.getSourcePath() + " against " + codeCfg.getEnemyAgentId() + "/" + codeCfg.getEnemyName() + System.lineSeparator() + System.lineSeparator());
        reportBuilder.append("Start : " + (new Date()) + System.lineSeparator());
    }

    public void addTestResult(TestOutput to) {
        results.add(to);

        if (!to.isError()) {
            int idx = to.isReverse() ? 1 : 0;
            T[idx]++;
            T[2]++;
            if (to.getScoreDiff() > 0) {
                W[idx]++;
                W[2]++;
            } else if (to.getScoreDiff() < 0) {
                L[idx]++;
                L[2]++;
            } else {
                D[idx]++;
                D[2]++;
            }
            if (to.isCrash()) {
                C[idx]++;
                C[2]++;
            }
        }

        // Print temporary results every 6 matches
        if (results.size() % 6 == 0) {
            LOG.info("Temporary results :" + getWinrateDetails());
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
        int total = T[0] + T[1];
        double winrate = total != 0 ? (100.0 * ((double) (W[0] + W[1])) / total) : 0;
        return formatter.format(winrate).replace(",", ".");
    }

    public String getWinrateDetails() {
        double winrate, lossrate, drawrate;
        int crash, total;
        String winrateString = "";

        for (int i = 0; i < 3; i++) {
            winrate = T[i] != 0 ? (100.0 * ((double) W[i]) / T[i]) : 0;
            lossrate = T[i] != 0 ? (100.0 * ((double) L[i]) / T[i]) : 0;
            drawrate = T[i] != 0 ? (100.0 * ((double) D[i]) / T[i]) : 0;
            crash = C[i];
            total = T[i];
            winrateString += "\n" + String.format(outputFormat, REPORT_PREFIXES[i], "W " + formatter.format(winrate) + "%", "L " + formatter.format(lossrate) + "%", "D " + formatter.format(drawrate) + "%", "[" + total + " games]", (crash > 0 ? " (crashed " + crash + " times)" : ""));
        }

        return winrateString;
    }

    public StringBuilder getReportBuilder() {
        return reportBuilder;
    }
}
