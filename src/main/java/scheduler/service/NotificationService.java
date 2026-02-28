package scheduler.service;

import scheduler.modele.*;
import scheduler.dao.NotificationDAO;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service de gestion des notifications utilisateur (internes et email).
 */
public class NotificationService {
    private EmailService emailService;
    private NotificationDAO notificationDAO;
    private Map<Integer, List<String>> notificationsUtilisateur;
    private Map<Integer, Integer> compteursNonLus;

    public NotificationService() {
        this.emailService             = new EmailService();
        this.notificationDAO          = new NotificationDAO();
        this.notificationsUtilisateur = new ConcurrentHashMap<>();
        this.compteursNonLus          = new ConcurrentHashMap<>();
    }

    /**
     * Ajoute une notification interne pour un utilisateur (persistée en base).
     */
    public void ajouterNotification(int utilisateurId, String message) {
        notificationsUtilisateur
                .computeIfAbsent(utilisateurId, k -> new ArrayList<>())
                .add(message);
        compteursNonLus.put(utilisateurId,
                compteursNonLus.getOrDefault(utilisateurId, 0) + 1);

        // Persistance MySQL en tâche de fond
        new Thread(() -> {
            try {
                notificationDAO.ajouter(new Notification(utilisateurId, message));
            } catch (Exception e) {
                // Silencieux si problème DB temporaire
            }
        }).start();
    }

    public List<String> getNotifications(int utilisateurId) {
        try {
            List<Notification> dbList = notificationDAO.listerParUtilisateur(utilisateurId);
            if (!dbList.isEmpty()) {
                List<String> result = new ArrayList<>();
                for (Notification n : dbList) result.add(n.getMessage());
                return result;
            }
        } catch (Exception ignored) {}
        return notificationsUtilisateur.getOrDefault(utilisateurId, new ArrayList<>());
    }

    public List<String> getNotificationsNonLues(int utilisateurId) {
        try {
            List<Notification> dbList = notificationDAO.listerNonLues(utilisateurId);
            if (!dbList.isEmpty()) {
                List<String> result = new ArrayList<>();
                for (Notification n : dbList) result.add(n.getMessage());
                return result;
            }
        } catch (Exception ignored) {}
        return new ArrayList<>(getNotifications(utilisateurId));
    }

    public int getNombreNotificationsNonLues(int utilisateurId) {
        try {
            return notificationDAO.compterNonLues(utilisateurId);
        } catch (Exception ignored) {}
        return compteursNonLus.getOrDefault(utilisateurId, 0);
    }

    public void marquerCommeLues(int utilisateurId) {
        compteursNonLus.remove(utilisateurId);
        new Thread(() -> {
            try {
                notificationDAO.marquerToutesCommeLues(utilisateurId);
            } catch (Exception ignored) {}
        }).start();
    }

    public void supprimerNotifications(int utilisateurId) {
        notificationsUtilisateur.remove(utilisateurId);
        compteursNonLus.remove(utilisateurId);
        new Thread(() -> {
            try {
                notificationDAO.supprimerToutes(utilisateurId);
            } catch (Exception ignored) {}
        }).start();
    }

    
    /**
     * Envoie une notification de conflit avec une liste de solutions
     */
    public void notifierConflitAvecSolutions(int utilisateurId, String conflitDescription,
            List<String> solutions, String emailDestinataire, String nomDestinataire) {
        
        String message = "⚠️ CONFLIT DÉTECTÉ\n" + conflitDescription;
        
        if (solutions != null && !solutions.isEmpty()) {
            message += "\n\n💡 SOLUTIONS PROPOSÉES :";
            for (int i = 0; i < solutions.size(); i++) {
                message += "\n   " + (i + 1) + ". " + solutions.get(i);
            }
        }
        
        ajouterNotification(utilisateurId, message);
        
        // Envoyer l'email
        if (emailDestinataire != null && nomDestinataire != null) {
            emailService.envoyerConflitGestionnaire(
                emailDestinataire,
                "Conflit de planification",
                conflitDescription,
                "Salle concernée",
                "",
                "",
                "",
                nomDestinataire
            );
        }
    }
    
    /**
     * Notifie la résolution d'un conflit
     */
    public void notifierResolutionConflit(int utilisateurId, String conflitType,
            String solution, String emailDestinataire, String nomDestinataire) {
        
        String message = "✅ CONFLIT RÉSOLU\nType: " + conflitType + "\nSolution: " + solution;
        ajouterNotification(utilisateurId, message);
        
        if (emailDestinataire != null && nomDestinataire != null) {
            emailService.envoyerResolutionConflit(
                emailDestinataire,
                nomDestinataire,
                conflitType,
                solution,
                "",
                "",
                "",
                ""
            );
        }
    }
    
    /**
     * Notifie un déplacement de cours conflictuel au gestionnaire
     */
    public void notifierDeplacementConflit(String emailGestionnaire, String professeurNom,
            String matiere, String typeCours, String ancienCreneau,
            String nouveauCreneau, String conflitDetails) {
        
        emailService.envoyerAlerteDeplacementConflit(
            emailGestionnaire,
            professeurNom,
            matiere,
            typeCours,
            ancienCreneau,
            nouveauCreneau,
            conflitDetails
        );
    }

    /**
     * Notifie la validation d'un emploi du temps.
     */
    public void notifierValidationEmploiDuTemps(int utilisateurId, String email,
            String nomComplet, String nomClasse, String titreEdt,
            String periode, byte[] pdfContent) {

        ajouterNotification(utilisateurId,
                "📅 L'emploi du temps «" + titreEdt + "» ("
                        + periode + ") pour la classe " + nomClasse + " a été validé.");

        new Thread(() -> {
            try {
                if (pdfContent != null) {
                    emailService.envoyerEmploiDuTemps(
                            email, nomComplet, nomClasse, periode, pdfContent);
                } else {
                    emailService.envoyerNotificationValidationEDT(
                            email, nomComplet, nomClasse, titreEdt, periode);
                }
            } catch (Exception e) {
                System.err.println("⚠️ Email non envoyé à " + email + " : " + e.getMessage());
            }
        }).start();
    }

    
    public void notifierNouvelleDemandeInscription(String nom, String prenom, String email) {
        ajouterNotification(1,
                "Nouvelle demande d'inscription de " + prenom + " " + nom + " (" + email + ")");
    }

    public void notifierNouvelleDemandeReinitMdp(int utilisateurId, String email) {
        ajouterNotification(1,
                "Demande de réinitialisation de mot de passe pour " + email);
    }

    public void notifierAnnulationCours(Cours cours, Creneau creneau, String motif) {
        String message = "Cours annulé : matière #" + cours.getMatiereId()
                + " le " + creneau.getJour() + " à " + creneau.getHeureDebut()
                + " - Motif : " + motif;
        ajouterNotification(cours.getEnseignantId(), message);
    }

    public void notifierChangementSalle(Cours cours, Creneau creneau,
            String ancienneSalle, String nouvelleSalle) {
        String message = "Changement de salle : cours #" + cours.getMatiereId()
                + " le " + creneau.getJour() + " à " + creneau.getHeureDebut()
                + " : " + ancienneSalle + " → " + nouvelleSalle;
        ajouterNotification(cours.getEnseignantId(), message);
    }

    public void notifierRappelReservation(Reservation reservation) {
        ajouterNotification(reservation.getUtilisateurId(),
                "⏰ Votre réservation salle #" + reservation.getSalleId()
                        + " se termine dans 5 min (" + reservation.getHeureFin() + ")");
    }

    public void notifierSignalementResolu(Signalement signalement) {
        ajouterNotification(signalement.getUtilisateurId(),
                "✅ Signalement #" + signalement.getId()
                        + " (salle #" + signalement.getSalleId() + ") résolu.");
    }

    public void notifierReservationValidee(Reservation reservation, Utilisateur enseignant) {
        ajouterNotification(reservation.getUtilisateurId(),
                "✅ Votre réservation du " + reservation.getDateReservation() + " a été validée.");
    }

    public void notifierReservationRefusee(Reservation reservation,
            Utilisateur enseignant, String motif) {
        ajouterNotification(reservation.getUtilisateurId(),
                "❌ Votre réservation du " + reservation.getDateReservation()
                        + " a été refusée. Motif : " + motif);
    }

    /**
     * Notifie un conflit de réservation au gestionnaire.
     */
    public void notifierConflitReservation(int gestionnaireId, Reservation reservation,
            String nomDemandeur, String salle, String details,
            EmailService emailService, String emailGestionnaire) {

        String message = "⚠️ Conflit réservation : " + nomDemandeur
                + " - Salle " + salle + " le " + reservation.getDateReservation()
                + " " + reservation.getHeureDebut() + "-" + reservation.getHeureFin()
                + " | " + details;
        ajouterNotification(gestionnaireId, message);

        if (emailService != null && emailGestionnaire != null) {
            new Thread(() -> emailService.envoyerConflitReservation(
                    emailGestionnaire, nomDemandeur, salle,
                    reservation.getDateReservation(),
                    reservation.getHeureDebut(), reservation.getHeureFin(), details)
            ).start();
        }
    }

    /**
     * Notifie un rappel de fin de réservation.
     */
    public void notifierRappelFinReservation(Reservation reservation,
            String nomUtilisateur, String salle,
            EmailService emailService, String emailUtilisateur) {

        ajouterNotification(reservation.getUtilisateurId(),
                "⏰ Votre réservation (Salle " + salle
                        + ") se termine dans 5 min (" + reservation.getHeureFin() + ")");

        if (emailService != null && emailUtilisateur != null) {
            new Thread(() -> emailService.envoyerRappelFinReservation(
                    emailUtilisateur, nomUtilisateur, salle, reservation.getHeureFin())
            ).start();
        }
    }

    /**
     * Notifie la résolution d'un conflit de réservation.
     */
    public void notifierResolutionConflit(int utilisateurId, Reservation reservation,
            String salle, String solution,
            EmailService emailService, String emailUtilisateur, String nomUtilisateur) {

        ajouterNotification(utilisateurId,
                "✅ Conflit résolu pour votre réservation (Salle " + salle
                        + " le " + reservation.getDateReservation() + ") : " + solution);

        if (emailService != null && emailUtilisateur != null) {
            new Thread(() -> emailService.envoyerResolutionConflit(
                    emailUtilisateur, nomUtilisateur, salle,
                    reservation.getDateReservation(),
                    reservation.getHeureDebut(), reservation.getHeureFin(), solution, nomUtilisateur)
            ).start();
        }
    }
}