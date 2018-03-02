package fr.svivien.cgbenchmark.model.request.session;

/**
 * Response for a SessionAPI
 */
public class SessionResponse {

    public SessionResponseSuccess success;

    public class SessionResponseSuccess {
        public String handle; // handle for puzzles
        public String testSessionHandle; // handle for contests
    }
}
