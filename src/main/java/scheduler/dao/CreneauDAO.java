package scheduler.dao;

import scheduler.modele.Creneau;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès aux données des créneaux horaires.
 */
public class CreneauDAO {
    
    public void ajouter(Creneau creneau) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "INSERT INTO creneaux (cours_id, jour, heure_debut, heure_fin, salle_id, statut) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            stmt.setInt(1, creneau.getCoursId());
            stmt.setString(2, creneau.getJour());
            stmt.setString(3, creneau.getHeureDebut());
            stmt.setString(4, creneau.getHeureFin());
            
            if (creneau.getSalleId() != null) {
                stmt.setInt(5, creneau.getSalleId());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }
            
            stmt.setString(6, creneau.getStatut());
            stmt.executeUpdate();
            
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                creneau.setId(rs.getInt(1));
            }
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public Creneau trouverParId(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM creneaux WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return creerCreneauDepuisResultSet(rs);
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
    
    public List<Creneau> listerParCours(int coursId) throws SQLException {
        List<Creneau> liste = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM creneaux WHERE cours_id = ? ORDER BY jour, heure_debut";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, coursId);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                liste.add(creerCreneauDepuisResultSet(rs));
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
    
    public List<Creneau> listerParSalle(int salleId) throws SQLException {
        List<Creneau> liste = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM creneaux WHERE salle_id = ? ORDER BY jour, heure_debut";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, salleId);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                liste.add(creerCreneauDepuisResultSet(rs));
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
    
    public List<Creneau> listerParJour(String jour) throws SQLException {
        List<Creneau> liste = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM creneaux WHERE jour = ? ORDER BY heure_debut";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, jour);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                liste.add(creerCreneauDepuisResultSet(rs));
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
    
    public List<Creneau> listerFuturs() throws SQLException {
        List<Creneau> liste = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.createStatement();
            String sql = "SELECT * FROM creneaux WHERE jour >= CURDATE() AND statut != 'annule' ORDER BY jour, heure_debut";
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                liste.add(creerCreneauDepuisResultSet(rs));
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
    
    public boolean verifierDisponibiliteSalle(int salleId, String jour, String heureDebut, String heureFin) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT COUNT(*) FROM creneaux WHERE salle_id = ? AND jour = ? AND " +
                        "((heure_debut <= ? AND heure_fin > ?) OR (heure_debut < ? AND heure_fin >= ?)) AND statut != 'annule'";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, salleId);
            stmt.setString(2, jour);
            stmt.setString(3, heureDebut);
            stmt.setString(4, heureDebut);
            stmt.setString(5, heureFin);
            stmt.setString(6, heureFin);
            
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
            return false;
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public boolean verifierDisponibiliteEnseignant(int enseignantId, String jour, String heureDebut, String heureFin) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT COUNT(*) FROM creneaux c " +
                        "JOIN cours co ON c.cours_id = co.id " +
                        "WHERE co.enseignant_id = ? AND c.jour = ? AND " +
                        "((c.heure_debut <= ? AND c.heure_fin > ?) OR (c.heure_debut < ? AND c.heure_fin >= ?)) AND c.statut != 'annule'";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, enseignantId);
            stmt.setString(2, jour);
            stmt.setString(3, heureDebut);
            stmt.setString(4, heureDebut);
            stmt.setString(5, heureFin);
            stmt.setString(6, heureFin);
            
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
            return false;
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void annuler(int id, String motif) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "UPDATE creneaux SET statut = 'annule', motif_annulation = ? WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, motif);
            stmt.setInt(2, id);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void modifier(Creneau creneau) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "UPDATE creneaux SET cours_id = ?, jour = ?, heure_debut = ?, heure_fin = ?, " +
                        "salle_id = ?, statut = ?, motif_annulation = ? WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, creneau.getCoursId());
            stmt.setString(2, creneau.getJour());
            stmt.setString(3, creneau.getHeureDebut());
            stmt.setString(4, creneau.getHeureFin());
            
            if (creneau.getSalleId() != null) {
                stmt.setInt(5, creneau.getSalleId());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }
            
            stmt.setString(6, creneau.getStatut());
            stmt.setString(7, creneau.getMotifAnnulation());
            stmt.setInt(8, creneau.getId());
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void supprimer(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "DELETE FROM creneaux WHERE id = ?";
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
    
    private Creneau creerCreneauDepuisResultSet(ResultSet rs) throws SQLException {
        Creneau creneau = new Creneau();
        creneau.setId(rs.getInt("id"));
        creneau.setCoursId(rs.getInt("cours_id"));
        creneau.setJour(rs.getString("jour"));
        creneau.setHeureDebut(rs.getString("heure_debut"));
        creneau.setHeureFin(rs.getString("heure_fin"));
        creneau.setSalleId(rs.getInt("salle_id"));
        if (rs.wasNull()) {
            creneau.setSalleId(null);
        }
        creneau.setStatut(rs.getString("statut"));
        creneau.setMotifAnnulation(rs.getString("motif_annulation"));
        return creneau;
    }
}