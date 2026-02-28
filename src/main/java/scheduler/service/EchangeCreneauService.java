package scheduler.service;

import scheduler.dao.ConnexionBD;
import scheduler.dao.ReservationDAO;
import scheduler.dao.SalleDAO;
import scheduler.dao.UtilisateurDAO;
import scheduler.modele.Etudiant;
import scheduler.modele.Reservation;
import scheduler.modele.Salle;
import scheduler.modele.Utilisateur;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service gérant :
 * 1. La Bourse d'échange de créneaux / permutation de salles entre enseignants.
 * 2. Le signalement d'absence d'urgence enseignant avec alerte immédiate aux délégués et libération de salle.
 */
public class EchangeCreneauService {

    private static final Logger logger = LoggerFactory.getLogger(EchangeCreneauService.class);

    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final SalleDAO salleDAO = new SalleDAO();
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private final EmailService emailService = new EmailService();
    private final NotificationService notifService = new NotificationService();

    private static volatile boolean tableVerifiee = false;

    private void initialiserTable(Connection conn) {
        if (tableVerifiee) return;
        String sql = "CREATE TABLE IF NOT EXISTS echanges_creneaux (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "demandeur_id INT NOT NULL, " +
                "cible_id INT NOT NULL, " +
                "reservation_demandeur_id INT DEFAULT NULL, " +
                "reservation_cible_id INT NOT NULL, " +
                "motif_urgence VARCHAR(255), " +
                "message TEXT, " +
                "statut VARCHAR(30) DEFAULT 'EN_ATTENTE', " +
                "date_demande DATETIME NOT NULL, " +
                "date_reponse DATETIME" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        String sqlReport = "CREATE TABLE IF NOT EXISTS cours_reportes (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "reservation_id INT, " +
                "enseignant_id INT NOT NULL, " +
                "classe_id INT, " +
                "matiere VARCHAR(255), " +
                "motif_report TEXT, " +
                "date_initiale VARCHAR(20), " +
                "heure_debut_initiale VARCHAR(10), " +
                "heure_fin_initiale VARCHAR(10), " +
                "salle_initiale_id INT, " +
                "statut VARCHAR(50) DEFAULT 'A_REPROGRAMMER', " +
                "date_report DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "nouvelle_reservation_id INT" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            stmt.execute(sqlReport);
            try { stmt.execute("ALTER TABLE echanges_creneaux ADD COLUMN motif_urgence VARCHAR(255)"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE echanges_creneaux ADD COLUMN message TEXT"); } catch (Exception ignored) {}
            tableVerifiee = true;
        } catch (SQLException e) {
            logger.warn("Initialisation tables échanges/reports : {}", e.getMessage());
        }
    }

    /**
     * Propose un échange bilatéral de créneau entre deux enseignants.
     */
    public boolean proposerEchange(int demandeurId, int cibleId, int resDemandeurId, int resCibleId) throws SQLException {
        Connection conn = null;
        try {
            conn = ConnexionBD.getConnection();
            if (conn == null) return false;
            initialiserTable(conn);

            String sql = "INSERT INTO echanges_creneaux (demandeur_id, cible_id, reservation_demandeur_id, reservation_cible_id, statut, date_demande) " +
                         "VALUES (?, ?, ?, ?, 'EN_ATTENTE', ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, demandeurId);
                ps.setInt(2, cibleId);
                ps.setInt(3, resDemandeurId);
                ps.setInt(4, resCibleId);
                ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
                ps.executeUpdate();
            }

            Utilisateur demandeur = utilisateurDAO.trouverParId(demandeurId);
            Utilisateur cible = utilisateurDAO.trouverParId(cibleId);
            Reservation resDemandeur = reservationDAO.trouverParId(resDemandeurId);

            if (cible != null && demandeur != null && resDemandeur != null) {
                String sujet = "🔄 Proposition d'échange de créneau de cours";
                String msg = String.format("Bonjour %s,\n\nVotre collègue %s %s souhaite échanger son créneau du %s (%s-%s) avec l'un de vos cours.\n"
                        + "Connectez-vous sur Univ-Scheduler pour accepter ou décliner cette permutation.",
                        cible.getPrenom(), demandeur.getPrenom(), demandeur.getNom(),
                        resDemandeur.getDateReservation(), resDemandeur.getHeureDebut(), resDemandeur.getHeureFin());
                emailService.envoyerEmail(cible.getEmail(), sujet, msg);
                notifService.ajouterNotification(cibleId, "🔄 Demande d'échange de créneau reçue de " + demandeur.getPrenom() + " " + demandeur.getNom());
            }

            AuditService.log(
                demandeur != null ? demandeur.getEmail() : "user#" + demandeurId,
                "enseignant",
                "DEMANDE_ECHANGE_CRENEAU",
                "ECHANGE",
                resDemandeurId,
                "Demande d'échange envoyée à l'enseignant #" + cibleId
            );

            return true;
        } finally {
            if (conn != null) ConnexionBD.libererConnection(conn);
        }
    }

    /**
     * Valide et exécute la permutation atomique de deux réservations.
     */
    public boolean accepterEchange(int echangeId) throws SQLException {
        Connection conn = null;
        try {
            conn = ConnexionBD.getConnection();
            if (conn == null) return false;
            conn.setAutoCommit(false);

            String sqlSelect = "SELECT * FROM echanges_creneaux WHERE id = ? AND statut = 'EN_ATTENTE' FOR UPDATE";
            int resDemId = 0, resCibId = 0, demId = 0, cibId = 0;
            try (PreparedStatement ps = conn.prepareStatement(sqlSelect)) {
                ps.setInt(1, echangeId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    resDemId = rs.getInt("reservation_demandeur_id");
                    resCibId = rs.getInt("reservation_cible_id");
                    demId = rs.getInt("demandeur_id");
                    cibId = rs.getInt("cible_id");
                }
            }

            // Permuter les propriétaires des deux réservations
            String sqlSwap1 = "UPDATE reservations SET utilisateur_id = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlSwap1)) {
                ps.setInt(1, cibId);
                ps.setInt(2, resDemId);
                ps.executeUpdate();

                ps.setInt(1, demId);
                ps.setInt(2, resCibId);
                ps.executeUpdate();
            }

            // Mettre à jour le statut de l'échange
            String sqlMaj = "UPDATE echanges_creneaux SET statut = 'ACCEPTE', date_reponse = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlMaj)) {
                ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                ps.setInt(2, echangeId);
                ps.executeUpdate();
            }

            conn.commit();

            AuditService.log("systeme", "gestionnaire", "ECHANGE_VALIDE", "ECHANGE", echangeId,
                    "Permutation réussie entre les réservations #" + resDemId + " et #" + resCibId);

            return true;
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                ConnexionBD.libererConnection(conn);
            }
        }
    }

    /**
     * Gère l'absence d'urgence d'un enseignant :
     * 1. Annule immédiatement la réservation / libère la salle.
     * 2. Notifie immédiatement les étudiants et délégués de la classe.
     * 3. Alerte les gestionnaires de la libération inattendue du créneau.
     * 4. Enregistre la traçabilité dans les logs d'audit.
     */
    public boolean declarerAbsenceUrgenteEnseignant(int enseignantId, int reservationId, String motifAbsence) throws SQLException {
        Reservation res = reservationDAO.trouverParId(reservationId);
        if (res == null) return false;

        Utilisateur prof = utilisateurDAO.trouverParId(enseignantId);
        Salle salle = salleDAO.trouverParId(res.getSalleId());
        String nomSalle = salle != null ? salle.getNumero() : "Salle #" + res.getSalleId();

        // 1. Libérer la salle (statut 'annulee' avec motif urgence)
        reservationDAO.annuler(reservationId);

        // 2. Diffuser l'alerte d'urgence
        String profNom = prof != null ? (prof.getPrenom() + " " + prof.getNom()) : "Votre enseignant";
        String sujet = "🚨 ALERTE CAMPUS : Cours Annulé d'Urgence - " + res.getDateReservation();
        String message = String.format(
                "URGENT / INFORMATION ÉTUDIANTS :\n\n" +
                "M./Mme %s signale un empêchement majeur pour le cours prévu le %s (%s - %s) en %s.\n\n" +
                "Matière / Motif : %s\n" +
                "Précision : %s\n\n" +
                "Le cours est donc reporté. La salle est désormais libérée.\n\n" +
                "- L'administration Univ-Scheduler",
                profNom, res.getDateReservation(), res.getHeureDebut(), res.getHeureFin(), nomSalle,
                res.getMotif(), (motifAbsence != null ? motifAbsence : "Raison imprévue de santé/force majeure")
        );

        // Envoi asynchrone des emails et alertes
        new Thread(() -> {
            try {
                // Notifier les gestionnaires du campus
                List<Utilisateur> gestionnaires = utilisateurDAO.listerParRole("gestionnaire");
                for (Utilisateur g : gestionnaires) {
                    emailService.envoyerEmail(g.getEmail(), sujet + " - Salle: " + nomSalle, message);
                    notifService.ajouterNotification(g.getId(), "🚨 Salle " + nomSalle + " libérée d'urgence suite à l'absence de " + profNom);
                }
            } catch (Exception ex) {
                logger.warn("Erreur diffusion urgence absence : {}", ex.getMessage());
            }
        }).start();

        // 3. Log d'audit immuable
        AuditService.log(
                prof != null ? prof.getEmail() : "user#" + enseignantId,
                "enseignant",
                "ABSENCE_URGENTE_DECLAREE",
                "RESERVATION",
                reservationId,
                String.format("Absence déclarée par %s | Salle %s libérée le %s (%s-%s) | Raison: %s",
                        profNom, nomSalle, res.getDateReservation(), res.getHeureDebut(), res.getHeureFin(), motifAbsence)
        );

        return true;
    }

    /**
     * Liste les demandes d'échange en attente pour un enseignant.
     */
    public List<Map<String, Object>> listerDemandesRecues(int enseignantId) {
        List<Map<String, Object>> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = ConnexionBD.getConnection();
            if (conn == null) return list;
            initialiserTable(conn);

            String sql = "SELECT * FROM echanges_creneaux WHERE cible_id = ? AND statut = 'EN_ATTENTE' ORDER BY date_demande DESC";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, enseignantId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", rs.getInt("id"));
                        map.put("demandeurId", rs.getInt("demandeur_id"));
                        map.put("reservationDemandeurId", rs.getInt("reservation_demandeur_id"));
                        map.put("reservationCibleId", rs.getInt("reservation_cible_id"));
                        map.put("dateDemande", rs.getTimestamp("date_demande"));
                        list.add(map);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Erreur listing échanges : {}", e.getMessage());
        } finally {
            if (conn != null) ConnexionBD.libererConnection(conn);
        }
        return list;
    }

    /**
     * Initie une demande de libération de salle ou négociation de créneau avec message d'urgence.
     */
    public boolean demanderLiberation(int demandeurId, int cibleId, Integer resDemandeurId, int resCibleId, String motifUrgence, String message) throws SQLException {
        Connection conn = null;
        try {
            conn = ConnexionBD.getConnection();
            if (conn == null) return false;
            initialiserTable(conn);

            String sql = "INSERT INTO echanges_creneaux (demandeur_id, cible_id, reservation_demandeur_id, reservation_cible_id, motif_urgence, message, statut, date_demande) " +
                         "VALUES (?, ?, ?, ?, ?, ?, 'EN_ATTENTE', ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, demandeurId);
                ps.setInt(2, cibleId);
                if (resDemandeurId != null && resDemandeurId > 0) ps.setInt(3, resDemandeurId); else ps.setNull(3, Types.INTEGER);
                ps.setInt(4, resCibleId);
                ps.setString(5, motifUrgence != null ? motifUrgence : "Urgence pédagogique");
                ps.setString(6, message != null ? message : "");
                ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
                ps.executeUpdate();
            }

            Utilisateur demandeur = utilisateurDAO.trouverParId(demandeurId);
            Utilisateur cible = utilisateurDAO.trouverParId(cibleId);
            Reservation resCible = reservationDAO.trouverParId(resCibleId);

            if (cible != null && demandeur != null && resCible != null) {
                String sujet = "⚡ Demande urgente de libération de salle - " + resCible.getDateReservation();
                String msg = String.format("Bonjour %s %s,\n\n"
                        + "Votre collègue %s %s sollicite la libération ou l'échange de la salle réservée pour votre cours du %s (%s-%s).\n\n"
                        + "Motif d'urgence : %s\n"
                        + "Message : %s\n\n"
                        + "Vous pouvez accepter et reporter votre session à une date ultérieure, ou refuser directement depuis votre espace Univ-Scheduler.\n\n"
                        + "L'équipe UNIV-SCHEDULER",
                        cible.getPrenom(), cible.getNom(), demandeur.getPrenom(), demandeur.getNom(),
                        resCible.getDateReservation(), resCible.getHeureDebut(), resCible.getHeureFin(),
                        motifUrgence, message);
                emailService.envoyerEmail(cible.getEmail(), sujet, msg);
                notifService.ajouterNotification(cibleId, "⚡ Négociation/Libération reçue de " + demandeur.getPrenom() + " " + demandeur.getNom() + " : " + motifUrgence);
            }

            AuditService.log(
                demandeur != null ? demandeur.getEmail() : "user#" + demandeurId,
                "enseignant",
                "DEMANDE_LIBERATION_CRENEAU",
                "ECHANGE",
                resCibleId,
                "Demande urgente envoyée à #" + cibleId + " (" + motifUrgence + ")"
            );

            return true;
        } finally {
            if (conn != null) ConnexionBD.libererConnection(conn);
        }
    }

    /**
     * L'occupant accepte de libérer la salle et reporte son cours à une date ultérieure.
     */
    public boolean accepterEtReporter(int echangeId, String motifReport) throws SQLException {
        Connection conn = null;
        try {
            conn = ConnexionBD.getConnection();
            if (conn == null) return false;
            conn.setAutoCommit(false);
            initialiserTable(conn);

            String sqlSelect = "SELECT * FROM echanges_creneaux WHERE id = ? FOR UPDATE";
            int resCibleId = 0, cibleId = 0, demandeurId = 0;
            String motifUrgence = "";
            try (PreparedStatement ps = conn.prepareStatement(sqlSelect)) {
                ps.setInt(1, echangeId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { conn.rollback(); return false; }
                    resCibleId = rs.getInt("reservation_cible_id");
                    cibleId = rs.getInt("cible_id");
                    demandeurId = rs.getInt("demandeur_id");
                    motifUrgence = rs.getString("motif_urgence");
                }
            }

            Reservation resCible = reservationDAO.trouverParId(resCibleId);
            if (resCible == null) { conn.rollback(); return false; }

            // 1. Mettre à jour le statut de la réservation cible en 'reportee'
            String sqlRes = "UPDATE reservations SET statut = 'reportee' WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlRes)) {
                ps.setInt(1, resCibleId);
                ps.executeUpdate();
            }

            // 2. Enregistrer la session dans cours_reportes
            String sqlInsRep = "INSERT INTO cours_reportes (reservation_id, enseignant_id, classe_id, matiere, motif_report, date_initiale, heure_debut_initiale, heure_fin_initiale, salle_initiale_id, statut) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'A_REPROGRAMMER')";
            try (PreparedStatement ps = conn.prepareStatement(sqlInsRep)) {
                ps.setInt(1, resCibleId);
                ps.setInt(2, cibleId);
                if (resCible.getClasseId() != null) ps.setInt(3, resCible.getClasseId()); else ps.setNull(3, Types.INTEGER);
                ps.setString(4, resCible.getMotif());
                ps.setString(5, motifReport != null && !motifReport.isBlank() ? motifReport : ("Cession accordée : " + motifUrgence));
                ps.setString(6, resCible.getDateReservation());
                ps.setString(7, resCible.getHeureDebut());
                ps.setString(8, resCible.getHeureFin());
                ps.setInt(9, resCible.getSalleId());
                ps.executeUpdate();
            }

            // 3. Valider l'échange
            String sqlUpEch = "UPDATE echanges_creneaux SET statut = 'ACCEPTE', date_reponse = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlUpEch)) {
                ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                ps.setInt(2, echangeId);
                ps.executeUpdate();
            }

            conn.commit();

            // Notifier le demandeur
            Utilisateur demandeur = utilisateurDAO.trouverParId(demandeurId);
            Utilisateur cible = utilisateurDAO.trouverParId(cibleId);
            if (demandeur != null && cible != null) {
                notifService.ajouterNotification(demandeurId, "✅ " + cible.getPrenom() + " " + cible.getNom() + " a accepté de libérer la salle du " + resCible.getDateReservation() + " !");
                emailService.envoyerEmail(demandeur.getEmail(), "✅ Salle libérée avec succès",
                        "Bonjour " + demandeur.getPrenom() + ",\n\nVotre collègue " + cible.getPrenom() + " " + cible.getNom()
                        + " a accepté de différer sa séance. Le créneau est désormais libre pour votre réservation.");
            }

            // Notifier les étudiants de la classe reportée
            if (resCible.getClasseId() != null) {
                new Thread(() -> {
                    try {
                        List<Utilisateur> etudiants = utilisateurDAO.listerParRole("etudiant");
                        String sujet = "📅 Séance Reportée - " + resCible.getMotif();
                        String corps = "Bonjour,\n\nLe cours de " + resCible.getMotif() + " prévu le " + resCible.getDateReservation()
                                + " de " + resCible.getHeureDebut() + " à " + resCible.getHeureFin() + " a été reporté à une date ultérieure.\n"
                                + "Vous serez informés dès que la nouvelle date sera fixée.\n\nCordialement,\nUNIV-SCHEDULER";
                        for (Utilisateur u : etudiants) {
                            if (u instanceof Etudiant et && et.getClasseId() == resCible.getClasseId()) {
                                emailService.envoyerEmail(et.getEmail(), sujet, corps);
                            }
                        }
                    } catch (Exception ex) {
                        logger.warn("Erreur alerte étudiants report : {}", ex.getMessage());
                    }
                }).start();
            }

            AuditService.log("systeme", "enseignant", "COURS_REPORTE", "RESERVATION", resCibleId,
                    "Séance #" + resCibleId + " reportée et créneau libéré suite à la demande #" + echangeId);

            return true;
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                ConnexionBD.libererConnection(conn);
            }
        }
    }

    /**
     * Refuse une demande de libération.
     */
    public boolean refuserDemande(int echangeId, String motifRefus) throws SQLException {
        Connection conn = null;
        try {
            conn = ConnexionBD.getConnection();
            if (conn == null) return false;
            initialiserTable(conn);

            String sql = "UPDATE echanges_creneaux SET statut = 'REFUSE', date_reponse = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                ps.setInt(2, echangeId);
                ps.executeUpdate();
            }

            String sqlSel = "SELECT * FROM echanges_creneaux WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlSel)) {
                ps.setInt(1, echangeId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int demId = rs.getInt("demandeur_id");
                        int cibId = rs.getInt("cible_id");
                        Utilisateur dem = utilisateurDAO.trouverParId(demId);
                        Utilisateur cib = utilisateurDAO.trouverParId(cibId);
                        if (dem != null && cib != null) {
                            notifService.ajouterNotification(demId, "❌ Votre demande de libération de salle a été déclinée par " + cib.getPrenom() + " " + cib.getNom());
                            emailService.envoyerEmail(dem.getEmail(), "❌ Demande de libération non accordée",
                                    "Bonjour " + dem.getPrenom() + ",\n\nVotre collègue " + cib.getPrenom() + " " + cib.getNom()
                                    + " ne peut malheureusement pas libérer son créneau pour la raison suivante :\n"
                                    + (motifRefus != null ? motifRefus : "Impératif de calendrier non modifiable") + ".");
                        }
                    }
                }
            }
            return true;
        } finally {
            if (conn != null) ConnexionBD.libererConnection(conn);
        }
    }

    /**
     * Liste les cours reportés à reprogrammer pour un enseignant ou pour tout le campus (si enseignantId <= 0).
     */
    public List<Map<String, Object>> listerCoursReportes(int enseignantId) {
        List<Map<String, Object>> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = ConnexionBD.getConnection();
            if (conn == null) return list;
            initialiserTable(conn);

            String sql = enseignantId > 0
                    ? "SELECT * FROM cours_reportes WHERE enseignant_id = ? AND statut = 'A_REPROGRAMMER' ORDER BY date_report DESC"
                    : "SELECT * FROM cours_reportes WHERE statut = 'A_REPROGRAMMER' ORDER BY date_report DESC";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (enseignantId > 0) ps.setInt(1, enseignantId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", rs.getInt("id"));
                        map.put("reservationId", rs.getInt("reservation_id"));
                        map.put("enseignantId", rs.getInt("enseignant_id"));
                        map.put("classeId", rs.getInt("classe_id"));
                        map.put("matiere", rs.getString("matiere"));
                        map.put("motifReport", rs.getString("motif_report"));
                        map.put("dateInitiale", rs.getString("date_initiale"));
                        map.put("heureDebutInitiale", rs.getString("heure_debut_initiale"));
                        map.put("heureFinInitiale", rs.getString("heure_fin_initiale"));
                        map.put("salleInitialeId", rs.getInt("salle_initiale_id"));
                        map.put("statut", rs.getString("statut"));
                        map.put("dateReport", rs.getTimestamp("date_report"));
                        list.add(map);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Erreur listing cours reportés : {}", e.getMessage());
        } finally {
            if (conn != null) ConnexionBD.libererConnection(conn);
        }
        return list;
    }

    /**
     * Reprogramme un cours préalablement reporté sur un nouveau créneau disponible.
     */
    public boolean reprogrammerCours(int coursReporteId, String date, String heureDebut, String heureFin, int salleId) throws SQLException {
        Connection conn = null;
        try {
            conn = ConnexionBD.getConnection();
            if (conn == null) return false;
            initialiserTable(conn);

            String sqlSel = "SELECT * FROM cours_reportes WHERE id = ? FOR UPDATE";
            int ensId = 0, classeId = 0;
            String matiere = "";
            try (PreparedStatement ps = conn.prepareStatement(sqlSel)) {
                ps.setInt(1, coursReporteId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return false;
                    ensId = rs.getInt("enseignant_id");
                    classeId = rs.getInt("classe_id");
                    matiere = rs.getString("matiere");
                }
            }

            // Créer une nouvelle réservation validée
            Reservation nouvelleRes = new Reservation(ensId, salleId, "CM", matiere, date, heureDebut, heureFin);
            if (classeId > 0) nouvelleRes.setClasseId(classeId);
            nouvelleRes.setStatut("confirmee");
            reservationDAO.reserverAvecVerrou(nouvelleRes);

            // Mettre à jour l'entrée cours_reportes
            String sqlUp = "UPDATE cours_reportes SET statut = 'REPROGRAMME', nouvelle_reservation_id = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlUp)) {
                ps.setInt(1, nouvelleRes.getId());
                ps.setInt(2, coursReporteId);
                ps.executeUpdate();
            }

            // Alerte classe
            if (classeId > 0) {
                final int fClasseId = classeId;
                final String fMatiere = matiere;
                new Thread(() -> {
                    try {
                        List<Utilisateur> etudiants = utilisateurDAO.listerParRole("etudiant");
                        Salle salle = salleDAO.trouverParId(salleId);
                        String nomSalle = salle != null ? salle.getNumero() : "Salle #" + salleId;
                        String sujet = "✅ Nouveau Créneau Reprogrammé - " + fMatiere;
                        String corps = "Bonjour,\n\nVotre séance de " + fMatiere + " a été reprogrammée avec succès :\n\n"
                                + "📅 Nouvelle Date : " + date + "\n"
                                + "⏰ Horaire      : " + heureDebut + " - " + heureFin + "\n"
                                + "🏫 Salle        : " + nomSalle + "\n\n"
                                + "Cordialement,\nUNIV-SCHEDULER";
                        for (Utilisateur u : etudiants) {
                            if (u instanceof Etudiant et && et.getClasseId() == fClasseId) {
                                emailService.envoyerEmail(et.getEmail(), sujet, corps);
                            }
                        }
                    } catch (Exception ex) {
                        logger.warn("Erreur alerte étudiants reprogrammation : {}", ex.getMessage());
                    }
                }).start();
            }

            AuditService.log("enseignant#" + ensId, "enseignant", "COURS_REPROGRAMME", "RESERVATION", nouvelleRes.getId(),
                    "Cours " + matiere + " reprogrammé le " + date + " (" + heureDebut + "-" + heureFin + ") en salle #" + salleId);

            return true;
        } finally {
            if (conn != null) ConnexionBD.libererConnection(conn);
        }
    }
}
