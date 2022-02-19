package fr.svivien.cgbenchmark.utils;

public class Constants {

    public static final String CG_HOST = "https://www.codingame.com";
    public static final String TIMEOUT_INFORMATION_PART = "timeout";
    public static final String TIMEOUT_BIS_INFORMATION_PART = "timed out";
    public static final String RESTRICTIONS_ERROR_MESSAGE = "You reached the limit of plays for a period of time.";
    public static final String INVALID_INFORMATION_PART = "is not a correct movement";
    public static final String INVALID_BIS_INFORMATION_PART = "invalid";
    public static final String CONTEST_SESSION_SERVICE_URL = "/services/ChallengeCandidateRemoteService/findChallengerByChallenge";
    public static final String PUZZLE_SESSION_SERVICE_URL = "/services/PuzzleRemoteService/generateSessionFromPuzzlePrettyId";
    public static final long RANDOM_SEED = 2820027331L;
    public static final int PLAY_MAX_RETRIES = 20;
    public static final String DOUBLE_FORMAT = "#0.00";
    public static final int[] COOLDOWNS_DURATION = {30, 45, 72, 108, 144};
    public static final String[] COOLDOWNS_NAMES = {"15 minutes", "1 hour", "3 hours", "6 hours", "24 hours"};
}
