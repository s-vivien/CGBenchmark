package fr.svivien.cgbenchmark.business.result;

import fr.svivien.cgbenchmark.business.Consumer;
import fr.svivien.cgbenchmark.model.config.CodeConfiguration;
import fr.svivien.cgbenchmark.model.config.EnemyConfiguration;
import fr.svivien.cgbenchmark.model.test.TestOutput;
import fr.svivien.cgbenchmark.utils.Constants;
import lombok.Data;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/**
 * Wraps test results for a single code
 */
@Data
public class ResultWrapper {

    private static final NumberFormat doubleFormatter = new DecimalFormat(Constants.DOUBLE_FORMAT);
    private static final Log LOG = LogFactory.getLog(ResultWrapper.class);

    private List<Consumer> consumers;
    private StringBuilder reportBuilder = new StringBuilder();
    private StringBuilder detailBuilder = new StringBuilder();
    private List<TestOutput> results = Collections.synchronizedList(new ArrayList<>());
    private PositionStats positionStats;
    private DominanceStats dominanceStats;

    private int totalTestNumber;
    private int maxEnemies;

    public ResultWrapper(CodeConfiguration codeCfg, List<Consumer> consumers, int totalTestNumber, int maxEnemies) {
        this.maxEnemies = maxEnemies;
        this.consumers = consumers;
        this.positionStats = new PositionStats(maxEnemies);
        this.dominanceStats = new DominanceStats();
        this.totalTestNumber = totalTestNumber;
        for (EnemyConfiguration ec : codeCfg.getEnemies()) {
            dominanceStats.addEnemy(ec.getAgentId(), ec.getName());
        }
        String logStr = "Testing " + codeCfg.getSourcePath() + " against ";
        for (EnemyConfiguration ec : codeCfg.getEnemies()) {
            logStr += " " + ec.getName() + "_" + ec.getAgentId();
        }
        reportBuilder.append(logStr).append(System.lineSeparator());
        reportBuilder.append("Start : ").append(new Date()).append(System.lineSeparator());
    }

    public void addTestResult(TestOutput to) {
        results.add(to);
        int playerNumber = to.getRankPerAgentId().size();

        if (!to.isError()) {
            int myRank = to.getRankPerAgentId().get(-1);
            positionStats.addStat(playerNumber, myRank);

            for (Map.Entry<Integer, Integer> entry : to.getRankPerAgentId().entrySet()) {
                if (entry.getKey() == -1) continue;
                int rankDiff = myRank - entry.getValue();
                dominanceStats.addStat(entry.getKey(), rankDiff);
            }

            dominanceStats.incrementGameNumber();
            if (to.isCrash()) dominanceStats.incrementCrash();
        }

        detailBuilder.append(to.getResultString()).append(System.lineSeparator());
        // Print temporary results every 6 matches
        if (results.size() % 6 == 0) {
            String winrateDetailsString = getWinrateDetails();
            LOG.info(" Temporary results " + getTimeLeftEstimationDetails() + ":" + System.lineSeparator() + winrateDetailsString);
            detailBuilder.append("Temporary results :").append(System.lineSeparator()).append(winrateDetailsString).append(System.lineSeparator());
        }
    }

    public void finishReport() {
        reportBuilder.append("End : ").append(new Date());
        reportBuilder.append(System.lineSeparator()).append(System.lineSeparator());
        reportBuilder.append(getWinrateDetails());
        reportBuilder.append(System.lineSeparator()).append(System.lineSeparator()).append(System.lineSeparator());
        reportBuilder.append(detailBuilder);
    }

    public String getShortFilenameWinrate() {
        return doubleFormatter.format(dominanceStats.getMeanWinrate(-1)).replace(",", ".");
    }

    public String getWinrateDetails() {
        String winrateString = "";
        winrateString += dominanceStats;
        if (maxEnemies > 1) {
            winrateString += System.lineSeparator();
            winrateString += positionStats;
        }
        return winrateString;
    }

    private String getTimeLeftEstimationDetails() {
        double meanTestDuration = 0;
        for (Consumer consumer : this.consumers) {
            double consumerMeanTestDuration = consumer.getMeanTestDuration();
            if (consumerMeanTestDuration != -1) {
                meanTestDuration += consumerMeanTestDuration;
            } else {
                // Not enough stats to compute accurate stats
                return "";
            }
        }

        meanTestDuration /= this.consumers.size();
        double timeLeft = meanTestDuration * (totalTestNumber - results.size());
        timeLeft /= this.consumers.size();

        return "(ETA : " + ((int) (timeLeft / (1000 * 60))) + " minutes) ";
    }
}
