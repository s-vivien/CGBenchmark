package fr.svivien.cgbenchmark.model.config;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class GlobalConfiguration {

    @Size(min = 1)
    @Valid
    private List<AccountConfiguration> accountConfigurationList;

    private List<String> seedList;

    @NotNull
    private Boolean randomSeed;

    @NotNull
    private Integer playerPosition;

    @NotNull
    @Min(1)
    @Max(3)
    private Integer minEnemiesNumber;

    @NotNull
    @Min(1)
    @Max(3)
    private Integer maxEnemiesNumber;

    @NotNull
    @Size(min = 1)
    @Valid
    private List<CodeConfiguration> codeConfigurationList;

    private Integer defaultNbReplays;

    private String defaultLanguage;

    @Valid
    private List<EnemyConfiguration> defaultEnemies;

    @NotNull
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
