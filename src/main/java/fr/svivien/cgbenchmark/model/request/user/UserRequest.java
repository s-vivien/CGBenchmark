package fr.svivien.cgbenchmark.model.request.user;

import java.util.ArrayList;

/**
 * Request body for a UserAPI
 */
public class UserRequest extends ArrayList<Object> {

    public UserRequest(String handle) {
        add(handle);
    }

}
