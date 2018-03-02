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
    private Boolean isContest = false;
    private boolean saveLogs;

    public void setSaveLogs(boolean saveLogs) {
        this.saveLogs = saveLogs;
    }

    public boolean isSaveLogs() {
        return saveLogs;
    }

    public String getMultiName() {
        return multiName;
    }

    public Boolean isContest() {
        return isContest;
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

    public boolean isEveryPositionConfiguration() {
        return playerPosition == -1;
    }

    public boolean isSingleRandomStartPosition() {
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
