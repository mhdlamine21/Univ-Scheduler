package scheduler.service;

import scheduler.dao.*;
import scheduler.modele.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
import java.util.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Service de gestion des réservations de salles.
 */
public class ReservationService {
    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);
    
    private ReservationDAO reservationDAO;
    private SalleDAO salleDAO;
    private UtilisateurDAO utilisateurDAO;
    private NotificationService notificationService;
    private EmailService emailService;
    private PlanningService planningService;
    private ClasseDAO classeDAO;
    private MatiereDAO matiereDAO;
    private CoursDAO coursDAO;
    
    // ✅ AJOUT : Variable pour limiter la fréquence des vérifications
    private long dernierRappel = 0;
    private static final long INTERVALLE_RAPPEL_MS = 60000; // 1 minute minimum entre deux vérifications
    
    public ReservationService() {
        this.reservationDAO = new ReservationDAO();
        this.salleDAO = new SalleDAO();
        this.utilisateurDAO = new UtilisateurDAO();
        this.notificationService = new NotificationService();
        this.emailService = new EmailService();
        this.planningService = new PlanningService();
        this.classeDAO = new ClasseDAO();
        this.matiereDAO = new MatiereDAO();
        this.coursDAO = new CoursDAO();
    }
    
    
    /**
     * Réservation avec préférences - recherche automatique de salle
     */
    public Map<String, Object> reserverAvecPreferences(int utilisateurId, String date, 
            String heureDebut, String heureFin, int capaciteMin, String typeSalle,
            List<Integer> equipementsRequis, String motif, String description,
            String typeReservation, Integer classeId, String enseignantNom) throws SQLException {
        
        Map<String, Object> resultat = new HashMap<>();
        List<Salle> sallesDisponibles = new ArrayList<>();
        
        List<Salle> toutesSalles = salleDAO.listerDisponibles();
        
        for (Salle salle : toutesSalles) {
            boolean estLibre = reservationDAO.verifierDisponibilite(
                salle.getId(), date, heureDebut, heureFin
            );
            boolean pasDeCours = planningService.salleEstDisponible(
                salle.getId(), date, heureDebut, heureFin
            );
            
            if (!estLibre || !pasDeCours) continue;
            if (salle.getCapacite() < capaciteMin) continue;
            if (typeSalle != null && !typeSalle.isEmpty() && !"Tous".equals(typeSalle)) {
                if (!salle.getType().equals(typeSalle)) continue;
            }
            if (equipementsRequis != null && !equipementsRequis.isEmpty()) {
                List<Integer> equipementsSalle = salle.getEquipements();
                if (!equipementsSalle.containsAll(equipementsRequis)) continue;
            }
            
            sallesDisponibles.add(salle);
        }
        
        if (sallesDisponibles.isEmpty()) {
            resultat.put("succes", false);
            resultat.put("message", "Aucune salle disponible correspondant aux critères");
            return resultat;
        }
        
        sallesDisponibles.sort((s1, s2) -> {
            int ecart1 = s1.getCapacite() - capaciteMin;
            int ecart2 = s2.getCapacite() - capaciteMin;
            return Integer.compare(ecart1, ecart2);
        });
        
        Salle salleChoisie = sallesDisponibles.get(0);
        
        Reservation reservation = new Reservation(
            utilisateurId, salleChoisie.getId(), typeReservation, motif,
            date, heureDebut, heureFin
        );
        reservation.setDescription(description);
        reservation.setStatut("confirmee");
        
        if ((typeReservation.equals("CM") || typeReservation.equals("TD") || 
             typeReservation.equals("TP") || typeReservation.equals("EXAMEN")) && classeId != null) {
            reservation.setClasseId(classeId);
        }
        
        reservationDAO.ajouter(reservation);
        
        notifierReservation(reservation, salleChoisie, classeId, enseignantNom);
        
        resultat.put("succes", true);
        resultat.put("reservation", reservation);
        resultat.put("salle", salleChoisie);
        resultat.put("message", "Réservation confirmée - Salle " + salleChoisie.getNumero());
        
        return resultat;
    }
    
    /**
     * Notifie les personnes concernées par une réservation
     * AMÉLIORÉ : Envoi d'emails aux étudiants pour les réservations de cours/examens
     */
    private void notifierReservation(Reservation reservation, Salle salle, 
                                      Integer classeId, String enseignantNom) {
        try {
            // Notifier l'utilisateur qui a réservé
            Utilisateur demandeur = utilisateurDAO.trouverParId(reservation.getUtilisateurId());
            if (demandeur != null) {
                String sujet = "✅ Réservation confirmée - Salle " + salle.getNumero();
                String contenu = "Bonjour " + demandeur.getPrenom() + " " + demandeur.getNom() + ",\n\n"
                    + "Votre réservation a été confirmée.\n\n"
                    + "Type: " + reservation.getType() + "\n"
                    + "Salle: " + salle.getNumero() + "\n"
                    + "Date: " + reservation.getDateReservation() + "\n"
                    + "Horaire: " + reservation.getHeureDebut() + " - " + reservation.getHeureFin() + "\n"
                    + "Motif: " + reservation.getMotif() + "\n\n"
                    + "Cordialement,\nL'équipe SCHEDULER";
                emailService.envoyerEmail(demandeur.getEmail(), sujet, contenu);
            }
            
            if (classeId != null && (reservation.getType().equals("CM") || 
                reservation.getType().equals("TD") || reservation.getType().equals("TP") ||
                reservation.getType().equals("EXAMEN"))) {
                
                Classe classe = classeDAO.trouverParId(classeId);
                
                if (classe != null) {
                    List<Utilisateur> tousUtilisateurs = utilisateurDAO.listerTous();
                    List<Etudiant> etudiants = new ArrayList<>();
                    
                    for (Utilisateur u : tousUtilisateurs) {
                        if (u instanceof Etudiant && u.isEstValide()) {
                            Etudiant e = (Etudiant) u;
                            if (e.getClasseId() == classeId) {
                                etudiants.add(e);
                            }
                        }
                    }
                    
                    String typeReservation = "";
                    switch (reservation.getType()) {
                        case "CM": typeReservation = "Cours Magistral"; break;
                        case "TD": typeReservation = "Travaux Dirigés"; break;
                        case "TP": typeReservation = "Travaux Pratiques"; break;
                        case "EXAMEN": typeReservation = "Examen"; break;
                        default: typeReservation = "Cours";
                    }
                    
                    String matiereNom = reservation.getMotif();
                    
                    for (Etudiant e : etudiants) {
                        emailService.envoyerReservationClasse(
                            e.getEmail(),
                            e.getPrenom() + " " + e.getNom(),
                            typeReservation,
                            classe.getIntitule(),
                            salle.getNumero(),
                            reservation.getDateReservation(),
                            reservation.getHeureDebut(),
                            reservation.getHeureFin(),
                            enseignantNom != null ? enseignantNom : demandeur != null ? demandeur.getPrenom() + " " + demandeur.getNom() : "Enseignant",
                            matiereNom
                        );
                        
                        notificationService.ajouterNotification(
                            e.getId(),
                            "📚 Nouveau " + typeReservation + " - " + matiereNom + "\n" +
                            "📅 " + reservation.getDateReservation() + " à " + reservation.getHeureDebut() + "\n" +
                            "🏫 Salle " + salle.getNumero()
                        );
                    }
                    
                    logger.info("✅ Emails envoyés à {} étudiants pour la réservation #{}", etudiants.size(), reservation.getId());
                }
            }
            
        } catch (SQLException e) {
            logger.error("Erreur lors de l'envoi des notifications", e);
        }
    }
    
    
    /**
     * Propose des prolongations possibles pour une réservation.
     */
    public Map<String, Object> proposerProlongation(int reservationId) throws SQLException {
        Map<String, Object> resultat = new HashMap<>();
        
        Reservation reservation = reservationDAO.trouverParId(reservationId);
        if (reservation == null) {
            resultat.put("succes", false);
            resultat.put("message", "Réservation non trouvée");
            return resultat;
        }
        
        String heureFin = reservation.getHeureFin();
        String[] parts = heureFin.split(":");
        int heure = Integer.parseInt(parts[0]);
        
        List<Map<String, Object>> creneauxLibres = new ArrayList<>();
        
        for (int i = 1; i <= 3; i++) {
            int nouvelleHeure = heure + i;
            if (nouvelleHeure > 20) break;
            
            String nouvelleHeureDebut = String.format("%02d:00", nouvelleHeure - 1);
            String nouvelleHeureFin = String.format("%02d:00", nouvelleHeure);
            
            boolean salleLibre = reservationDAO.verifierDisponibilite(
                reservation.getSalleId(),
                reservation.getDateReservation(),
                nouvelleHeureDebut,
                nouvelleHeureFin
            );
            
            boolean pasDeCours = planningService.salleEstDisponible(
                reservation.getSalleId(),
                reservation.getDateReservation(),
                nouvelleHeureDebut,
                nouvelleHeureFin
            );
            
            if (salleLibre && pasDeCours) {
                Map<String, Object> creneau = new HashMap<>();
                creneau.put("heureDebut", nouvelleHeureDebut);
                creneau.put("heureFin", nouvelleHeureFin);
                creneau.put("duree", i);
                creneauxLibres.add(creneau);
            }
        }
        
        resultat.put("succes", !creneauxLibres.isEmpty());
        resultat.put("creneaux", creneauxLibres);
        resultat.put("reservation", reservation);
        
        return resultat;
    }
    
    /**
     * Prolonge une réservation.
     */
    public boolean prolongerReservation(int id, String nouvelleHeureFin) throws SQLException {
        Reservation reservation = reservationDAO.trouverParId(id);
        
        if (reservation == null) {
            return false;
        }
        
        boolean disponible = reservationDAO.verifierDisponibilite(
            reservation.getSalleId(),
            reservation.getDateReservation(),
            reservation.getHeureFin(),
            nouvelleHeureFin
        );
        
        boolean pasDeCours = planningService.salleEstDisponible(
            reservation.getSalleId(),
            reservation.getDateReservation(),
            reservation.getHeureFin(),
            nouvelleHeureFin
        );
        
        if (!disponible || !pasDeCours) {
            return false;
        }
        
        reservationDAO.prolonger(id, nouvelleHeureFin);
        
        Utilisateur utilisateur = utilisateurDAO.trouverParId(reservation.getUtilisateurId());
        if (utilisateur != null) {
            notificationService.ajouterNotification(utilisateur.getId(), 
                "Votre réservation a été prolongée jusqu'à " + nouvelleHeureFin);
            
            emailService.envoyerEmail(
                utilisateur.getEmail(),
                "⏰ Prolongation de réservation",
                "Bonjour " + utilisateur.getPrenom() + ",\n\n" +
                "Votre réservation a été prolongée jusqu'à " + nouvelleHeureFin + ".\n\n" +
                "Cordialement,\nL'équipe SCHEDULER"
            );
        }
        
        return true;
    }
    
    
    /**
     * Recherche les salles disponibles avec filtres.
     */
    public List<Map<String, Object>> getSallesDisponiblesAvecFiltres(
            String date, String heureDebut, String heureFin,
            Integer capaciteMin, String typeSalle, List<Integer> equipementsRequis) throws SQLException {
        
        List<Map<String, Object>> resultats = new ArrayList<>();
        List<Salle> toutesSalles = salleDAO.listerDisponibles();
        
        for (Salle salle : toutesSalles) {
            boolean estLibre = reservationDAO.verifierDisponibilite(
                salle.getId(), date, heureDebut, heureFin
            );
            boolean pasDeCours = planningService.salleEstDisponible(
                salle.getId(), date, heureDebut, heureFin
            );
            
            if (!estLibre || !pasDeCours) continue;
            if (capaciteMin != null && salle.getCapacite() < capaciteMin) continue;
            if (typeSalle != null && !typeSalle.isEmpty() && !"Tous".equals(typeSalle)) {
                if (!salle.getType().equals(typeSalle)) continue;
            }
            if (equipementsRequis != null && !equipementsRequis.isEmpty()) {
                List<Integer> equipementsSalle = salle.getEquipements();
                if (!equipementsSalle.containsAll(equipementsRequis)) continue;
            }
            
            Map<String, Object> info = new HashMap<>();
            info.put("salle", salle);
            info.put("numero", salle.getNumero());
            info.put("capacite", salle.getCapacite());
            info.put("type", salle.getType());
            info.put("etage", salle.getEtage());
            info.put("batimentId", salle.getBatimentId());
            info.put("equipements", salle.getEquipements());
            
            resultats.add(info);
        }
        
        return resultats;
    }
    
    
    /**
     * Vérifie les rappels de fin de réservation.
     * ✅ CORRECTION : Ajout d'un délai minimum entre deux vérifications
     */
    public void verifierRappelsFinReservation() {
        // ✅ Éviter les exécutions trop fréquentes (1 minute minimum entre deux vérifications)
        long maintenant = System.currentTimeMillis();
        if (maintenant - dernierRappel < INTERVALLE_RAPPEL_MS) {
            return;
        }
        dernierRappel = maintenant;
        
        new Thread(() -> {
            try {
                String aujourdhui = LocalDate.now().toString();
                LocalTime maintenantHeure = LocalTime.now();
                LocalTime dans5Min = maintenantHeure.plusMinutes(5);
                String heureDans5Min = dans5Min.format(DateTimeFormatter.ofPattern("HH:mm"));
                
                List<Reservation> reservationsDuJour = reservationDAO.listerParDate(aujourdhui);
                
                for (Reservation r : reservationsDuJour) {
                    if ("annulee".equals(r.getStatut())) continue;
                    
                    String heureFin = r.getHeureFin();
                    if (heureFin != null && heureFin.equals(heureDans5Min)) {
                        envoyerRappelFin(r);
                    }
                }
                
            } catch (SQLException e) {
                logger.error("Erreur lors de la vérification des rappels", e);
            }
        }).start();
    }
    
    private void envoyerRappelFin(Reservation reservation) {
        try {
            Utilisateur utilisateur = utilisateurDAO.trouverParId(reservation.getUtilisateurId());
            Salle salle = salleDAO.trouverParId(reservation.getSalleId());
            
            if (utilisateur != null && salle != null) {
                emailService.envoyerRappelFinReservation(
                    utilisateur.getEmail(),
                    utilisateur.getPrenom() + " " + utilisateur.getNom(),
                    salle.getNumero(),
                    reservation.getHeureFin()
                );
                
                notificationService.ajouterNotification(utilisateur.getId(),
                    "⏰ Votre réservation de la salle " + salle.getNumero() + 
                    " se termine dans 5 minutes (" + reservation.getHeureFin() + ")");
            }
        } catch (SQLException e) {
            logger.error("Erreur envoi rappel", e);
        }
    }
    
    
    private static final String SIGNATURE = "Cordialement,\nL'équipe SCHEDULER\nUniversité Iba Der Thiam de Thiès";
    
    public void reserver(Reservation reservation) throws SQLException {
        boolean disponible = verifierDisponibilite(
            reservation.getSalleId(),
            reservation.getDateReservation(),
            reservation.getHeureDebut(),
            reservation.getHeureFin()
        );
        
        boolean pasDeCours = planningService.salleEstDisponible(
            reservation.getSalleId(),
            reservation.getDateReservation(),
            reservation.getHeureDebut(),
            reservation.getHeureFin()
        );

        if (!disponible || !pasDeCours) {
            Utilisateur utilisateur = utilisateurDAO.trouverParId(reservation.getUtilisateurId());
            String nomDemandeur = utilisateur != null ?
                utilisateur.getPrenom() + " " + utilisateur.getNom() : "Utilisateur #" + reservation.getUtilisateurId();
            String salle = "Salle #" + reservation.getSalleId();

            List<Utilisateur> gestionnaires = utilisateurDAO.listerParRole("gestionnaire");
            for (Utilisateur g : gestionnaires) {
                notificationService.notifierConflitReservation(
                    g.getId(), reservation, nomDemandeur, salle,
                    "La salle n'est pas disponible sur ce créneau",
                    emailService, g.getEmail()
                );
                
                emailService.envoyerConflitGestionnaire(
                    g.getEmail(),
                    "Réservation",
                    "La salle est déjà occupée",
                    salle,
                    reservation.getDateReservation(),
                    reservation.getHeureDebut(),
                    reservation.getHeureFin(),
                    nomDemandeur
                );
            }
            throw new SQLException("CONFLIT: La salle n'est pas disponible sur ce créneau.");
        }

        // 🔒 Réservation atomique sécurisée contre les accès concurrents
        reservationDAO.reserverAvecVerrou(reservation);
        
        Salle salle = salleDAO.trouverParId(reservation.getSalleId());
        Integer classeId = null;
        if (reservation.getClasseId() != null) {
            classeId = reservation.getClasseId();
        }
        
        notifierReservation(reservation, salle, classeId, null);
    }
    
    public Reservation trouverParId(int id) throws SQLException {
        return reservationDAO.trouverParId(id);
    }
    
    public void validerReservation(int id) throws SQLException {
        Reservation reservation = reservationDAO.trouverParId(id);
        if (reservation == null) return;
        
        reservationDAO.valider(id);
        
        Utilisateur enseignant = utilisateurDAO.trouverParId(reservation.getUtilisateurId());
        if (enseignant != null) {
            notificationService.ajouterNotification(enseignant.getId(), 
                "Votre réservation pour le " + reservation.getDateReservation() + " a été validée");
        }
    }
    
    /**
     * Résout un conflit de réservation avec une solution
     */
    public void resoudreConflit(int reservationId, Integer nouvelleSalleId,
            String nouveauJour, String nouvelleHeureDebut, String nouvelleHeureFin,
            String solution, EmailService email, UtilisateurService utilisateurService,
            int gestionnaireId) throws SQLException {
        
        Reservation reservation = reservationDAO.trouverParId(reservationId);
        if (reservation == null) {
            throw new SQLException("Réservation non trouvée");
        }

        if (nouvelleSalleId != null) {
            reservation.setSalleId(nouvelleSalleId);
        }
        if (nouveauJour != null && !nouveauJour.isEmpty()) {
            reservation.setDateReservation(nouveauJour);
        }
        if (nouvelleHeureDebut != null && !nouvelleHeureDebut.isEmpty()) {
            reservation.setHeureDebut(nouvelleHeureDebut);
        }
        if (nouvelleHeureFin != null && !nouvelleHeureFin.isEmpty()) {
            reservation.setHeureFin(nouvelleHeureFin);
        }
        
        boolean disponible = reservationDAO.verifierDisponibilite(
            reservation.getSalleId(),
            reservation.getDateReservation(),
            reservation.getHeureDebut(),
            reservation.getHeureFin()
        );
        
        if (!disponible) {
            throw new SQLException("Le nouveau créneau n'est pas disponible");
        }
        
        reservation.setStatut("confirmee");
        reservationDAO.modifier(reservation);
        
        // Ajouter à l'historique
        HistoriqueReservationDAO historiqueDAO = new HistoriqueReservationDAO();
        HistoriqueReservation historique = new HistoriqueReservation(
            reservation.getId(),
            "CONFLIT_RESOLU",
            gestionnaireId,
            "Conflit résolu : " + solution
        );
        historiqueDAO.ajouter(historique);

        // Notifier l'utilisateur
        Utilisateur utilisateur = utilisateurDAO.trouverParId(reservation.getUtilisateurId());
        if (utilisateur != null && email != null) {
            email.envoyerResolutionConflit(
                utilisateur.getEmail(),
                utilisateur.getPrenom() + " " + utilisateur.getNom(),
                "Réservation",
                solution,
                "Salle #" + reservation.getSalleId(),
                reservation.getDateReservation(),
                reservation.getHeureDebut(),
                reservation.getHeureFin()
            );
        }
        
        logger.info("✅ Conflit de réservation #{} résolu par gestionnaire #{}", reservationId, gestionnaireId);
    }
    
   
    /**
     * Modifie une réservation existante
     */
    public void modifier(Reservation reservation) throws SQLException {
        Reservation existante = reservationDAO.trouverParId(reservation.getId());
        if (existante == null) {
            throw new SQLException("Réservation non trouvée");
        }
        
        boolean disponible = reservationDAO.verifierDisponibilite(
            reservation.getSalleId(),
            reservation.getDateReservation(),
            reservation.getHeureDebut(),
            reservation.getHeureFin()
        );
        
        boolean pasDeCours = planningService.salleEstDisponible(
            reservation.getSalleId(),
            reservation.getDateReservation(),
            reservation.getHeureDebut(),
            reservation.getHeureFin()
        );
        
        if (!disponible || !pasDeCours) {
            throw new SQLException("La salle n'est pas disponible sur ce créneau");
        }
        
        reservationDAO.modifier(reservation);
        
        HistoriqueReservationDAO historiqueDAO = new HistoriqueReservationDAO();
        HistoriqueReservation historique = new HistoriqueReservation(
            reservation.getId(),
            "MODIFICATION",
            reservation.getUtilisateurId(),
            "Réservation modifiée : nouveau créneau le " + reservation.getDateReservation() + 
            " de " + reservation.getHeureDebut() + " à " + reservation.getHeureFin()
        );
        historiqueDAO.ajouter(historique);
        
        Utilisateur utilisateur = utilisateurDAO.trouverParId(reservation.getUtilisateurId());
        if (utilisateur != null) {
            String sujet = "✏️ Modification de votre réservation";
            String contenu = "Bonjour " + utilisateur.getPrenom() + " " + utilisateur.getNom() + ",\n\n" +
                "Votre réservation a été modifiée.\n\n" +
                "Nouveau créneau :\n" +
                "📅 Date : " + reservation.getDateReservation() + "\n" +
                "⏰ Horaire : " + reservation.getHeureDebut() + " - " + reservation.getHeureFin() + "\n" +
                "🏫 Salle : " + getNumeroSalle(reservation.getSalleId()) + "\n\n" +
                "Cordialement,\nL'équipe SCHEDULER";
            emailService.envoyerEmail(utilisateur.getEmail(), sujet, contenu);
        }
        
        logger.info("✅ Réservation #{} modifiée", reservation.getId());
    }
    
    
    /**
     * Récupère le numéro d'une salle par son ID
     */
    private String getNumeroSalle(int salleId) {
        try {
            Salle salle = salleDAO.trouverParId(salleId);
            return salle != null ? salle.getNumero() : "Salle " + salleId;
        } catch (SQLException e) {
            return "Salle " + salleId;
        }
    }
    
    public boolean salleLibreApres(int reservationId, String nouvelleHeureFin) throws SQLException {
        Reservation reservation = reservationDAO.trouverParId(reservationId);
        if (reservation == null) return false;
        
        boolean disponible = reservationDAO.verifierDisponibilite(
            reservation.getSalleId(),
            reservation.getDateReservation(),
            reservation.getHeureFin(),
            nouvelleHeureFin
        );
        
        boolean pasDeCours = true;
        if (planningService != null) {
            pasDeCours = planningService.salleEstDisponible(
                reservation.getSalleId(),
                reservation.getDateReservation(),
                reservation.getHeureFin(),
                nouvelleHeureFin
            );
        }
        
        return disponible && pasDeCours;
    }
    
    public void refuserReservation(int id, String motif) throws SQLException {
        Reservation reservation = reservationDAO.trouverParId(id);
        if (reservation == null) return;
        
        reservationDAO.refuser(id);
        
        Utilisateur enseignant = utilisateurDAO.trouverParId(reservation.getUtilisateurId());
        if (enseignant != null) {
            notificationService.ajouterNotification(enseignant.getId(), 
                "Votre réservation pour le " + reservation.getDateReservation() + " a été refusée");
        }
    }
    
    public boolean verifierDisponibilite(int salleId, String date, 
                                          String heureDebut, String heureFin) throws SQLException {
        return reservationDAO.verifierDisponibilite(salleId, date, heureDebut, heureFin);
    }

    public Reservation trouverConflit(int salleId, String date, String heureDebut, String heureFin) throws SQLException {
        return reservationDAO.trouverConflit(salleId, date, heureDebut, heureFin);
    }
    
    /**
     * Annule une réservation
     */
    public void annulerReservation(int id) throws SQLException {
        Reservation reservation = reservationDAO.trouverParId(id);
        if (reservation == null) return;
        
        reservationDAO.annuler(id);
        
        HistoriqueReservationDAO historiqueDAO = new HistoriqueReservationDAO();
        HistoriqueReservation historique = new HistoriqueReservation(
            id,
            "ANNULATION",
            reservation.getUtilisateurId(),
            "Réservation annulée"
        );
        historiqueDAO.ajouter(historique);
        
        Utilisateur utilisateur = utilisateurDAO.trouverParId(reservation.getUtilisateurId());
        if (utilisateur != null) {
            emailService.envoyerEmail(
                utilisateur.getEmail(),
                "❌ Annulation de réservation",
                "Bonjour " + utilisateur.getPrenom() + " " + utilisateur.getNom() + ",\n\n" +
                "Votre réservation du " + reservation.getDateReservation() +
                " a été annulée.\n\nCordialement,\nL'équipe SCHEDULER"
            );
        }
    }

    
    public List<Reservation> getReservationsUtilisateur(int utilisateurId) throws SQLException {
        return reservationDAO.listerParUtilisateur(utilisateurId);
    }
    
    public List<Reservation> getReservationsSalle(int salleId) throws SQLException {
        return reservationDAO.listerParSalle(salleId);
    }
    
    public List<Reservation> getReservationsJour(String date) throws SQLException {
        return reservationDAO.listerParDate(date);
    }
    
    public List<Reservation> getReservationsEnAttente() throws SQLException {
        return reservationDAO.listerEnAttente();
    }
    
    public List<Reservation> listerTous() throws SQLException {
        return reservationDAO.listerTous();
    }
}