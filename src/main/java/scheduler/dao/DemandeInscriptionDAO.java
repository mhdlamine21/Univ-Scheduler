package scheduler.dao;

import scheduler.modele.DemandeInscription;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès aux demandes d'inscription.
 */
public class DemandeInscriptionDAO {
    
    public void ajouter(DemandeInscription demande) throws SQLException {
        
        if (demande.getStatut() == null || demande.getStatut().isEmpty()) {
            demande.setStatut("en_attente");
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "INSERT INTO demandes_inscription (nom, prenom, email, role_demande, " +
                        "numero_etudiant, ufr_id, classe_id, matricule_enseignant, grade, statut, type_etudiant) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            stmt.setString(1, demande.getNom());
            stmt.setString(2, demande.getPrenom());
            stmt.setString(3, demande.getEmail());
            stmt.setString(4, demande.getRoleDemande());
            stmt.setString(5, demande.getNumeroEtudiant());
            
            if (demande.getUfrId() != null) {
                stmt.setInt(6, demande.getUfrId());
            } else {
                stmt.setNull(6, Types.INTEGER);
            }
            
            if (demande.getClasseId() != null) {
                stmt.setInt(7, demande.getClasseId());
            } else {
                stmt.setNull(7, Types.INTEGER);
            }
            
            stmt.setString(8, demande.getMatriculeEnseignant());
            stmt.setString(9, demande.getGrade());
            stmt.setString(10, demande.getStatut());
            stmt.setString(11, demande.getTypeEtudiant() != null ? demande.getTypeEtudiant() : "normal");
            stmt.executeUpdate();
            
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                demande.setId(rs.getInt(1));
            }
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public DemandeInscription trouverParId(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM demandes_inscription WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return creerDemandeDepuisResultSet(rs);
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
    
    public List<DemandeInscription> listerEnAttente() throws SQLException {
        List<DemandeInscription> liste = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT * FROM demandes_inscription WHERE statut = 'en_attente' ORDER BY date_demande";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                liste.add(creerDemandeDepuisResultSet(rs));
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
    
    public List<DemandeInscription> listerToutes() throws SQLException {
        List<DemandeInscription> liste = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.createStatement();
            String sql = "SELECT * FROM demandes_inscription ORDER BY date_demande DESC";
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                liste.add(creerDemandeDepuisResultSet(rs));
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
    
    public void valider(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "UPDATE demandes_inscription SET statut = 'validee' WHERE id = ?";
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
    
    public void refuser(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "UPDATE demandes_inscription SET statut = 'refusee' WHERE id = ?";
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
    
    public boolean emailExiste(String email) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "SELECT COUNT(*) FROM demandes_inscription WHERE email = ? AND statut != 'refusee'";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
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
    
    public void supprimer(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            String sql = "DELETE FROM demandes_inscription WHERE id = ?";
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
    
    private DemandeInscription creerDemandeDepuisResultSet(ResultSet rs) throws SQLException {
        DemandeInscription demande = new DemandeInscription();
        demande.setId(rs.getInt("id"));
        demande.setNom(rs.getString("nom"));
        demande.setPrenom(rs.getString("prenom"));
        demande.setEmail(rs.getString("email"));
        demande.setRoleDemande(rs.getString("role_demande"));
        demande.setNumeroEtudiant(rs.getString("numero_etudiant"));
        demande.setUfrId(rs.getInt("ufr_id"));
        if (rs.wasNull()) {
            demande.setUfrId(null);
        }
        demande.setClasseId(rs.getInt("classe_id"));
        if (rs.wasNull()) {
            demande.setClasseId(null);
        }
        demande.setMatriculeEnseignant(rs.getString("matricule_enseignant"));
        demande.setGrade(rs.getString("grade"));
        demande.setStatut(rs.getString("statut"));
        demande.setDateDemande(rs.getTimestamp("date_demande").toLocalDateTime());
        try {
            String te = rs.getString("type_etudiant");
            if (te != null && !te.isEmpty()) {
                demande.setTypeEtudiant(te);
            }
        } catch (SQLException ignored) { }
        return demande;
    }
}