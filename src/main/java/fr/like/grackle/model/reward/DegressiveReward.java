package fr.like.grackle.model.reward;

public class DegressiveReward implements RewardStrategy {

    private final int max;
    private final int range;
    private final int participation;

    public DegressiveReward(int max, int range, int participation) {
        this.max = max;
        this.range = range;
        this.participation = participation;
    }

    @Override
    public int getPoints(int position) {
        int calculated = max - range * (position - 1);
        return Math.max(calculated, participation);
    }

    @Override
    public String describe() {
        return "Dégressif (max=" + max + ", range=" + range + ", participation=" + participation + ")";
    }

    public int getMax() { return max; }
    public int getRange() { return range; }
    public int getParticipation() { return participation; }
}
