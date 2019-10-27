package fr.svivien.cgbenchmark;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import fr.svivien.cgbenchmark.api.LoginApi;
import fr.svivien.cgbenchmark.api.SessionApi;
import fr.svivien.cgbenchmark.business.Consumer;
import fr.svivien.cgbenchmark.business.TestBroker;
import fr.svivien.cgbenchmark.business.result.ResultWrapper;
import fr.svivien.cgbenchmark.model.config.AccountConfiguration;
import fr.svivien.cgbenchmark.model.config.CodeConfiguration;
import fr.svivien.cgbenchmark.model.config.EnemyConfiguration;
import fr.svivien.cgbenchmark.model.config.GlobalConfiguration;
import fr.svivien.cgbenchmark.model.request.login.LoginRequest;
import fr.svivien.cgbenchmark.model.request.login.LoginResponse;
import fr.svivien.cgbenchmark.model.request.session.SessionRequest;
import fr.svivien.cgbenchmark.model.request.session.SessionResponse;
import fr.svivien.cgbenchmark.model.test.TestInput;
import fr.svivien.cgbenchmark.utils.Constants;
import fr.svivien.cgbenchmark.utils.SeedCleaner;
import okhttp3.OkHttpClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CGBenchmark {

    private static final Log LOG = LogFactory.getLog(CGBenchmark.class);

    private GlobalConfiguration globalConfiguration = null;
    private final List<Consumer> consumers = new ArrayList<>();
    private final TestBroker testBroker = new TestBroker();
    private final Random rnd = new Random();
    private final EnemyConfiguration me = new EnemyConfiguration(-1, "[ME]");
    private final AtomicBoolean pause = new AtomicBoolean(false);

    public CGBenchmark(String cfgFilePath, boolean saveLogs) {
        // Parsing configuration file
        try {
            globalConfiguration = parseConfigurationFile(cfgFilePath);
            globalConfiguration.setSaveLogs(saveLogs);
            checkConfiguration(globalConfiguration);
        } catch (FileNotFoundException | JsonIOException | JsonSyntaxException e) {
            LOG.fatal("Failed to parse configuration file", e);
            System.exit(1);
        } catch (IllegalArgumentException e) {
            LOG.fatal("Configuration is invalid", e);
            System.exit(1);
        }

        // Creating account consumers
        LOG.info("Registering " + globalConfiguration.getAccountConfigurationList().size() + " account(s)");
        for (AccountConfiguration accountCfg : globalConfiguration.getAccountConfigurationList()) {
            try {
                retrieveAccountCookieAndSession(accountCfg);
            } catch (IllegalStateException e) {
                LOG.fatal("Error while retrieving account cookie and session", e);
                System.exit(1);
            }
            consumers.add(new Consumer(testBroker, accountCfg, globalConfiguration.getRequestCooldown(), pause, globalConfiguration.isSaveLogs()));
            LOG.info("Account " + accountCfg.getAccountName() + " successfully registered");
        }
    }

    public void launch() {
        // Launching tests
        for (CodeConfiguration codeCfg : globalConfiguration.getCodeConfigurationList()) {

            ExecutorService threadPool = Executors.newFixedThreadPool(consumers.size());

            String codeContent = null;
            try {
                codeContent = new String(Files.readAllBytes(Paths.get(codeCfg.getSourcePath())));
            } catch (IOException e) {
                LOG.error("An error has occurred while reading source code for " + codeCfg.getSourcePath(), e);
            }

            testBroker.reset();
            testBroker.setCodeContent(codeContent);
            testBroker.setCodeLanguage(codeCfg.getLanguage());

            createTests(codeCfg);

            Path p = Paths.get(codeCfg.getSourcePath());
            String codeName = p.getFileName().toString();
            String logStr = "Launching " + testBroker.size() + " tests " + codeName + " against";
            for (EnemyConfiguration ec : codeCfg.getEnemies()) {
                logStr += " " + ec.getName() + "_" + ec.getAgentId();
            }
            LOG.info(logStr);

            // Brand new resultWrapper for this test
            ResultWrapper resultWrapper = new ResultWrapper(codeCfg, consumers, testBroker.size(), globalConfiguration.getMaxEnemiesNumber());

            // Adding consumers in the thread-pool
            for (Consumer consumer : consumers) {
                consumer.resetDurationStats();
                consumer.setResultWrapper(resultWrapper);
                threadPool.execute(consumer);
            }

            try {
                // Unleash the executor
                threadPool.shutdown();
                threadPool.awaitTermination(5, TimeUnit.DAYS); // If 5 days is not enough, you're doing it wrong
            } catch (InterruptedException e) {
                LOG.error("An error has occurred within the broker/executor", e);
            }

            LOG.info("Final results :" + resultWrapper.getWinrateDetails());

            // Complete the report with all the results and final winrate
            resultWrapper.finishReport();

            // Write report to external file
            String reportFileName = computeReportFileName(codeName, resultWrapper);

            LOG.info("Writing final report to : " + reportFileName);
            try (PrintWriter out = new PrintWriter(reportFileName)) {
                out.println(resultWrapper.getReportBuilder().toString());
            } catch (Exception e) {
                LOG.warn("An error has occurred when writing final report", e);
            }
        }

        LOG.info("No more tests. Ending.");
    }

    private String computeReportFileName(String codeName, ResultWrapper resultWrapper) {
        String reportFileName = codeName + "-" + resultWrapper.getShortFilenameWinrate();
        // Add suffix to avoid overwriting existing report file
        File file = new File(reportFileName + ".txt");
        if (file.exists() && !file.isDirectory()) {
            int suffix = -1;
            do {
                suffix++;
                file = new File(reportFileName + "_" + suffix + ".txt");
            } while (file.exists() && !file.isDirectory());
            reportFileName += "_" + suffix;
        }
        reportFileName += ".txt";
        return reportFileName;
    }

    private void retrieveAccountCookieAndSession(AccountConfiguration accountCfg) {
        LOG.info("Retrieving cookie and session for account " + accountCfg.getAccountName());

        OkHttpClient client = new OkHttpClient.Builder().readTimeout(600, TimeUnit.SECONDS).build();
        Retrofit retrofit = new Retrofit.Builder().client(client).baseUrl(Constants.CG_HOST).addConverterFactory(GsonConverterFactory.create()).build();
        LoginApi loginApi = retrofit.create(LoginApi.class);

        LoginRequest loginRequest = new LoginRequest(accountCfg.getAccountLogin(), accountCfg.getAccountPassword());
        Call<LoginResponse> loginCall = loginApi.login(loginRequest);

        // Calling getSessionHandle API
        retrofit2.Response<LoginResponse> loginResponse;
        try {
            loginResponse = loginCall.execute();
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Login request failed");
        }

        if (loginResponse.body() == null || loginResponse.body().success == null || loginResponse.body().success.userId == null) {
            throw new IllegalStateException("Login failed, please check login/pwd in configuration");
        }

        String cookie = String.join("; ", loginResponse.headers().values(Constants.SET_COOKIE));
        // Setting the cookie in the account configuration
        accountCfg.setAccountCookie(cookie);

        // Retrieving IDE handle
        String handle = retrieveHandle(retrofit, loginResponse.body().success.userId, accountCfg.getAccountCookie());

        // Setting the IDE session in the account configuration
        accountCfg.setAccountIde(handle);
    }

    private String retrieveHandle(Retrofit retrofit, Integer userId, String accountCookie) {
        SessionApi sessionApi = retrofit.create(SessionApi.class);
        SessionRequest sessionRequest = new SessionRequest(userId, globalConfiguration.getMultiName(), globalConfiguration.getIsContest());
        Call<SessionResponse> sessionCall;
        sessionCall = sessionApi.getSessionHandle(globalConfiguration.getIsContest() ? Constants.CONTEST_SESSION_SERVICE_URL : Constants.PUZZLE_SESSION_SERVICE_URL, sessionRequest, Constants.CG_HOST, accountCookie);

        retrofit2.Response<SessionResponse> sessionResponse;
        try {
            sessionResponse = sessionCall.execute();
            if (sessionResponse.body() == null) {
                throw new IllegalStateException("Session request failed");
            }
            if (globalConfiguration.getIsContest()) {
                return sessionResponse.body().success.testSessionHandle;
            } else {
                return sessionResponse.body().success.handle;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error while retrieving session handle", e);
        }
    }

    private void createTests(CodeConfiguration codeCfg) {
        rnd.setSeed(Constants.RANDOM_SEED);

        // Filling the broker with all the tests
        for (int replay = 0; replay < codeCfg.getNbReplays(); replay++) {
            if (globalConfiguration.getRandomSeed()) {
                List<EnemyConfiguration> selectedPlayers = getRandomEnemies(codeCfg);
                int myStartingPosition = globalConfiguration.isSingleRandomStartPosition() ? rnd.nextInt(selectedPlayers.size() + 1) : globalConfiguration.getPlayerPosition();
                addTestFixedPosition(selectedPlayers, replay, null, myStartingPosition);
            } else {
                for (int testNumber = 0; testNumber < globalConfiguration.getSeedList().size() && testBroker.size() < codeCfg.getCap(); testNumber++) {
                    List<EnemyConfiguration> selectedPlayers = getRandomEnemies(codeCfg);
                    String seed = SeedCleaner.cleanSeed(globalConfiguration.getSeedList().get(testNumber), globalConfiguration.getMultiName(), selectedPlayers.size() + 1);
                    if (globalConfiguration.isEveryPositionConfiguration()) {
                        addTestAllPermutations(selectedPlayers, testNumber, seed);
                    } else {
                        int myStartingPosition = globalConfiguration.isSingleRandomStartPosition() ? rnd.nextInt(selectedPlayers.size() + 1) : globalConfiguration.getPlayerPosition();
                        addTestFixedPosition(selectedPlayers, testNumber, seed, myStartingPosition);
                    }
                }
            }
        }
    }

    private void addTestAllPermutations(List<EnemyConfiguration> selectedPlayers, int seedNumber, String seed) {
        List<EnemyConfiguration> players = new ArrayList<>(selectedPlayers);
        players.add(me);
        List<List<EnemyConfiguration>> permutations = generatePermutations(players);
        for (List<EnemyConfiguration> permutation : permutations) {
            testBroker.addTest(new TestInput(seedNumber, seed, permutation));
        }
    }

    private void addTestFixedPosition(List<EnemyConfiguration> selectedPlayers, int seedNumber, String seed, int myStartingPosition) {
        List<EnemyConfiguration> players = new ArrayList<>(selectedPlayers);
        players.add(myStartingPosition, me);
        testBroker.addTest(new TestInput(seedNumber, seed, players));
    }

    private List<List<EnemyConfiguration>> generatePermutations(List<EnemyConfiguration> original) {
        if (original.size() == 0) {
            List<List<EnemyConfiguration>> result = new ArrayList<>();
            result.add(new ArrayList<>());
            return result;
        }
        EnemyConfiguration firstElement = original.remove(0);
        List<List<EnemyConfiguration>> returnValue = new ArrayList<>();
        List<List<EnemyConfiguration>> permutations = generatePermutations(original);
        for (List<EnemyConfiguration> smallerPermuted : permutations) {
            for (int index = 0; index <= smallerPermuted.size(); index++) {
                List<EnemyConfiguration> temp = new ArrayList<>(smallerPermuted);
                temp.add(index, firstElement);
                returnValue.add(temp);
            }
        }
        return returnValue;
    }

    private List<EnemyConfiguration> getRandomEnemies(CodeConfiguration codeCfg) {
        List<EnemyConfiguration> selectedPlayers = new ArrayList<>();

        List<EnemyConfiguration> playerPool = new ArrayList<>(codeCfg.getEnemies());
        int pickSize = globalConfiguration.getMinEnemiesNumber() + rnd.nextInt(globalConfiguration.getEnemiesNumberDelta() + 1);

        for (int i = 0; i < pickSize; i++) {
            playerPool.stream().forEach(p -> p.setWeight(1D / Math.pow(p.getPicked() + 1, 3D)));
            double totalWeight = playerPool.stream().mapToDouble(EnemyConfiguration::getWeight).sum();
            playerPool.sort((a, b) -> b.getWeight().compareTo(a.getWeight()));
            double randomWeight = rnd.nextDouble() * totalWeight;
            double sumWeight = 0;
            for (EnemyConfiguration e : playerPool) {
                sumWeight += e.getWeight();
                if (sumWeight >= randomWeight) {
                    e.incrementPicked();
                    selectedPlayers.add(e);
                    playerPool.remove(e);
                    break;
                }
            }
        }

        return selectedPlayers;
    }

    private void checkConfiguration(GlobalConfiguration globalConfiguration) throws IllegalArgumentException {
        // Checks if every code file exists
        for (CodeConfiguration codeCfg : globalConfiguration.getCodeConfigurationList()) {
            if (!Files.isReadable(Paths.get(codeCfg.getSourcePath()))) {
                throw new IllegalArgumentException("Cannot read " + codeCfg.getSourcePath());
            }
        }

        // Checks write permission for final reports
        if (!Files.isWritable(Paths.get(""))) {
            throw new IllegalArgumentException("Cannot write in current directory");
        }

        // Checks account number
        if (globalConfiguration.getAccountConfigurationList().isEmpty()) {
            throw new IllegalArgumentException("You must provide at least one valid account");
        }

        // Checks that no account field is missing
        for (AccountConfiguration accountCfg : globalConfiguration.getAccountConfigurationList()) {
            if (accountCfg.getAccountName() == null) {
                throw new IllegalArgumentException("You must provide account name");
            }
            if (accountCfg.getAccountLogin() == null || accountCfg.getAccountPassword() == null) {
                throw new IllegalArgumentException("You must provide account login/pwd");
            }
        }

        // Checks that there are seeds to test
        if (!globalConfiguration.getRandomSeed() && globalConfiguration.getSeedList().isEmpty()) {
            throw new IllegalArgumentException("You must provide some seeds or enable randomSeed");
        }

        // Checks that there is a fixed seed list when playing with every starting position configuration
        if (globalConfiguration.getRandomSeed() && globalConfiguration.isEveryPositionConfiguration()) {
            throw new IllegalArgumentException("Playing each seed with swapped positions requires fixed seed list");
        }

        // Checks player position
        if (globalConfiguration.getPlayerPosition() == null || globalConfiguration.getPlayerPosition() < -2 || globalConfiguration.getPlayerPosition() > 3) {
            throw new IllegalArgumentException("You must provide a valid player position (-1, 0 or 1)");
        }
    }

    private GlobalConfiguration parseConfigurationFile(String cfgFilePath) throws FileNotFoundException {
        LOG.info("Loading configuration file : " + cfgFilePath);
        Yaml yaml = new Yaml(new Constructor(GlobalConfiguration.class));
        FileInputStream configFileInputStream = new FileInputStream(cfgFilePath);
        return yaml.load(configFileInputStream);
    }

    void pause() {
        this.pause.set(true);
    }

    void resume() {
        this.pause.set(false);
    }

}
