package fr.svivien.cgbenchmark.model.config;

import lombok.Data;

@Data
public class EnemyConfiguration {
    private Integer agentId;
    private String name;
    private int picked = 0;
    private Double weight;

    public EnemyConfiguration() {
    }

    public EnemyConfiguration(Integer agentId, String name) {
        this.agentId = agentId;
        this.name = name;
    }

    public void incrementPicked() {
        this.picked++;
    }

}
