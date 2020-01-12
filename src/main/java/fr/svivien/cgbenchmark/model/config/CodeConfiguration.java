package fr.svivien.cgbenchmark.model.config;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class CodeConfiguration {

    @NotNull
    private String sourcePath;

    @NotNull
    @Min(1)
    private Integer nbReplays;

    private Integer cap = 99999999;

    @NotNull
    private String language;

    @NotNull
    @Size(min = 1)
    @Valid
    private List<EnemyConfiguration> enemies;

}
