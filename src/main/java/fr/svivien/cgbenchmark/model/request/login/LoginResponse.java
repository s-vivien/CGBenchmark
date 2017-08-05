package fr.svivien.cgbenchmark.model.request.login;

/**
 * Response for CG getSessionHandle
 */
public class LoginResponse {

    public LoginResponseSuccess success;

    public class LoginResponseSuccess {
        public Integer userId;
    }
}
