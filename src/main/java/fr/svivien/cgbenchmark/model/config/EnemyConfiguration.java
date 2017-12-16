package fr.svivien.cgbenchmark.model.config;

public class EnemyConfiguration {
    private Integer agentId;
    private String name;

    public EnemyConfiguration(Integer agentId, String name) {
        this.agentId = agentId;
        this.name = name;
    }

    public EnemyConfiguration(EnemyConfiguration other) {
        this.agentId = other.agentId;
        this.name = other.name;
    }

    public Integer getAgentId() {
        return agentId;
    }

    public String getName() {
        return name;
    }

}
