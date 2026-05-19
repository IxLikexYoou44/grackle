# Grackle

Plugin Velocity de gestion d'événements et de parties multijoueurs.

---

## Fonctionnement général

```
Velocity (Grackle)
 ├── gère les events, catégories, parties, récompenses
 └── communique avec les serveurs backend via des messages plugin
         └── Backend (Paper/Spigot)
              ├── UI (inventory) et commandes client → envoyé au proxy
              └── téléportation des joueurs / démarrage du jeu (géré côté backend)
```

Les téléportations (spawn de départ, retour) et l'hébergement des parties (serveur dédié) sont gérées par vos serveurs backend ou par un plugin de minijeu existant. Grackle gère la logique de matchmaking, métadonnées et distribution des récompenses.

---

## Exigences
- Java 17+ (compilation et runtime)
- Velocity proxy (plugin `grackle`)
- Paper/Spigot backends (plugin `grackle-paper`)
- (Optionnel mais recommandé) LuckPerms pour la gestion de tags d'équipes

---

## Stockage JSON
Les données sont sauvegardées dans le dossier `plugins/grackle/` sur le proxy : `categories.json`, `events.json`.

La sauvegarde est automatique à chaque création/suppression et au shutdown du proxy.

---

## Commandes (Paper/backends)
Pour éviter les collisions avec d'autres plugins, les commandes Paper ont été namespacées :
- `/grackle` (alias `/gk`) — ouvre l'UI
- `/grackle-event` (alias `gkevent`) — gestion des events (create/list/info/delete)
- `/grackle-category` (alias `gkcategory`) — gestion des catégories (create/list/delete)

Usage rapide (Paper):
- `/grackle-event create <name> <solo|team> [options]`
- `/grackle-event list`
- `/grackle-event info <name>`
- `/grackle-event delete <name>`
- `/grackle-category create <name> [description]`
- `/grackle-category list`
- `/grackle-category delete <name>`

Permissions:
- `grackle.ui` — ouvrir UI
- `grackle.event` — créer/supprimer events
- `grackle.category` — créer/supprimer catégories

---

## Tab-completion améliorée
- TAB propose les sous-commandes (`create`, `list`, `info`, `delete`).
- Pour `create` : la 3ème position propose `--category`, `--desc`, `--min`, `--max-players`, `--mode`, `--teams`, `--random`, `--winner`, `--loser`, `--range`, `--participation`.
- Après `--category`, TAB propose les catégories existantes (cache du backend).
- Les listes d'événements et catégories proviennent du cache synchronisé via le canal plugin messaging (STATE_RESPONSE / STATE_UPDATE).

---

## Mini-jeux — intégration et pratiques recommandées
1. Teleport / Zone de jeu
   - Grackle ne téléporte pas automatiquement. Lorsqu'une partie démarre, utilisez un listener serveur/back-end ou un plugin dédié qui écoute l'événement (ou le message via le canal interne) et téléporte les joueurs vers la zone de jeu.
   - Possibilité : créer un handler dans votre backend qui, à la réception d'un `ACTION_RESULT` indiquant `START_GAME`, exécute la logique de téléportation et change le monde si nécessaire.

2. Changement de serveur (proxy)
   - Pour des parties isolées, déplacez les joueurs vers un serveur dédié via l'API du proxy (Velocity: `player.createConnectionRequest(...)`). Grackle n'effectue pas le transfert automatiquement mais fournit l'ID de la partie et les métadonnées nécessaires.

3. Compatibilité avec d'autres plugins de minijeux
   - Grackle fournit métadonnées (events, teams, récompenses). Intégrer Grackle avec un plugin de minijeu implique généralement :
     - Recevoir la notification de création/démarrage de partie
     - Gérer la création d'une instance du jeu (map, zone)
     - Gérer la téléportation et le swap de serveur
     - Après fin de partie, appeler `/grackle end <gameId>` ou envoyer l'action équivalente via messaging
   - Recommandations :
     - Utilisez LuckPerms pour tags/permissions si vous comptez mapper des tags à des équipes.
     - Nommez vos commandes pour éviter collisions (c'est pourquoi Paper utilise `grackle-event` / `grackle-category`).

4. Exemple d'intégration simple (pseudo)
```java
// Backend listener: quand l'utilisateur clique "Lancer une partie" dans l'UI
plugin.getMessagingHandler().requestState(player); // already done by UI
// Paper UI includes an optional 'server' field on the event JSON; when present the proxy will
// forward the initiating player to that server on START_GAME. Alternatively, you can run
// on the proxy: /grackle start <eventName> <serverName> to create a game and move the command player.

// Example (Velocity side):
proxy.getPlayer(uuid).ifPresent(p -> p.createConnectionRequest(targetServer).connect());
```

---

## Dépannage rapide
- Si les commandes ne répondent pas, vérifiez collisions de noms (utilisez `/grackle-event` au lieu de `/event`).
- Assurez-vous que la communication plugin messaging est autorisée et que les canaux sont enregistrés côté proxy et backend.
- Vérifiez que le proxy a les permissions d'écriture pour `plugins/grackle/`.

---

## Exemple `events.json` et `categories.json`
(identiques aux exemples fournis dans la documentation du code). 

---

Besoin d'exemples concrets de code pour téléporter / changer de serveur automatiquement après le START_GAME ? Dites "Oui" et je fournis un exemple minimal pour Velocity + Paper.