package fr.svivien.cgbenchmark.model.config;

public class EnemyConfiguration {
    private Integer agentId;
    private String name;
    private int picked = 0;
    private Double weight;

    public EnemyConfiguration(Integer agentId, String name) {
        this.agentId = agentId;
        this.name = name;
    }

    public Integer getAgentId() {
        return agentId;
    }

    public String getName() {
        return name;
    }

    public int getPicked() {
        return picked;
    }

    public void incrementPicked() {
        this.picked++;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
}
