package fr.svivien.cgbenchmark.model.test;

/**
 * Test input data
 */
public class TestInput {

    private int seedNumber;
    private int agentId;
    private String seed;
    private String code;
    private String lang;
    private boolean reverse;

    public TestInput(int seedNumber, int agentId, String seed, String code, String lang, boolean reverse) {
        this.seedNumber = seedNumber;
        this.agentId = agentId;
        this.seed = seed;
        this.code = code;
        this.lang = lang;
        this.reverse = reverse;
    }

    public int getSeedNumber() {
        return seedNumber;
    }

    public int getAgentId() {
        return agentId;
    }

    public String getSeed() {
        return seed;
    }

    public String getCode() {
        return code;
    }

    public String getLang() {
        return lang;
    }

    public boolean isReverse() {
        return reverse;
    }
}
