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
- Java 21+ (compilation et runtime)
- Velocity proxy (plugin `grackle-velocity`)
- Paper/Spigot backends (plugin `grackle-paper`)
- (Optionnel mais recommandé) LuckPerms pour la gestion de tags d'équipes

---

## Stockage JSON
Les données sont sauvegardées dans le dossier `plugins/grackle/` sur le proxy : `categories.json`, `events.json`.

La sauvegarde est automatique à chaque création/suppression et au shutdown du proxy.

> **Note :** les IDs de parties sont générés de façon incrémentale et remis à zéro au redémarrage du proxy. Ne pas s'appuyer dessus comme identifiant persistant.

---

## Commandes — côté proxy (Velocity)

Ces commandes sont disponibles dans la console Velocity ou pour un joueur ayant les permissions appropriées :

| Commande | Alias | Permission | Description |
|---|---|---|---|
| `/grackle` | `/gk` | `grackle.admin` | Gestion des parties |
| `/grackle-event` | `/gkevent` | `grackle.event` | Créer/supprimer des events |
| `/grackle-category` | `/gkcat` | `grackle.category` | Créer/supprimer des catégories |

Usage (proxy) :
- `/grackle start <eventName> [serverName]`
- `/grackle join <gameId>`
- `/grackle leave <gameId>`
- `/grackle startGame <gameId>`
- `/grackle refreshTeams <gameId>`
- `/grackle end <gameId>`
- `/grackle setPosition <gameId> <nom> <position>`
- `/grackle status <gameId>`
- `/grackle-event create <nom> <solo|team> [options]`
- `/grackle-event list | info <nom> | delete <nom>`
- `/grackle-category create <nom> [description]`
- `/grackle-category list | delete <nom>`

---

## Commandes — côté Paper (backends)

Pour éviter les collisions avec d'autres plugins, les commandes Paper ont été namespacées :
- `/grackle` (alias `/gk`) — ouvre l'UI inventaire
- `/grackle-event` (alias `/gkevent`) — gestion des events (create/list/info/delete)
- `/grackle-category` (alias `/gkcategory`) — gestion des catégories (create/list/delete)

Ces commandes sont transmises au proxy via le plugin messaging channel `grackle:main`.

Usage (Paper) :
- `/grackle-event create <nom> <solo|team> [options]`
- `/grackle-event list`
- `/grackle-event info <nom>`
- `/grackle-event delete <nom>`
- `/grackle-category create <nom> [description]`
- `/grackle-category list`
- `/grackle-category delete <nom>`

Permissions :
- `grackle.ui` — ouvrir l'UI inventaire
- `grackle.event` — créer/supprimer des events
- `grackle.category` — créer/supprimer des catégories

---

## Options de création d'événement

Options communes :
- `--min <n>` — nombre minimum de participants (défaut : 2)
- `--max-players <n>` — nombre maximum de participants (`-1` = illimité)
- `--category <nom>` — catégorie de l'event
- `--desc <texte>` — description
- `--server <nom>` — serveur backend préféré pour héberger cet event

Options solo :
- `--mode <ranked|participation>` — mode de récompense (défaut : `ranked`)
- `--max <n>` — récompense maximale (1er)
- `--range <n>` — décrémentation par position
- `--participation <n>` — récompense minimale garantie

Options team :
- `--teams <n>` — nombre d'équipes (défaut : 2)
- `--random` — affectation aléatoire aux équipes
- `--winner <n>` — points pour l'équipe gagnante (TeamReward)
- `--loser <n>` — points pour les équipes perdantes (TeamReward)
- Ou `--max`/`--range`/`--participation` pour un DegressiveReward par équipe

---

## Tab-completion améliorée (Paper)
- TAB propose les sous-commandes (`create`, `list`, `info`, `delete`).
- Pour `create` : la 3ème position propose `solo` ou `team`.
- Après `delete` / `info` : TAB propose les noms d'events ou de catégories existants (depuis le cache synchronisé via `STATE_RESPONSE` / `STATE_UPDATE`).

---

## Mini-jeux — intégration et pratiques recommandées

### 1. Téléportation / zone de jeu
Grackle ne téléporte pas automatiquement. Lorsqu'une partie démarre, utilisez un listener côté backend qui écoute l'`ACTION_RESULT` avec `START_GAME` et exécute la logique de téléportation.

### 2. Changement de serveur (proxy)
Pour des parties isolées, déplacez les joueurs vers un serveur dédié via l'API Velocity (`player.createConnectionRequest(...)`). Vous pouvez renseigner le champ `server` sur l'event (option `--server` à la création) : Grackle transfèrera automatiquement le joueur initiateur vers ce serveur au démarrage de la partie.

### 3. Compatibilité avec d'autres plugins de minijeux
Grackle fournit métadonnées (events, teams, récompenses). Intégrer Grackle avec un plugin de minijeu implique généralement :
- Recevoir la notification de création/démarrage de partie
- Gérer la création d'une instance du jeu (map, zone)
- Gérer la téléportation et le swap de serveur
- Après fin de partie, appeler `/grackle end <gameId>` ou envoyer l'action `END_GAME` via messaging

Recommandations :
- Utilisez LuckPerms pour tags/permissions si vous comptez mapper des tags à des équipes (l'intégration côté Velocity est prévue dans `GameManager.playerHasTag`).
- Nommez vos commandes pour éviter collisions (c'est pourquoi Paper utilise `grackle-event` / `grackle-category`).

### 4. Exemple d'intégration simple
```java
// Paper : quand le joueur clique "Lancer" dans l'UI, le plugin envoie automatiquement START_GAME.
// Velocity : si l'event a un champ "server", le joueur est transféré automatiquement.

// Pour forcer le transfert côté Velocity via commande :
// /grackle start <eventName> <serverName>
```

---

## Dépannage rapide
- Si les commandes ne répondent pas, vérifiez les collisions de noms (utilisez `/grackle-event` au lieu de `/event`).
- Assurez-vous que la communication plugin messaging est autorisée et que les canaux sont enregistrés côté proxy et backend.
- Vérifiez que le proxy a les permissions d'écriture pour `plugins/grackle/`.
- Si le cache Paper est vide après connexion, vérifiez que le canal `grackle:main` est bien enregistré des deux côtés et que le délai de 1 seconde au `PlayerJoin` est suffisant (augmentez le `runTaskLater` si nécessaire).

---

## Exemples JSON

### `categories.json`
```json
[
  {
    "name": "PvP",
    "description": "Événements de combat entre joueurs"
  },
  {
    "name": "Aventure",
    "description": null
  }
]
```

### `events.json`
```json
[
  {
    "name": "BedWars",
    "type": "TEAM",
    "soloMode": null,
    "minParticipants": 4,
    "maxParticipants": 16,
    "category": "PvP",
    "description": "Détruisez le lit adverse et éliminez les équipes.",
    "server": "minigames-1",
    "reward": {
      "type": "TEAM",
      "winner": 10,
      "loser": 2
    },
    "teamConfig": {
      "teamCount": 4,
      "randomTeams": false,
      "fixedTeamSizes": {},
      "teamTags": {
        "grackle_team_red": "Red",
        "grackle_team_blue": "Blue"
      }
    }
  },
  {
    "name": "Spleef",
    "type": "SOLO",
    "soloMode": "RANKED",
    "minParticipants": 2,
    "maxParticipants": 8,
    "category": "PvP",
    "description": "Le dernier survivant gagne.",
    "server": null,
    "reward": {
      "type": "DEGRESSIVE",
      "max": 10,
      "range": 2,
      "participation": 1
    },
    "teamConfig": null
  }
]
```
