package scheduler.dao;

import scheduler.modele.HistoriqueReservation;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès à l'historique des réservations.
 */
public class HistoriqueReservationDAO {
    
    public void ajouter(HistoriqueReservation historique) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "INSERT INTO historique_reservations (reservation_id, action, utilisateur_id, details) VALUES (?, ?, ?, ?)";
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            stmt.setInt(1, historique.getReservationId());
            stmt.setString(2, historique.getAction());
            stmt.setInt(3, historique.getUtilisateurId());
            stmt.setString(4, historique.getDetails());
            stmt.executeUpdate();
            
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                historique.setId(rs.getInt(1));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public List<HistoriqueReservation> listerParReservation(int reservationId) throws SQLException {
        List<HistoriqueReservation> liste = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM historique_reservations WHERE reservation_id = ? ORDER BY date_action DESC";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, reservationId);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                liste.add(creerHistoriqueDepuisResultSet(rs));
            }
            return liste;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public List<HistoriqueReservation> listerParUtilisateur(int utilisateurId) throws SQLException {
        List<HistoriqueReservation> liste = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM historique_reservations WHERE utilisateur_id = ? ORDER BY date_action DESC";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, utilisateurId);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                liste.add(creerHistoriqueDepuisResultSet(rs));
            }
            return liste;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public List<HistoriqueReservation> listerTous() throws SQLException {
        List<HistoriqueReservation> liste = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM historique_reservations ORDER BY date_action DESC";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                liste.add(creerHistoriqueDepuisResultSet(rs));
            }
            return liste;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    private HistoriqueReservation creerHistoriqueDepuisResultSet(ResultSet rs) throws SQLException {
        HistoriqueReservation historique = new HistoriqueReservation();
        historique.setId(rs.getInt("id"));
        historique.setReservationId(rs.getInt("reservation_id"));
        historique.setAction(rs.getString("action"));
        historique.setUtilisateurId(rs.getInt("utilisateur_id"));
        historique.setDetails(rs.getString("details"));
        historique.setDateAction(rs.getTimestamp("date_action").toLocalDateTime());
        return historique;
    }
}