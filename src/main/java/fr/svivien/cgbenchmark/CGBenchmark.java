package fr.svivien.cgbenchmark;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import fr.svivien.cgbenchmark.api.LoginApi;
import fr.svivien.cgbenchmark.api.SessionApi;
import fr.svivien.cgbenchmark.model.config.AccountConfiguration;
import fr.svivien.cgbenchmark.model.config.CodeConfiguration;
import fr.svivien.cgbenchmark.model.config.GlobalConfiguration;
import fr.svivien.cgbenchmark.model.request.login.LoginRequest;
import fr.svivien.cgbenchmark.model.request.login.LoginResponse;
import fr.svivien.cgbenchmark.model.request.session.SessionRequest;
import fr.svivien.cgbenchmark.model.request.session.SessionResponse;
import fr.svivien.cgbenchmark.model.test.ResultWrapper;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CGBenchmark {

    private static final Log LOG = LogFactory.getLog(CGBenchmark.class);

    private GlobalConfiguration globalConfiguration = null;
    private List<Consumer> accountConsumerList = new ArrayList<>();
    private Broker testBroker = new Broker();

    public CGBenchmark(String cfgFilePath) {
        // Parsing configuration file
        try {
            globalConfiguration = parseConfigurationFile(cfgFilePath);
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
            accountConsumerList.add(new Consumer(accountCfg.getAccountName(), testBroker, accountCfg.getAccountCookie(), accountCfg.getAccountIde(), globalConfiguration.getRequestCooldown()));
            LOG.info("Account " + accountCfg.getAccountName() + " successfully registered");
        }
    }

    public void launch() {
        // Launching tests
        for (CodeConfiguration codeCfg : globalConfiguration.getCodeConfigurationList()) {

            ExecutorService threadPool = Executors.newFixedThreadPool(accountConsumerList.size());

            Path p = Paths.get(codeCfg.getSourcePath());
            String codeName = p.getFileName().toString();

            // Brand new resultWrapper for this test
            ResultWrapper resultWrapper = new ResultWrapper(codeCfg);

            try {
                createTests(codeCfg);

                LOG.info("Launching " + testBroker.getTestSize() + " tests " + codeName + " against " + codeCfg.getEnemyAgentId() + "/" + codeCfg.getEnemyName() + " ...");

                // Adding consumers in the thread-pool and wiring fresh new resultWrapper
                for (Consumer consumer : accountConsumerList) {
                    consumer.setResultWrapper(resultWrapper);
                    threadPool.execute(consumer);
                }

                // Unleash the executor
                threadPool.shutdown();
                threadPool.awaitTermination(5, TimeUnit.DAYS);

                LOG.info("Final results :" + resultWrapper.getWinrateDetails());

                // Complete the report with all the results and final winrate
                resultWrapper.finishReport();

                // Write report to external file
                String reportFileName = codeName + "-" + codeCfg.getEnemyName() + "-" + codeCfg.getEnemyAgentId() + "-" + resultWrapper.getShortFilenameWinrate() + ".txt";
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
            if (globalConfiguration.getRandomSeed()) {
                testBroker.putTest(globalConfiguration.isPlayedWithEachPositions(), globalConfiguration.isPositionReversed(), replay, codeCfg.getEnemyAgentId(), null, codeContent, codeCfg.getLanguage());
            } else {
                for (int testNumber = 0; testNumber < globalConfiguration.getSeedList().size(); testNumber++) {
                    String seed = globalConfiguration.getSeedList().get(testNumber);
                    testBroker.putTest(globalConfiguration.isPlayedWithEachPositions(), globalConfiguration.isPositionReversed(), testNumber, codeCfg.getEnemyAgentId(), seed, codeContent, codeCfg.getLanguage());
                }
            }
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

        // Checks player position
        if (globalConfiguration.getPlayerPosition() == null || globalConfiguration.getPlayerPosition() < -1 || globalConfiguration.getPlayerPosition() > 1) {
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

}
