package fr.svivien.cgbenchmark.utils;

public class Constants {

    public static final String CG_HOST = "https://www.codingame.com";
    public static final String SET_COOKIE = "Set-Cookie";
    public static final String TIMEOUT_INFORMATION_PART = "timeout";
    public static final String INVALID_INFORMATION_PART = "invalid";
    public static final String CONTEST_SESSION_SERVICE_URL = "/services/ChallengeCandidateRemoteService/findChallengerByChallenge";
    public static final String PUZZLE_SESSION_SERVICE_URL = "/services/PuzzleRemoteService/generateSessionFromPuzzlePrettyId";
    public static final long RANDOM_SEED = 2820027331L;
    public static final int PLAY_MAX_RETRIES = 20;
    public static final int PLAY_TRIES_BEFORE_DEGRADED = 10;
    public static final int PLAY_ERROR_RETRY_COOLDOWN = 20000;
    public static final int PLAY_ERROR_RETRY_DEGRADED_COOLDOWN = 40000;
    public static final String DOUBLE_FORMAT = "#0.00";

}
