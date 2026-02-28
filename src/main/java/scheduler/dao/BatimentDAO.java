package scheduler.dao;

import scheduler.modele.Batiment;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès aux données des bâtiments.
 */
public class BatimentDAO {
    
    public void ajouter(Batiment batiment) throws SQLException {
        String sql = "INSERT INTO batiments (nom, localisation, nb_etages, ufr_id, statut, photo_url) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            stmt.setString(1, batiment.getNom());
            stmt.setString(2, batiment.getLocalisation());
            stmt.setInt(3, batiment.getNbEtages());
            stmt.setInt(4, batiment.getUfrId());
            stmt.setString(5, batiment.getStatut());
            stmt.setString(6, batiment.getPhotoUrl());
            stmt.executeUpdate();
            
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                batiment.setId(rs.getInt(1));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public Batiment trouverParId(int id) throws SQLException {
        String sql = "SELECT * FROM batiments WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Batiment batiment = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                batiment = creerBatimentDepuisResultSet(rs);
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        return batiment;
    }
    
    public List<Batiment> listerTous() throws SQLException {
        List<Batiment> liste = new ArrayList<>();
        String sql = "SELECT * FROM batiments ORDER BY nom";
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                liste.add(creerBatimentDepuisResultSet(rs));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        return liste;
    }
    
    public List<Batiment> listerParUfr(int ufrId) throws SQLException {
        List<Batiment> liste = new ArrayList<>();
        String sql = "SELECT * FROM batiments WHERE ufr_id = ? ORDER BY nom";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, ufrId);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                liste.add(creerBatimentDepuisResultSet(rs));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        return liste;
    }
    
    public void rendreIndisponible(int id, String motif, String dateDebut, String dateFin) throws SQLException {
        String sql = "UPDATE batiments SET statut = 'indisponible', motif_indisponibilite = ?, " +
                    "date_debut_indisponibilite = ?, date_fin_indisponibilite = ? WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, motif);
            stmt.setString(2, dateDebut);
            stmt.setString(3, dateFin);
            stmt.setInt(4, id);
            stmt.executeUpdate();
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void rendreDisponible(int id) throws SQLException {
        String sql = "UPDATE batiments SET statut = 'disponible', motif_indisponibilite = NULL, " +
                    "date_debut_indisponibilite = NULL, date_fin_indisponibilite = NULL WHERE id = ?";
        
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
    
    public void modifier(Batiment batiment) throws SQLException {
        String sql = "UPDATE batiments SET nom = ?, localisation = ?, nb_etages = ?, ufr_id = ?, photo_url = ? WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, batiment.getNom());
            stmt.setString(2, batiment.getLocalisation());
            stmt.setInt(3, batiment.getNbEtages());
            stmt.setInt(4, batiment.getUfrId());
            stmt.setString(5, batiment.getPhotoUrl());
            stmt.setInt(6, batiment.getId());
            stmt.executeUpdate();
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM batiments WHERE id = ?";
        
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
    
    private Batiment creerBatimentDepuisResultSet(ResultSet rs) throws SQLException {
        Batiment batiment = new Batiment();
        batiment.setId(rs.getInt("id"));
        batiment.setNom(rs.getString("nom"));
        batiment.setLocalisation(rs.getString("localisation"));
        batiment.setNbEtages(rs.getInt("nb_etages"));
        batiment.setUfrId(rs.getInt("ufr_id"));
        batiment.setStatut(rs.getString("statut"));
        batiment.setMotifIndisponibilite(rs.getString("motif_indisponibilite"));
        batiment.setDateDebutIndisponibilite(rs.getString("date_debut_indisponibilite"));
        batiment.setDateFinIndisponibilite(rs.getString("date_fin_indisponibilite"));
        try {
            batiment.setPhotoUrl(rs.getString("photo_url"));
        } catch (Exception ignored) {}
        return batiment;
    }
}