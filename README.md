# Privacy Guard - Application de Protection de Confidentialité

## 🎯 Vue d'ensemble

Privacy Guard est une application Android innovante qui surveille l'environnement en temps réel et masque automatiquement le contenu sensible à l'écran lorsqu'une menace pour la vie privée est détectée. Elle fonctionne comme une couche de sécurité universelle au-dessus de toutes les autres applications.

## 🌟 Concept Principal

L'application utilise une combinaison de capteurs (caméra frontale, microphone, accéléromètre, gyroscope, capteur de proximité, luminosité, GPS) pour détecter les menaces potentielles à la vie privée et réagir en masquant automatiquement le contenu sensible.

## 🎯 Cas d'Usage

- 🏢 **Au bureau** : Éviter le shoulder surfing des collègues curieux
- 🚇 **Transports** : Protection contre les regards indiscrets
- ☕ **Cafés publics** : Sécurité dans lieux bondés
- 🏠 **À la maison** : Privacy vis-à-vis de la famille/colocataires
- 💼 **Professionnels** : Protection de données sensibles clients
- 🔐 **Activistes** : Sécurité renforcée dans contextes à risque

## 🚀 Démarrage Rapide

### Prérequis

- Android SDK 26+ (Android 8.0 Oreo)
- Kotlin 1.9+
- Android Studio Hedgehog ou supérieur
- Gradle 8.0+

### Permissions Requises

- `CAMERA` : Détection de visages multiples
- `RECORD_AUDIO` : Analyse audio pour détection de menaces
- `ACCESS_FINE_LOCATION` : Géofencing et zones de confiance
- `BIND_ACCESSIBILITY_SERVICE` : Overlay sur toutes les applications
- `FOREGROUND_SERVICE` : Service en arrière-plan continu
- `SYSTEM_ALERT_WINDOW` : Affichage d'overlay

## 📚 Documentation

- [Architecture Technique](./ARCHITECTURE.md)
- [Fonctionnalités Détaillées](./FEATURES.md)
- [Capteurs et Détection](./SENSORS.md)
- [Interface Utilisateur](./UI_UX.md)
- [Sécurité et Confidentialité](./SECURITY_PRIVACY.md)
- [Défis Techniques](./TECHNICAL_CHALLENGES.md)
- [Feuille de Route](./ROADMAP.md)

## 🛠️ Technologies Utilisées

- **Langage** : Kotlin
- **Framework** : Android SDK
- **ML** : TensorFlow Lite
- **Détection faciale** : ML Kit / CameraX
- **Audio Processing** : Tarsos DSP
- **Architecture** : MVVM + Clean Architecture
- **DI** : Hilt/Dagger
- **Base de données** : Room
- **Reactive** : Kotlin Coroutines + Flow

## 📱 Modes de Protection

### Mode Paranoïa (Maximum)
Moindre mouvement autour du téléphone → verrouillage immédiat

### Mode Équilibré (Recommandé)
Détection de visages supplémentaires + mouvements suspects

### Mode Discret (Minimum)
Uniquement menaces directes

### Mode Zones de Confiance
Activation/désactivation automatique selon le lieu

## 🔒 Engagement Confidentialité

- ✅ Traitement 100% local (aucune donnée cloud)
- ✅ Aucune télémétrie par défaut
- ✅ Chiffrement bout en bout des logs
- ✅ Open source (à venir)
- ✅ Auditable et transparent

## 📄 Licence

À définir (suggestion : GPL-3.0 ou Apache 2.0)

## 👥 Contribution

Les contributions sont les bienvenues ! Consultez [CONTRIBUTING.md](./CONTRIBUTING.md) pour plus de détails.

## 📞 Contact

Pour toute question ou suggestion, ouvrez une issue sur GitHub.

---

**⚠️ Note Légale** : Cette application nécessite des permissions sensibles (caméra, microphone). Toutes les données sont traitées localement sur l'appareil. Aucune donnée n'est transmise à des serveurs tiers.

