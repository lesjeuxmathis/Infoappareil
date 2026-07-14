# Info Appareil

Application Android (Kotlin) qui liste les composants matériels et logiciels du téléphone :
appareil/OS, processeur, RAM, stockage, batterie, écran, capteurs, caméras, réseau et Bluetooth.

## Prérequis

- [Android Studio](https://developer.android.com/studio) (version récente, Koala ou plus)
- JDK 17 (installé automatiquement avec Android Studio)
- Un smartphone Android (câble USB + mode développeur/débogage USB activé) ou un émulateur

## Ouvrir et lancer le projet

1. Décompresse ce zip quelque part sur ton ordinateur.
2. Ouvre Android Studio → **File > Open** → sélectionne le dossier `InfoAppareil`.
3. Laisse Android Studio synchroniser Gradle (il télécharge les dépendances, ça prend
   1 à 2 minutes la première fois). Si un wrapper Gradle est proposé automatiquement,
   accepte-le.
4. Branche ton téléphone en USB (ou lance un émulateur) et clique sur le bouton
   ▶ **Run 'app'**.
5. Au premier lancement, l'app demandera l'autorisation Bluetooth (Android 12+) pour
   pouvoir afficher le nom de l'adaptateur — tu peux refuser, le reste de l'app
   fonctionne normalement.

## Compiler automatiquement via GitHub Actions (sans Android Studio)

Ce projet inclut un workflow (`.github/workflows/build.yml`) qui compile l'APK sur les
serveurs de GitHub à chaque push, sans que tu aies besoin d'Android Studio ni du SDK
Android sur ta machine.

1. Crée un nouveau dépôt sur [github.com/new](https://github.com/new) (public ou privé).
2. Pousse ce dossier dedans :
   ```bash
   cd InfoAppareil
   git init
   git add .
   git commit -m "Premier commit"
   git branch -M main
   git remote add origin https://github.com/<ton-utilisateur>/<ton-depot>.git
   git push -u origin main
   ```
3. Va dans l'onglet **Actions** de ton dépôt GitHub : le workflow "Build APK" se lance
   automatiquement.
4. Une fois terminé (icône verte ✅, ~3-5 min), clique sur le run, puis en bas de la
   page dans la section **Artifacts**, télécharge `InfoAppareil-debug-apk` : c'est un
   zip contenant `app-debug.apk`, installable directement sur un téléphone Android
   (active "Sources inconnues" dans les paramètres pour l'installer).

## Notes

- `applicationId` est actuellement `com.example.infoappareil` : change-le avant toute
  publication sur le Play Store.
- L'icône fournie est un simple placeholder vectoriel violet — remplace-la si tu veux
  une identité visuelle personnalisée.
- Certaines infos (opérateur, nom Bluetooth, volumes SD) peuvent apparaître comme
  "Inconnu"/"Indisponible" selon le fabricant du téléphone ou les restrictions
  d'Android — c'est normal et géré proprement par l'app plutôt que de planter.
