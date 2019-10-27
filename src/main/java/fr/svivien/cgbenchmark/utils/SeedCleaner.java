package fr.svivien.cgbenchmark.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Util class for seed cleaning
 */
public class SeedCleaner {

    private final static Map<String, Pattern> patterns;

    static {
        patterns = new HashMap<>();
        patterns.put("tron-battle", Pattern.compile("(\\(\\d+,\\d+\\))"));
    }

    public static String cleanSeed(String originalSeed, String multiName, int playerNumber) {
        String cleaned = originalSeed;
        if (patterns.get(multiName) != null) {
            Matcher m = patterns.get(multiName).matcher(originalSeed);
            cleaned = "";
            for (int i = 0; i < playerNumber && m.find(); i++) {
                cleaned += m.group();
            }
        }
        return cleaned;
    }

}
