# 📋 Guide de Test - Visages de Confiance

## 🎯 Objectif

Tester la fonctionnalité de reconnaissance faciale pour vérifier que :
1. ✅ Les visages peuvent être enregistrés
2. ✅ Les visages de confiance sont reconnus en temps réel
3. ✅ La protection est désactivée/réduite pour les visages de confiance
4. ✅ Les visages inconnus déclenchent toujours des alertes

---

## 📱 Étape 1 : Accéder à la Gestion des Visages

1. **Ouvre l'application** Privacy Guard
2. **Démarre le service** (bouton Start)
3. **Clique sur** "👤 Visages de Confiance"
4. **Vérifie** que tu arrives sur l'écran de liste (vide au départ)

**Résultat attendu** : Écran vide avec message "Aucun visage de confiance" + bouton FAB (+)

---

## 📸 Étape 2 : Enregistrer un Premier Visage

1. **Clique sur le bouton FAB** (+) en bas à droite
2. **Autorise la permission caméra** si demandée
3. **Entre un nom** (optionnel) : "Toi", "Moi", ou ton prénom
4. **Suis les instructions** pour les 3 photos :
   - 📸 **Photo 1/3** : Regarde la caméra de face
   - Clique "Capturer" (icône +)
   - 📸 **Photo 2/3** : Tourne légèrement la tête à gauche (profil gauche)
   - Clique "Capturer"
   - 📸 **Photo 3/3** : Tourne légèrement la tête à droite (profil droit)
   - Clique "Capturer"
5. **Clique "Enregistrer"** (icône ✓)
6. **Attends** le traitement (quelques secondes)

**Résultat attendu** : 
- ✅ Retour à l'écran de liste
- ✅ Ta carte de visage apparaît avec ton nom et la photo
- ✅ Message "👥 1 visage de confiance"

**Logs à vérifier** (optionnel) :
```bash
adb logcat | grep "TrustFacesManager"
```
Devrait afficher :
```
TrustFacesManager: Registered face 'Toi'
```

---

## 🔄 Étape 3 : Enregistrer un Deuxième Visage

1. **Répète l'étape 2** avec une autre personne (ami, conjoint, collègue)
2. **Entre son nom** : "Maman", "Papa", "Marie", etc.
3. **Prends 3 photos** de cette personne
4. **Enregistre**

**Résultat attendu** : 
- ✅ "👥 2 visages de confiance"
- ✅ Les deux cartes s'affichent dans la liste

---

## 🧪 Étape 4 : Tester la Reconnaissance (TOI SEUL)

### Test 4A : Toi seul devant le téléphone

1. **Reviens à l'écran principal** (bouton retour)
2. **Vérifie que le service est actif**
3. **Mets en mode PARANOIA** (Paramètres → Mode PARANOIA)
4. **Redémarre le service** (Stop puis Start)
5. **Place-toi seul devant le téléphone**
6. **Regarde l'écran**

**Résultat attendu** : 
- 🟢 **Indicateur VERT** ou 🟡 **JAUNE** (faible menace)
- ❌ **PAS de flou**, pas d'écran leurre
- Protection réduite car tu es reconnu comme visage de confiance

**Logs à vérifier** :
```bash
adb logcat | grep "FaceMatcher\|CameraSensor"
```
Devrait afficher :
```
FaceMatcher: ✓ MATCH 'Toi' (confidence=85%)
CameraSensor: ✓ Visage de confiance reconnu: Toi
CameraSensor: ✓ Tous les visages sont de confiance - réduction de menace
```

---

## 🧪 Étape 5 : Tester la Reconnaissance (VISAGE CONNU)

### Test 5A : Personne enregistrée regarde l'écran

1. **Demande à la personne enregistrée** (étape 3) de se placer devant le téléphone
2. **Elle regarde l'écran**

**Résultat attendu** : 
- 🟢 **Indicateur VERT** ou 🟡 **JAUNE** 
- ❌ **PAS de protection active**
- Visage reconnu

**Logs attendus** :
```
FaceMatcher: ✓ MATCH 'Maman' (confidence=82%)
CameraSensor: ✓ Visage de confiance reconnu: Maman
```

---

## 🧪 Étape 6 : Tester la Reconnaissance (VISAGE INCONNU)

### Test 6A : Personne NON enregistrée

1. **Demande à une personne NON enregistrée** de se placer devant le téléphone
2. **Elle regarde l'écran**

**Résultat attendu** : 
- 🔴 **Indicateur ROUGE** (menace détectée)
- ✅ **Flou ou écran leurre activé**
- Protection déclenchée car visage inconnu

**Logs attendus** :
```
FaceMatcher: ✗ No match found (best similarity=0.45)
CameraSensor: Visage inconnu détecté
ThreatLevel=HIGH, unknownFacesCount=1
```

---

## 🧪 Étape 7 : Tester Mixte (Connu + Inconnu)

### Test 7A : Toi + personne inconnue

1. **Place-toi avec une personne NON enregistrée** devant l'écran
2. **Vous regardez tous les deux l'écran**

**Résultat attendu** : 
- 🔴 **Indicateur ROUGE** ou 🟡 **JAUNE** (selon les réglages)
- ✅ **Protection activée** car il y a AU MOINS 1 visage inconnu
- 1 visage reconnu, 1 visage inconnu

**Logs attendus** :
```
FaceMatcher: ✓ MATCH 'Toi' (confidence=87%)
FaceMatcher: ✗ No match found
CameraSensor: facesDetected=2, unknownFacesCount=1
ThreatLevel=MEDIUM ou HIGH
```

---

## 🗑️ Étape 8 : Gestion des Visages

### Test 8A : Supprimer un visage

1. **Va dans** "👤 Visages de Confiance"
2. **Clique sur l'icône poubelle** d'une carte de visage
3. **Confirme** la suppression

**Résultat attendu** : 
- ✅ La carte disparaît de la liste
- ✅ Compteur mis à jour ("👥 1 visage de confiance")

### Test 8B : Supprimer tous les visages

1. **Clique sur l'icône poubelle** en haut à droite
2. **Confirme** "Supprimer tout"

**Résultat attendu** : 
- ✅ Liste vide
- ✅ Message "Aucun visage de confiance"

---

## 📊 Tableau de Synthèse des Tests

| Test | Condition | Indicateur attendu | Protection attendue | Logs clés |
|------|-----------|-------------------|---------------------|-----------|
| **4A** | Toi seul (connu) | 🟢 Vert | ❌ Aucune | `MATCH 'Toi'` |
| **5A** | Personne connue | 🟢/🟡 Vert/Jaune | ❌ Aucune | `MATCH 'Nom'` |
| **6A** | Personne inconnue | 🔴 Rouge | ✅ Flou/Leurre | `No match found` |
| **7A** | Connu + inconnu | 🟡/🔴 Jaune/Rouge | ✅ Protection partielle | `unknownFacesCount=1` |
| **8A** | Suppression 1 | - | - | `Deleted face` |
| **8B** | Suppression tous | - | - | `Deleted all faces` |

---

## 🐛 Problèmes Potentiels et Solutions

### Problème 1 : "Impossible de détecter un visage"

**Causes possibles** :
- Éclairage trop faible/fort
- Visage trop loin de la caméra
- Caméra floue/sale

**Solutions** :
- Assure-toi d'un bon éclairage (lumière de face)
- Rapproche-toi de la caméra (20-40cm)
- Nettoie la caméra frontale
- Essaie dans une autre pièce

---

### Problème 2 : "Visage connu non reconnu"

**Causes possibles** :
- Changement d'apparence (lunettes, barbe, maquillage)
- Angle trop différent des photos d'enregistrement
- Éclairage très différent

**Solutions** :
- Ré-enregistre le visage dans les conditions actuelles
- Prends des photos avec lunettes ET sans lunettes
- Ajoute plusieurs entrées pour la même personne dans différentes conditions

**Logs à vérifier** :
```bash
adb logcat | grep "similarity"
```
Si tu vois `similarity=0.68` (< 0.75), c'est trop faible. Le seuil est à 75% par défaut.

---

### Problème 3 : "Visage inconnu reconnu comme connu (FAUX POSITIF)"

**Gravité** : ⚠️ **CRITIQUE** - Problème de sécurité !

**Solution immédiate** :
1. Supprime le visage concerné
2. Ré-enregistre avec 5 photos (au lieu de 3)
3. Augmente le seuil de confiance dans le code si nécessaire

**Logs à envoyer** :
```bash
adb logcat | grep "FaceMatcher" > face_match_logs.txt
```

---

## 📈 Métriques de Performance

### Temps de Reconnaissance
- ✅ **< 300ms** : Excellent
- ⚠️ **300-500ms** : Acceptable
- ❌ **> 500ms** : Trop lent

**Vérifier dans les logs** :
```bash
adb logcat | grep "CameraSensor.*processing"
```

### Taux de Reconnaissance
- ✅ **> 95%** : Excellent (sur 20 tests)
- ⚠️ **85-95%** : Acceptable
- ❌ **< 85%** : Insuffisant

**Test** : Place-toi 20 fois devant la caméra pendant 30 secondes → compte combien de fois tu es reconnu.

---

## 🎉 Checklist de Validation

- [ ] ✅ J'ai pu enregistrer mon visage (3 photos)
- [ ] ✅ J'ai pu enregistrer un 2ème visage
- [ ] ✅ Quand JE regarde l'écran → Indicateur VERT (pas d'alerte)
- [ ] ✅ Quand personne CONNUE regarde → Indicateur VERT
- [ ] ✅ Quand personne INCONNUE regarde → Indicateur ROUGE + Protection
- [ ] ✅ Avec CONNU + INCONNU → Protection activée (au moins 1 inconnu)
- [ ] ✅ J'ai pu supprimer un visage
- [ ] ✅ Les logs montrent "MATCH" pour les visages connus
- [ ] ✅ Les logs montrent "No match" pour les visages inconnus
- [ ] ✅ Pas de faux positifs (inconnu reconnu comme connu)
- [ ] ✅ Taux de reconnaissance > 90%
- [ ] ✅ Temps de reconnaissance < 500ms

---

## 📞 Logs Complets pour Debug

**Commande à exécuter pendant les tests** :

```bash
# Windows PowerShell
adb logcat -c  # Vider les logs
adb logcat | Select-String "FaceMatcher|CameraSensor|TrustFaces"

# Linux/Mac
adb logcat -c
adb logcat | grep -E "FaceMatcher|CameraSensor|TrustFaces"
```

**Enregistrer les logs dans un fichier** :

```bash
adb logcat > trust_faces_test_logs.txt
```

Laisse tourner pendant tes tests, puis analyse le fichier.

---

## 🚀 Prochaines Étapes

Si tous les tests passent ✅ :
1. **Optimisations Batterie** (sampling adaptatif)
2. **Mode Panique** (déclenchement d'urgence)
3. **Amélioration ML** (modèle FaceNet custom)

---

**Bonne chance pour les tests ! 🎉**  
**N'hésite pas à remonter les problèmes ou les suggestions d'amélioration.**





