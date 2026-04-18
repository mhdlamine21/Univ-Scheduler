# UNIV-SCHEDULER

**Système Universitaire de Gestion des Salles, des Emplois du Temps et des Réservations**  
Université Iba Der Thiam de Thiès (UIDT) — UFR Sciences et Technologies  
Projet académique de **Licence 2 Informatique (Semestre 4)**  
Cours : **Programmation Orientée Objet 1 (POO 1)**

---

## 👥 Auteurs du Projet

- **[Mouhamadou Lamine Niang](mailto:mouhamedlniang@gmail.com)**
- **[Mouhameth Nguer](mailto:mouhameth.nguer@univ-thies.sn)**

---

## 🎯 Contexte & Présentation

Développé dans le cadre du cours de **Programmation Orientée Objet 1 (POO 1)** en **Licence 2 Informatique (Semestre 4)** à l'Université Iba Der Thiam de Thiès, **UNIV-SCHEDULER** est une application logicielle de bureau conçue pour moderniser, centraliser et fiabiliser la gestion logistique des espaces pédagogiques et la confection des emplois du temps universitaires.

L'application répond à des problématiques concrètes d'optimisation d'espaces :

- Prévention et détection proactive des chevauchements d'horaires et collisions de salles.
- Traçabilité et équité dans les attributions d'amphithéâtres, salles de TD et laboratoires de TP.
- Gestion différenciée des accès selon le statut académique.
- Suivi de la maintenance du matériel pédagogique et gestion des imprévus de cours.

---

## 🏛️ Rôles & Espaces de Travail

UNIV-SCHEDULER propose quatre interfaces adaptées aux responsabilités de chaque acteur du campus :

| Profil                 | Périmètre d'action & Fonctionnalités                                                                                                                                                                                         |
| ---------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Administrateur**     | Pilotage global de l'infrastructure (UFR, bâtiments, étages, salles, équipements), supervision des comptes utilisateurs, validation des inscriptions, suivi des indicateurs d'occupation et journaux d'audit.                |
| **Gestionnaire**       | Conception et publication des emplois du temps, planification des cours semestriels, détection et résolution assistée des conflits de créneaux.                                                                              |
| **Enseignant**         | Consultation personnalisée du planning hebdomadaire, réservation ponctuelle d'espaces, déclaration de pannes ou besoins spécifiques.                                                                                         |
| **Étudiant / Délégué** | Consultation directe de l'emploi du temps de sa promotion, recherche en direct de salles disponibles. Les **délégués de classe** bénéficient d'un droit de réservation pour les séances de tutorat et révisions collectives. |

---

## 🧩 Architecture Logicielle & Principes POO

Le projet met en application les bonnes pratiques de conception orientée objet :

- **Pattern Modèle-Vue-Contrôleur (MVC) :** Séparation stricte entre les interfaces graphiques déclaratives (`.fxml`), la logique applicative (`Controleurs`) et le domaine métier (`Modele`).
- **Data Access Object (DAO) :** Encapsulation complète des requêtes SQL et de la persistance JDBC.
- **Couche Service Métier :** Centralisation des règles de gestion (validation de créneaux, vérification des capacités, détection d'indisponibilité).
- **Asynchronisme & Réactivité JavaFX :** Exécution des traitements lourds (envois d'e-mails, requêtes analytiques) via des tâches d'arrière-plan (`javafx.concurrent.Task`) garantissant la fluidité de l'interface.
- **Charte Graphique :** Thème universitaire sobre et élégant (teintes ivoire crème, marron expresso, cuir chaud et ambre doré).

---

## 💻 Technologies & Bibliothèques

- **Langage :** Java (JDK 17+)
- **Interface Graphique :** JavaFX 17 / 21
- **Base de Données :** MySQL 8.0+ (encodage UTF-8 / `utf8mb4`)
- **Gestionnaire de Production :** Apache Maven
- **Journalisation :** SLF4J / Logback
- **Messagerie :** Jakarta Mail (JavaMail TLS)

---

## 🚀 Installation & Démarrage

### 1. Cloner le dépôt

```bash
git clone https://github.com/mhdlamine21/Univ-Scheduler.git
cd Univ-Scheduler
```

### 2. Initialisation de la Base de Données

Créez une base de données MySQL avec l'encodage `utf8mb4` :

```sql
CREATE DATABASE scheduler_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Importez le script d'initialisation fourni à la racine :

```bash
mysql -u root -p scheduler_db < script.sql
```

### 3. Paramétrage de la Connexion

Vérifiez ou adaptez les paramètres d'accès dans `src/main/resources/config/bd.properties` :

```properties
db.url=jdbc:mysql://localhost:3306/scheduler_db?useSSL=false&serverTimezone=Africa/Dakar&useUnicode=true&characterEncoding=UTF-8
db.user=root
db.password=VOTRE_MOT_DE_PASSE
```

### 4. Compilation et Exécution

Avec Maven ou le Wrapper Maven inclus :

```bash
# Compilation du projet
mvn clean compile
# ou avec le wrapper :
# Windows : .\mvnw.cmd clean compile
# Linux/macOS : ./mvnw clean compile

# Lancement de l'application JavaFX
mvn javafx:run
# ou avec le wrapper :
# Windows : .\mvnw.cmd javafx:run
# Linux/macOS : ./mvnw javafx:run
```

Sous Windows, vous pouvez également lancer directement le script automatisé :

```text
lancer_app.bat
```

---

## 🔑 Compte Administrateur par Défaut

Pour le premier accès à l'application :

- **Identifiant :** `admin@univ-thies.sn`
- **Mot de passe :** `Admin2025`

---

## 📂 Organisation du Code Source

```text
Univ-Scheduler/
├── pom.xml                                ← Configuration Maven & dépendances
├── script.sql                             ← Schéma relationnel et données initiales
├── lancer_app.bat                         ← Lanceur Windows
├── README.md                              ← Documentation du projet
└── src/
    └── main/
        ├── java/scheduler/
        │   ├── Launcher.java              ← Lanceur JVM standard
        │   ├── Main.java                  ← Point d'entrée JavaFX
        │   ├── controleur/                ← Contrôleurs des interfaces FXML
        │   ├── dao/                       ← Objets d'accès aux données (DAO JDBC)
        │   ├── modele/                    ← Classes métier et entités du domaine
        │   ├── service/                   ← Services de traitement et règles métier
        │   └── util/                      ← Boîte à outils, aide visuelle, sécurité
        └── resources/
            ├── fxml/                      ← Vues graphiques JavaFX
            ├── css/style.css              ← Feuilles de style et tokens visuels
            ├── config/                    ← Configuration base de données & logs
            └── images/                    ← Logos, blasons et pictogrammes
```

---

_Projet académique - Université Iba Der Thiam de Thiès (UIDT)_
