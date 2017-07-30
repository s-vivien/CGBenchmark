package fr.svivien.cgbenchmark.model.config;

public class CodeConfiguration {

    private String sourcePath;
    private Integer nbReplays;
    private String language;
    private Integer enemyAgentId;
    private String enemyName = "UNKNOWN";

    public String getSourcePath() {
        return sourcePath;
    }

    public Integer getNbReplays() {
        return nbReplays;
    }

    public String getLanguage() {
        return language;
    }

    public Integer getEnemyAgentId() {
        return enemyAgentId;
    }

    public String getEnemyName() {
        return enemyName;
    }
}
