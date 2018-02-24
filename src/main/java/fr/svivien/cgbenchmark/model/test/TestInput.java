package fr.svivien.cgbenchmark.model.test;

import fr.svivien.cgbenchmark.model.config.EnemyConfiguration;

import java.util.List;

/**
 * Test input data
 */
public class TestInput {

    private int seedNumber;
    private String seed;
    private String code;
    private String lang;
    private List<EnemyConfiguration> players;

    public TestInput(int seedNumber, String seed, String code, String lang, List<EnemyConfiguration> players) {
        this.seedNumber = seedNumber;
        this.seed = seed;
        this.code = code;
        this.lang = lang;
        this.players = players;
    }

    public int getSeedNumber() {
        return seedNumber;
    }

    public String getSeed() {
        return seed;
    }

    public String getCode() {
        return code;
    }

    public List<EnemyConfiguration> getPlayers() {
        return players;
    }

    public String getLang() {
        return lang;
    }
}
