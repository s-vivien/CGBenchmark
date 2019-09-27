package fr.svivien.cgbenchmark.model.config;

import lombok.Data;

import java.util.List;

@Data
public class GlobalConfiguration {

    private List<AccountConfiguration> accountConfigurationList;
    private List<String> seedList;
    private Integer requestCooldown;
    private Boolean randomSeed;
    private Integer playerPosition;
    private Integer minEnemiesNumber;
    private Integer maxEnemiesNumber;
    private List<CodeConfiguration> codeConfigurationList;
    private String multiName;
    private Boolean isContest = false;
    private boolean saveLogs;

    public boolean isEveryPositionConfiguration() {
        return playerPosition == -1;
    }

    public boolean isSingleRandomStartPosition() {
        return playerPosition == -2;
    }

    public Integer getEnemiesNumberDelta() {
        return maxEnemiesNumber - minEnemiesNumber;
    }
}
