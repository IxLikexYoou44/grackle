package fr.like.grackle.messaging;

public enum MessageType {
    // Paper → Velocity
    REQUEST_STATE,   // demande l'état complet (events, catégories, parties)
    ACTION,          // joueur déclenche une action (start, join, end, etc.)

    // Velocity → Paper
    STATE_RESPONSE,  // réponse à REQUEST_STATE
    STATE_UPDATE,    // push après un changement d'état
    ACTION_RESULT    // résultat d'une action (succès / erreur + message)
}
