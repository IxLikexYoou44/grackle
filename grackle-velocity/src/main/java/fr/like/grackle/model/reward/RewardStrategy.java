package fr.like.grackle.model.reward;

public interface RewardStrategy {
    /**
     * Returns the points for a given position (1-based).
     * For team rewards, position 1 = winner, position 2 = loser.
     */
    int getPoints(int position);

    String describe();
}
