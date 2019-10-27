package fr.svivien.cgbenchmark.business.result;

import fr.svivien.cgbenchmark.utils.Constants;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;

/**
 * Holds separated position statistics for each type of matchup (1v1, 1v2, 1v3)
 */
public class PositionStats {

    private static final String positionsOutputFormat = "    Positions (%d players)    %-12s %-12s %-12s %-12s";
    private static final NumberFormat doubleFormatter = new DecimalFormat(Constants.DOUBLE_FORMAT);

    /**
     * first dimension : player number
     * second dimension : 0 : total, 1 2 3 4 : position wins
     */
    private final int[][] positions;

    PositionStats(int maxEnemies) {
        positions = new int[maxEnemies][maxEnemies + 2];
    }

    void addStat(int playerNumber, int rank) {
        positions[playerNumber - 2][0]++;
        positions[playerNumber - 2][rank]++;
    }

    private int getGamesTotal(int playerNumber) {
        return positions[playerNumber - 2][0];
    }

    private double getStat(int playerNumber, int position) {
        return positions[playerNumber - 2][position] != 0 ? 100.0 * (((double) positions[playerNumber - 2][position]) / positions[playerNumber - 2][0]) : 0;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        String[] winrates = new String[4];
        Arrays.fill(winrates, ""); // Using String array to handle increasing number of players, and increasing number of winrates to display
        for (int playerNumber = 2; playerNumber <= positions.length + 1; playerNumber++) {
            if (getGamesTotal(playerNumber) > 0) {
                for (int position = 1; position <= playerNumber; position++) {
                    winrates[position - 1] = position + ": " + doubleFormatter.format(getStat(playerNumber, position)) + "%";
                }
                if (builder.length() > 0) builder.append(System.lineSeparator());
                builder.append(String.format(positionsOutputFormat, playerNumber, winrates[0], winrates[1], winrates[2], winrates[3]));
            }
        }
        return builder.toString();
    }
}
