package fr.svivien.cgbenchmark.model.config;

import java.util.List;

public class GlobalConfiguration {

    private List<AccountConfiguration> accountConfigurationList;
    private List<String> seedList;
    private Integer requestCooldown;
    private Boolean randomSeed;
    private Integer playerPosition;
    private List<CodeConfiguration> codeConfigurationList;
    private String multiName;

    public String getMultiName() {
        return multiName;
    }

    public List<AccountConfiguration> getAccountConfigurationList() {
        return accountConfigurationList;
    }

    public Integer getPlayerPosition() {
        return playerPosition;
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

    public boolean isPlayedWithEachPositions() {
        return playerPosition == -1;
    }

    public boolean isPositionReversed() {
        return playerPosition == 1;
    }
}
