package scheduler.dao;

import scheduler.modele.Salle;
import scheduler.modele.Signalement;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Accès aux signalements de problèmes.
 */
public class SignalementDAO {
    
    public void ajouter(Signalement signalement) throws SQLException {
        String sql = "INSERT INTO signalements (utilisateur_id, salle_id, type_probleme, description, statut) VALUES (?, ?, ?, ?, ?)";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            stmt.setInt(1, signalement.getUtilisateurId());
            stmt.setInt(2, signalement.getSalleId());
            stmt.setString(3, signalement.getTypeProbleme());
            stmt.setString(4, signalement.getDescription());
            stmt.setString(5, signalement.getStatut() != null ? signalement.getStatut() : "en_attente");
            
            stmt.executeUpdate();
            
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                signalement.setId(rs.getInt(1));
            }
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public int compterNonTraites() throws SQLException {
        String sql = "SELECT COUNT(*) FROM signalements WHERE statut IN ('en_attente', 'en_cours')";
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
            
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public Map<String, Integer> getStatistiquesParMois() throws SQLException {
        Map<String, Integer> stats = new LinkedHashMap<>();
        String sql = "SELECT DATE_FORMAT(date_signalement, '%Y-%m') as mois, COUNT(*) as total FROM signalements GROUP BY mois ORDER BY mois DESC LIMIT 12";
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                stats.put(rs.getString("mois"), rs.getInt("total"));
            }
            
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        
        return stats;
    }
    
    public Map<String, Integer> getStatistiquesParType() throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT type_probleme, COUNT(*) as total FROM signalements GROUP BY type_probleme";
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                stats.put(rs.getString("type_probleme"), rs.getInt("total"));
            }
            
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        
        return stats;
    }
    
    public List<Map<String, Object>> getTopSallesAvecProblemes() throws SQLException {
        List<Map<String, Object>> resultats = new ArrayList<>();
        String sql = "SELECT salle_id, COUNT(*) as total FROM signalements GROUP BY salle_id ORDER BY total DESC LIMIT 5";
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        SalleDAO salleDAO = new SalleDAO();
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                Map<String, Object> item = new HashMap<>();
                int salleId = rs.getInt("salle_id");
                item.put("salleId", salleId);
                item.put("total", rs.getInt("total"));
                
                try {
                    Salle salle = salleDAO.trouverParId(salleId);
                    item.put("salleNumero", salle != null ? salle.getNumero() : "Salle " + salleId);
                } catch (SQLException e) {
                    item.put("salleNumero", "Salle " + salleId);
                }
                
                resultats.add(item);
            }
            
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        
        return resultats;
    }
    
    public double getTempsMoyenResolution() throws SQLException {
        String sql = "SELECT AVG(DATEDIFF(date_resolution, date_signalement)) as moyenne FROM signalements WHERE statut = 'resolu' AND date_resolution IS NOT NULL";
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            if (rs.next()) {
                return rs.getDouble("moyenne");
            }
            return 0;
            
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void updatePriorite(int id, String priorite) throws SQLException {
        String sql = "UPDATE signalements SET priorite = ? WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, priorite);
            stmt.setInt(2, id);
            stmt.executeUpdate();
            
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public Signalement trouverParId(int id) throws SQLException {
        String sql = "SELECT * FROM signalements WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Signalement signalement = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                signalement = creerSignalementDepuisResultSet(rs);
            }
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        
        return signalement;
    }
    
    public List<Signalement> listerTous() throws SQLException {
        List<Signalement> liste = new ArrayList<>();
        String sql = "SELECT * FROM signalements ORDER BY date_signalement DESC";
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                liste.add(creerSignalementDepuisResultSet(rs));
            }
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        
        return liste;
    }
    
    public List<Signalement> listerParSalle(int salleId) throws SQLException {
        List<Signalement> liste = new ArrayList<>();
        String sql = "SELECT * FROM signalements WHERE salle_id = ? ORDER BY date_signalement DESC";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, salleId);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                liste.add(creerSignalementDepuisResultSet(rs));
            }
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        
        return liste;
    }
    
    public List<Signalement> listerParStatut(String statut) throws SQLException {
        List<Signalement> liste = new ArrayList<>();
        String sql = "SELECT * FROM signalements WHERE statut = ? ORDER BY date_signalement DESC";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, statut);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                liste.add(creerSignalementDepuisResultSet(rs));
            }
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        
        return liste;
    }
    
    public void mettreEnCours(int id) throws SQLException {
        String sql = "UPDATE signalements SET statut = 'en_cours' WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void resoudre(int id, String commentaire) throws SQLException {
        String sql = "UPDATE signalements SET statut = 'resolu', date_resolution = CURDATE(), commentaire_resolution = ? WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, commentaire);
            stmt.setInt(2, id);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void modifier(Signalement signalement) throws SQLException {
        String sql = "UPDATE signalements SET type_probleme = ?, description = ?, statut = ? WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            
            stmt.setString(1, signalement.getTypeProbleme());
            stmt.setString(2, signalement.getDescription());
            stmt.setString(3, signalement.getStatut());
            stmt.setInt(4, signalement.getId());
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM signalements WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    private Signalement creerSignalementDepuisResultSet(ResultSet rs) throws SQLException {
        Signalement signalement = new Signalement();
        signalement.setId(rs.getInt("id"));
        signalement.setUtilisateurId(rs.getInt("utilisateur_id"));
        signalement.setSalleId(rs.getInt("salle_id"));
        signalement.setTypeProbleme(rs.getString("type_probleme"));
        signalement.setDescription(rs.getString("description"));
        signalement.setStatut(rs.getString("statut"));
        signalement.setCommentaireResolution(rs.getString("commentaire_resolution"));
        return signalement;
    }
}