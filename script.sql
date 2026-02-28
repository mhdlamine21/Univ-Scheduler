-- SCRIPT DE CRÉATION DE LA BASE DE DONNÉES
-- Projet: SCHEDULER - Gestion des emplois du temps
-- Université Iba Der Thiam de Thiès (UIDT)

DROP DATABASE IF EXISTS univ_scheduler; CREATE DATABASE univ_scheduler; USE univ_scheduler;

-- TABLE ufr (Unités de Formation et de Recherche)
CREATE TABLE ufr (id INT PRIMARY KEY AUTO_INCREMENT, nom VARCHAR(100) NOT NULL UNIQUE, description TEXT, date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP);

-- TABLE batiments (liés aux UFR)
CREATE TABLE batiments (id INT PRIMARY KEY AUTO_INCREMENT, nom VARCHAR(100) NOT NULL, localisation VARCHAR(255), nb_etages INT DEFAULT 1, ufr_id INT NOT NULL, statut ENUM('disponible', 'indisponible') DEFAULT 'disponible', motif_indisponibilite TEXT, date_debut_indisponibilite DATE, date_fin_indisponibilite DATE, FOREIGN KEY (ufr_id) REFERENCES ufr(id) ON DELETE CASCADE);

-- TABLE salles (liées aux bâtiments)
CREATE TABLE salles (id INT PRIMARY KEY AUTO_INCREMENT, numero VARCHAR(20) NOT NULL, capacite INT NOT NULL, type ENUM('TD', 'TP', 'Amphi', 'Autre') DEFAULT 'Autre', batiment_id INT NOT NULL, etage INT DEFAULT 0, statut ENUM('disponible', 'indisponible') DEFAULT 'disponible', motif_indisponibilite TEXT, date_debut_indisponibilite DATE, date_fin_indisponibilite DATE, FOREIGN KEY (batiment_id) REFERENCES batiments(id) ON DELETE CASCADE);

-- TABLE equipements
CREATE TABLE equipements (id INT PRIMARY KEY AUTO_INCREMENT, nom VARCHAR(100) NOT NULL UNIQUE, description TEXT);

-- TABLE liaison salle_equipement
CREATE TABLE salle_equipement (salle_id INT, equipement_id INT, PRIMARY KEY (salle_id, equipement_id), FOREIGN KEY (salle_id) REFERENCES salles(id) ON DELETE CASCADE, FOREIGN KEY (equipement_id) REFERENCES equipements(id) ON DELETE CASCADE);

-- TABLE classes
CREATE TABLE classes (id INT PRIMARY KEY AUTO_INCREMENT, intitule VARCHAR(200) NOT NULL, filiere VARCHAR(100) NOT NULL, niveau VARCHAR(50) NOT NULL, annee_scolaire VARCHAR(20) NOT NULL, ufr_id INT NOT NULL, effectif INT DEFAULT 0, nb_groupes INT DEFAULT 1, est_active BOOLEAN DEFAULT TRUE, date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (ufr_id) REFERENCES ufr(id) ON DELETE CASCADE, UNIQUE KEY unique_classe (intitule, annee_scolaire));

-- TABLE utilisateurs
CREATE TABLE utilisateurs (id INT PRIMARY KEY AUTO_INCREMENT, nom VARCHAR(100) NOT NULL, prenom VARCHAR(100) NOT NULL, email VARCHAR(150) UNIQUE NOT NULL, mot_de_passe VARCHAR(255) NOT NULL, role ENUM('admin', 'gestionnaire', 'enseignant', 'etudiant') NOT NULL, date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP, est_valide BOOLEAN DEFAULT FALSE, date_validation DATE, numero_etudiant VARCHAR(50), matricule_enseignant VARCHAR(50), ufr_id INT, classe_id INT, FOREIGN KEY (ufr_id) REFERENCES ufr(id) ON DELETE SET NULL, FOREIGN KEY (classe_id) REFERENCES classes(id) ON DELETE SET NULL);

-- TABLE demandes_inscription
CREATE TABLE demandes_inscription (id INT PRIMARY KEY AUTO_INCREMENT, nom VARCHAR(100) NOT NULL, prenom VARCHAR(100) NOT NULL, email VARCHAR(150) NOT NULL, role_demande ENUM('etudiant', 'enseignant', 'gestionnaire') NOT NULL, numero_etudiant VARCHAR(50), ufr_id INT, classe_id INT, matricule_enseignant VARCHAR(50), date_demande TIMESTAMP DEFAULT CURRENT_TIMESTAMP, statut ENUM('en_attente', 'validee', 'refusee') DEFAULT 'en_attente', FOREIGN KEY (ufr_id) REFERENCES ufr(id) ON DELETE SET NULL, FOREIGN KEY (classe_id) REFERENCES classes(id) ON DELETE SET NULL);

-- TABLE demandes_reinit_mot_de_passe
CREATE TABLE demandes_reinit_mot_de_passe (id INT PRIMARY KEY AUTO_INCREMENT, utilisateur_id INT NOT NULL, email VARCHAR(150) NOT NULL, nom VARCHAR(100), prenom VARCHAR(100), date_demande TIMESTAMP DEFAULT CURRENT_TIMESTAMP, statut ENUM('en_attente', 'traitee', 'expiree') DEFAULT 'en_attente', FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id) ON DELETE CASCADE);

-- TABLE matieres
CREATE TABLE matieres (id INT PRIMARY KEY AUTO_INCREMENT, nom VARCHAR(200) NOT NULL, code VARCHAR(50) UNIQUE NOT NULL, filiere VARCHAR(100) NOT NULL, volume_horaire INT DEFAULT 0, description TEXT, date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP);

-- TABLE cours
CREATE TABLE cours (id INT PRIMARY KEY AUTO_INCREMENT, matiere_id INT NOT NULL, enseignant_id INT NOT NULL, classe_id INT NOT NULL, type_cours ENUM('CM', 'TD', 'TP') NOT NULL, groupes VARCHAR(255), volume_horaire INT NOT NULL, date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (matiere_id) REFERENCES matieres(id) ON DELETE CASCADE, FOREIGN KEY (enseignant_id) REFERENCES utilisateurs(id) ON DELETE CASCADE, FOREIGN KEY (classe_id) REFERENCES classes(id) ON DELETE CASCADE);

-- TABLE creneaux
CREATE TABLE creneaux (id INT PRIMARY KEY AUTO_INCREMENT, cours_id INT NOT NULL, jour DATE NOT NULL, heure_debut TIME NOT NULL, heure_fin TIME NOT NULL, salle_id INT, statut ENUM('planifie', 'annule', 'deplace') DEFAULT 'planifie', motif_annulation TEXT, FOREIGN KEY (cours_id) REFERENCES cours(id) ON DELETE CASCADE, FOREIGN KEY (salle_id) REFERENCES salles(id) ON DELETE SET NULL);

-- TABLE emplois_du_temps
CREATE TABLE emplois_du_temps (id INT PRIMARY KEY AUTO_INCREMENT, classe_id INT NOT NULL, periode_type ENUM('hebdomadaire', 'mensuel', 'semestriel') NOT NULL, periode_debut DATE NOT NULL, periode_fin DATE NOT NULL, date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP, est_valide BOOLEAN DEFAULT FALSE, date_validation DATE, FOREIGN KEY (classe_id) REFERENCES classes(id) ON DELETE CASCADE);

-- TABLE emploi_du_temps_creneaux
CREATE TABLE emploi_du_temps_creneaux (emploi_id INT, creneau_id INT, PRIMARY KEY (emploi_id, creneau_id), FOREIGN KEY (emploi_id) REFERENCES emplois_du_temps(id) ON DELETE CASCADE, FOREIGN KEY (creneau_id) REFERENCES creneaux(id) ON DELETE CASCADE);

-- TABLE reservations (réservations ponctuelles)
CREATE TABLE reservations (id INT PRIMARY KEY AUTO_INCREMENT, utilisateur_id INT NOT NULL, salle_id INT NOT NULL, motif VARCHAR(255) NOT NULL, description TEXT, date_reservation DATE NOT NULL, heure_debut TIME NOT NULL, heure_fin TIME NOT NULL, statut ENUM('confirmee', 'en_cours', 'terminee', 'annulee') DEFAULT 'confirmee', date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id) ON DELETE CASCADE, FOREIGN KEY (salle_id) REFERENCES salles(id) ON DELETE CASCADE);

-- TABLE signalements
CREATE TABLE signalements (id INT PRIMARY KEY AUTO_INCREMENT, utilisateur_id INT NOT NULL, salle_id INT NOT NULL, type_probleme VARCHAR(100) NOT NULL, description TEXT NOT NULL, date_signalement TIMESTAMP DEFAULT CURRENT_TIMESTAMP, statut ENUM('en_attente', 'en_cours', 'resolu') DEFAULT 'en_attente', date_resolution DATE, commentaire_resolution TEXT, FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id) ON DELETE CASCADE, FOREIGN KEY (salle_id) REFERENCES salles(id) ON DELETE CASCADE);

-- INSERTION DES DONNÉES DE TEST - UFR (UIDT)
INSERT INTO ufr (nom, description) VALUES ('UFR Sciences et Technologies (UFR SET)', 'Sciences exactes, informatique, mathématiques, physique, hydrosciences'), ('UFR Sciences de l\'Ingénieur (UFR SI)', 'Génie civil, génie informatique, géotechnique, hydrogéologie, architecture'), ('UFR Sciences Économiques et Sociales (UFR SES)', 'Économie, gestion, tourisme, langues, management'), ('UFR Sciences de la Santé (UFR Santé)', 'Médecine générale'), ('École Nationale Supérieure d\'Agriculture (ENSA)', 'Agronomie, productions végétales/animales, foresterie'), ('Institut Universitaire de Technologie (IUT)', 'Formations techniques courtes (DUT)');

-- INSERTION DES CLASSES
INSERT INTO classes (intitule, filiere, niveau, annee_scolaire, ufr_id, effectif, nb_groupes) VALUES ('L1 Informatique', 'Informatique', 'Licence 1', '2024-2025', 1, 150, 5), ('L2 Informatique', 'Informatique', 'Licence 2', '2024-2025', 1, 120, 4), ('L3 Informatique', 'Informatique', 'Licence 3', '2024-2025', 1, 90, 3), ('L1 Mathématiques', 'Mathématiques', 'Licence 1', '2024-2025', 1, 100, 4), ('L2 Mathématiques', 'Mathématiques', 'Licence 2', '2024-2025', 1, 85, 3), ('L1 Génie Civil', 'Génie Civil', 'Licence 1', '2024-2025', 2, 80, 3), ('L2 Génie Informatique', 'Génie Informatique', 'Licence 2', '2024-2025', 2, 70, 3), ('L1 Sciences Économiques', 'Sciences Économiques', 'Licence 1', '2024-2025', 3, 120, 4), ('L2 Management', 'Management', 'Licence 2', '2024-2025', 3, 90, 3), ('L1 Agronomie', 'Agronomie', 'Licence 1', '2024-2025', 5, 60, 2);

-- INSERTION DES UTILISATEURS
INSERT INTO utilisateurs (nom, prenom, email, mot_de_passe, role, est_valide, date_validation) VALUES ('Niang', 'Mouhamadou Lamine', 'mouhamedlniang@gmail.com', 'admin123', 'admin', TRUE, CURDATE()), ('Nguer', 'Mouhameth', 'mouhamethnguer@gmail.com', 'admin123', 'admin', TRUE, CURDATE());

INSERT INTO utilisateurs (nom, prenom, email, mot_de_passe, role, est_valide, date_validation) VALUES ('Diop', 'Fatou', 'fatou.diop@univ.sn', 'gestion123', 'gestionnaire', TRUE, CURDATE());

INSERT INTO utilisateurs (nom, prenom, email, mot_de_passe, role, matricule_enseignant, est_valide, date_validation) VALUES ('Fall', 'Oumar', 'oumar.fall@univ.sn', 'prof123', 'enseignant', 'ENS2024001', TRUE, CURDATE());

INSERT INTO utilisateurs (nom, prenom, email, mot_de_passe, role, numero_etudiant, ufr_id, classe_id, est_valide, date_validation) VALUES ('Sy', 'Aminata', 'aminata.sy@etudiant.univ.sn', 'etud123', 'etudiant', '20240001', 1, 1, TRUE, CURDATE());

-- INSERTION DES ÉQUIPEMENTS
INSERT INTO equipements (nom, description) VALUES ('Vidéoprojecteur', 'Projecteur HDMI/VGA'), ('Tableau interactif', 'Tableau blanc tactile'), ('Climatisation', 'Climatisation réversible'), ('Système audio', 'Enceintes et micro'), ('Ordinateur', 'PC fixe avec écran');

-- INSERTION DES BÂTIMENTS
INSERT INTO batiments (nom, localisation, nb_etages, ufr_id) VALUES ('Bâtiment A', 'Campus Nord', 3, 1), ('Bâtiment B', 'Campus Nord', 2, 1), ('Bâtiment C', 'Campus Sud', 4, 2), ('Bâtiment D', 'Campus Sud', 3, 3), ('Bâtiment E', 'Campus Est', 2, 5);

-- INSERTION DES SALLES
INSERT INTO salles (numero, capacite, type, batiment_id, etage) VALUES ('A101', 30, 'TD', 1, 1), ('A102', 25, 'TD', 1, 1), ('A103', 40, 'TD', 1, 1), ('A201', 50, 'Amphi', 1, 2), ('B101', 30, 'TD', 2, 1), ('B202', 100, 'Amphi', 2, 2), ('C101', 30, 'TD', 3, 1), ('C201', 25, 'TP', 3, 2), ('D101', 40, 'TD', 4, 1), ('E101', 60, 'Amphi', 5, 1);

-- INSERTION DES LIAISONS SALLES-ÉQUIPEMENTS
INSERT INTO salle_equipement (salle_id, equipement_id) VALUES (1, 1), (1, 2), (2, 1), (3, 1), (3, 2), (3, 3), (4, 1), (4, 3), (4, 4), (5, 1), (6, 1), (6, 3), (6, 4), (7, 1), (8, 1), (8, 2), (8, 5), (9, 1), (9, 3), (10, 1), (10, 4);

-- INSERTION DES MATIÈRES
INSERT INTO matieres (nom, code, filiere, volume_horaire) VALUES ('Mathématiques pour l\'informatique', 'INF101', 'Informatique', 60), ('Algorithmique', 'INF102', 'Informatique', 45), ('Programmation Java', 'INF201', 'Informatique', 60), ('Bases de données', 'INF202', 'Informatique', 45), ('Réseaux', 'INF301', 'Informatique', 45), ('Mécanique des sols', 'GC101', 'Génie Civil', 50), ('Résistance des matériaux', 'GC102', 'Génie Civil', 55), ('Microéconomie', 'ECO101', 'Sciences Économiques', 40);

-- INSERTION DES COURS
INSERT INTO cours (matiere_id, enseignant_id, classe_id, type_cours, groupes, volume_horaire) VALUES (1, 3, 2, 'CM', 'tous', 30), (2, 3, 2, 'TD', 'G1,G2,G3,G4', 30), (3, 3, 2, 'CM', 'tous', 30), (4, 3, 2, 'CM', 'tous', 25), (1, 3, 1, 'CM', 'tous', 30), (6, 3, 6, 'CM', 'tous', 30), (8, 3, 8, 'CM', 'tous', 25);

-- INSERTION DES DEMANDES D'INSCRIPTION (en attente)
INSERT INTO demandes_inscription (nom, prenom, email, role_demande, numero_etudiant, ufr_id, classe_id) VALUES ('Dieng', 'Mamadou', 'mamadou.dieng@etudiant.fr', 'etudiant', '20240002', 1, 1), ('Sow', 'Aissatou', 'aissatou.sow@etudiant.fr', 'etudiant', '20240003', 1, 2), ('Faye', 'Ibrahima', 'ibrahima.faye@enseignant.fr', 'enseignant', NULL, NULL, NULL), ('Gueye', 'Mariama', 'mariama.gueye@univ.fr', 'gestionnaire', NULL, NULL, NULL);

ALTER TABLE equipements ADD COLUMN quantite INT DEFAULT 1;

-- Option 1: Ajouter une colonne quantite
ALTER TABLE salle_equipement ADD COLUMN quantite INT DEFAULT 1;

-- Option 2: Ou laisser comme ça et gérer par le nombre d'entrées

CREATE TABLE stock_equipements (
    equipement_id INT PRIMARY KEY,
    quantite_totale INT DEFAULT 0,
    quantite_utilisee INT DEFAULT 0,
    FOREIGN KEY (equipement_id) REFERENCES equipements(id)
);
-- Current Database: `univ_scheduler`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `univ_scheduler` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `univ_scheduler`;

--
-- Table structure for table `batiments`
--

DROP TABLE IF EXISTS `batiments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `batiments` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nom` varchar(100) NOT NULL,
  `localisation` varchar(255) DEFAULT NULL,
  `nb_etages` int DEFAULT '1',
  `ufr_id` int NOT NULL,
  `statut` enum('disponible','indisponible') DEFAULT 'disponible',
  `motif_indisponibilite` text,
  `date_debut_indisponibilite` date DEFAULT NULL,
  `date_fin_indisponibilite` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ufr_id` (`ufr_id`),
  CONSTRAINT `batiments_ibfk_1` FOREIGN KEY (`ufr_id`) REFERENCES `ufr` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `classes`
--

DROP TABLE IF EXISTS `classes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `classes` (
  `id` int NOT NULL AUTO_INCREMENT,
  `intitule` varchar(200) NOT NULL,
  `filiere` varchar(100) NOT NULL,
  `niveau` varchar(50) NOT NULL,
  `annee_scolaire` varchar(20) NOT NULL,
  `ufr_id` int NOT NULL,
  `effectif` int DEFAULT '0',
  `nb_groupes` int DEFAULT '1',
  `est_active` tinyint(1) DEFAULT '1',
  `date_creation` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_classe` (`intitule`,`annee_scolaire`),
  KEY `ufr_id` (`ufr_id`),
  CONSTRAINT `classes_ibfk_1` FOREIGN KEY (`ufr_id`) REFERENCES `ufr` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cours`
--

DROP TABLE IF EXISTS `cours`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cours` (
  `id` int NOT NULL AUTO_INCREMENT,
  `matiere_id` int NOT NULL,
  `enseignant_id` int NOT NULL,
  `classe_id` int NOT NULL,
  `type_cours` enum('CM','TD','TP') NOT NULL,
  `groupes` varchar(255) DEFAULT NULL,
  `volume_horaire` int NOT NULL,
  `date_creation` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `matiere_id` (`matiere_id`),
  KEY `enseignant_id` (`enseignant_id`),
  KEY `classe_id` (`classe_id`),
  CONSTRAINT `cours_ibfk_1` FOREIGN KEY (`matiere_id`) REFERENCES `matieres` (`id`) ON DELETE CASCADE,
  CONSTRAINT `cours_ibfk_2` FOREIGN KEY (`enseignant_id`) REFERENCES `utilisateurs` (`id`) ON DELETE CASCADE,
  CONSTRAINT `cours_ibfk_3` FOREIGN KEY (`classe_id`) REFERENCES `classes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `creneaux`
--

DROP TABLE IF EXISTS `creneaux`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `creneaux` (
  `id` int NOT NULL AUTO_INCREMENT,
  `cours_id` int NOT NULL,
  `jour` date NOT NULL,
  `heure_debut` time NOT NULL,
  `heure_fin` time NOT NULL,
  `salle_id` int DEFAULT NULL,
  `statut` enum('planifie','annule','deplace') DEFAULT 'planifie',
  `motif_annulation` text,
  PRIMARY KEY (`id`),
  KEY `cours_id` (`cours_id`),
  KEY `salle_id` (`salle_id`),
  CONSTRAINT `creneaux_ibfk_1` FOREIGN KEY (`cours_id`) REFERENCES `cours` (`id`) ON DELETE CASCADE,
  CONSTRAINT `creneaux_ibfk_2` FOREIGN KEY (`salle_id`) REFERENCES `salles` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `demandes_inscription`
--

DROP TABLE IF EXISTS `demandes_inscription`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `demandes_inscription` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nom` varchar(100) NOT NULL,
  `prenom` varchar(100) NOT NULL,
  `email` varchar(150) NOT NULL,
  `role_demande` enum('etudiant','enseignant','gestionnaire') NOT NULL,
  `numero_etudiant` varchar(50) DEFAULT NULL,
  `ufr_id` int DEFAULT NULL,
  `classe_id` int DEFAULT NULL,
  `matricule_enseignant` varchar(50) DEFAULT NULL,
  `date_demande` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `statut` enum('en_attente','validee','refusee') DEFAULT 'en_attente',
  PRIMARY KEY (`id`),
  KEY `ufr_id` (`ufr_id`),
  KEY `classe_id` (`classe_id`),
  CONSTRAINT `demandes_inscription_ibfk_1` FOREIGN KEY (`ufr_id`) REFERENCES `ufr` (`id`) ON DELETE SET NULL,
  CONSTRAINT `demandes_inscription_ibfk_2` FOREIGN KEY (`classe_id`) REFERENCES `classes` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `demandes_reinit_mot_de_passe`
--

DROP TABLE IF EXISTS `demandes_reinit_mot_de_passe`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `demandes_reinit_mot_de_passe` (
  `id` int NOT NULL AUTO_INCREMENT,
  `utilisateur_id` int NOT NULL,
  `email` varchar(150) NOT NULL,
  `nom` varchar(100) DEFAULT NULL,
  `prenom` varchar(100) DEFAULT NULL,
  `date_demande` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `statut` enum('en_attente','traitee','expiree') DEFAULT 'en_attente',
  PRIMARY KEY (`id`),
  KEY `utilisateur_id` (`utilisateur_id`),
  CONSTRAINT `demandes_reinit_mot_de_passe_ibfk_1` FOREIGN KEY (`utilisateur_id`) REFERENCES `utilisateurs` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `emploi_du_temps_creneaux`
--

DROP TABLE IF EXISTS `emploi_du_temps_creneaux`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `emploi_du_temps_creneaux` (
  `emploi_id` int NOT NULL,
  `creneau_id` int NOT NULL,
  PRIMARY KEY (`emploi_id`,`creneau_id`),
  KEY `creneau_id` (`creneau_id`),
  CONSTRAINT `emploi_du_temps_creneaux_ibfk_1` FOREIGN KEY (`emploi_id`) REFERENCES `emplois_du_temps` (`id`) ON DELETE CASCADE,
  CONSTRAINT `emploi_du_temps_creneaux_ibfk_2` FOREIGN KEY (`creneau_id`) REFERENCES `creneaux` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `emplois_du_temps`
--

DROP TABLE IF EXISTS `emplois_du_temps`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `emplois_du_temps` (
  `id` int NOT NULL AUTO_INCREMENT,
  `classe_id` int NOT NULL,
  `periode_type` enum('hebdomadaire','mensuel','semestriel') NOT NULL,
  `periode_debut` date NOT NULL,
  `periode_fin` date NOT NULL,
  `date_creation` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `est_valide` tinyint(1) DEFAULT '0',
  `date_validation` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `classe_id` (`classe_id`),
  CONSTRAINT `emplois_du_temps_ibfk_1` FOREIGN KEY (`classe_id`) REFERENCES `classes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `equipements`
--

DROP TABLE IF EXISTS `equipements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `equipements` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nom` varchar(100) NOT NULL,
  `description` text,
  `quantite` int DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `nom` (`nom`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `matieres`
--

DROP TABLE IF EXISTS `matieres`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `matieres` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nom` varchar(200) NOT NULL,
  `code` varchar(50) NOT NULL,
  `filiere` varchar(100) NOT NULL,
  `volume_horaire` int DEFAULT '0',
  `description` text,
  `date_creation` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reservations`
--

DROP TABLE IF EXISTS `reservations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reservations` (
  `id` int NOT NULL AUTO_INCREMENT,
  `utilisateur_id` int NOT NULL,
  `salle_id` int NOT NULL,
  `motif` varchar(255) NOT NULL,
  `description` text,
  `date_reservation` date NOT NULL,
  `heure_debut` time NOT NULL,
  `heure_fin` time NOT NULL,
  `statut` enum('confirmee','en_cours','terminee','annulee') DEFAULT 'confirmee',
  `date_creation` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `utilisateur_id` (`utilisateur_id`),
  KEY `salle_id` (`salle_id`),
  CONSTRAINT `reservations_ibfk_1` FOREIGN KEY (`utilisateur_id`) REFERENCES `utilisateurs` (`id`) ON DELETE CASCADE,
  CONSTRAINT `reservations_ibfk_2` FOREIGN KEY (`salle_id`) REFERENCES `salles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `salle_equipement`
--

DROP TABLE IF EXISTS `salle_equipement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `salle_equipement` (
  `salle_id` int NOT NULL,
  `equipement_id` int NOT NULL,
  `quantite` int DEFAULT '1',
  PRIMARY KEY (`salle_id`,`equipement_id`),
  KEY `equipement_id` (`equipement_id`),
  CONSTRAINT `salle_equipement_ibfk_1` FOREIGN KEY (`salle_id`) REFERENCES `salles` (`id`) ON DELETE CASCADE,
  CONSTRAINT `salle_equipement_ibfk_2` FOREIGN KEY (`equipement_id`) REFERENCES `equipements` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `salles`
--

DROP TABLE IF EXISTS `salles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `salles` (
  `id` int NOT NULL AUTO_INCREMENT,
  `numero` varchar(20) NOT NULL,
  `capacite` int NOT NULL,
  `type` enum('TD','TP','Amphi','Autre') DEFAULT 'Autre',
  `batiment_id` int NOT NULL,
  `etage` int DEFAULT '0',
  `statut` enum('disponible','indisponible') DEFAULT 'disponible',
  `motif_indisponibilite` text,
  `date_debut_indisponibilite` date DEFAULT NULL,
  `date_fin_indisponibilite` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `batiment_id` (`batiment_id`),
  CONSTRAINT `salles_ibfk_1` FOREIGN KEY (`batiment_id`) REFERENCES `batiments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `signalements`
--

DROP TABLE IF EXISTS `signalements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `signalements` (
  `id` int NOT NULL AUTO_INCREMENT,
  `utilisateur_id` int NOT NULL,
  `salle_id` int NOT NULL,
  `type_probleme` varchar(100) NOT NULL,
  `description` text NOT NULL,
  `date_signalement` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `statut` enum('en_attente','en_cours','resolu') DEFAULT 'en_attente',
  `date_resolution` date DEFAULT NULL,
  `commentaire_resolution` text,
  PRIMARY KEY (`id`),
  KEY `utilisateur_id` (`utilisateur_id`),
  KEY `salle_id` (`salle_id`),
  CONSTRAINT `signalements_ibfk_1` FOREIGN KEY (`utilisateur_id`) REFERENCES `utilisateurs` (`id`) ON DELETE CASCADE,
  CONSTRAINT `signalements_ibfk_2` FOREIGN KEY (`salle_id`) REFERENCES `salles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_equipements`
--

DROP TABLE IF EXISTS `stock_equipements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_equipements` (
  `equipement_id` int NOT NULL,
  `quantite_totale` int DEFAULT '0',
  `quantite_utilisee` int DEFAULT '0',
  PRIMARY KEY (`equipement_id`),
  CONSTRAINT `stock_equipements_ibfk_1` FOREIGN KEY (`equipement_id`) REFERENCES `equipements` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ufr`
--

DROP TABLE IF EXISTS `ufr`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ufr` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nom` varchar(100) NOT NULL,
  `description` text,
  `date_creation` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `nom` (`nom`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `utilisateurs`
--

DROP TABLE IF EXISTS `utilisateurs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `utilisateurs` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nom` varchar(100) NOT NULL,
  `prenom` varchar(100) NOT NULL,
  `email` varchar(150) NOT NULL,
  `mot_de_passe` varchar(255) NOT NULL,
  `role` enum('admin','gestionnaire','enseignant','etudiant') NOT NULL,
  `date_creation` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `est_valide` tinyint(1) DEFAULT '0',
  `date_validation` date DEFAULT NULL,
  `numero_etudiant` varchar(50) DEFAULT NULL,
  `matricule_enseignant` varchar(50) DEFAULT NULL,
  `ufr_id` int DEFAULT NULL,
  `classe_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  KEY `ufr_id` (`ufr_id`),
  KEY `classe_id` (`classe_id`),
  CONSTRAINT `utilisateurs_ibfk_1` FOREIGN KEY (`ufr_id`) REFERENCES `ufr` (`id`) ON DELETE SET NULL,
  CONSTRAINT `utilisateurs_ibfk_2` FOREIGN KEY (`classe_id`) REFERENCES `classes` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- /*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

-- Reset variables supprimé car non déclarées en amont

CREATE TABLE IF NOT EXISTS demandes_reinit_mot_de_passe (
  id INT PRIMARY KEY AUTO_INCREMENT,
  utilisateur_id INT NOT NULL,
  email VARCHAR(150) NOT NULL,
  date_demande TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  traite BOOLEAN DEFAULT FALSE,
  date_traitement TIMESTAMP NULL,
  nouveau_mdp_envoye BOOLEAN DEFAULT FALSE,
  statut VARCHAR(50) DEFAULT 'en_attente',
  FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id) ON DELETE CASCADE
);

UPDATE creneaux SET statut = 'planifie' WHERE statut IS NULL;
ALTER TABLE creneaux MODIFY statut VARCHAR(50) NOT NULL DEFAULT 'planifie';

ALTER TABLE reservations ADD COLUMN type VARCHAR(50) DEFAULT 'AUTRE';
UPDATE reservations SET type = 'AUTRE' WHERE type IS NULL;

-- Ajouter la colonne statut à la table emplois_du_temps
ALTER TABLE emplois_du_temps ADD COLUMN statut VARCHAR(50) DEFAULT 'en_cours';

-- Mettre à jour les enregistrements existants
UPDATE emplois_du_temps SET statut = 'en_cours' WHERE statut IS NULL;
UPDATE emplois_du_temps SET statut = 'termine' WHERE periode_fin < CURDATE();
UPDATE emplois_du_temps SET statut = 'valide' WHERE est_valide = TRUE AND periode_fin >= CURDATE();

CREATE TABLE IF NOT EXISTS historique_reservations (
    id INT PRIMARY KEY AUTO_INCREMENT,
    reservation_id INT NOT NULL,
    action VARCHAR(50) NOT NULL,
    date_action TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    utilisateur_id INT NOT NULL,
    details TEXT,
    FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id) ON DELETE CASCADE
);

-- Index pour accélérer les recherches
CREATE INDEX idx_historique_date ON historique_reservations(date_action);
CREATE INDEX idx_historique_action ON historique_reservations(action);


ALTER TABLE utilisateurs 
ADD COLUMN grade VARCHAR(20) DEFAULT 'Dr.' AFTER matricule_enseignant;

ALTER TABLE utilisateurs 
MODIFY COLUMN grade ENUM('Mr.', 'Mme.', 'Dr.') DEFAULT NULL;

ALTER TABLE demandes_inscription 
MODIFY COLUMN statut ENUM('en_attente', 'validee', 'refusee') DEFAULT 'en_attente';

ALTER TABLE emplois_du_temps 
  MODIFY COLUMN statut ENUM('en_attente', 'en_cours', 'termine') 
  NOT NULL DEFAULT 'en_attente';
  
  ALTER TABLE demandes_inscription MODIFY statut ENUM('en_attente', 'validee', 'refusee') DEFAULT 'en_attente';
  
   alter table emplois_du_temps add column titre varchar(255) null after classe_id;
   ALTER TABLE demandes_inscription ADD COLUMN grade VARCHAR(50);
   
   ALTER TABLE utilisateurs ADD COLUMN type_etudiant VARCHAR(20) DEFAULT 'normal';

UPDATE utilisateurs SET type_etudiant = 'normal' WHERE role = 'etudiant' AND type_etudiant IS NULL;

ALTER TABLE emplois_du_temps ADD COLUMN heures_config VARCHAR(100) DEFAULT '8:2:18';

ALTER TABLE emplois_du_temps ADD COLUMN jours_config VARCHAR(100) DEFAULT 'Lundi,Mardi,Mercredi,Jeudi,Vendredi';


-- TRIGGERS POUR L'HISTORIQUE DES RÉSERVATIONS
-- À EXÉCUTER DANS MySQL

-- Trigger pour l'insertion
DELIMITER $$
CREATE TRIGGER after_reservation_insert
AFTER INSERT ON reservations
FOR EACH ROW
BEGIN
    INSERT INTO historique_reservations (reservation_id, action, utilisateur_id, details)
    VALUES (NEW.id, 'CREATION', NEW.utilisateur_id, 
            CONCAT('Réservation créée le ', NEW.date_reservation, ' de ', NEW.heure_debut, ' à ', NEW.heure_fin));
END$$

-- Trigger pour la modification
CREATE TRIGGER after_reservation_update
AFTER UPDATE ON reservations
FOR EACH ROW
BEGIN
    IF OLD.statut != NEW.statut THEN
        INSERT INTO historique_reservations (reservation_id, action, utilisateur_id, details)
        VALUES (NEW.id, 'CHANGEMENT_STATUT', NEW.utilisateur_id, 
                CONCAT('Statut changé de ', OLD.statut, ' à ', NEW.statut));
    ELSE
        INSERT INTO historique_reservations (reservation_id, action, utilisateur_id, details)
        VALUES (NEW.id, 'MODIFICATION', NEW.utilisateur_id, 
                CONCAT('Réservation modifiée le ', NEW.date_reservation, ' de ', NEW.heure_debut, ' à ', NEW.heure_fin));
    END IF;
END$$

-- Trigger pour la suppression
CREATE TRIGGER after_reservation_delete
AFTER DELETE ON reservations
FOR EACH ROW
BEGIN
    INSERT INTO historique_reservations (reservation_id, action, utilisateur_id, details)
    VALUES (OLD.id, 'SUPPRESSION', OLD.utilisateur_id, 
            CONCAT('Réservation supprimée (', OLD.date_reservation, ' ', OLD.heure_debut, '-', OLD.heure_fin, ')'));
END$$

DELIMITER ;

-- NOUVELLES FONCTIONNALITÉS & SÉCURITÉ UNIV-SCHEDULER v2.0

-- 1. Support des photos pour UFR et Salles
ALTER TABLE `ufr` ADD COLUMN `photo_url` VARCHAR(255) DEFAULT NULL;
ALTER TABLE `salles` ADD COLUMN `photo_url` VARCHAR(255) DEFAULT NULL;

-- 2. Table d'audit légal et de traçabilité immuable
CREATE TABLE IF NOT EXISTS `audit_logs` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `timestamp` DATETIME NOT NULL,
  `utilisateur_email` VARCHAR(150) DEFAULT NULL,
  `role` VARCHAR(50) DEFAULT NULL,
  `action` VARCHAR(80) NOT NULL,
  `entite` VARCHAR(80) NOT NULL,
  `entite_id` INT DEFAULT 0,
  `details` TEXT,
  `adresse_ip` VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_audit_timestamp` (`timestamp`),
  KEY `idx_audit_user` (`utilisateur_email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 3. Bourse d'échange de créneaux (Swap Marketplace)
CREATE TABLE IF NOT EXISTS `echanges_creneaux` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `demandeur_id` INT NOT NULL,
  `cible_id` INT NOT NULL,
  `reservation_demandeur_id` INT NOT NULL,
  `reservation_cible_id` INT NOT NULL,
  `statut` VARCHAR(30) DEFAULT 'EN_ATTENTE',
  `date_demande` DATETIME NOT NULL,
  `date_reponse` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_ech_demandeur` (`demandeur_id`),
  KEY `fk_ech_cible` (`cible_id`),
  CONSTRAINT `fk_ech_demandeur` FOREIGN KEY (`demandeur_id`) REFERENCES `utilisateurs` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ech_cible` FOREIGN KEY (`cible_id`) REFERENCES `utilisateurs` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- FIN DU SCRIPT UNIV-SCHEDULER