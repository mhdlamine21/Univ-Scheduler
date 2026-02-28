package scheduler.dao;

import scheduler.modele.Salle;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) responsable des interactions avec la table `salles`.
 * 
 * Assure les opérations de persistance CRUD, la gestion des équipements associés,
 * la vérification des disponibilités et le chargement avec filtrage avancé (bâtiment, UFR, capacité).
 */
public class SalleDAO {
    
    /**
     * Insère une nouvelle salle en base de données et met à jour l'identifiant auto-généré.
     * Gère automatiquement un mécanisme de fallback si la colonne photo_url est absente.
     * 
     * @param salle l'instance de salle à persister
     * @throws SQLException en cas d'échec SQL
     */
    public void ajouter(Salle salle) throws SQLException {
        String sql = "INSERT INTO salles (numero, capacite, type, batiment_id, etage, statut, photo_url) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            stmt.setString(1, salle.getNumero());
            stmt.setInt(2, salle.getCapacite());
            stmt.setString(3, salle.getType());
            stmt.setInt(4, salle.getBatimentId());
            stmt.setInt(5, salle.getEtage());
            stmt.setString(6, salle.getStatut() != null ? salle.getStatut() : "disponible");
            stmt.setString(7, salle.getPhotoUrl());
            
            stmt.executeUpdate();
            
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                salle.setId(rs.getInt(1));
            }
            
        } catch (SQLException e) {
            // Fallback si la colonne photo_url n'existe pas encore
            try {
                if (stmt != null) stmt.close();
                String fallbackSql = "INSERT INTO salles (numero, capacite, type, batiment_id, etage, statut) VALUES (?, ?, ?, ?, ?, ?)";
                stmt = conn.prepareStatement(fallbackSql, Statement.RETURN_GENERATED_KEYS);
                stmt.setString(1, salle.getNumero());
                stmt.setInt(2, salle.getCapacite());
                stmt.setString(3, salle.getType());
                stmt.setInt(4, salle.getBatimentId());
                stmt.setInt(5, salle.getEtage());
                stmt.setString(6, salle.getStatut() != null ? salle.getStatut() : "disponible");
                stmt.executeUpdate();
                rs = stmt.getGeneratedKeys();
                if (rs.next()) salle.setId(rs.getInt(1));
            } catch (SQLException ex) {
                throw e;
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public Salle trouverParId(int id) throws SQLException {
        String sql = "SELECT * FROM salles WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return creerSalleDepuisResultSet(rs);
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
    
    public List<Salle> listerTous() throws SQLException {
        List<Salle> liste = new ArrayList<>();
        String sql = "SELECT * FROM salles ORDER BY numero";
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                Salle salle = creerSalleDepuisResultSet(rs);
                liste.add(salle);
            }
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        
        for (Salle salle : liste) {
            try {
                salle.setEquipements(getEquipementsSalle(salle.getId()));
            } catch (SQLException e) { }
        }
        
        return liste;
    }
    
    public List<Salle> listerParBatiment(int batimentId) throws SQLException {
        List<Salle> liste = new ArrayList<>();
        String sql = "SELECT * FROM salles WHERE batiment_id = ? ORDER BY etage, numero";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, batimentId);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                Salle salle = creerSalleDepuisResultSet(rs);
                liste.add(salle);
            }
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        
        for (Salle salle : liste) {
            try {
                salle.setEquipements(getEquipementsSalle(salle.getId()));
            } catch (SQLException e) { }
        }
        
        return liste;
    }
    
    public List<Salle> listerDisponibles() throws SQLException {
        List<Salle> liste = new ArrayList<>();
        String sql = "SELECT * FROM salles WHERE statut = 'disponible' ORDER BY numero";
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                Salle salle = creerSalleDepuisResultSet(rs);
                liste.add(salle);
            }
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
        
        for (Salle salle : liste) {
            try {
                salle.setEquipements(getEquipementsSalle(salle.getId()));
            } catch (SQLException e) { }
        }
        
        return liste;
    }
    
    public void rendreIndisponible(int id, String motif, String dateDebut, String dateFin) throws SQLException {
        String sql = "UPDATE salles SET statut = 'indisponible', motif_indisponibilite = ?, date_debut_indisponibilite = ?, date_fin_indisponibilite = ? WHERE id = ?";
        
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
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void rendreDisponible(int id) throws SQLException {
        String sql = "UPDATE salles SET statut = 'disponible', motif_indisponibilite = NULL, date_debut_indisponibilite = NULL, date_fin_indisponibilite = NULL WHERE id = ?";
        
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
    
    public void ajouterEquipement(int salleId, int equipementId) throws SQLException {
        if (equipementExisteDansSalle(salleId, equipementId)) {
            return;
        }
        
        String sql = "INSERT INTO salle_equipement (salle_id, equipement_id) VALUES (?, ?)";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, salleId);
            stmt.setInt(2, equipementId);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void retirerEquipement(int salleId, int equipementId) throws SQLException {
        String sql = "DELETE FROM salle_equipement WHERE salle_id = ? AND equipement_id = ? LIMIT 1";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, salleId);
            stmt.setInt(2, equipementId);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    private boolean equipementExisteDansSalle(int salleId, int equipementId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM salle_equipement WHERE salle_id = ? AND equipement_id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, salleId);
            stmt.setInt(2, equipementId);
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
    
    public int compterEquipementDansSalle(int salleId, int equipementId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM salle_equipement WHERE salle_id = ? AND equipement_id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, salleId);
            stmt.setInt(2, equipementId);
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
    
    public List<Integer> getEquipementsSalle(int salleId) throws SQLException {
        List<Integer> equipements = new ArrayList<>();
        String sql = "SELECT equipement_id FROM salle_equipement WHERE salle_id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, salleId);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                equipements.add(rs.getInt("equipement_id"));
            }
            return equipements;
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            ConnexionBD.libererConnection(conn);
        }
    }
    
    public void modifier(Salle salle) throws SQLException {
        String sql = "UPDATE salles SET numero = ?, capacite = ?, type = ?, batiment_id = ?, etage = ?, photo_url = ? WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = ConnexionBD.getConnection();
            stmt = conn.prepareStatement(sql);
            
            stmt.setString(1, salle.getNumero());
            stmt.setInt(2, salle.getCapacite());
            stmt.setString(3, salle.getType());
            stmt.setInt(4, salle.getBatimentId());
            stmt.setInt(5, salle.getEtage());
            stmt.setString(6, salle.getPhotoUrl());
            stmt.setInt(7, salle.getId());
            
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
        
        try {
            conn = ConnexionBD.getConnection();
            conn.setAutoCommit(false);
            
            String sqlEquipements = "DELETE FROM salle_equipement WHERE salle_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlEquipements)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            }
            
            String sqlReservations = "DELETE FROM reservations WHERE salle_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlReservations)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            }
            
            String sqlSignalements = "DELETE FROM signalements WHERE salle_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlSignalements)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            }
            
            String sqlCreneaux = "UPDATE creneaux SET salle_id = NULL WHERE salle_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlCreneaux)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            }
            
            String sqlSalle = "DELETE FROM salles WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlSalle)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            }
            
            conn.commit();
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) { }
            }
            throw e;
            
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) { }
                ConnexionBD.libererConnection(conn);
            }
        }
    }
    
    private Salle creerSalleDepuisResultSet(ResultSet rs) throws SQLException {
        Salle salle = new Salle();
        salle.setId(rs.getInt("id"));
        salle.setNumero(rs.getString("numero"));
        salle.setCapacite(rs.getInt("capacite"));
        salle.setType(rs.getString("type"));
        salle.setBatimentId(rs.getInt("batiment_id"));
        salle.setEtage(rs.getInt("etage"));
        salle.setStatut(rs.getString("statut"));
        salle.setMotifIndisponibilite(rs.getString("motif_indisponibilite"));
        salle.setDateDebutIndisponibilite(rs.getString("date_debut_indisponibilite"));
        salle.setDateFinIndisponibilite(rs.getString("date_fin_indisponibilite"));
        try {
            salle.setPhotoUrl(rs.getString("photo_url"));
        } catch (Exception ignored) {}
        return salle;
    }
}