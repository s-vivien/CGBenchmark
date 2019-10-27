package fr.svivien.cgbenchmark.model.test;

import fr.svivien.cgbenchmark.model.config.EnemyConfiguration;
import lombok.Data;

import java.util.List;

/**
 * Test input data
 */
@Data
public class TestInput {

    private int seedNumber;
    private String seed;
    private List<EnemyConfiguration> players;

    public TestInput(int seedNumber, String seed, List<EnemyConfiguration> players) {
        this.seedNumber = seedNumber;
        this.seed = seed;
        this.players = players;
    }
}
