# overlay-network

Ce projet est une simulation de réseau comprenant une couche virtuelle et physique, réalisée à l'aide de RabbitMQ en Java.

# Test

Pour tester le code, utilisez le script `make.sh`. Voici comment l'utiliser :

- **Test par Défaut :** Lancez le script sans aucun argument pour exécuter le code avec une interface utilisant une configuration de routeur par défaut pour les tests.

```bash
    bash make.sh
```

- **Test avec un Fichier Spécifique :** Spécifiez un fichier de configuration à tester en tant qu'argument. Les fichiers de configuration se trouvent dans le dossier `graphs`.

```bash
    bash make.sh <nom_du_fichier>
```

Remplacez `<nom_du_fichier>` par le nom du fichier de configuration souhaité.

- **Test avec Graphique :** Si vous souhaitez que le graphe de configuration du routeur apparaisse dans l'interface, ajoutez l'argument `-g` avec le nom du fichier. Cela installera également les packages Python nécessaires pour la génération du graphe.

```bash
    bash make.sh <nom_du_fichier> -g
```

Remplacez `<nom_du_fichier>` par le nom du fichier de configuration souhaité.

# En cas d'erreurs avec python

## Si vous n'avez pas pip sur votre machine

Veuillez installer pip à l'aide de la commande suivante :

```bash
    sudo apt install python3-pip
```

## Si vous avez des problèmes avec les bibliothèques python

```bash
    pip install -r requirements.txt
```

# Logging des Requêtes

Toutes les requêtes effectuées d'un nœud virtuel à un autre sont enregistrées dans le fichier `log/log.txt`. Ce journal contient des informations détaillées sur la communication entre les nœuds virtuels et leurs nœuds physiques correspondants, ainsi que sur le routage des requêtes dans la couche physique pour atteindre leur destination.
