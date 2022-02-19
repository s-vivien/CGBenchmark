package fr.svivien.cgbenchmark.model.request.user;

/**
 * Response for a UserAPI
 */
public class UserResponse {

    public Codingamer codingamer;

    public class Codingamer {
        public String pseudo;
        public Integer userId;
    }
}
