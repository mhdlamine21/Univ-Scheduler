-- UNIV-SCHEDULER v2.0 - Script de création de la base de données
-- Université Iba Der Thiam de Thiès

CREATE DATABASE IF NOT EXISTS scheduler_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE scheduler_db;

-- UFR
CREATE TABLE IF NOT EXISTS ufr (
  id   INT AUTO_INCREMENT PRIMARY KEY,
  nom  VARCHAR(200) NOT NULL,
  code VARCHAR(20)  NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Utilisateurs
CREATE TABLE IF NOT EXISTS utilisateurs (
  id                    INT AUTO_INCREMENT PRIMARY KEY,
  nom                   VARCHAR(100) NOT NULL,
  prenom                VARCHAR(100) NOT NULL,
  email                 VARCHAR(255) NOT NULL UNIQUE,
  mot_de_passe          VARCHAR(255) NOT NULL,
  role                  ENUM('admin','gestionnaire','enseignant','etudiant') NOT NULL,
  est_valide            BOOLEAN DEFAULT FALSE,
  date_creation         DATETIME DEFAULT NOW(),
  date_validation       DATETIME,
  numero_etudiant       VARCHAR(50),
  matricule_enseignant  VARCHAR(80),   -- Format: "GRADE|MATRICULE" ex: "Dr.|ENS001"
  ufr_id                INT,
  classe_id             INT,
  FOREIGN KEY (ufr_id) REFERENCES ufr(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Compte admin par défaut (mot de passe: Admin2025)
INSERT IGNORE INTO utilisateurs
  (nom, prenom, email, mot_de_passe, role, est_valide)
VALUES
  ('Admin', 'Système', 'admin@univ-thies.sn',
   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
   'admin', TRUE);

-- Bâtiments
CREATE TABLE IF NOT EXISTS batiments (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  nom         VARCHAR(100) NOT NULL,
  code        VARCHAR(20),
  localisation VARCHAR(200),
  nb_etages   INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Salles
CREATE TABLE IF NOT EXISTS salles (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  numero      VARCHAR(20)  NOT NULL,
  nom         VARCHAR(100),
  capacite    INT          NOT NULL DEFAULT 30,
  type        ENUM('TD','TP','Amphi','Informatique','Reunion','Autre') DEFAULT 'TD',
  batiment_id INT,
  etage       INT DEFAULT 0,
  statut      ENUM('disponible','occupee','maintenance') DEFAULT 'disponible',
  FOREIGN KEY (batiment_id) REFERENCES batiments(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Équipements
CREATE TABLE IF NOT EXISTS equipements (
  id       INT AUTO_INCREMENT PRIMARY KEY,
  nom      VARCHAR(100) NOT NULL,
  type     VARCHAR(50),
  salle_id INT,
  statut   ENUM('fonctionnel','en_panne','maintenance') DEFAULT 'fonctionnel',
  FOREIGN KEY (salle_id) REFERENCES salles(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Classes
CREATE TABLE IF NOT EXISTS classes (
  id        INT AUTO_INCREMENT PRIMARY KEY,
  intitule  VARCHAR(100) NOT NULL,
  code      VARCHAR(20),
  niveau    VARCHAR(20),
  nb_etudiants INT DEFAULT 0,
  ufr_id    INT,
  FOREIGN KEY (ufr_id) REFERENCES ufr(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Matières
CREATE TABLE IF NOT EXISTS matieres (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  nom            VARCHAR(150) NOT NULL,
  code           VARCHAR(30),
  filiere        VARCHAR(100),
  volume_horaire INT DEFAULT 0,
  description    TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Cours
CREATE TABLE IF NOT EXISTS cours (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  matiere_id      INT NOT NULL,
  enseignant_id   INT NOT NULL,
  classe_id       INT NOT NULL,
  type_cours      ENUM('CM','TD','TP','EXAMEN','SOUTENANCE') DEFAULT 'CM',
  groupes         VARCHAR(100),
  volume_horaire  INT DEFAULT 2,
  FOREIGN KEY (matiere_id)    REFERENCES matieres(id) ON DELETE CASCADE,
  FOREIGN KEY (enseignant_id) REFERENCES utilisateurs(id) ON DELETE CASCADE,
  FOREIGN KEY (classe_id)     REFERENCES classes(id)    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Emplois du temps
CREATE TABLE IF NOT EXISTS emplois_du_temps (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  classe_id    INT NOT NULL,
  titre        VARCHAR(200),
  periode_type ENUM('hebdomadaire','mensuel','semestriel') DEFAULT 'hebdomadaire',
  periode_debut DATE,
  periode_fin   DATE,
  statut        ENUM('brouillon','valide','archive') DEFAULT 'brouillon',
  date_creation DATETIME DEFAULT NOW(),
  FOREIGN KEY (classe_id) REFERENCES classes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Créneaux
CREATE TABLE IF NOT EXISTS creneaux (
  id               INT AUTO_INCREMENT PRIMARY KEY,
  cours_id         INT NOT NULL,
  emploi_temps_id  INT,
  jour             DATE NOT NULL,
  heure_debut      TIME NOT NULL,
  heure_fin        TIME NOT NULL,
  salle_id         INT,
  statut           ENUM('planifie','annule','deplace') DEFAULT 'planifie',
  motif_annulation TEXT,
  FOREIGN KEY (cours_id)        REFERENCES cours(id)             ON DELETE CASCADE,
  FOREIGN KEY (emploi_temps_id) REFERENCES emplois_du_temps(id)  ON DELETE SET NULL,
  FOREIGN KEY (salle_id)        REFERENCES salles(id)            ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Réservations
CREATE TABLE IF NOT EXISTS reservations (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  utilisateur_id  INT NOT NULL,
  salle_id        INT NOT NULL,
  type            ENUM('CM','TD','TP','EXAMEN','SOUTENANCE','REUNION','AUTRE') NOT NULL,
  motif           VARCHAR(255),
  description     TEXT,
  date_reservation DATE NOT NULL,
  heure_debut      TIME NOT NULL,
  heure_fin        TIME NOT NULL,
  statut           ENUM('en_cours','confirmee','refusee','annulee') DEFAULT 'en_cours',
  date_creation    DATETIME DEFAULT NOW(),
  classe_id        INT,
  enseignant_id    INT,
  FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id) ON DELETE CASCADE,
  FOREIGN KEY (salle_id)       REFERENCES salles(id)       ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Historique réservations
CREATE TABLE IF NOT EXISTS historique_reservations (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  reservation_id INT,
  utilisateur_id INT,
  action         VARCHAR(100),
  date_action    DATETIME DEFAULT NOW(),
  details        TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Demandes d'inscription
CREATE TABLE IF NOT EXISTS demandes_inscription (
  id                   INT AUTO_INCREMENT PRIMARY KEY,
  nom                  VARCHAR(100) NOT NULL,
  prenom               VARCHAR(100) NOT NULL,
  email                VARCHAR(255) NOT NULL,
  role_demande         ENUM('etudiant','enseignant','gestionnaire','admin') NOT NULL,
  numero_etudiant      VARCHAR(50),
  ufr_id               INT,
  classe_id            INT,
  matricule_enseignant VARCHAR(80),
  statut               ENUM('en_attente','validee','refusee') DEFAULT 'en_attente',
  date_demande         DATETIME DEFAULT NOW()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Demandes réinitialisation mot de passe
CREATE TABLE IF NOT EXISTS demandes_reinit_mdp (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  utilisateur_id INT NOT NULL,
  email          VARCHAR(255) NOT NULL,
  nom            VARCHAR(100),
  prenom         VARCHAR(100),
  statut         ENUM('en_attente','traitee') DEFAULT 'en_attente',
  date_demande   DATETIME DEFAULT NOW(),
  date_traitement DATETIME,
  FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Signalements
CREATE TABLE IF NOT EXISTS signalements (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  utilisateur_id INT NOT NULL,
  salle_id       INT,
  type_probleme  VARCHAR(100),
  description    TEXT,
  statut         ENUM('ouvert','en_cours','resolu') DEFAULT 'ouvert',
  priorite       ENUM('basse','moyenne','haute') DEFAULT 'moyenne',
  date_signalement DATETIME DEFAULT NOW(),
  FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id) ON DELETE CASCADE,
  FOREIGN KEY (salle_id)       REFERENCES salles(id)       ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Notifications
CREATE TABLE IF NOT EXISTS notifications (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  utilisateur_id INT NOT NULL,
  message        TEXT NOT NULL,
  est_lue        BOOLEAN DEFAULT FALSE,
  date_creation  DATETIME DEFAULT NOW(),
  FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Index pour performance
CREATE INDEX IF NOT EXISTS idx_creneaux_jour       ON creneaux(jour);
CREATE INDEX IF NOT EXISTS idx_creneaux_salle       ON creneaux(salle_id);
CREATE INDEX IF NOT EXISTS idx_reservations_date    ON reservations(date_reservation);
CREATE INDEX IF NOT EXISTS idx_reservations_salle   ON reservations(salle_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user   ON notifications(utilisateur_id, est_lue);
