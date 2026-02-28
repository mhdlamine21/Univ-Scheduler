package scheduler.service;

import scheduler.dao.*;
import scheduler.modele.*;
import java.sql.SQLException;
import java.util.*;

/**
 * Détection et résolution des conflits d'emploi du temps.
 * Vérifie les conflits de salle, d'enseignant et de classe,
 * et propose des solutions concrètes applicables.
 */
public class ConflitService {

    private CreneauDAO creneauDAO;
    private CoursDAO coursDAO;
    private SalleDAO salleDAO;
    private UtilisateurDAO utilisateurDAO;
    private ClasseDAO classeDAO;
    private MatiereDAO matiereDAO;

    public ConflitService() {
        this.creneauDAO   = new CreneauDAO();
        this.coursDAO     = new CoursDAO();
        this.salleDAO     = new SalleDAO();
        this.utilisateurDAO = new UtilisateurDAO();
        this.classeDAO    = new ClasseDAO();
        this.matiereDAO   = new MatiereDAO();
    }

    /** Un conflit détecté avec ses solutions proposées. */
    public static class ResultatConflit {
        private final String typeConflit;   // "SALLE" | "ENSEIGNANT" | "CLASSE"
        private final String description;
        private final Creneau creneauConcerne;
        private final List<SolutionConflit> solutions = new ArrayList<>();

        public ResultatConflit(String typeConflit, String description, Creneau creneau) {
            this.typeConflit      = typeConflit;
            this.description      = description;
            this.creneauConcerne  = creneau;
        }

        public void ajouterSolution(SolutionConflit s) { solutions.add(s); }
        public String getTypeConflit()               { return typeConflit; }
        public String getDescription()               { return description; }
        public Creneau getCreneauConcerne()          { return creneauConcerne; }
        public List<SolutionConflit> getSolutions()  { return solutions; }
        public boolean aSolutions()                  { return !solutions.isEmpty(); }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("❌ [").append(typeConflit).append("] ").append(description);
            if (!solutions.isEmpty()) {
                sb.append("\n   💡 Solutions proposées :");
                for (SolutionConflit sol : solutions) {
                    sb.append("\n      → ").append(sol.getDescription());
                }
            }
            return sb.toString();
        }
    }

    /**
     * Solution concrète à un conflit.
     */    public static class SolutionConflit {
        private final String type;        // "CHANGER_SALLE" | "CHANGER_HORAIRE"
        private final String description;
        private final Object valeur;      // Salle ou String[] {heureDebut, heureFin}

        public SolutionConflit(String type, String description, Object valeur) {
            this.type        = type;
            this.description = description;
            this.valeur      = valeur;
        }

        public String getType()        { return type; }
        public String getDescription() { return description; }
        public Object getValeur()      { return valeur; }
    }

   
     /**
     * Vérifie tous les conflits pour un créneau.
     * @return liste des messages de conflit
     */
    public List<String> verifierConflits(Creneau creneau) throws SQLException {
        List<String> messages = new ArrayList<>();
        for (ResultatConflit r : verifierConflitsAvecSolutions(creneau)) {
            messages.add(r.toString());
        }
        return messages;
    }

    /**
     * Vérifie tous les conflits et retourne des résultats avec solutions.
     */
    public List<ResultatConflit> verifierConflitsAvecSolutions(Creneau creneau) throws SQLException {
        List<ResultatConflit> resultats = new ArrayList<>();

        // 1. Vérification du conflit de salle physique (si une salle est assignée au créneau)
        if (creneau.getSalleId() != null) {
            ResultatConflit c = verifierConflitSalle(creneau);
            if (c != null) resultats.add(c);
        }

        // 2. Vérification du conflit d'agenda pour l'enseignant dispensant le cours
        ResultatConflit ce = verifierConflitEnseignant(creneau);
        if (ce != null) resultats.add(ce);

        // 3. Vérification du conflit pour la promotion d'étudiants (éviter les cours simultanés)
        ResultatConflit cc = verifierConflitClasse(creneau);
        if (cc != null) resultats.add(cc);

        return resultats;
    }

    /**
     * Analyse la disponibilité de la salle assignée et génère des solutions de contournement :
     * - Recherche de salles alternatives libres dans le même bâtiment ou sur le campus ayant capacité suffisante.
     * - Recherche de créneaux libres dans la journée pour cette même salle.
     */
    private ResultatConflit verifierConflitSalle(Creneau creneau) throws SQLException {
        // Contrôle en base : la salle est-elle déjà mobilisée par un autre créneau actif ?
        boolean dispo = creneauDAO.verifierDisponibiliteSalle(
                creneau.getSalleId(), creneau.getJour(),
                creneau.getHeureDebut(), creneau.getHeureFin());
        if (dispo) return null; // Aucun conflit, la salle est parfaitement libre

        Salle salle = salleDAO.trouverParId(creneau.getSalleId());
        String nomSalle = salle != null ? "Salle " + salle.getNumero() : "Salle #" + creneau.getSalleId();

        ResultatConflit conflit = new ResultatConflit("SALLE",
                nomSalle + " déjà occupée le " + creneau.getJour()
                        + " de " + creneau.getHeureDebut() + " à " + creneau.getHeureFin(),
                creneau);

        // Algorithme de recherche 1 : Salles alternatives avec type et capacité équivalents
        for (Salle alt : trouverSallesAlternatives(creneau, salle)) {
            conflit.ajouterSolution(new SolutionConflit("CHANGER_SALLE",
                    "Utiliser la Salle " + alt.getNumero()
                            + " (capacité " + alt.getCapacite() + ", " + alt.getType() + ")",
                    alt));
        }

        // Algorithme de recherche 2 : Créneaux horaires libres pour la même salle
        for (String[] h : trouverCreneauxLibresSalle(creneau)) {
            conflit.ajouterSolution(new SolutionConflit("CHANGER_HORAIRE",
                    "Déplacer à " + h[0] + "-" + h[1] + " (même salle disponible)", h));
        }

        // Stratégie de repli : invite manuelle si aucun créneau automatique n'a été trouvé
        if (!conflit.aSolutions()) {
            conflit.ajouterSolution(new SolutionConflit("CHANGER_HORAIRE",
                    "Choisir manuellement un autre créneau ou une autre salle", null));
        }
        return conflit;
    }

    /**
     * Vérifie si l'enseignant est déjà assigné à un autre cours sur la même plage horaire.
     */
    private ResultatConflit verifierConflitEnseignant(Creneau creneau) throws SQLException {
        Cours cours = coursDAO.trouverParId(creneau.getCoursId());
        if (cours == null) return null;

        // Contrôle en base sur l'agenda de l'enseignant
        boolean dispo = creneauDAO.verifierDisponibiliteEnseignant(
                cours.getEnseignantId(), creneau.getJour(),
                creneau.getHeureDebut(), creneau.getHeureFin());
        if (dispo) return null;

        Utilisateur ens = utilisateurDAO.trouverParId(cours.getEnseignantId());
        String nomEns = ens != null ? ens.getPrenom() + " " + ens.getNom()
                : "Enseignant #" + cours.getEnseignantId();

        ResultatConflit conflit = new ResultatConflit("ENSEIGNANT",
                nomEns + " déjà occupé(e) le " + creneau.getJour()
                        + " de " + creneau.getHeureDebut() + " à " + creneau.getHeureFin(),
                creneau);

        for (String[] h : trouverCreneauxLibresEnseignant(cours.getEnseignantId(), creneau)) {
            conflit.ajouterSolution(new SolutionConflit("CHANGER_HORAIRE",
                    "Déplacer à " + h[0] + "-" + h[1] + " (" + nomEns + " disponible)", h));
        }

        if (!conflit.aSolutions()) {
            conflit.ajouterSolution(new SolutionConflit("CHANGER_HORAIRE",
                    "Choisir manuellement un créneau où " + nomEns + " est disponible", null));
        }
        return conflit;
    }

    private ResultatConflit verifierConflitClasse(Creneau creneau) throws SQLException {
        Cours cours = coursDAO.trouverParId(creneau.getCoursId());
        if (cours == null) return null;

        List<Creneau> creneauxJour = creneauDAO.listerParJour(creneau.getJour());
        for (Creneau autreC : creneauxJour) {
            if (autreC.getId() == creneau.getId()) continue;
            if ("annule".equals(autreC.getStatut())) continue;

            Cours autreCours = coursDAO.trouverParId(autreC.getCoursId());
            if (autreCours == null || autreCours.getClasseId() != cours.getClasseId()) continue;

            if (seChevauchent(creneau.getHeureDebut(), creneau.getHeureFin(),
                    autreC.getHeureDebut(), autreC.getHeureFin())) {

                Classe classe = classeDAO.trouverParId(cours.getClasseId());
                String nomClasse = classe != null ? classe.getIntitule() : "Classe #" + cours.getClasseId();
                Matiere autreM = matiereDAO.trouverParId(autreCours.getMatiereId());
                String nomAutreM = autreM != null ? autreM.getNom() : "cours #" + autreC.getCoursId();

                ResultatConflit conflit = new ResultatConflit("CLASSE",
                        "La classe " + nomClasse + " a déjà «" + nomAutreM + "» le "
                                + creneau.getJour() + " de " + autreC.getHeureDebut()
                                + " à " + autreC.getHeureFin(),
                        creneau);

                for (String[] h : trouverCreneauxLibresClasse(cours.getClasseId(), creneau)) {
                    conflit.ajouterSolution(new SolutionConflit("CHANGER_HORAIRE",
                            "Déplacer à " + h[0] + "-" + h[1] + " (classe disponible)", h));
                }

                if (!conflit.aSolutions()) {
                    conflit.ajouterSolution(new SolutionConflit("CHANGER_HORAIRE",
                            "Déplacer ce cours à un autre créneau", null));
                }
                return conflit;
            }
        }
        return null;
    }

    
    /**
     * Vérifie les conflits pour une liste de créneaux.
     */
    public List<ResultatConflit> verifierListeCreneaux(List<Creneau> creneaux) throws SQLException {
        List<ResultatConflit> tous = new ArrayList<>();
        
        for (Creneau c : creneaux) {
            tous.addAll(verifierConflitsAvecSolutions(c));
            
            for (Creneau autre : creneaux) {
                if (c.getId() == autre.getId()) continue;
                if (!c.getJour().equals(autre.getJour())) continue;
                if (!seChevauchent(c.getHeureDebut(), c.getHeureFin(),
                        autre.getHeureDebut(), autre.getHeureFin())) continue;
                
                if (c.getSalleId() != null && c.getSalleId().equals(autre.getSalleId())) {
                    Salle s = salleDAO.trouverParId(c.getSalleId());
                    String nomS = s != null ? "Salle " + s.getNumero() : "même salle";
                    ResultatConflit conf = new ResultatConflit("SALLE",
                            "Deux cours utilisent " + nomS + " le " + c.getJour()
                                    + " à " + c.getHeureDebut(), c);
                    for (Salle alt : trouverSallesAlternatives(autre, s)) {
                        conf.ajouterSolution(new SolutionConflit("CHANGER_SALLE",
                                "Affecter l'autre cours à la Salle " + alt.getNumero(), alt));
                    }
                    tous.add(conf);
                }
                
                Cours cours1 = coursDAO.trouverParId(c.getCoursId());
                Cours cours2 = coursDAO.trouverParId(autre.getCoursId());
                if (cours1 != null && cours2 != null
                        && cours1.getEnseignantId() == cours2.getEnseignantId()) {
                    Utilisateur ens = utilisateurDAO.trouverParId(cours1.getEnseignantId());
                    String nomEns = ens != null ? ens.getPrenom() + " " + ens.getNom() : "même enseignant";
                    ResultatConflit conf = new ResultatConflit("ENSEIGNANT",
                            nomEns + " est affecté à deux cours en même temps le "
                                    + c.getJour() + " à " + c.getHeureDebut(), c);
                    for (String[] h : trouverCreneauxLibresEnseignant(cours1.getEnseignantId(), autre)) {
                        conf.ajouterSolution(new SolutionConflit("CHANGER_HORAIRE",
                                "Déplacer l'un des cours à " + h[0] + "-" + h[1], h));
                    }
                    tous.add(conf);
                }
                
                if (cours1 != null && cours2 != null
                        && cours1.getClasseId() == cours2.getClasseId()) {
                    Classe cl = classeDAO.trouverParId(cours1.getClasseId());
                    String nomCl = cl != null ? cl.getIntitule() : "même classe";
                    ResultatConflit conf = new ResultatConflit("CLASSE",
                            "La classe " + nomCl + " a deux cours superposés le "
                                    + c.getJour() + " à " + c.getHeureDebut(), c);
                    for (String[] h : trouverCreneauxLibresClasse(cours1.getClasseId(), autre)) {
                        conf.ajouterSolution(new SolutionConflit("CHANGER_HORAIRE",
                                "Déplacer l'un des cours à " + h[0] + "-" + h[1], h));
                    }
                    tous.add(conf);
                }
            }
        }
        
        return tous;
    }

    
    public List<Salle> trouverSallesAlternatives(Creneau creneau, Salle salleActuelle) throws SQLException {
        List<Salle> alternatives = new ArrayList<>();
        int capaciteRequise = 1;
        if (salleActuelle != null) {
            capaciteRequise = salleActuelle.getCapacite();
        } else {
            Cours cours = coursDAO.trouverParId(creneau.getCoursId());
            if (cours != null) {
                Classe classe = classeDAO.trouverParId(cours.getClasseId());
                if (classe != null) capaciteRequise = classe.getEffectif();
            }
        }

        for (Salle s : salleDAO.listerTous()) {
            if (salleActuelle != null && s.getId() == salleActuelle.getId()) continue;
            if (!"disponible".equals(s.getStatut())) continue;
            if (s.getCapacite() < capaciteRequise) continue;
            if (creneauDAO.verifierDisponibiliteSalle(
                    s.getId(), creneau.getJour(),
                    creneau.getHeureDebut(), creneau.getHeureFin())) {
                alternatives.add(s);
                if (alternatives.size() >= 3) break;
            }
        }
        return alternatives;
    }

    private List<String[]> trouverCreneauxLibresSalle(Creneau creneau) throws SQLException {
        List<String[]> libres = new ArrayList<>();
        if (creneau.getSalleId() == null) return libres;
        int duree = calculerDureeHeures(creneau.getHeureDebut(), creneau.getHeureFin());
        for (String debut : new String[]{"08:00","09:00","10:00","11:00","13:00","14:00","15:00","16:00"}) {
            if (debut.equals(creneau.getHeureDebut())) continue;
            String fin = ajouterHeures(debut, duree);
            if (heureEnInt(fin) > 18 * 60) continue;
            if (creneauDAO.verifierDisponibiliteSalle(creneau.getSalleId(), creneau.getJour(), debut, fin)) {
                libres.add(new String[]{debut, fin});
                if (libres.size() >= 2) break;
            }
        }
        return libres;
    }

    private List<String[]> trouverCreneauxLibresEnseignant(int enseignantId, Creneau creneau) throws SQLException {
        List<String[]> libres = new ArrayList<>();
        int duree = calculerDureeHeures(creneau.getHeureDebut(), creneau.getHeureFin());
        for (String debut : new String[]{"08:00","09:00","10:00","11:00","13:00","14:00","15:00","16:00"}) {
            if (debut.equals(creneau.getHeureDebut())) continue;
            String fin = ajouterHeures(debut, duree);
            if (heureEnInt(fin) > 18 * 60) continue;
            if (creneauDAO.verifierDisponibiliteEnseignant(enseignantId, creneau.getJour(), debut, fin)) {
                libres.add(new String[]{debut, fin});
                if (libres.size() >= 2) break;
            }
        }
        return libres;
    }

    private List<String[]> trouverCreneauxLibresClasse(int classeId, Creneau creneau) throws SQLException {
        List<String[]> libres = new ArrayList<>();
        int duree = calculerDureeHeures(creneau.getHeureDebut(), creneau.getHeureFin());
        List<Creneau> creneauxJour = creneauDAO.listerParJour(creneau.getJour());
        for (String debut : new String[]{"08:00","09:00","10:00","11:00","13:00","14:00","15:00","16:00"}) {
            if (debut.equals(creneau.getHeureDebut())) continue;
            String fin = ajouterHeures(debut, duree);
            if (heureEnInt(fin) > 18 * 60) continue;
            boolean conflit = false;
            for (Creneau c : creneauxJour) {
                if ("annule".equals(c.getStatut())) continue;
                Cours co = coursDAO.trouverParId(c.getCoursId());
                if (co == null || co.getClasseId() != classeId) continue;
                if (seChevauchent(debut, fin, c.getHeureDebut(), c.getHeureFin())) { conflit = true; break; }
            }
            if (!conflit) {
                libres.add(new String[]{debut, fin});
                if (libres.size() >= 2) break;
            }
        }
        return libres;
    }

    
    /**
     * Propose des alternatives concrètes pour résoudre un conflit.
     */
    public List<Map<String, Object>> proposerAlternatives(Creneau creneau, int classeId)
            throws SQLException {

        List<Map<String, Object>> alternatives = new ArrayList<>();

        Salle salleActuelle = creneau.getSalleId() != null
                ? salleDAO.trouverParId(creneau.getSalleId()) : null;

        int capaciteRequise = 1;
        Classe classe = classeDAO.trouverParId(classeId);
        if (classe != null) capaciteRequise = classe.getEffectif();

        for (Salle s : salleDAO.listerTous()) {
            if (salleActuelle != null && s.getId() == salleActuelle.getId()) continue;
            if (!"disponible".equals(s.getStatut())) continue;
            if (s.getCapacite() < capaciteRequise) continue;
            if (creneauDAO.verifierDisponibiliteSalle(
                    s.getId(), creneau.getJour(),
                    creneau.getHeureDebut(), creneau.getHeureFin())) {
                Map<String, Object> alt = new HashMap<>();
                alt.put("type", "salle_alternative");
                alt.put("message", "🏫 Salle " + s.getNumero()
                        + " (cap. " + s.getCapacite() + " - " + s.getType() + ")");
                alt.put("salle", s);
                alternatives.add(alt);
                if (alternatives.size() >= 3) break;
            }
        }

        int duree = calculerDureeHeures(creneau.getHeureDebut(), creneau.getHeureFin());
        Cours cours = coursDAO.trouverParId(creneau.getCoursId());

        for (String debut : new String[]{"08:00","09:00","10:00","11:00","13:00","14:00","15:00","16:00"}) {
            if (debut.equals(creneau.getHeureDebut())) continue;
            String fin = ajouterHeures(debut, duree);
            if (heureEnInt(fin) > 18 * 60) continue;

            boolean salleLibre = creneau.getSalleId() == null
                    || creneauDAO.verifierDisponibiliteSalle(
                            creneau.getSalleId(), creneau.getJour(), debut, fin);
            boolean ensLibre = cours == null
                    || creneauDAO.verifierDisponibiliteEnseignant(
                            cours.getEnseignantId(), creneau.getJour(), debut, fin);

            if (salleLibre && ensLibre) {
                Map<String, Object> alt = new HashMap<>();
                alt.put("type", "creneau_alternatif");
                alt.put("message", "⏰ Même jour à " + debut + " - " + fin);
                alt.put("heureDebut", debut);
                alt.put("heureFin", fin);
                alternatives.add(alt);
                if (alternatives.stream()
                        .filter(a -> "creneau_alternatif".equals(a.get("type")))
                        .count() >= 2) break;
            }
        }

        if (creneau.getJour() != null) {
            try {
                java.time.LocalDate dateBase = java.time.LocalDate.parse(creneau.getJour());
                for (int delta = 1; delta <= 6; delta++) {
                    java.time.LocalDate autreJour = dateBase.plusDays(delta);
                    if (autreJour.getDayOfWeek().getValue() == 7) continue;

                    String autreJourStr = autreJour.toString();
                    boolean salleLibre = creneau.getSalleId() == null
                            || creneauDAO.verifierDisponibiliteSalle(
                                    creneau.getSalleId(), autreJourStr,
                                    creneau.getHeureDebut(), creneau.getHeureFin());
                    boolean ensLibre = cours == null
                            || creneauDAO.verifierDisponibiliteEnseignant(
                                    cours.getEnseignantId(), autreJourStr,
                                    creneau.getHeureDebut(), creneau.getHeureFin());

                    if (salleLibre && ensLibre) {
                        String nomJour = autreJour.getDayOfWeek()
                                .getDisplayName(java.time.format.TextStyle.FULL,
                                        java.util.Locale.FRENCH);
                        Map<String, Object> alt = new HashMap<>();
                        alt.put("type", "jour_alternatif");
                        alt.put("message", "📅 " + nomJour + " " + autreJourStr
                                + " à " + creneau.getHeureDebut());
                        alt.put("jour", autreJourStr);
                        alternatives.add(alt);
                        if (alternatives.stream()
                                .filter(a -> "jour_alternatif".equals(a.get("type")))
                                .count() >= 2) break;
                    }
                }
            } catch (Exception ignored) { }
        }

        return alternatives;
    }

    
    private boolean seChevauchent(String d1, String f1, String d2, String f2) {
        int id1 = heureEnInt(d1), if1 = heureEnInt(f1);
        int id2 = heureEnInt(d2), if2 = heureEnInt(f2);
        return id1 < if2 && id2 < if1;
    }

    private int heureEnInt(String heure) {
        try {
            String[] p = heure.split(":");
            return Integer.parseInt(p[0]) * 60 + (p.length > 1 ? Integer.parseInt(p[1]) : 0);
        } catch (Exception e) { return 0; }
    }

    private int calculerDureeHeures(String debut, String fin) {
        return (heureEnInt(fin) - heureEnInt(debut)) / 60;
    }

    private String ajouterHeures(String heure, int heures) {
        try {
            String[] p = heure.split(":");
            return String.format("%02d:%02d", Integer.parseInt(p[0]) + heures,
                    p.length > 1 ? Integer.parseInt(p[1]) : 0);
        } catch (Exception e) { return heure; }
    }
}