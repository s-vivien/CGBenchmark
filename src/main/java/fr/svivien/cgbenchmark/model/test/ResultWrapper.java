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

    private static final Log LOG = LogFactory.getLog(ResultWrapper.class);
    private static final NumberFormat formatter = new DecimalFormat("#0.000");

    private StringBuilder reportBuilder = new StringBuilder();
    private List<TestOutput> results = Collections.synchronizedList(new ArrayList<>());

    public ResultWrapper(CodeConfiguration codeCfg) {
        reportBuilder.append("Testing " + codeCfg.getSourcePath() + " against " + codeCfg.getEnemyAgentId() + "/" + codeCfg.getEnemyName() + System.lineSeparator() + System.lineSeparator());
        reportBuilder.append("Start : " + (new Date()) + System.lineSeparator());
    }

    public void addTestResult(TestOutput testOutput) {
        results.add(testOutput);

        // Print temporary results every 6 matches
        if (results.size() % 6 == 0) {
            LOG.info("Temporary results : " + getWinrateString());
        }
    }

    public void finishReport() {
        // Sorting results by ID to keep the same order regardless of the consumers order/speed, for easier comparison between code results
        results.sort((a, b) -> a.getResultString().compareTo(b.getResultString()));
        reportBuilder.append("End : " + (new Date()) + System.lineSeparator() + System.lineSeparator());
        reportBuilder.append(getWinrateString() + System.lineSeparator() + System.lineSeparator());
        for (TestOutput testOutput : results) {
            reportBuilder.append(testOutput.getResultString() + System.lineSeparator());
        }
    }

    public String getWinrateString() {
        int W = 0, T = 0, C = 0;
        for (TestOutput to : results) {
            if (!to.isError()) {
                T++;
                if (to.isWin()) W++;
                if (to.isCrash()) C++;
            }
        }

        double winrate = (100.0 * ((double) W) / T);
        return formatter.format(winrate) + "% out of " + T + " matches" + (C > 0 ? " (crashed " + C + " times)" : "");
    }

    public StringBuilder getReportBuilder() {
        return reportBuilder;
    }
}
