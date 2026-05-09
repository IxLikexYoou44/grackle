package fr.like.grackle.model.reward;

public class TeamReward implements RewardStrategy {

    private final int winnerPoints;
    private final int loserPoints;

    public TeamReward(int winnerPoints, int loserPoints) {
        this.winnerPoints = winnerPoints;
        this.loserPoints = loserPoints;
    }

    /** position 1 = gagnant, tout autre = perdant */
    @Override
    public int getPoints(int position) {
        return position == 1 ? winnerPoints : loserPoints;
    }

    @Override
    public String describe() {
        return "Équipe (gagnant=" + winnerPoints + "pts, perdant=" + loserPoints + "pts)";
    }

    public int getWinnerPoints() { return winnerPoints; }
    public int getLoserPoints() { return loserPoints; }
}
