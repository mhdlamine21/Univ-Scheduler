package scheduler.dao;

import scheduler.modele.Reservation;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès aux données des réservations de salles.
 */
public class ReservationDAO {
    
    public void ajouter(Reservation reservation) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "INSERT INTO reservations (utilisateur_id, salle_id, type, motif, description, date_reservation, heure_debut, heure_fin, statut, classe_id, enseignant_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, reservation.getUtilisateurId());
            stmt.setInt(2, reservation.getSalleId());
            stmt.setString(3, reservation.getType());
            stmt.setString(4, reservation.getMotif());
            stmt.setString(5, reservation.getDescription());
            stmt.setString(6, reservation.getDateReservation());
            stmt.setString(7, reservation.getHeureDebut());
            stmt.setString(8, reservation.getHeureFin());
            stmt.setString(9, reservation.getStatut());
            if (reservation.getClasseId() != null) stmt.setInt(10, reservation.getClasseId()); else stmt.setNull(10, Types.INTEGER);
            if (reservation.getEnseignantId() != null) stmt.setInt(11, reservation.getEnseignantId()); else stmt.setNull(11, Types.INTEGER);
            stmt.executeUpdate();
            
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                reservation.setId(rs.getInt(1));
            }
            
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }

    /**
     * Réservation concurrente ultra-sécurisée avec verrou pessimiste SELECT FOR UPDATE et transaction ACID.
     */
    public void reserverAvecVerrou(Reservation reservation) throws SQLException {
        Connection conn = null;
        PreparedStatement stmtVerif = null;
        PreparedStatement stmtInsert = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            conn.setAutoCommit(false); // Début transaction ACID

            // 🔒 Verrouillage pessimiste : on vérifie la disponibilité
            // avec chevauchement temporel strict et verrouillage
            String sqlVerif = "SELECT id FROM reservations " +
                              "WHERE salle_id = ? AND date_reservation = ? " +
                              "AND ((heure_debut <= ? AND heure_fin > ?) OR (heure_debut < ? AND heure_fin >= ?) OR (heure_debut >= ? AND heure_fin <= ?)) " +
                              "AND statut NOT IN ('annulee', 'refusee', 'reportee') FOR UPDATE";
            
            stmtVerif = conn.prepareStatement(sqlVerif);
            stmtVerif.setInt(1, reservation.getSalleId());
            stmtVerif.setString(2, reservation.getDateReservation());
            stmtVerif.setString(3, reservation.getHeureDebut());
            stmtVerif.setString(4, reservation.getHeureDebut());
            stmtVerif.setString(5, reservation.getHeureFin());
            stmtVerif.setString(6, reservation.getHeureFin());
            stmtVerif.setString(7, reservation.getHeureDebut());
            stmtVerif.setString(8, reservation.getHeureFin());
            
            rs = stmtVerif.executeQuery();
            if (rs.next()) {
                conn.rollback();
                throw new SQLException("Conflit de réservation : cette salle vient d'être réservée par un autre utilisateur.");
            }

            String sqlInsert = "INSERT INTO reservations (utilisateur_id, salle_id, type, motif, description, date_reservation, heure_debut, heure_fin, statut, classe_id, enseignant_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            stmtInsert = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS);
            stmtInsert.setInt(1, reservation.getUtilisateurId());
            stmtInsert.setInt(2, reservation.getSalleId());
            stmtInsert.setString(3, reservation.getType());
            stmtInsert.setString(4, reservation.getMotif());
            stmtInsert.setString(5, reservation.getDescription());
            stmtInsert.setString(6, reservation.getDateReservation());
            stmtInsert.setString(7, reservation.getHeureDebut());
            stmtInsert.setString(8, reservation.getHeureFin());
            stmtInsert.setString(9, reservation.getStatut());
            if (reservation.getClasseId() != null) stmtInsert.setInt(10, reservation.getClasseId()); else stmtInsert.setNull(10, Types.INTEGER);
            if (reservation.getEnseignantId() != null) stmtInsert.setInt(11, reservation.getEnseignantId()); else stmtInsert.setNull(11, Types.INTEGER);
            stmtInsert.executeUpdate();
            
            ResultSet keys = stmtInsert.getGeneratedKeys();
            if (keys.next()) {
                reservation.setId(keys.getInt(1));
            }
            conn.commit(); // Validation transaction
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (stmtVerif != null) try { stmtVerif.close(); } catch (SQLException ignored) {}
            if (stmtInsert != null) try { stmtInsert.close(); } catch (SQLException ignored) {}
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                ConnexionBD.libererConnection(conn);
            }
        }
    }
    
    public Reservation trouverParId(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Reservation resultat = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM reservations WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                resultat = creerReservationDepuisResultSet(rs);
            }
            
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        
        return resultat;
    }
    
    public List<Reservation> listerParUtilisateur(int utilisateurId) throws SQLException {
        List<Reservation> liste = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM reservations WHERE utilisateur_id = ? ORDER BY date_reservation DESC, heure_debut";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, utilisateurId);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                liste.add(creerReservationDepuisResultSet(rs));
            }
            
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        
        return liste;
    }
    
    public List<Reservation> listerParSalle(int salleId) throws SQLException {
        List<Reservation> liste = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM reservations WHERE salle_id = ? ORDER BY date_reservation, heure_debut";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, salleId);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                liste.add(creerReservationDepuisResultSet(rs));
            }
            
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        
        return liste;
    }
    
    public List<Reservation> listerParDate(String date) throws SQLException {
        List<Reservation> liste = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM reservations WHERE date_reservation = ? ORDER BY heure_debut";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, date);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                liste.add(creerReservationDepuisResultSet(rs));
            }
            
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        
        return liste;
    }
    
    public List<Reservation> listerTous() throws SQLException {
        List<Reservation> liste = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM reservations ORDER BY date_reservation DESC, heure_debut";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                liste.add(creerReservationDepuisResultSet(rs));
            }
            
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        
        return liste;
    }
    
    public List<Reservation> listerEnAttente() throws SQLException {
        List<Reservation> liste = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM reservations WHERE statut = 'en_cours' ORDER BY date_reservation, heure_debut";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                liste.add(creerReservationDepuisResultSet(rs));
            }
            
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        
        return liste;
    }
    
    public boolean verifierDisponibilite(int salleId, String date, String heureDebut, String heureFin) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean disponible = false;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT COUNT(*) FROM reservations WHERE salle_id = ? AND date_reservation = ? AND ((heure_debut <= ? AND heure_fin > ?) OR (heure_debut < ? AND heure_fin >= ?)) AND statut NOT IN ('annulee', 'refusee', 'reportee')";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, salleId);
            stmt.setString(2, date);
            stmt.setString(3, heureDebut);
            stmt.setString(4, heureDebut);
            stmt.setString(5, heureFin);
            stmt.setString(6, heureFin);
            
            rs = stmt.executeQuery();
            if (rs.next()) {
                disponible = rs.getInt(1) == 0;
            }
            
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        
        return disponible;
    }
    
    public void annuler(int id) throws SQLException {
        executerUpdateSimple("UPDATE reservations SET statut = 'annulee' WHERE id = ?", id);
    }
    
    public void valider(int id) throws SQLException {
        executerUpdateSimple("UPDATE reservations SET statut = 'confirmee' WHERE id = ?", id);
    }
    
    public void refuser(int id) throws SQLException {
        executerUpdateSimple("UPDATE reservations SET statut = 'refusee' WHERE id = ?", id);
    }
    
    public void prolonger(int id, String nouvelleHeureFin) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "UPDATE reservations SET heure_fin = ? WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, nouvelleHeureFin);
            stmt.setInt(2, id);
            stmt.executeUpdate();
            
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void modifier(Reservation reservation) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "UPDATE reservations SET type = ?, motif = ?, description = ?, date_reservation = ?, heure_debut = ?, heure_fin = ?, statut = ? WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, reservation.getType());
            stmt.setString(2, reservation.getMotif());
            stmt.setString(3, reservation.getDescription());
            stmt.setString(4, reservation.getDateReservation());
            stmt.setString(5, reservation.getHeureDebut());
            stmt.setString(6, reservation.getHeureFin());
            stmt.setString(7, reservation.getStatut());
            stmt.setInt(8, reservation.getId());
            stmt.executeUpdate();
            
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void supprimer(int id) throws SQLException {
        executerUpdateSimple("DELETE FROM reservations WHERE id = ?", id);
    }
    
    private void executerUpdateSimple(String sql, int id) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            stmt.executeUpdate();
            
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public Reservation trouverConflit(int salleId, String date, String heureDebut, String heureFin) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Reservation resultat = null;
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM reservations WHERE salle_id = ? AND date_reservation = ? " +
                         "AND ((heure_debut <= ? AND heure_fin > ?) OR (heure_debut < ? AND heure_fin >= ?) OR (heure_debut >= ? AND heure_fin <= ?)) " +
                         "AND statut NOT IN ('annulee', 'refusee', 'reportee') ORDER BY id DESC LIMIT 1";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, salleId);
            stmt.setString(2, date);
            stmt.setString(3, heureDebut);
            stmt.setString(4, heureDebut);
            stmt.setString(5, heureFin);
            stmt.setString(6, heureFin);
            stmt.setString(7, heureDebut);
            stmt.setString(8, heureFin);
            rs = stmt.executeQuery();
            if (rs.next()) {
                resultat = creerReservationDepuisResultSet(rs);
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
            ConnexionBD.libererConnection(conn);
        }
        return resultat;
    }

    private Reservation creerReservationDepuisResultSet(ResultSet rs) throws SQLException {
        Reservation reservation = new Reservation();
        reservation.setId(rs.getInt("id"));
        reservation.setUtilisateurId(rs.getInt("utilisateur_id"));
        reservation.setSalleId(rs.getInt("salle_id"));
        reservation.setType(rs.getString("type"));
        reservation.setMotif(rs.getString("motif"));
        reservation.setDescription(rs.getString("description"));
        reservation.setDateReservation(rs.getString("date_reservation"));
        reservation.setHeureDebut(rs.getString("heure_debut"));
        reservation.setHeureFin(rs.getString("heure_fin"));
        reservation.setStatut(rs.getString("statut"));
        try {
            int cId = rs.getInt("classe_id");
            if (!rs.wasNull()) reservation.setClasseId(cId);
        } catch (SQLException ignored) {}
        try {
            int eId = rs.getInt("enseignant_id");
            if (!rs.wasNull()) reservation.setEnseignantId(eId);
        } catch (SQLException ignored) {}
        return reservation;
    }
}