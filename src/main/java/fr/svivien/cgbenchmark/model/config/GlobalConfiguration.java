package fr.svivien.cgbenchmark.model.config;

import java.util.List;

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

    public String getMultiName() {
        return multiName;
    }

    public List<AccountConfiguration> getAccountConfigurationList() {
        return accountConfigurationList;
    }

    public List<String> getSeedList() {
        return seedList;
    }

    public Integer getRequestCooldown() {
        return requestCooldown;
    }

    public Boolean getRandomSeed() {
        return randomSeed;
    }

    public List<CodeConfiguration> getCodeConfigurationList() {
        return codeConfigurationList;
    }

    public Integer getPlayerPosition() {
        return playerPosition;
    }

    public boolean is1v1PlayedWithEachPositions() {
        return playerPosition == -1;
    }

    public boolean isFullRandomStartPosition() {
        return playerPosition == -2;
    }

    public Integer getMinEnemiesNumber() {
        return minEnemiesNumber;
    }

    public Integer getMaxEnemiesNumber() {
        return maxEnemiesNumber;
    }

    public Integer getEnemiesNumberDelta() {
        return maxEnemiesNumber - minEnemiesNumber;
    }
}
