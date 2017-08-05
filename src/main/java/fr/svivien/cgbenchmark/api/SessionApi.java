package fr.svivien.cgbenchmark.api;

import fr.svivien.cgbenchmark.Constants;
import fr.svivien.cgbenchmark.model.request.session.SessionRequest;
import fr.svivien.cgbenchmark.model.request.session.SessionResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface SessionApi {
    @POST("/services/PuzzleRemoteService/generateSessionFromPuzzlePrettyId")
    @Headers({
            "Host: www.codingame.com",
            "Connection: keep-alive",
            "Content-Length: 256",
            "Accept: application/json, text/plain, */*",
            "Origin: " + Constants.CG_HOST,
            "User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.97 Safari/537.36",
            "Content-Type: application/json;charset=UTF-8",
            "Accept-Encoding: deflate",
            "Accept-Language: fr-FR,fr;q=0.8,en-US;q=0.6,en;q=0.4",
    })
    Call<SessionResponse> getSessionHandle(@Body SessionRequest body, @Header("Referer") String referer, @Header("Cookie") String userCookie);
}
