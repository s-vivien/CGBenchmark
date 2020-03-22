package fr.svivien.cgbenchmark.model.config;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class EnemyConfiguration {

    @NotNull
    private Integer agentId;

    @NotNull
    private String name;

    private int picked = 0;

    private Double weight;

    public EnemyConfiguration() {
    }

    public EnemyConfiguration(EnemyConfiguration other) {
        this.agentId = other.agentId;
        this.name = other.name;
    }

    public EnemyConfiguration(Integer agentId, String name) {
        this.agentId = agentId;
        this.name = name;
    }

    public void incrementPicked() {
        this.picked++;
    }

}
