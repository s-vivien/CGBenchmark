package fr.svivien.cgbenchmark.model.config;

import lombok.Data;

import java.util.List;

@Data
public class CodeConfiguration {

    private String sourcePath;
    private Integer nbReplays;
    private Integer cap = 99999999;
    private String language;
    private List<EnemyConfiguration> enemies;

}
