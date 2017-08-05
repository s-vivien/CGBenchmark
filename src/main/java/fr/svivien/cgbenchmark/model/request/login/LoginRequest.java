package fr.svivien.cgbenchmark.model.request.login;

import java.util.ArrayList;

/**
 * Request body for CG getSessionHandle
 */
public class LoginRequest extends ArrayList<Object> {

    public LoginRequest(String username, String password) {
        add(username);
        add(password);
        add(true);
    }

}
