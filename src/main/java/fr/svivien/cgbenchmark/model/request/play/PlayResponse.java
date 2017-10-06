package fr.svivien.cgbenchmark.model.request.play;

import java.util.List;

/**
 * Response for a PLAY in the CG IDE
 */
public class PlayResponse {
    public PlayResponseSuccess success;
    public Error error;

    public class Error {
        public int id;
        public String type;
        public String message;
    }

    public class Frame {
        public int agentId;
        public String gameInformation;
    }

    public class PlayResponseSuccess {
        public List<Integer> scores;
        public List<Frame> frames;
        public int gameId;
    }
}
