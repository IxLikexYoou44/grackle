package fr.like.grackle.model.reward;

import java.util.Map;
import java.util.TreeMap;

public class DefinedRankReward implements RewardStrategy {

    /** position → points. The last entry acts as the "other" fallback. */
    private final TreeMap<Integer, Integer> rankPoints;
    private final int otherPoints;

    public DefinedRankReward(Map<Integer, Integer> rankPoints, int otherPoints) {
        this.rankPoints = new TreeMap<>(rankPoints);
        this.otherPoints = otherPoints;
    }

    @Override
    public int getPoints(int position) {
        Integer pts = rankPoints.get(position);
        return pts != null ? pts : otherPoints;
    }

    @Override
    public String describe() {
        StringBuilder sb = new StringBuilder("Défini (");
        rankPoints.forEach((pos, pts) -> sb.append(pos).append("e=").append(pts).append("pts, "));
        sb.append("autre=").append(otherPoints).append("pts)");
        return sb.toString();
    }

    public TreeMap<Integer, Integer> getRankPoints() { return rankPoints; }
    public int getOtherPoints() { return otherPoints; }
}
