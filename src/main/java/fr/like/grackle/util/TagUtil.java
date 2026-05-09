package fr.like.grackle.util;

/**
 * Conventions de nommage des tags utilisés par le plugin.
 *
 * Préfixe équipe : "grackle_team_<teamName>"
 * Exemple prophunt : grackle_team_hunter, grackle_team_prop
 *
 * Le command block exécute /grackle refreshTeams <gameId>
 * pour déclencher la lecture des tags et l'affectation des équipes.
 */
public final class TagUtil {

    public static final String TEAM_TAG_PREFIX = "grackle_team_";

    private TagUtil() {}

    public static String teamTag(String teamName) {
        return TEAM_TAG_PREFIX + teamName.toLowerCase();
    }

    public static boolean isTeamTag(String tag) {
        return tag.startsWith(TEAM_TAG_PREFIX);
    }

    public static String teamNameFromTag(String tag) {
        if (!isTeamTag(tag)) return null;
        return tag.substring(TEAM_TAG_PREFIX.length());
    }
}
