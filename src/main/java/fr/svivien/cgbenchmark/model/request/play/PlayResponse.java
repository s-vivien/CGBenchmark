package fr.svivien.cgbenchmark.model.request.play;

import java.util.List;

/**
 * Response for a PLAY in the CG IDE
 */
public class PlayResponse {
    public List<Integer> scores;
    public List<Frame> frames;
    public List<String> tooltips;
    public long gameId;
    public String message;

    public class Frame {
        public int agentId;
        public String gameInformation;
        public String summary;
        public String stderr;
        public Error error;
    }

    public class Tooltip {
        public int event;
    }

    public class Error {
        public int line;
        public String message;
    }
}
