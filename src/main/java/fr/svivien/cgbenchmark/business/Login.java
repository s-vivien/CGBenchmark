package fr.svivien.cgbenchmark.business;

import fr.svivien.cgbenchmark.api.LoginApi;
import fr.svivien.cgbenchmark.api.SessionApi;
import fr.svivien.cgbenchmark.model.config.AccountConfiguration;
import fr.svivien.cgbenchmark.model.config.GlobalConfiguration;
import fr.svivien.cgbenchmark.model.request.login.LoginRequest;
import fr.svivien.cgbenchmark.model.request.login.LoginResponse;
import fr.svivien.cgbenchmark.model.request.session.SessionRequest;
import fr.svivien.cgbenchmark.model.request.session.SessionResponse;
import fr.svivien.cgbenchmark.utils.Constants;
import okhttp3.OkHttpClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Login {

    private static final Log LOG = LogFactory.getLog(Login.class);

    public static void retrieveAccountCookieAndSession(AccountConfiguration accountCfg, GlobalConfiguration globalConfiguration) {
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

        if (loginResponse.body() == null || loginResponse.body().userId == null) {
            throw new IllegalStateException("Login failed, please check login/pwd in configuration");
        }

        String cookie = String.join("; ", loginResponse.headers().values(Constants.SET_COOKIE));
        // Setting the cookie in the account configuration
        accountCfg.setAccountCookie(cookie);

        // Retrieving IDE handle
        String handle = retrieveHandle(retrofit, loginResponse.body().userId, accountCfg.getAccountCookie(), globalConfiguration);

        // Setting the IDE session in the account configuration
        accountCfg.setAccountIde(handle);
    }

    private static String retrieveHandle(Retrofit retrofit, Integer userId, String accountCookie, GlobalConfiguration globalConfiguration) {
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
                return sessionResponse.body().testSessionHandle;
            } else {
                return sessionResponse.body().handle;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error while retrieving session handle", e);
        }
    }

}
