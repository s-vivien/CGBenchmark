package fr.svivien.cgbenchmark;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import fr.svivien.cgbenchmark.api.LoginApi;
import fr.svivien.cgbenchmark.api.SessionApi;
import fr.svivien.cgbenchmark.model.config.AccountConfiguration;
import fr.svivien.cgbenchmark.model.config.CodeConfiguration;
import fr.svivien.cgbenchmark.model.config.EnemyConfiguration;
import fr.svivien.cgbenchmark.model.config.GlobalConfiguration;
import fr.svivien.cgbenchmark.model.request.login.LoginRequest;
import fr.svivien.cgbenchmark.model.request.login.LoginResponse;
import fr.svivien.cgbenchmark.model.request.session.SessionRequest;
import fr.svivien.cgbenchmark.model.request.session.SessionResponse;
import fr.svivien.cgbenchmark.model.test.ResultWrapper;
import fr.svivien.cgbenchmark.model.test.TestInput;
import fr.svivien.cgbenchmark.producerconsumer.Broker;
import fr.svivien.cgbenchmark.producerconsumer.Consumer;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import retrofit2.Call;
import retrofit2.GsonConverterFactory;
import retrofit2.Retrofit;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class CGBenchmark {

    private static final Log LOG = LogFactory.getLog(CGBenchmark.class);

    private GlobalConfiguration globalConfiguration = null;
    private List<Consumer> accountConsumerList = new ArrayList<>();
    private Broker testBroker = new Broker();
    private Random rnd = new Random();
    private EnemyConfiguration me = new EnemyConfiguration(-1, "[ME]");
    private AtomicBoolean pause = new AtomicBoolean(false);

    public CGBenchmark(String cfgFilePath, boolean saveLogs) {
        // Parsing configuration file
        try {
            globalConfiguration = parseConfigurationFile(cfgFilePath);
            globalConfiguration.setSaveLogs(saveLogs);
            checkConfiguration(globalConfiguration);
        } catch (UnsupportedEncodingException | FileNotFoundException | JsonIOException | JsonSyntaxException e) {
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
                retrieveAccountCookieAndSession(accountCfg, globalConfiguration.getMultiName());
            } catch (IllegalStateException e) {
                LOG.fatal("Error while retrieving account cookie and session", e);
                System.exit(1);
            }
            accountConsumerList.add(new Consumer(accountCfg.getAccountName(), testBroker, accountCfg.getAccountCookie(), accountCfg.getAccountIde(), globalConfiguration.getRequestCooldown(), pause, globalConfiguration.isSaveLogs()));
            LOG.info("Account " + accountCfg.getAccountName() + " successfully registered");
        }
    }

    public void launch() {
        // Launching tests
        for (CodeConfiguration codeCfg : globalConfiguration.getCodeConfigurationList()) {

            // Enemies are sorted by agentId to keep report filename consistent
            codeCfg.getEnemies().sort((a, b) -> a.getAgentId() - b.getAgentId());

            ExecutorService threadPool = Executors.newFixedThreadPool(accountConsumerList.size());

            Path p = Paths.get(codeCfg.getSourcePath());
            String codeName = p.getFileName().toString();

            // Brand new resultWrapper for this test
            ResultWrapper resultWrapper = new ResultWrapper(codeCfg);

            try {
                createTests(codeCfg);

                String logStr = "Launching " + testBroker.getTestSize() + " tests " + codeName + " against";
                for (EnemyConfiguration ec : codeCfg.getEnemies()) {
                    logStr += " " + ec.getName() + "_" + ec.getAgentId();
                }
                LOG.info(logStr);

                // Adding consumers in the thread-pool and wiring fresh new resultWrapper
                for (Consumer consumer : accountConsumerList) {
                    consumer.setResultWrapper(resultWrapper);
                    threadPool.execute(consumer);
                }

                // Unleash the executor
                threadPool.shutdown();
                threadPool.awaitTermination(5, TimeUnit.DAYS); // If 5 days is not enough, you're doing it wrong

                LOG.info("Final results :" + resultWrapper.getWinrateDetails());

                // Complete the report with all the results and final winrate
                resultWrapper.finishReport();

                // Write report to external file
                String reportFileName = codeName + "-" + resultWrapper.getShortFilenameWinrate();

                // Add suffix to avoid overwriting existing report file
                File file = new File(reportFileName + ".txt");
                if (file.exists() && !file.isDirectory()) {
                    int suffix = -1;
                    do {
                        suffix++;
                        file = new File(reportFileName + "_" + suffix + ".txt");
                    } while (file.exists() && !file.isDirectory());
                    reportFileName += "_" + suffix + ".txt";
                }

                LOG.info("Writing final report to : " + reportFileName);
                try (PrintWriter out = new PrintWriter(reportFileName)) {
                    out.println(resultWrapper.getReportBuilder().toString());
                } catch (Exception e) {
                    LOG.warn("An error has occurred when writing final report", e);
                }
            } catch (IOException e) {
                LOG.error("An error has occurred while reading source for " + codeCfg.getSourcePath(), e);
            } catch (InterruptedException e) {
                LOG.error("An error has occurred within the broker/executor", e);
            }
        }

        LOG.info("No more tests. Ending.");
    }

    private void retrieveAccountCookieAndSession(AccountConfiguration accountCfg, String multiName) {
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

        if (loginResponse.body().success == null || loginResponse.body().success.userId == null) {
            throw new IllegalStateException("Login failed, please check login/pwd in configuration");
        }

        // Selecting appropriate cookie; we keep the one that expires the later
        Optional<Cookie> optCookie = loginResponse.headers().values(Constants.SET_COOKIE).stream()
                .map(c -> Cookie.parse(HttpUrl.parse(Constants.CG_HOST), c))
                .filter(c -> c.name().equals(Constants.REMCG) && c.expiresAt() > new Date().getTime())
                .sorted((a, b) -> (int) (b.expiresAt() - a.expiresAt()))
                .findFirst();

        if (!optCookie.isPresent()) {
            throw new IllegalStateException("Cannot find required cookie in getSessionHandle response");
        }

        // Setting the cookie in the account configuration
        accountCfg.setAccountCookie(optCookie.get().toString());

        SessionApi sessionApi = retrofit.create(SessionApi.class);
        SessionRequest sessionRequest = new SessionRequest(loginResponse.body().success.userId, multiName);
        Call<SessionResponse> sessionCall = sessionApi.getSessionHandle(sessionRequest, Constants.CG_HOST + "/puzzle/" + multiName, accountCfg.getAccountCookie());
        retrofit2.Response<SessionResponse> sessionResponse;
        try {
            sessionResponse = sessionCall.execute();
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Session request failed");
        }

        // Setting the IDE session in the account configuration
        accountCfg.setAccountIde(sessionResponse.body().success.handle);
    }

    private void createTests(CodeConfiguration codeCfg) throws IOException, InterruptedException {
        String codeContent = new String(Files.readAllBytes(Paths.get(codeCfg.getSourcePath())));

        // Filling the broker with all the tests
        for (int replay = 0; replay < codeCfg.getNbReplays(); replay++) {
            rnd.setSeed(28731L); /** More arbitrary values ... */

            if (globalConfiguration.getRandomSeed()) {
                List<EnemyConfiguration> selectedPlayers = getRandomEnemies(codeCfg);
                int myStartingPosition = globalConfiguration.isSingleRandomStartPosition() ? rnd.nextInt(selectedPlayers.size() + 1) : globalConfiguration.getPlayerPosition();
                addTestFixedPosition(selectedPlayers, replay, null, codeContent, codeCfg.getLanguage(), myStartingPosition);
            } else {
                for (int testNumber = 0; testNumber < globalConfiguration.getSeedList().size(); testNumber++) {
                    List<EnemyConfiguration> selectedPlayers = getRandomEnemies(codeCfg);
                    String seed = SeedCleaner.cleanSeed(globalConfiguration.getSeedList().get(testNumber), globalConfiguration.getMultiName(), selectedPlayers.size() + 1);
                    if (globalConfiguration.isEveryPositionConfiguration()) {
                        addTestAllPermutations(selectedPlayers, testNumber, seed, codeContent, codeCfg.getLanguage());
                    } else {
                        int myStartingPosition = globalConfiguration.isSingleRandomStartPosition() ? rnd.nextInt(selectedPlayers.size() + 1) : globalConfiguration.getPlayerPosition();
                        addTestFixedPosition(selectedPlayers, testNumber, seed, codeContent, codeCfg.getLanguage(), myStartingPosition);
                    }
                }
            }
        }
    }

    private void addTestAllPermutations(List<EnemyConfiguration> selectedPlayers, int seedNumber, String seed, String codeContent, String lang) throws InterruptedException {
        List<EnemyConfiguration> players = selectedPlayers.stream().collect(Collectors.toList());
        players.add(me);
        List<List<EnemyConfiguration>> permutations = generatePermutations(players);
        for (List<EnemyConfiguration> permutation : permutations) {
            testBroker.queue.put(new TestInput(seedNumber, seed, codeContent, lang, permutation));
        }
    }

    private void addTestFixedPosition(List<EnemyConfiguration> selectedPlayers, int seedNumber, String seed, String codeContent, String lang, int myStartingPosition) throws InterruptedException {
        List<EnemyConfiguration> players = selectedPlayers.stream().collect(Collectors.toList());
        players.add(myStartingPosition, me);
        testBroker.queue.put(new TestInput(seedNumber, seed, codeContent, lang, players));
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
        List<EnemyConfiguration> selectedPlayers = codeCfg.getEnemies().stream().collect(Collectors.toList());
        Collections.shuffle(selectedPlayers, rnd);
        if (globalConfiguration.getEnemiesNumberDelta() > 0) {
            return selectedPlayers.subList(0, globalConfiguration.getMinEnemiesNumber() + rnd.nextInt(globalConfiguration.getEnemiesNumberDelta() + 1));
        } else {
            return selectedPlayers.subList(0, globalConfiguration.getMinEnemiesNumber());
        }
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

    private GlobalConfiguration parseConfigurationFile(String cfgFilePath) throws UnsupportedEncodingException, FileNotFoundException {
        LOG.info("Loading configuration file : " + cfgFilePath);
        Gson gson = new Gson();
        FileInputStream configFileInputStream = new FileInputStream(cfgFilePath);
        JsonReader reader = new JsonReader(new InputStreamReader(configFileInputStream, "UTF-8"));
        return gson.fromJson(reader, GlobalConfiguration.class);
    }

    public void pause() {
        this.pause.set(true);
    }

    public void resume() {
        this.pause.set(false);
    }

}
