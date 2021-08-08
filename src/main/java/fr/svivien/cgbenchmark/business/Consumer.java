package fr.svivien.cgbenchmark.business;

import fr.svivien.cgbenchmark.api.CGPlayApi;
import fr.svivien.cgbenchmark.business.result.ResultWrapper;
import fr.svivien.cgbenchmark.model.config.AccountConfiguration;
import fr.svivien.cgbenchmark.model.request.play.PlayRequest;
import fr.svivien.cgbenchmark.model.request.play.PlayResponse;
import fr.svivien.cgbenchmark.model.request.play.PlayResponse.Frame;
import fr.svivien.cgbenchmark.model.test.TestInput;
import fr.svivien.cgbenchmark.model.test.TestOutput;
import fr.svivien.cgbenchmark.utils.Constants;
import lombok.Data;
import okhttp3.OkHttpClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Consumes tests in the testBroker, runs them against CG API and stores the results in synchronized collection
 */
@Data
public class Consumer implements Runnable {

    private static final Log LOG = LogFactory.getLog(Consumer.class);

    private String name;
    private TestBroker testBroker;
    private CGPlayApi cgPlayApi;
    private ResultWrapper resultWrapper;
    private String cookie;
    private String ide;
    private Integer cooldown;
    private int cooldownIdx = 0;
    private boolean saveLogs;
    private long globalStartTime = 0;
    private long totalTestNumber = 0;
    private long totalPauseDuration = 0;

    private AtomicBoolean pause;

    public Consumer(TestBroker testBroker, AccountConfiguration accountCfg, Integer cooldown, AtomicBoolean pause, boolean saveLogs) {
        OkHttpClient client = new OkHttpClient.Builder().readTimeout(600, TimeUnit.SECONDS).build();
        Retrofit retrofit = new Retrofit.Builder().client(client).baseUrl(Constants.CG_HOST).addConverterFactory(GsonConverterFactory.create()).build();
        this.cookie = accountCfg.getAccountCookie();
        this.ide = accountCfg.getAccountIde();
        this.name = accountCfg.getAccountName();
        this.testBroker = testBroker;
        this.pause = pause;
        this.cgPlayApi = retrofit.create(CGPlayApi.class);
        this.cooldown = cooldown;
        this.saveLogs = saveLogs;
    }

    @Override
    public void run() {
        try {
            globalStartTime = System.currentTimeMillis();
            while (true) {
                // Retrieves next test in the testBroker
                TestInput test = testBroker.getNextTest();
                long tryStart = System.currentTimeMillis();

                // No more tests in the testBroker
                if (test == null) break;

                for (int tries = 0; tries < Constants.PLAY_MAX_RETRIES; tries++) {
                    tryStart = System.currentTimeMillis();
                    TestOutput result = testCode(cgPlayApi, test);
                    LOG.info(result.getResultString());
                    if (!result.isError()) {
                        totalTestNumber++;
                        resultWrapper.addTestResult(result);
                        break;
                    } else {
                        triggerPause();
                        // Error occurred, waiting before retrying again
                        if (cooldown == null && result.getResultString().contains(Constants.RESTRICTIONS_ERROR_MESSAGE)) {
                            if (cooldownIdx + 1 < Constants.COOLDOWNS_DURATION.length) {
                                cooldownIdx++;
                                LOG.info(String.format("Hitting the server limitations, now using a cooldown of %d seconds between games, suited for a %s long benchmark", Constants.COOLDOWNS_DURATION[cooldownIdx], Constants.COOLDOWNS_NAMES[cooldownIdx]));
                            }
                        }
                        applyCooldown(tryStart);
                        triggerPause();
                    }
                }

                if (testBroker.size() > 0) {
                    applyCooldown(tryStart);
                }
            }
            LOG.info("Consumer " + this.name + " finished its job.");
        } catch (InterruptedException ex) {
            LOG.fatal("Consumer " + name + " has encountered an issue.", ex);
        }
    }

    private void applyCooldown(long tryStart) throws InterruptedException {
        triggerPause();
        // The cooldown is applied on the start-time of each test, and not on the end-time of previous test
        int cooldownDuration = cooldown != null ? cooldown : Constants.COOLDOWNS_DURATION[cooldownIdx];
        Thread.sleep(Math.max(100, cooldownDuration * 1000L - (System.currentTimeMillis() - tryStart)));
        triggerPause();
    }

    public void resetDurationStats() {
        totalTestNumber = 0;
        totalPauseDuration = 0;
    }

    public double getMeanTestDuration() {
        if (totalTestNumber == 0) return -1;
        return ((double) ((System.currentTimeMillis() - globalStartTime) - totalPauseDuration) / (double) totalTestNumber);
    }

    private void triggerPause() {
        if (pause.get()) {
            long pauseStart = System.currentTimeMillis();
            LOG.info("Consumer " + name + " PAUSED");
            while (pause.get()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    LOG.fatal("Consumer " + name + " has encountered an issue while resuming from pause", ex);
                }
            }
            totalPauseDuration += (System.currentTimeMillis() - pauseStart);
        }
    }

    private void dumpLogForPlay(TestInput test, PlayResponse response) {
        // gameId as filename
        final String fileName = "." + File.separator + "logs" + File.separator + response.gameId + ".log";

        StringBuilder logStringBuilder = new StringBuilder();

        for (int iframe = 0; iframe < response.frames.size(); iframe++) {
            Frame currentFrame = response.frames.get(iframe);
            String logHeader = "----- " + iframe + " / " + response.frames.size() + " -----" + System.lineSeparator();

            if (currentFrame.error != null) { // Error frame
                logStringBuilder.append(logHeader);
                logStringBuilder.append("ERROR at line ").append(currentFrame.error.line).append(":").append(System.lineSeparator());
                logStringBuilder.append(currentFrame.error.message);
                logStringBuilder.append(System.lineSeparator());
            } else if (currentFrame.gameInformation.contains(Constants.TIMEOUT_INFORMATION_PART) && currentFrame.agentId != -1) { // Timeout frame
                logStringBuilder.append(logHeader);
                logStringBuilder.append(test.getPlayers().get(currentFrame.agentId).getName()).append(" TIMEOUT !");
                logStringBuilder.append(System.lineSeparator());
            } else if (currentFrame.stderr != null && currentFrame.agentId != -1 && test.getPlayers().get(currentFrame.agentId).getAgentId() == -1) { // Regular frame
                logStringBuilder.append(logHeader);
                logStringBuilder.append(currentFrame.stderr);
                logStringBuilder.append(System.lineSeparator());
            }
        }

        // If nothing has been logged, we avoid creating an empty file
        if (logStringBuilder.length() > 0) {
            // Creates folder and file
            try {
                Path pathToFile = Paths.get(fileName);
                Files.createDirectories(pathToFile.getParent());
                Files.createFile(pathToFile);
            } catch (IOException ex) {
                LOG.error("Unable to create log file for " + response.gameId, ex);
            }

            // Writes content to file
            try (FileWriter fw = new FileWriter(fileName)) {
                fw.write(logStringBuilder.toString());
                fw.flush();
            } catch (IOException ex) {
                LOG.error("Unable to write log file for " + response.gameId, ex);
            }
        }
    }

    private TestOutput testCode(CGPlayApi cgPlayApi, TestInput test) {
        PlayRequest request = new PlayRequest(testBroker.getCodeContent(), testBroker.getCodeLanguage(), ide, test.getSeed(), test.getPlayers());
        Call<PlayResponse> call = cgPlayApi.play(request, Constants.CG_HOST + "/ide/" + ide, cookie);
        TestOutput testOutput;
        PlayResponse playResponse = null;
        try {
            Response<PlayResponse> response = call.execute();
            if (response.isSuccessful()) {
                playResponse = response.body();
                testOutput = new TestOutput(test, name, playResponse);
            } else {
                testOutput = new TestOutput(test, name, response.errorBody() != null ? response.errorBody().string() : "");
            }
        } catch (IOException e) {
            testOutput = new TestOutput(test, name, "");
        }

        if (saveLogs && playResponse != null) {
            try {
                dumpLogForPlay(test, playResponse);
            } catch (RuntimeException e) {
                LOG.error("Error while dumping logs in file", e);
            }
        }

        return testOutput;
    }

    //             DUMMY for test purpose
    //    Random rnd = new Random(2323);
    //    private TestOutput testCode(CGPlayApi cgPlayApi, TestInput test) {
    //        PlayResponse resp = new PlayResponse();
    //        resp.gameId = (long) (297629806 + rnd.nextDouble() * 702370193);
    //        resp.frames = new java.util.ArrayList<>();
    //        if (rnd.nextDouble() < 0.015) { // Add random crash
    //            for (int i = 0; i < test.getPlayers().size(); i++) {
    //                Frame f = resp.new Frame();
    //                f.gameInformation = "This is timeout";
    //                f.agentId = i;
    //                resp.frames.add(f);
    //            }
    //        }
    //        resp.scores = new java.util.ArrayList<>();
    //        for (int i = 0; i < test.getPlayers().size(); i++) {
    //            resp.scores.add((int) (rnd.nextDouble() * 10));
    //        }
    //        if (saveLogs) dumpLogForPlay(test, resp);
    //        return new TestOutput(test, name, resp);
    //    }
}
