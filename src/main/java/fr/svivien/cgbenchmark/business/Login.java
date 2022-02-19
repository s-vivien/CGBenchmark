package fr.svivien.cgbenchmark.business;

import fr.svivien.cgbenchmark.api.SessionApi;
import fr.svivien.cgbenchmark.api.UserApi;
import fr.svivien.cgbenchmark.model.CGAccount;
import fr.svivien.cgbenchmark.model.config.AccountConfiguration;
import fr.svivien.cgbenchmark.model.config.GlobalConfiguration;
import fr.svivien.cgbenchmark.model.request.session.SessionRequest;
import fr.svivien.cgbenchmark.model.request.session.SessionResponse;
import fr.svivien.cgbenchmark.model.request.user.UserRequest;
import fr.svivien.cgbenchmark.model.request.user.UserResponse;
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

    public static CGAccount retrieveAccountCookieAndSession(AccountConfiguration accountCfg, GlobalConfiguration globalConfiguration) {
        LOG.info("Retrieving session for account " + accountCfg.getAccountId());
        OkHttpClient client = new OkHttpClient.Builder().readTimeout(600, TimeUnit.SECONDS).build();
        Retrofit retrofit = new Retrofit.Builder().client(client).baseUrl(Constants.CG_HOST).addConverterFactory(GsonConverterFactory.create()).build();
        CGAccount account = retrieveCGAccount(retrofit, accountCfg);
        String handle = retrieveHandle(retrofit, account.getUserId(), account.getAccountCookie(), globalConfiguration);
        account.setAccountIde(handle);
        client.connectionPool().evictAll();
        return account;
    }

    private static CGAccount retrieveCGAccount(Retrofit retrofit, AccountConfiguration accountCfg) {
        UserApi userApi = retrofit.create(UserApi.class);
        UserRequest userRequest = new UserRequest(accountCfg.getAccountId());
        Call<UserResponse> userCall;

        CGAccount cgAccount = new CGAccount(accountCfg.getRememberMe());
        userCall = userApi.retrieveAccountData(userRequest, cgAccount.getAccountCookie());

        retrofit2.Response<UserResponse> userResponse;
        try {
            userResponse = userCall.execute();
            UserResponse body = userResponse.body();
            if (body == null || body.codingamer == null) {
                throw new IllegalStateException("User scrapping failed, please check account configuration");
            }
            cgAccount.setAccountName(body.codingamer.pseudo);
            cgAccount.setUserId(body.codingamer.userId);
        } catch (IOException e) {
            throw new IllegalStateException("Error while retrieving account data", e);
        }

        return cgAccount;
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
