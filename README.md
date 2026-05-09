# Grackle

Plugin Velocity de gestion d'événements et de parties multijoueurs.

---

## Fonctionnement général

```
Velocity (Grackle)
 ├── gère les events, catégories, parties, récompenses
 └── communique avec les serveurs backend via des commandes
         └── Backend (Paper/Spigot)
              ├── command blocks → /grackle refreshTeams <gameId>
              └── téléportation des joueurs (géré côté backend)
```

Les téléportations (spawn de départ, retour) sont déléguées au serveur backend. Grackle se charge uniquement de la logique de partie côté proxy.

---

## Stockage JSON

Les données sont sauvegardées dans le dossier `plugins/grackle/` :

| Fichier | Contenu |
|---|---|
| `categories.json` | Liste des catégories |
| `events.json` | Liste des événements |

La sauvegarde est automatique à chaque création/suppression et au shutdown du proxy.

### Exemple `categories.json`
```json
[
  { "name": "pvp", "description": "Combats entre joueurs" },
  { "name": "course" }
]
```

### Exemple `events.json`
```json
[
  {
    "name": "deathrun",
    "type": "SOLO",
    "soloMode": "RANKED",
    "minParticipants": 4,
    "maxParticipants": 20,
    "category": "course",
    "description": "Évitez les pièges !",
    "reward": {
      "type": "DEGRESSIVE",
      "max": 10,
      "range": 2,
      "participation": 1
    }
  },
  {
    "name": "bedwars",
    "type": "TEAM",
    "minParticipants": 4,
    "maxParticipants": 16,
    "reward": {
      "type": "TEAM",
      "winner": 5,
      "loser": 1
    },
    "teamConfig": {
      "teamCount": 4,
      "randomTeams": false,
      "fixedTeamSizes": {},
      "teamTags": {
        "grackle_team_rouge": "rouge",
        "grackle_team_bleu": "bleu",
        "grackle_team_vert": "vert",
        "grackle_team_jaune": "jaune"
      }
    }
  }
]
```

---

## Types de récompenses

### Dégressif (`DEGRESSIVE`)

> Formule : `pts = max(max - range × (position - 1), participation)`

| Paramètre | Rôle |
|---|---|
| `max` | Points du 1er |
| `range` | Écart entre chaque place |
| `participation` | Plancher minimal |

**Exemple** — max=5, range=1, participation=2, 6 joueurs :

| Place | Calcul | Points |
|---|---|---|
| 1er | 5 - 1×0 | **5** |
| 2e | 5 - 1×1 | **4** |
| 3e | 5 - 1×2 | **3** |
| 4e | 5 - 1×3 | **2** |
| 5e | 5 - 1×4 = 1 → plancher | **2** |
| 6e | 5 - 1×5 = 0 → plancher | **2** |

### Défini par rang (`DEFINED`)

Mapping explicite position → points, avec un fallback "autre" pour tous les rangs non listés.

```json
{
  "type": "DEFINED",
  "ranks": { "1": 6, "2": 5, "3": 4 },
  "other": 3
}
```

### Équipe (`TEAM`)

```json
{ "type": "TEAM", "winner": 5, "loser": 1 }
```

---

## Système de tags (affectation d'équipe)

Velocity n'a pas d'API de tags native. L'affectation d'équipe repose sur des tags gérés par **LuckPerms** (ou équivalent) côté backend.

**Convention de nommage :**
```
grackle_team_<nomEquipe>
```

**Flux command block :**
```
# Exécuté par un command block côté backend
execute as @a[tag=grackle_team_rouge] run <tag rouge>
/grackle refreshTeams <gameId>
```

Quand `/grackle refreshTeams <gameId>` est reçu, le plugin lit les tags de chaque participant et les affecte à l'équipe correspondante selon la config de l'event.

> La méthode `playerHasTag()` dans `GameManager` est un stub à connecter à LuckPerms ou à votre API de tags.

---

## Commandes

### `/grackle` (alias `/gk`) — Permission `grackle.admin`

| Commande | Description |
|---|---|
| `/grackle start <event>` | Crée une partie pour cet event |
| `/grackle join <gameId>` | Rejoindre une partie (joueur) |
| `/grackle leave <gameId>` | Quitter une partie (joueur) |
| `/grackle refreshTeams <gameId>` | Lire les tags et affecter les équipes |
| `/grackle startGame <gameId>` | Lancer la partie (passe en IN_GAME) |
| `/grackle setPosition <gameId> <nom> <pos>` | Définir le classement final (joueur ou équipe) |
| `/grackle end <gameId>` | Terminer la partie et distribuer les récompenses |
| `/grackle status <gameId>` | Voir l'état d'une partie |

### `/event` (alias `/ev`) — Permission `grackle.event`

| Commande | Description |
|---|---|
| `/event create <nom> <solo\|team> [options]` | Créer un event |
| `/event list` | Lister tous les events |
| `/event info <nom>` | Détails d'un event |
| `/event delete <nom>` | Supprimer un event |

**Options de création :**

| Option | Type | Description |
|---|---|---|
| `--min <n>` | commun | Participants minimum |
| `--max-players <n>` | commun | Participants maximum |
| `--category <nom>` | commun | Catégorie associée |
| `--desc <texte>` | commun | Description |
| `--mode <ranked\|participation>` | solo | Mode de classement |
| `--max <n>` | récompense | Points maximum (dégressif) |
| `--range <n>` | récompense | Écart entre places (dégressif) |
| `--participation <n>` | récompense | Plancher de points (dégressif) |
| `--winner <n>` | team | Points gagnant |
| `--loser <n>` | team | Points perdant |
| `--teams <n>` | team | Nombre d'équipes (défaut: 2) |
| `--random` | team | Équipes aléatoires |

**Exemples :**
```
/event create deathrun solo --min 4 --max-players 20 --mode ranked --max 10 --range 2 --participation 1 --category course
/event create bedwars team --teams 4 --winner 5 --loser 1 --min 4
/event create marathon solo --mode participation --participation 3
```

### `/category` (alias `/cat`) — Permission `grackle.category`

| Commande | Description |
|---|---|
| `/category create <nom> [description]` | Créer une catégorie |
| `/category list` | Lister les catégories |
| `/category delete <nom>` | Supprimer une catégorie |

---

## Cycle de vie d'une partie

```
WAITING
  │  /grackle start <event>     → partie créée
  │  /grackle join <id>         → joueurs rejoignent
  ▼
REFRESH_TEAM
  │  /grackle refreshTeams <id> → lecture des tags, affectation équipes
  ▼
IN_GAME
  │  /grackle startGame <id>
  │  (jeu en cours, les tags peuvent changer)
  │  /grackle setPosition <id> <nom> <pos>
  ▼
ENDED
     /grackle end <id>          → récompenses distribuées, partie retirée
```

---

## Permissions

| Permission | Description |
|---|---|
| `grackle.admin` | Toutes les commandes `/grackle` |
| `grackle.event` | Gestion des events |
| `grackle.category` | Gestion des catégories |

---

## Cas particuliers (tailles d'équipe fixées)

Certains jeux nécessitent une équipe avec un nombre précis de joueurs (ex. PropHunt : 1 chasseur).  
Configurez `fixedTeamSizes` dans la `teamConfig` de l'event JSON :

```json
"teamConfig": {
  "teamCount": 2,
  "randomTeams": true,
  "fixedTeamSizes": { "chasseur": 1 },
  "teamTags": {
    "grackle_team_chasseur": "chasseur",
    "grackle_team_prop": "prop"
  }
}
```

La contrainte de taille est exposée via `TeamConfig.getFixedTeamSizes()` — la logique d'attribution aléatoire tenant compte de cette contrainte est à implémenter dans `GameManager.refreshTeams()` selon vos besoins.
