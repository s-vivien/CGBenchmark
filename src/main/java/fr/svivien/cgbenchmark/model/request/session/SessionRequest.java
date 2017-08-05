package fr.svivien.cgbenchmark.model.request.session;

import java.util.ArrayList;

/**
 * Request body for a SessionAPI
 */
public class SessionRequest extends ArrayList<Object> {

    public SessionRequest(int userId, String multiName) {
        add(userId);
        add(multiName);
        add(false);
    }

}
