package scheduler.util;

import scheduler.dao.ConnexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.logging.Logger;

/**
 * Script de peuplement exhaustif et cohérent pour tester toutes les interfaces
 * d'UNIV-SCHEDULER : Salles, Bâtiments, Équipements, Cours, Conflits, Réservations,
 * Signalements, Demandes d'inscription, Audit logs.
 */
public class PeuplementBDComplet {

    private static final Logger logger = Logger.getLogger(PeuplementBDComplet.class.getName());

    public static void main(String[] args) {
        System.out.println("⏳ Démarrage du peuplement complet de la base de données...");
        try (Connection conn = ConnexionBD.getConnection();
             Statement stmt = conn.createStatement()) {

            conn.setAutoCommit(false);

            // 1. UFRs
            System.out.println("-> Insertion des UFRs...");
            stmt.executeUpdate("INSERT IGNORE INTO ufr (id, nom, description) VALUES " +
                "(1, 'UFR Sciences et Technologies (UFR SET)', 'Informatique, Mathématiques, Physique et Sciences Appliquées'), " +
                "(2, 'UFR Sciences de l\\'Ingénieur (UFR SI)', 'Génie Civil, Électronique, Télécoms et Mécanique'), " +
                "(3, 'UFR Sciences Économiques et Sociales (UFR SES)', 'Management, Gestion de Projets, Économie Appliquée'), " +
                "(4, 'UFR Sciences de la Santé (UFR Santé)', 'Médecine Générale, Pharmacie, Odontostomatologie');");

            // 2. Bâtiments
            System.out.println("-> Insertion des Bâtiments avec photos...");
            stmt.executeUpdate("INSERT INTO batiments (id, nom, localisation, nb_etages, ufr_id, statut, photo_url) VALUES " +
                "(1, 'Bâtiment A - Informatique & Mathématiques', 'Campus Principal - Allée Centrale', 3, 1, 'disponible', 'images/logo.png'), " +
                "(2, 'Bâtiment B - Physique & Chimie', 'Campus Principal - Aile Ouest', 2, 1, 'disponible', 'images/logo.png'), " +
                "(3, 'Bâtiment C - Génie Civil & Technologies', 'Campus 2 - Technopôle', 4, 2, 'disponible', 'images/logo.png'), " +
                "(4, 'Bâtiment D - Sciences Économiques', 'Campus 2 - Pavillon D', 3, 3, 'disponible', 'images/logo.png'), " +
                "(5, 'Pavillon Médical & Soins', 'Campus Santé - Hôpital Universitaire', 2, 4, 'disponible', 'images/logo.png'), " +
                "(6, 'Complexe des Grands Amphis', 'Esplanade Centrale', 1, 1, 'disponible', 'images/logo.png') " +
                "ON DUPLICATE KEY UPDATE nom=VALUES(nom), localisation=VALUES(localisation), photo_url=VALUES(photo_url);");

            // 3. Équipements
            System.out.println("-> Insertion des Équipements...");
            stmt.executeUpdate("INSERT INTO equipements (id, nom, description) VALUES " +
                "(1, 'Vidéoprojecteur Laser 4K', 'Haute luminosité 5000 lumens, connexion HDMI/sans-fil'), " +
                "(2, 'Sonorisation & Micro Sans-Fil', 'Système amplificateur surround avec 2 micros HF'), " +
                "(3, 'Tableau Blanc Interactif (TBI)', 'Écran tactile 75 pouces avec stylet et logiciel tableau'), " +
                "(4, 'Climatiseur Inverter 18000 BTU', 'Climatisation silencieuse réversible classe A++'), " +
                "(5, 'Postes Informatiques Étudiants Core i7', 'PC de bureau 16Go RAM, SSD NVMe, double écran'), " +
                "(6, 'Baie de Brassage & Switchs Réseau', 'Équipement TP Réseaux Cisco Gigabit managé'), " +
                "(7, 'Caméra de Visioconférence HD', 'Webcam panoramique auto-tracking pour cours hybrides') " +
                "ON DUPLICATE KEY UPDATE nom=VALUES(nom), description=VALUES(description);");

            // 4. Salles
            System.out.println("-> Insertion des Salles avec photos...");
            stmt.executeUpdate("INSERT INTO salles (id, numero, capacite, type, batiment_id, etage, statut, photo_url) VALUES " +
                "(1, 'Amphi Bécaye', 250, 'Amphi', 6, 0, 'disponible', 'images/logo.png'), " +
                "(2, 'Amphi Galilée', 180, 'Amphi', 6, 0, 'disponible', 'images/logo.png'), " +
                "(3, 'Salle TD 101', 45, 'TD', 1, 1, 'disponible', 'images/logo.png'), " +
                "(4, 'Salle TD 102', 45, 'TD', 1, 1, 'disponible', 'images/logo.png'), " +
                "(5, 'Salle TD 103', 40, 'TD', 1, 1, 'indisponible', 'images/logo.png'), " +
                "(6, 'Labo Info A - Développement', 35, 'TP', 1, 2, 'disponible', 'images/logo.png'), " +
                "(7, 'Labo Info B - Réseaux & Systèmes', 30, 'TP', 1, 2, 'disponible', 'images/logo.png'), " +
                "(8, 'Labo Électronique C301', 25, 'TP', 3, 3, 'disponible', 'images/logo.png'), " +
                "(9, 'Salle Conférence Décanat', 20, 'Autre', 4, 1, 'disponible', 'images/logo.png'), " +
                "(10, 'Salle 204 - SES', 60, 'TD', 4, 2, 'disponible', 'images/logo.png'), " +
                "(11, 'Amphi Pasteur (Santé)', 150, 'Amphi', 5, 0, 'disponible', 'images/logo.png'), " +
                "(12, 'Salle Travaux Dirigés SI-12', 50, 'TD', 3, 1, 'disponible', 'images/logo.png') " +
                "ON DUPLICATE KEY UPDATE numero=VALUES(numero), capacite=VALUES(capacite), type=VALUES(type), statut=VALUES(statut), photo_url=VALUES(photo_url);");

            // Affectation des équipements aux salles
            stmt.executeUpdate("DELETE FROM salle_equipement;");
            stmt.executeUpdate("INSERT INTO salle_equipement (salle_id, equipement_id) VALUES " +
                "(1, 1), (1, 2), (1, 4), " + // Amphi Bécaye: Projecteur, Son, Clim
                "(2, 1), (2, 2), (2, 4), " + // Amphi Galilée: Projecteur, Son, Clim
                "(3, 1), (3, 4), " +         // Salle 101: Projecteur, Clim
                "(4, 1), " +                 // Salle 102: Projecteur
                "(6, 1), (6, 4), (6, 5), " + // Labo A: Projecteur, Clim, PC
                "(7, 1), (7, 5), (7, 6), " + // Labo B: Projecteur, PC, Réseaux
                "(9, 1), (9, 4), (9, 7);");  // Décanat: Projecteur, Clim, Visio

            // 5. Classes
            System.out.println("-> Insertion des Classes...");
            stmt.executeUpdate("INSERT INTO classes (id, intitule, filiere, niveau, annee_scolaire, ufr_id, effectif, nb_groupes, est_active) VALUES " +
                "(1, 'Licence 3 Informatique', 'Informatique', 'L3', '2025-2026', 1, 65, 2, TRUE), " +
                "(2, 'Master 1 Génie Logiciel', 'Informatique', 'M1', '2025-2026', 1, 35, 1, TRUE), " +
                "(3, 'Master 2 Sécurité des Systèmes', 'Informatique', 'M2', '2025-2026', 1, 25, 1, TRUE), " +
                "(4, 'Licence 2 Mathématiques Appliquées', 'Mathématiques', 'L2', '2025-2026', 1, 50, 2, TRUE), " +
                "(5, 'Licence 3 Génie Civil', 'Génie Civil', 'L3', '2025-2026', 2, 45, 2, TRUE), " +
                "(6, 'Licence 1 Sciences Économiques', 'Économie', 'L1', '2025-2026', 3, 110, 3, TRUE) " +
                "ON DUPLICATE KEY UPDATE intitule=VALUES(intitule), effectif=VALUES(effectif);");

            // 6. Utilisateurs
            System.out.println("-> Insertion des Utilisateurs...");
            stmt.executeUpdate("INSERT INTO utilisateurs (id, nom, prenom, email, mot_de_passe, role, est_valide, numero_etudiant, matricule_enseignant, ufr_id, classe_id) VALUES " +
                "(1, 'Administrateur', 'Principal', 'admin@univ.sn', 'admin123', 'admin', TRUE, NULL, NULL, 1, NULL), " +
                "(2, 'Gestionnaire', 'Scolarité', 'gestionnaire@univ.sn', 'gest123', 'gestionnaire', TRUE, NULL, NULL, 1, NULL), " +
                "(3, 'Sy', 'Amadou', 'prof@univ.sn', 'prof123', 'enseignant', TRUE, NULL, 'ENS-2021-089', 1, NULL), " +
                "(4, 'Fall', 'Mouhamed', 'etudiant@univ.sn', 'etu123', 'etudiant', TRUE, 'ETU-2023-014', NULL, 1, 1), " +
                "(5, 'Ndiaye', 'Dr. Fatou', 'fatou.ndiaye@univ.sn', 'prof123', 'enseignant', TRUE, NULL, 'ENS-2019-042', 1, NULL), " +
                "(6, 'Diop', 'Dr. Ousmane', 'ousmane.diop@univ.sn', 'prof123', 'enseignant', TRUE, NULL, 'ENS-2018-011', 1, NULL), " +
                "(7, 'Ba', 'Aïssatou', 'aissatou.ba@univ.sn', 'etu123', 'etudiant', TRUE, 'ETU-2024-055', NULL, 1, 2) " +
                "ON DUPLICATE KEY UPDATE email=VALUES(email), mot_de_passe=VALUES(mot_de_passe), role=VALUES(role);");

            // 7. Matières
            System.out.println("-> Insertion des Matières...");
            stmt.executeUpdate("INSERT INTO matieres (id, nom, code, filiere, volume_horaire, description) VALUES " +
                "(1, 'Génie Logiciel & Architecture JavaFX', 'INFO-301', 'Informatique', 45, 'Patrons de conception, MVC, interfaces JavaFX et persistance SQL'), " +
                "(2, 'Algorithmique Avancée & Complexité', 'INFO-302', 'Informatique', 40, 'Structures de données arborescentes et graphes'), " +
                "(3, 'Administration Systèmes & Réseaux', 'INFO-303', 'Informatique', 35, 'Protocoles routage, Linux avancé, virtualisation'), " +
                "(4, 'Optimisation & Recherche Opérationnelle', 'MATH-201', 'Mathématiques', 30, 'Programmation linéaire et algorithme du simplexe'), " +
                "(5, 'Mécanique des Structures & Résistance', 'GC-301', 'Génie Civil', 50, 'RDM avancée et calcul poutres') " +
                "ON DUPLICATE KEY UPDATE nom=VALUES(nom), code=VALUES(code);");

            // 8. Cours
            System.out.println("-> Insertion des Cours...");
            stmt.executeUpdate("INSERT INTO cours (id, matiere_id, enseignant_id, classe_id, type_cours, volume_horaire) VALUES " +
                "(1, 1, 3, 1, 'CM', 20), " + // GL CM par Prof Sy pour L3 Info
                "(2, 1, 3, 1, 'TP', 25), " + // GL TP par Prof Sy pour L3 Info
                "(3, 2, 5, 1, 'CM', 20), " + // Algo CM par Dr Ndiaye pour L3 Info
                "(4, 2, 5, 1, 'TD', 20), " + // Algo TD par Dr Ndiaye pour L3 Info
                "(5, 3, 6, 1, 'TP', 35), " + // Réseaux TP par Dr Diop pour L3 Info
                "(6, 4, 5, 4, 'CM', 30) " +  // Maths CM par Dr Ndiaye pour L2 Maths
                "ON DUPLICATE KEY UPDATE type_cours=VALUES(type_cours), volume_horaire=VALUES(volume_horaire);");

            // 9. Créneaux de planning (semaine en cours et semaine suivante)
            System.out.println("-> Insertion des Créneaux de planning avec simulation de conflit...");
            stmt.executeUpdate("DELETE FROM emploi_du_temps_creneaux;");
            stmt.executeUpdate("DELETE FROM creneaux;");

            LocalDate lundi = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
            String j1 = lundi.toString();                     // Lundi
            String j2 = lundi.plusDays(1).toString();         // Mardi
            String j3 = lundi.plusDays(2).toString();         // Mercredi
            String j4 = lundi.plusDays(3).toString();         // Jeudi
            String j5 = lundi.plusDays(4).toString();         // Vendredi

            stmt.executeUpdate("INSERT INTO creneaux (id, cours_id, jour, heure_debut, heure_fin, salle_id, statut) VALUES " +
                "(1, 1, '" + j1 + "', '08:00:00', '10:00:00', 1, 'planifie'), " + // Lundi 8h GL CM en Amphi Bécaye
                "(2, 3, '" + j1 + "', '10:00:00', '12:00:00', 1, 'planifie'), " + // Lundi 10h Algo CM en Amphi Bécaye
                "(3, 2, '" + j2 + "', '14:00:00', '16:00:00', 6, 'planifie'), " + // Mardi 14h GL TP au Labo Info A
                "(4, 5, '" + j3 + "', '08:00:00', '11:00:00', 7, 'planifie'), " + // Mercredi 8h Réseaux TP au Labo Info B
                "(5, 4, '" + j4 + "', '10:00:00', '12:00:00', 3, 'planifie'), " + // Jeudi 10h Algo TD en Salle 101
                "(6, 6, '" + j4 + "', '10:00:00', '12:00:00', 3, 'planifie'), " + // ⚠️ CONFLIT VOLONTAIRE : Cours 6 en Salle 101 au même horaire !
                "(7, 1, '" + j5 + "', '14:00:00', '16:00:00', 2, 'planifie');");  // Vendredi 14h GL CM en Amphi Galilée

            // 10. Emplois du temps
            stmt.executeUpdate("DELETE FROM emplois_du_temps;");
            stmt.executeUpdate("INSERT INTO emplois_du_temps (id, classe_id, periode_type, periode_debut, periode_fin, est_valide) VALUES " +
                "(1, 1, 'hebdomadaire', '" + j1 + "', '" + j5 + "', TRUE), " +
                "(2, 4, 'hebdomadaire', '" + j1 + "', '" + j5 + "', TRUE);");

            stmt.executeUpdate("INSERT INTO emploi_du_temps_creneaux (emploi_id, creneau_id) VALUES " +
                "(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 7), " +
                "(2, 6);");

            // 11. Réservations
            System.out.println("-> Insertion des Réservations ponctuelles...");
            stmt.executeUpdate("DELETE FROM reservations;");
            stmt.executeUpdate("INSERT INTO reservations (id, utilisateur_id, salle_id, motif, description, date_reservation, heure_debut, heure_fin, statut) VALUES " +
                "(1, 3, 1, 'Conférence Annuelle de l\\'Intelligence Artificielle', 'Séminaire ouvert aux étudiants de master et doctorants', '" + j2 + "', '09:00:00', '12:00:00', 'confirmee'), " +
                "(2, 5, 9, 'Soutenance de Thèse de Doctorat en Maths', 'Jury mixte international - Visioconférence requise', '" + j3 + "', '14:00:00', '17:00:00', 'confirmee'), " +
                "(3, 4, 6, 'Séance de Révision Tutorat Étudiant', 'Préparation aux TP notés de Programmation Java', '" + j4 + "', '16:00:00', '18:00:00', 'confirmee'), " +
                "(4, 2, 2, 'Cérémonie d\\'Accueil des Nouveaux Bacheliers', 'Présentation des filières et remise des livrets pédagogiques', '" + j5 + "', '09:00:00', '12:00:00', 'confirmee'), " +
                "(5, 3, 4, 'Rattrapage Partiel Base de Données', 'Session exceptionnelle de rattrapage pour les absents justifiés', '" + j1 + "', '14:00:00', '16:00:00', 'confirmee');");

            // 12. Signalements d'incidents
            System.out.println("-> Insertion des Signalements d'incidents...");
            stmt.executeUpdate("DELETE FROM signalements;");
            stmt.executeUpdate("INSERT INTO signalements (id, utilisateur_id, salle_id, type_probleme, description, statut, commentaire_resolution, photo_url) VALUES " +
                "(1, 3, 3, 'Panne Vidéoprojecteur', 'L\\'image scintille et l\\'ampoule clignote rouge après 5 minutes d\\'usage.', 'en_cours', 'Technicien mandaté pour remplacement lampe', 'images/logo.png'), " +
                "(2, 4, 7, 'Prise Réseau Défectueuse', 'La prise murale RJ45 du poste 12 ne transmet aucun signal Ethernet.', 'en_attente', NULL, 'images/logo.png'), " +
                "(3, 5, 1, 'Microphone Enseignant Grillé', 'Bruit parasite strident lors de l\\'activation du micro sans-fil principal.', 'resolu', 'Micro remplacé par un nouveau kit Shure HF', 'images/logo.png'), " +
                "(4, 2, 5, 'Fuite Climatiseur', 'Écoulement d\\'eau important le long du mur près du tableau.', 'en_cours', 'Vidange en cours, salle temporairement indisponible', 'images/logo.png');");

            // 13. Demandes d'inscription
            System.out.println("-> Insertion des Demandes d'inscription en attente...");
            stmt.executeUpdate("DELETE FROM demandes_inscription;");
            stmt.executeUpdate("INSERT INTO demandes_inscription (nom, prenom, email, role_demande, numero_etudiant, ufr_id, classe_id, statut) VALUES " +
                "('Sarr', 'Babacar', 'babacar.sarr@univ.sn', 'etudiant', 'ETU-2025-102', 1, 1, 'en_attente'), " +
                "('Diallo', 'Mariama', 'mariama.diallo@univ.sn', 'etudiant', 'ETU-2025-103', 1, 2, 'en_attente'), " +
                "('Camara', 'Dr. Ibrahima', 'ibrahima.camara@univ.sn', 'enseignant', NULL, 1, NULL, 'en_attente');");

            // 14. Audit logs pour tester les rapports et traçabilité
            System.out.println("-> Insertion des Logs d'audit...");
            stmt.executeUpdate("DELETE FROM audit_logs;");
            stmt.executeUpdate("INSERT INTO audit_logs (utilisateur_email, role, action, entite, entite_id, adresse_ip, details, timestamp) VALUES " +
                "('admin@univ.sn', 'ADMIN', 'CREATION_SALLE', 'Salle', 1, '127.0.0.1', 'Création de l\\'Amphi Bécaye avec 250 places', NOW()), " +
                "('admin@univ.sn', 'ADMIN', 'AFFECTATION_EQUIPEMENT', 'Equipement', 1, '127.0.0.1', 'Affectation Vidéoprojecteur 4K à la Salle 101', NOW()), " +
                "('gestionnaire@univ.sn', 'GESTIONNAIRE', 'VALIDATION_EDT', 'EmploiDuTemps', 1, '127.0.0.1', 'Validation de l\\'emploi du temps L3 Info', NOW()), " +
                "('prof@univ.sn', 'ENSEIGNANT', 'RESERVATION_SALLE', 'Reservation', 1, '127.0.0.1', 'Réservation de l\\'Amphi Bécaye pour conférence', NOW()), " +
                "('etudiant@univ.sn', 'ETUDIANT', 'SIGNALEMENT_PANNE', 'Signalement', 1, '127.0.0.1', 'Signalement prise réseau défectueuse Labo B', NOW());");

            conn.commit();
            System.out.println("✅ Peuplement complet réussi avec succès ! Toutes les tables sont garnies de données logiques et interconnectées.");

        } catch (Exception e) {
            System.err.println("❌ Erreur pendant le peuplement de la BD : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
