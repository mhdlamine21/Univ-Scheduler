package scheduler.service;

import scheduler.dao.*;
import scheduler.modele.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service central de gestion du planning universitaire et des créneaux de cours.
 * 
 * Responsabilités principales :
 * - Manipulation et persistance des créneaux horaires (création, modification, déplacement).
 * - Association des créneaux aux grilles d'emplois du temps validées.
 * - Requêtes spécialisées de restitution par cours, par salle, par classe ou par jour.
 * - Notification automatique par e-mail des étudiants et enseignants en cas d'ajustement horaire.
 */
public class PlanningService {
    private static final Logger logger = LoggerFactory.getLogger(PlanningService.class);
    
    private CreneauDAO creneauDAO;
    private CoursDAO coursDAO;
    private SalleDAO salleDAO;
    private EmploiDuTempsDAO emploiDuTempsDAO;
    private EmploiDuTempsCreneauDAO edtCreneauDAO;
    private UtilisateurDAO utilisateurDAO; 
    private EmailService emailService; 
    
    /**
     * Initialise le service de planning en instanciant ses DAOs associés.
     */
    public PlanningService() {
        this.creneauDAO = new CreneauDAO();
        this.coursDAO = new CoursDAO();
        this.salleDAO = new SalleDAO();
        this.emploiDuTempsDAO = new EmploiDuTempsDAO();
        this.edtCreneauDAO = new EmploiDuTempsCreneauDAO();
        this.utilisateurDAO = new UtilisateurDAO(); 
        this.emailService = new EmailService(); 
    }
    
    /**
     * Enregistre un nouveau créneau horaire en base de données.
     * @param creneau l'entité créneau à insérer
     */
    public void ajouterCreneau(Creneau creneau) throws SQLException {
        creneauDAO.ajouter(creneau);
    }
    
    /**
     * Ajoute par lot une série de créneaux horaires.
     * @param creneaux liste des créneaux à persister
     */
    public void ajouterCreneaux(List<Creneau> creneaux) throws SQLException {
        for (Creneau c : creneaux) {
            creneauDAO.ajouter(c);
        }
    }
    
    /**
     * Recherche un créneau spécifique par son identifiant unique.
     * @param id identifiant numérique du créneau
     * @return le créneau correspondant ou null si introuvable
     */
    public Creneau trouverCreneauParId(int id) throws SQLException {
        return creneauDAO.trouverParId(id);
    }
    
    /**
     * Récupère tous les créneaux associés à un cours d'enseignement.
     * @param coursId identifiant du cours
     * @return liste des créneaux programmés
     */
    public List<Creneau> getCreneauxParCours(int coursId) throws SQLException {
        return creneauDAO.listerParCours(coursId);
    }
    
    /**
     * Liste l'ensemble des créneaux occupant une salle donnée.
     * @param salleId identifiant de la salle
     * @return liste des créneaux affectés
     */
    public List<Creneau> getCreneauxParSalle(int salleId) throws SQLException {
        return creneauDAO.listerParSalle(salleId);
    }
    
    /**
     * Liste les créneaux positionnés sur un jour donné de la semaine (Lundi..Samedi).
     * @param jour nom du jour en français
     * @return liste ordonnée des créneaux
     */
    public List<Creneau> getCreneauxParJour(String jour) throws SQLException {
        return creneauDAO.listerParJour(jour);
    }
    
    public List<Creneau> getCreneauxFuturs() throws SQLException {
        return creneauDAO.listerFuturs();
    }
    
    public List<Creneau> getCreneauxParSemaine(LocalDate date) throws SQLException {
        List<Creneau> resultats = new ArrayList<>();
        LocalDate debutSemaine = date.minusDays(date.getDayOfWeek().getValue() - 1);
        
        for (int i = 0; i < 5; i++) {
            String jour = debutSemaine.plusDays(i).toString();
            resultats.addAll(creneauDAO.listerParJour(jour));
        }
        
        return resultats;
    }
    
    public List<Creneau> getCreneauxParMois(LocalDate date) throws SQLException {
        List<Creneau> resultats = new ArrayList<>();
        int annee = date.getYear();
        int mois = date.getMonthValue();
        
        LocalDate premierJour = LocalDate.of(annee, mois, 1);
        LocalDate dernierJour = premierJour.plusMonths(1).minusDays(1);
        
        LocalDate current = premierJour;
        while (!current.isAfter(dernierJour)) {
            resultats.addAll(creneauDAO.listerParJour(current.toString()));
            current = current.plusDays(1);
        }
        
        return resultats;
    }
    
    /**
     * Retourne les emplois du temps d'une classe, du plus récent au plus ancien.
     */
    public List<EmploiDuTemps> getEdtsParClasse(int classeId) throws SQLException {
        List<EmploiDuTemps> liste = emploiDuTempsDAO.listerParClasse(classeId);
        // Trier du plus récent au plus ancien
        liste.sort((a, b) -> {
            if (a.getPeriodeDebut() == null) return 1;
            if (b.getPeriodeDebut() == null) return -1;
            return b.getPeriodeDebut().compareTo(a.getPeriodeDebut());
        });
        return liste;
    }

    /**
     * Retourne les créneaux d'une classe filtrés sur une période.
     */
    public List<Creneau> getCreneauxParClasse(int classeId, String periodeDebut, String periodeFin)
            throws SQLException {
        List<Cours> coursList = coursDAO.listerParClasse(classeId);
        List<Creneau> tousCreneaux = new ArrayList<>();
        for (Cours cours : coursList) {
            tousCreneaux.addAll(creneauDAO.listerParCours(cours.getId()));
        }
        LocalDate debut = periodeDebut != null ? LocalDate.parse(periodeDebut) : LocalDate.MIN;
        LocalDate fin   = periodeFin   != null ? LocalDate.parse(periodeFin)   : LocalDate.MAX;
        return tousCreneaux.stream()
            .filter(c -> {
                try {
                    LocalDate d = LocalDate.parse(c.getJour());
                    return !d.isBefore(debut) && !d.isAfter(fin);
                } catch (Exception e) { return false; }
            })
            .sorted((c1, c2) -> {
                int cmp = c1.getJour().compareTo(c2.getJour());
                if (cmp == 0) cmp = c1.getHeureDebut().compareTo(c2.getHeureDebut());
                return cmp;
            })
            .collect(Collectors.toList());
    }

    public List<Creneau> getCreneauxParClasse(int classeId) throws SQLException {
        List<Cours> coursList = coursDAO.listerParClasse(classeId);
        List<Creneau> tousCreneaux = new ArrayList<>();
        
        for (Cours cours : coursList) {
            tousCreneaux.addAll(creneauDAO.listerParCours(cours.getId()));
        }
        
        LocalDate aujourdhui = LocalDate.now();
        List<Creneau> actifs = tousCreneaux.stream()
            .filter(c -> LocalDate.parse(c.getJour()).isAfter(aujourdhui) || 
                         LocalDate.parse(c.getJour()).isEqual(aujourdhui))
            .collect(Collectors.toList());
        
        Collections.sort(actifs, (c1, c2) -> {
            int cmp = c1.getJour().compareTo(c2.getJour());
            if (cmp == 0) {
                cmp = c1.getHeureDebut().compareTo(c2.getHeureDebut());
            }
            return cmp;
        });
        
        return actifs;
    }
    
    /**
     * Récupère tous les créneaux d'un enseignant
     */
    public List<Creneau> getCreneauxParEnseignant(int enseignantId) throws SQLException {
        List<Creneau> resultats = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = ConnexionBD.getConnection();
            
            String sql = "SELECT c.* FROM creneaux c " +
                         "INNER JOIN cours co ON c.cours_id = co.id " +
                         "WHERE co.enseignant_id = ? " +
                         "ORDER BY c.jour, c.heure_debut";
            
            System.out.println("🔍 getCreneauxParEnseignant - Enseignant ID: " + enseignantId);
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, enseignantId);
            rs = stmt.executeQuery();
            
            int compteur = 0;
            while (rs.next()) {
                Creneau creneau = new Creneau();
                creneau.setId(rs.getInt("id"));
                creneau.setCoursId(rs.getInt("cours_id"));
                creneau.setJour(rs.getString("jour"));
                creneau.setHeureDebut(rs.getString("heure_debut"));
                creneau.setHeureFin(rs.getString("heure_fin"));
                creneau.setSalleId(rs.getInt("salle_id"));
                if (rs.wasNull()) creneau.setSalleId(null);
                creneau.setStatut(rs.getString("statut"));
                creneau.setMotifAnnulation(rs.getString("motif_annulation"));
                resultats.add(creneau);
                compteur++;
            }
            
            System.out.println("   ✅ " + compteur + " créneaux trouvés");
            return resultats;
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur getCreneauxParEnseignant: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
            ConnexionBD.libererConnection(conn);
        }
    }
    
         
    
    public void annulerCreneau(int id, String motif) throws SQLException {
        creneauDAO.annuler(id, motif);
    }
    
    public void deplacerCreneau(int id, String nouveauJour, String nouvelleHeureDebut, 
                                 String nouvelleHeureFin, Integer nouvelleSalleId) throws SQLException {
        Creneau creneau = creneauDAO.trouverParId(id);
        if (creneau != null) {
            creneau.setJour(nouveauJour);
            creneau.setHeureDebut(nouvelleHeureDebut);
            creneau.setHeureFin(nouvelleHeureFin);
            creneau.setSalleId(nouvelleSalleId);
            creneau.setStatut("deplace");
            creneauDAO.modifier(creneau);
        }
    }
    
    public void modifierCreneau(Creneau creneau) throws SQLException {
        creneauDAO.modifier(creneau);
    }
    
    public void supprimerCreneau(int id) throws SQLException {
        creneauDAO.supprimer(id);
    }
    
    
    /**
     * Crée un emploi du temps avec ses créneaux.
     */
    public EmploiDuTemps creerEmploiDuTemps(EmploiDuTemps edt, List<Creneau> creneaux) throws SQLException {
        if (edt.getStatut() == null || edt.getStatut().isEmpty()) {
            edt.setStatut("en_attente");
        }
        
        edt.mettreAJourStatut();
        
        emploiDuTempsDAO.ajouter(edt);
        
        for (Creneau c : creneaux) {
            creneauDAO.ajouter(c);
            edtCreneauDAO.ajouterCreneau(edt.getId(), c.getId());
        }
        
        return edt;
    }
    
    public EmploiDuTemps getEmploiDuTempsParId(int id) throws SQLException {
        EmploiDuTemps edt = emploiDuTempsDAO.trouverParId(id);
        if (edt != null) {
            edt.mettreAJourStatut();
        }
        return edt;
    }
    
    /**
     * Liste tous les emplois du temps
     */
    public List<EmploiDuTemps> listerTous() throws SQLException {
        return emploiDuTempsDAO.listerTous();
    }
    
    /**
     * Liste les emplois du temps actifs (en cours ou en attente)
     */
    public List<EmploiDuTemps> listerActifs() throws SQLException {
        List<EmploiDuTemps> tous = emploiDuTempsDAO.listerTous();
        List<EmploiDuTemps> actifs = new ArrayList<>();
        for (EmploiDuTemps edt : tous) {
            edt.mettreAJourStatut();
            if ("en_attente".equals(edt.getStatut()) || "en_cours".equals(edt.getStatut())) {
                actifs.add(edt);
            }
        }
        return actifs;
    }
    
    public List<EmploiDuTemps> getEmploisDuTempsParClasse(int classeId) throws SQLException {
        List<EmploiDuTemps> liste = emploiDuTempsDAO.listerParClasse(classeId);
        for (EmploiDuTemps edt : liste) {
            edt.mettreAJourStatut();
        }
        return liste;
    }
    
    public List<EmploiDuTemps> getTousEmploisDuTemps() throws SQLException {
        List<EmploiDuTemps> liste = emploiDuTempsDAO.listerTous();
        for (EmploiDuTemps edt : liste) {
            edt.mettreAJourStatut();
        }
        return liste;
    }
    
    public List<Creneau> getCreneauxParEmploiDuTemps(int edtId) throws SQLException {
        List<Creneau> resultats = new ArrayList<>();
        
        String sql = "SELECT c.* FROM creneaux c " +
                     "JOIN emploi_du_temps_creneaux ec ON c.id = ec.creneau_id " +
                     "WHERE ec.emploi_id = ? ORDER BY c.jour, c.heure_debut";
        
        try (Connection conn = ConnexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, edtId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                resultats.add(creerCreneauDepuisResultSet(rs));
            }
        }
        
        return resultats;
    }
    
    public void validerEmploiDuTemps(int id) throws SQLException {
        emploiDuTempsDAO.valider(id);
    }
    
    public void supprimerEmploiDuTemps(int id) throws SQLException {
        edtCreneauDAO.supprimerTousCreneaux(id);
        emploiDuTempsDAO.supprimer(id);
    }
    
    /**
     * Met à jour tous les statuts des emplois du temps expirés
     */
    public void mettreAJourStatuts() throws SQLException {
        List<EmploiDuTemps> tous = getTousEmploisDuTemps();
        for (EmploiDuTemps edt : tous) {
            if (edt.estTermine() && !"termine".equals(edt.getStatut())) {
                emploiDuTempsDAO.marquerTermine(edt.getId());
            }
        }
    }
    
    
    /**
     * Génère une période hebdomadaire.
     */
    public Map<String, Object> genererPeriodeHebdo(int annee, int numeroSemaine) {
        Map<String, Object> periode = new HashMap<>();
        
        LocalDate debut = getPremierJourSemaine(annee, numeroSemaine);
        LocalDate fin = debut.plusDays(4); // Lundi au vendredi
        
        periode.put("type", "hebdomadaire");
        periode.put("annee", annee);
        periode.put("semaine", numeroSemaine);
        periode.put("debut", debut.toString());
        periode.put("fin", fin.toString());
        periode.put("label", String.format("Semaine %d (%s - %s)", 
            numeroSemaine,
            debut.format(DateTimeFormatter.ofPattern("dd/MM")),
            fin.format(DateTimeFormatter.ofPattern("dd/MM"))));
        
        return periode;
    }
    
    public Map<String, Object> genererPeriodeMensuel(int annee, int mois) {
        Map<String, Object> periode = new HashMap<>();
        
        LocalDate debut = LocalDate.of(annee, mois, 1);
        LocalDate fin = debut.plusMonths(1).minusDays(1);
        
        String nomMois = debut.format(DateTimeFormatter.ofPattern("MMMM"));
        
        periode.put("type", "mensuel");
        periode.put("annee", annee);
        periode.put("mois", mois);
        periode.put("debut", debut.toString());
        periode.put("fin", fin.toString());
        periode.put("label", nomMois + " " + annee);
        
        return periode;
    }
    
    public Map<String, Object> genererPeriodeSemestriel(LocalDate debut, LocalDate fin) {
        Map<String, Object> periode = new HashMap<>();
        
        periode.put("type", "semestriel");
        periode.put("debut", debut.toString());
        periode.put("fin", fin.toString());
        periode.put("label", String.format("Semestre du %s au %s",
            debut.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
            fin.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        
        return periode;
    }
    
    private LocalDate getPremierJourSemaine(int annee, int numeroSemaine) {
        LocalDate date = LocalDate.of(annee, 1, 1);
        while (date.getDayOfWeek().getValue() != 1) {
            date = date.plusDays(1);
        }
        return date.plusWeeks(numeroSemaine - 1);
    }
    
    public int getNumeroSemaine(LocalDate date) {
        return (date.getDayOfYear() / 7) + 1;
    }
    
    
    public boolean salleEstDisponible(int salleId, String jour, String heureDebut, String heureFin) throws SQLException {
        return creneauDAO.verifierDisponibiliteSalle(salleId, jour, heureDebut, heureFin);
    }

    
    public boolean enseignantEstDisponible(int enseignantId, String jour, String heureDebut, String heureFin) throws SQLException {
        return creneauDAO.verifierDisponibiliteEnseignant(enseignantId, jour, heureDebut, heureFin);
    }
    
    public List<Creneau> getCreneauxEnConflit(Creneau creneau) throws SQLException {
        List<Creneau> conflits = new ArrayList<>();
        
        List<Creneau> creneauxSalle = creneauDAO.listerParSalle(creneau.getSalleId());
        for (Creneau c : creneauxSalle) {
            if (c.getId() != creneau.getId() && 
                c.getJour().equals(creneau.getJour()) &&
                !(c.getHeureFin().compareTo(creneau.getHeureDebut()) <= 0 ||
                  c.getHeureDebut().compareTo(creneau.getHeureFin()) >= 0)) {
                conflits.add(c);
            }
        }
        
        return conflits;
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
    
    private EmploiDuTemps creerEmploiDuTempsDepuisResultSet(ResultSet rs) throws SQLException {
        EmploiDuTemps edt = new EmploiDuTemps();
        edt.setId(rs.getInt("id"));
        edt.setClasseId(rs.getInt("classe_id"));
        edt.setPeriodeType(rs.getString("periode_type"));
        edt.setPeriodeDebut(rs.getString("periode_debut"));
        edt.setPeriodeFin(rs.getString("periode_fin"));
        edt.setEstValide(rs.getBoolean("est_valide"));
        String statut = rs.getString("statut");
        edt.setStatut(statut != null ? statut : "en_cours");
        return edt;
    }

    /**
     * Crée un emploi du temps avec un titre et le valide directement (statut en_cours).
     */
    public EmploiDuTemps creerEtValiderEmploiDuTemps(
            EmploiDuTemps edt, List<Creneau> creneaux, String titre) throws SQLException {
        edt.setStatut("en_attente");
        edt.mettreAJourStatut();
        emploiDuTempsDAO.ajouter(edt);

        emploiDuTempsDAO.mettreAJourTitre(edt.getId(), titre);

        for (Creneau c : creneaux) {
            creneauDAO.ajouter(c);
            edtCreneauDAO.ajouterCreneau(edt.getId(), c.getId());
        }

        emploiDuTempsDAO.valider(edt.getId());
        edt.setStatut("en_cours");
        edt.setEstValide(true);

        return edt;
    }

    /**
     * Met à jour le statut de tous les EDT expirés.
     */
    public void synchroniserStatuts() throws SQLException {
        List<EmploiDuTemps> tous = getTousEmploisDuTemps();
        for (EmploiDuTemps edt : tous) {
            edt.mettreAJourStatut();
            if ("termine".equals(edt.getStatut()) && !"termine".equals(edt.getStatut())) {
                emploiDuTempsDAO.marquerTermine(edt.getId());
            }
        }
    }
    
    

    /**
     * Vérifie si un déplacement de cours crée un conflit
     * Retourne la liste des conflits détectés
     */
    public List<String> verifierConflitDeplacement(Creneau creneau, int nouvelEnseignantId,
            String nouveauJour, String nouvelleHeureDebut, String nouvelleHeureFin) throws SQLException {
        
        List<String> conflits = new ArrayList<>();
        
        if (creneau.getSalleId() != null) {
            boolean salleLibre = creneauDAO.verifierDisponibiliteSalle(
                creneau.getSalleId(), nouveauJour, nouvelleHeureDebut, nouvelleHeureFin
            );
            if (!salleLibre) {
                conflits.add("❌ La salle est déjà occupée à ce créneau");
            }
        }
        
        boolean enseignantLibre = creneauDAO.verifierDisponibiliteEnseignant(
            nouvelEnseignantId, nouveauJour, nouvelleHeureDebut, nouvelleHeureFin
        );
        if (!enseignantLibre) {
            conflits.add("❌ L'enseignant est déjà occupé à ce créneau");
        }
        
        Cours cours = coursDAO.trouverParId(creneau.getCoursId());
        if (cours != null) {
            List<Creneau> creneauxJour = creneauDAO.listerParJour(nouveauJour);
            for (Creneau c : creneauxJour) {
                if ("annule".equals(c.getStatut())) continue;
                Cours autreCours = coursDAO.trouverParId(c.getCoursId());
                if (autreCours != null && autreCours.getClasseId() == cours.getClasseId()) {
                    if (seChevauchent(nouvelleHeureDebut, nouvelleHeureFin, 
                                      c.getHeureDebut(), c.getHeureFin())) {
                        conflits.add("❌ La classe a déjà un cours à ce créneau");
                        break;
                    }
                }
            }
        }
        
        return conflits;
    }
    
    /**
     * Vérifie si deux créneaux se chevauchent
     */
    private boolean seChevauchent(String d1, String f1, String d2, String f2) {
        try {
            String[] debut1 = d1.split(":");
            String[] fin1 = f1.split(":");
            String[] debut2 = d2.split(":");
            String[] fin2 = f2.split(":");
            
            int debut1Min = Integer.parseInt(debut1[0]) * 60 + Integer.parseInt(debut1[1]);
            int fin1Min = Integer.parseInt(fin1[0]) * 60 + Integer.parseInt(fin1[1]);
            int debut2Min = Integer.parseInt(debut2[0]) * 60 + Integer.parseInt(debut2[1]);
            int fin2Min = Integer.parseInt(fin2[0]) * 60 + Integer.parseInt(fin2[1]);
            
            return debut1Min < fin2Min && debut2Min < fin1Min;
        } catch (Exception e) {
            return false;
        }
    }
    
   
    /**
     * Envoie l'emploi du temps validé aux étudiants et enseignants concernés
     */
    public void envoyerNotificationsEDT(int edtId, byte[] pdfContent) throws SQLException {
        EmploiDuTemps edt = emploiDuTempsDAO.trouverParId(edtId);
        if (edt == null) return;
        
        Classe classe = new ClasseDAO().trouverParId(edt.getClasseId());
        if (classe == null) return;
        
        List<Creneau> creneaux = getCreneauxParEmploiDuTemps(edtId);
        
        Set<Integer> enseignantIds = new HashSet<>();
        for (Creneau c : creneaux) {
            Cours cours = coursDAO.trouverParId(c.getCoursId());
            if (cours != null) {
                enseignantIds.add(cours.getEnseignantId());
            }
        }
        
        List<Etudiant> etudiants = new ArrayList<>();
        List<Utilisateur> tousUtilisateurs = utilisateurDAO.listerTous();
        for (Utilisateur u : tousUtilisateurs) {
            if (u instanceof Etudiant && u.isEstValide()) {
                Etudiant e = (Etudiant) u;
                if (e.getClasseId() == classe.getId()) {
                    etudiants.add(e);
                }
            }
        }
        
        String periode = edt.getPeriodeType() + " du " + edt.getPeriodeDebut() + " au " + edt.getPeriodeFin();
        String titre = emploiDuTempsDAO.getTitre(edtId);
        
        for (Etudiant e : etudiants) {
            emailService.envoyerEmploiDuTemps(
                e.getEmail(),
                e.getPrenom() + " " + e.getNom(),
                classe.getIntitule(),
                periode,
                pdfContent
            );
        }
        
        for (int ensId : enseignantIds) {
            Utilisateur ens = utilisateurDAO.trouverParId(ensId);
            if (ens != null && ens.isEstValide()) {
                emailService.envoyerEmploiDuTemps(
                    ens.getEmail(),
                    ens.getPrenom() + " " + ens.getNom(),
                    classe.getIntitule(),
                    periode,
                    pdfContent
                );
            }
        }
        
        logger.info("✅ EDT #{} envoyé à {} étudiants et {} enseignants",
            edtId, etudiants.size(), enseignantIds.size());
    }
}