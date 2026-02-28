package scheduler.dao;

import scheduler.modele.Equipement;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès aux équipements des salles.
 */
public class EquipementDAO {
    
    public void ajouter(Equipement equipement) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "INSERT INTO equipements (nom, description, quantite) VALUES (?, ?, ?)";
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            stmt.setString(1, equipement.getNom());
            stmt.setString(2, equipement.getDescription());
            stmt.setInt(3, equipement.getQuantite() > 0 ? equipement.getQuantite() : 1);
            
            stmt.executeUpdate();
            
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                equipement.setId(rs.getInt(1));
            }
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public Equipement trouverParId(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM equipements WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                Equipement e = creerEquipementDepuisResultSet(rs);
                return e;
            }
            return null;
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public List<Equipement> listerTous() throws SQLException {
        List<Equipement> liste = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM equipements ORDER BY nom";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                liste.add(creerEquipementDepuisResultSet(rs));
            }
            return liste;
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public List<Equipement> listerParSalle(int salleId) throws SQLException {
        List<Equipement> liste = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT e.* FROM equipements e " +
                        "JOIN salle_equipement se ON e.id = se.equipement_id " +
                        "WHERE se.salle_id = ? ORDER BY e.nom";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, salleId);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                Equipement e = creerEquipementDepuisResultSet(rs);
                liste.add(e);
            }
            return liste;
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public int getQuantiteTotale(int equipementId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT quantite FROM equipements WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, equipementId);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("quantite");
            }
            return 0;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public int getQuantiteUtilisee(int equipementId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT COUNT(*) as total FROM salle_equipement WHERE equipement_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, equipementId);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("total");
            }
            return 0;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public int getQuantiteDisponible(int equipementId) throws SQLException {
        int totale = getQuantiteTotale(equipementId);
        int utilisee = getQuantiteUtilisee(equipementId);
        return totale - utilisee;
    }
    
    public void modifier(Equipement equipement) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "UPDATE equipements SET nom = ?, description = ?, quantite = ? WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            
            stmt.setString(1, equipement.getNom());
            stmt.setString(2, equipement.getDescription());
            stmt.setInt(3, equipement.getQuantite());
            stmt.setInt(4, equipement.getId());
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void updateQuantite(int id, int nouvelleQuantite) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "UPDATE equipements SET quantite = ? WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            
            stmt.setInt(1, nouvelleQuantite);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void supprimer(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt1 = null;
        PreparedStatement stmt2 = null;
        
        try {
            conn = ConnexionBD.getConnection();
            conn.setAutoCommit(false);
            
            String sqlLiaisons = "DELETE FROM salle_equipement WHERE equipement_id = ?";
            stmt1 = conn.prepareStatement(sqlLiaisons);
            stmt1.setInt(1, id);
            stmt1.executeUpdate();
            
            String sqlEquipement = "DELETE FROM equipements WHERE id = ?";
            stmt2 = conn.prepareStatement(sqlEquipement);
            stmt2.setInt(1, id);
            stmt2.executeUpdate();
            
            conn.commit();
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                }
            }
            throw e;
            
        } finally {
            if (stmt1 != null) try { stmt1.close(); } catch (SQLException e) { }
            if (stmt2 != null) try { stmt2.close(); } catch (SQLException e) { }
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) { }
                ConnexionBD.libererConnection(conn);
            }
        }
    }
    
    public boolean estUtilise(int equipementId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT COUNT(*) FROM salle_equipement WHERE equipement_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, equipementId);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public int getNombreSallesUtilisant(int equipementId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT COUNT(DISTINCT salle_id) FROM salle_equipement WHERE equipement_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, equipementId);
            rs = stmt.executeQuery();
            
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
    
    public int getTotalEquipements() throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT COUNT(*) FROM equipements";
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
    
    public int getTotalQuantite() throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT SUM(quantite) FROM equipements";
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
    
    public boolean nomExiste(String nom) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT COUNT(*) FROM equipements WHERE nom = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, nom);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public List<Equipement> rechercherParNom(String recherche) throws SQLException {
        List<Equipement> liste = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM equipements WHERE nom LIKE ? OR description LIKE ? ORDER BY nom";
            stmt = conn.prepareStatement(sql);
            
            String pattern = "%" + recherche + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                liste.add(creerEquipementDepuisResultSet(rs));
            }
            
            return liste;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    private Equipement creerEquipementDepuisResultSet(ResultSet rs) throws SQLException {
        Equipement equipement = new Equipement();
        equipement.setId(rs.getInt("id"));
        equipement.setNom(rs.getString("nom"));
        equipement.setDescription(rs.getString("description"));
        equipement.setQuantite(rs.getInt("quantite"));
        return equipement;
    }
}