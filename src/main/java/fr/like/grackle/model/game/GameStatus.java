package fr.like.grackle.model.game;

public enum GameStatus {
    WAITING,       // en attente de joueurs
    REFRESH_TEAM,  // lecture des tags, affectation des équipes (command block trigger)
    IN_GAME,       // partie en cours
    ENDED          // partie terminée, récompenses distribuées
}
