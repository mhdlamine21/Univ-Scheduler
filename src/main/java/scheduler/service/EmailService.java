package scheduler.service;

import scheduler.modele.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service d'envoi d'emails avec mise en page professionnelle.
 */
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final String smtpHost = "smtp.gmail.com";
    private final String smtpPort = "587";
    private final String username;
    private final String password;
    private final boolean configOk;
    private final int maxRetries = 3;

    public EmailService() {
        this.username  = "mouhamedlniang@gmail.com";
        this.password  = "tukw ejvt kusx fnyd";
        this.configOk  = username != null && !username.isEmpty()
                      && password != null && !password.isEmpty();
    }

    /**
     * Envoie un email simple en texte.
     */
    public void envoyerEmail(String destinataire, String sujet, String contenuTexte) {
        envoyerEmailHTML(destinataire, sujet, contenuTexte, null);
    }

    public void envoyerEmailHTML(String destinataire, String sujet, String contenuHTML, byte[] pdfContent) {
        if (!configOk) {
            logger.warn("⚠️ Configuration email non initialisée - email non envoyé à {}", destinataire);
            return;
        }

        int tentative = 0;
        while (tentative < maxRetries) {
            tentative++;
            try {
                Session session = creerSession();
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(username));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
                message.setSubject(sujet);

                MimeBodyPart htmlPart = new MimeBodyPart();
                htmlPart.setContent(contenuHTML, "text/html; charset=UTF-8");

                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(htmlPart);

                if (pdfContent != null) {
                    MimeBodyPart pdfPart = new MimeBodyPart();
                    pdfPart.setFileName("emploi_du_temps.pdf");
                    pdfPart.setContent(pdfContent, "application/pdf");
                    multipart.addBodyPart(pdfPart);
                }

                message.setContent(multipart);
                Transport.send(message);
                logger.info("✅ Email envoyé à {}", destinataire);
                return;
            } catch (MessagingException e) {
                if (tentative >= maxRetries) {
                    logger.error("❌ Échec définitif après {} tentatives pour {}", maxRetries, destinataire, e);
                } else {
                    try { Thread.sleep(2000); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    public void envoyerEmailAvecPDF(String destinataire, String sujet,
            String contenuHTML, byte[] pdfContent, String nomFichierPDF) {
        envoyerEmailHTML(destinataire, sujet, contenuHTML, pdfContent);
    }

    private Session creerSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",                "true");
        props.put("mail.smtp.starttls.enable",     "true");
        props.put("mail.smtp.host",                smtpHost);
        props.put("mail.smtp.port",                smtpPort);
        props.put("mail.smtp.ssl.trust",           smtpHost);
        props.put("mail.smtp.connectiontimeout",   "10000");
        props.put("mail.smtp.timeout",             "10000");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    
    private String genererEnTete() {
        return "<div style=\"background-color: #1565C0; padding: 20px; text-align: center;\">" +
               "<h1 style=\"color: white; margin: 0; font-size: 28px;\">UNIV-SCHEDULER</h1>" +
               "<p style=\"color: #BBDEFB; margin: 5px 0 0 0; font-size: 12px;\">Gestion des emplois du temps</p>" +
               "</div>";
    }

    private String genererPiedPage() {
        return "<div style=\"background-color: #F5F7FA; padding: 15px; text-align: center; border-top: 1px solid #E2E8F0;\">" +
               "<p style=\"margin: 0 0 5px 0; font-size: 12px; color: #718096;\">" +
               "Université Iba Der Thiam de Thiès</p>" +
               "<p style=\"margin: 0; font-size: 11px; color: #A0AEC0;\">" +
               "© 2025 SCHEDULER - Tous droits réservés</p>" +
               "</div>";
    }

    private String genererLigneDetail(String label, String valeur) {
        return "<tr>" +
               "<td style=\"padding: 10px; border-bottom: 1px solid #E2E8F0; font-weight: bold; color: #4A5568; width: 120px;\">" + label + "</td>" +
               "<td style=\"padding: 10px; border-bottom: 1px solid #E2E8F0; color: #1A202C;\">" + valeur + "</td>" +
               "</tr>";
    }

    private String genererBouton(String texte, String lien) {
        return "<div style=\"text-align: center; margin: 20px 0;\">" +
               "<a href=\"" + lien + "\" style=\"background-color: #1565C0; color: white; padding: 10px 25px; " +
               "text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block;\">" +
               texte + "</a>" +
               "</div>";
    }

    /**
     * Envoie un email de validation d'inscription avec mot de passe généré.
     */
    public void envoyerValidationInscription(String email, String nom, String prenom, String motDePasse) {
        String sujet = "✅ Votre compte UNIV-SCHEDULER a été validé";
        String html = genererEnTete() +
            "<div style=\"padding: 30px; font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">" +
            "<p style=\"font-size: 16px; color: #1A202C;\">Bonjour <strong>" + prenom + " " + nom + "</strong>,</p>" +
            "<p style=\"color: #4A5568;\">Votre demande de création de compte a été validée.</p>" +
            "<table style=\"width: 100%; border-collapse: collapse; margin: 20px 0; background-color: #F7FAFC; border-radius: 8px;\">" +
            genererLigneDetail("📧 Email", email) +
            genererLigneDetail("🔑 Mot de passe", motDePasse) +
            "</table>" +
            "<p style=\"color: #E65100; font-size: 14px;\">⚠️ Pour des raisons de sécurité, veuillez changer votre mot de passe dès votre première connexion.</p>" +
            genererBouton("Se connecter", "#") +
            "<p style=\"color: #718096; font-size: 14px;\">Cordialement,<br><strong>UNIV-SCHEDULER</strong></p>" +
            "</div>" +
            genererPiedPage();
        envoyerEmailHTML(email, sujet, html, null);
    }

    public void envoyerEmailRefus(String email, String nom, String prenom, String motif) {
        String sujet = "❌ Votre demande d'inscription à SCHEDULER";
        String html = genererEnTete() +
            "<div style=\"padding: 30px; font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">" +
            "<p style=\"font-size: 16px; color: #1A202C;\">Bonjour <strong>" + prenom + " " + nom + "</strong>,</p>" +
            "<p style=\"color: #4A5568;\">Votre demande de création de compte n'a pas pu être validée.</p>" +
            "<p style=\"color: #C62828; font-weight: bold;\">Motif du refus :</p>" +
            "<p style=\"color: #4A5568; background-color: #FFEBEE; padding: 10px; border-radius: 5px;\">" + motif + "</p>" +
            "<p style=\"color: #718096; font-size: 14px;\">Si vous pensez qu'il s'agit d'une erreur, veuillez contacter l'administration.</p>" +
            "<p style=\"color: #718096; font-size: 14px;\">Cordialement,<br><strong>UNIV-SCHEDULER</strong></p>" +
            "</div>" +
            genererPiedPage();
        envoyerEmailHTML(email, sujet, html, null);
    }

    /**
     * Envoie un email avec le nouveau mot de passe généré automatiquement
     */
    public void envoyerNouveauMotDePasse(String email, String nom, String prenom, String nouveauMotDePasse) {
        String sujet = "🔐 Réinitialisation de votre mot de passe - SCHEDULER";
        String html = genererEnTete() +
            "<div style=\"padding: 30px; font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">" +
            "<p style=\"font-size: 16px; color: #1A202C;\">Bonjour <strong>" + prenom + " " + nom + "</strong>,</p>" +
            "<p style=\"color: #4A5568;\">Vous avez demandé la réinitialisation de votre mot de passe.</p>" +
            "<table style=\"width: 100%; border-collapse: collapse; margin: 20px 0; background-color: #F7FAFC; border-radius: 8px;\">" +
            genererLigneDetail("🔑 NOUVEAU MOT DE PASSE", nouveauMotDePasse) +
            "<td>" +
            "<p style=\"color: #E65100; font-size: 14px;\">⚠️ Pour des raisons de sécurité, veuillez changer ce mot de passe dès votre prochaine connexion.</p>" +
            genererBouton("Se connecter", "#") +
            "<p style=\"color: #718096; font-size: 14px;\">Cordialement,<br><strong>UNIV-SCHEDULER</strong></p>" +
            "</div>" +
            genererPiedPage();
        
        envoyerEmailHTML(email, sujet, html, null);
    }

    /**
     * Envoie un emploi du temps en PDF par email.
     */
    public void envoyerEmploiDuTemps(String email, String nom, String classe, String periode, byte[] pdfContent) {
        String sujet = "📅 Emploi du temps - " + classe;
        String html = genererEnTete() +
            "<div style=\"padding: 30px; font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">" +
            "<p style=\"font-size: 16px; color: #1A202C;\">Bonjour <strong>" + nom + "</strong>,</p>" +
            "<p style=\"color: #4A5568;\">Veuillez trouver ci-joint l'emploi du temps pour la classe <strong>" + classe + "</strong>.</p>" +
            "<table style=\"width: 100%; border-collapse: collapse; margin: 20px 0; background-color: #F7FAFC; border-radius: 8px;\">" +
            genererLigneDetail("📚 Classe", classe) +
            genererLigneDetail("📅 Période", periode) +
            "</table>" +
            "<p style=\"color: #4A5568;\">Consultez le PDF joint pour tous les détails des créneaux.</p>" +
            "<p style=\"color: #718096; font-size: 14px;\">Cordialement,<br><strong>UNIV-SCHEDULER</strong></p>" +
            "</div>" +
            genererPiedPage();
        
        String nomFichier = "emploi_du_temps_" + classe.replaceAll("[^a-zA-Z0-9]", "_") + ".pdf";
        envoyerEmailAvecPDF(email, sujet, html, pdfContent, nomFichier);
    }

    /**
     * Envoie une notification de validation d'emploi du temps.
     */
    public void envoyerNotificationValidationEDT(String email, String nom, String titre, 
            String classe, String periode) {
        String sujet = "📅 Nouvel emploi du temps validé - " + classe;
        String html = genererEnTete() +
            "<div style=\"padding: 30px; font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">" +
            "<p style=\"font-size: 16px; color: #1A202C;\">Bonjour <strong>" + nom + "</strong>,</p>" +
            "<p style=\"color: #4A5568;\">Un nouvel emploi du temps a été validé et vous concerne.</p>" +
            "<table style=\"width: 100%; border-collapse: collapse; margin: 20px 0; background-color: #F7FAFC; border-radius: 8px;\">" +
            genererLigneDetail("📚 Titre", titre) +
            genererLigneDetail("🏫 Classe", classe) +
            genererLigneDetail("📅 Période", periode) +
            "</table>" +
            genererBouton("Voir mon emploi du temps", "#") +
            "<p style=\"color: #718096; font-size: 14px;\">Connectez-vous à l'application pour consulter votre emploi du temps.</p>" +
            "<p style=\"color: #718096; font-size: 14px; margin-top: 20px;\">Cordialement,<br><strong>UNIV-SCHEDULER</strong></p>" +
            "</div>" +
            genererPiedPage();
        envoyerEmailHTML(email, sujet, html, null);
    }

    
    public void envoyerAnnulationCours(String email, String nomDestinataire,
            String matiere, String typeCours, String date,
            String heureDebut, String motif) {
        String sujet = "❌ Annulation de cours - " + matiere;
        String html = genererEnTete() +
            "<div style=\"padding: 30px; font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">" +
            "<p style=\"font-size: 16px; color: #1A202C;\">Bonjour <strong>" + nomDestinataire + "</strong>,</p>" +
            "<p style=\"color: #4A5568;\">Le cours suivant a été annulé.</p>" +
            "<table style=\"width: 100%; border-collapse: collapse; margin: 20px 0; background-color: #F7FAFC; border-radius: 8px;\">" +
            genererLigneDetail("📚 Matière", matiere + " (" + typeCours + ")") +
            genererLigneDetail("📅 Date", formaterDateLongue(date)) +
            genererLigneDetail("⏰ Heure", heureDebut) +
            genererLigneDetail("📝 Motif", motif) +
            "</table>" +
            "<p style=\"color: #718096; font-size: 14px;\">Cordialement,<br><strong>UNIV-SCHEDULER</strong></p>" +
            "</div>" +
            genererPiedPage();
        envoyerEmailHTML(email, sujet, html, null);
    }

    /**
     * Envoie une notification de déplacement de cours.
     */
    public void envoyerDeplacementCours(String email, String nomDestinataire,
            String matiere, String typeCours,
            String ancienneDate, String ancienneHeure, String ancienneSalle,
            String nouvelleDate, String nouvelleHeure, String nouvelleSalle) {
        String sujet = "🔄 Déplacement de cours - " + matiere;
        String html = genererEnTete() +
            "<div style=\"padding: 30px; font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">" +
            "<p style=\"font-size: 16px; color: #1A202C;\">Bonjour <strong>" + nomDestinataire + "</strong>,</p>" +
            "<p style=\"color: #4A5568;\">Le cours suivant a été déplacé.</p>" +
            "<h3 style=\"color: #C62828; margin: 20px 0 10px 0;\">❌ ANCIEN CRÉNEAU</h3>" +
            "<table style=\"width: 100%; border-collapse: collapse; margin: 0 0 20px 0; background-color: #FFF5F5; border-radius: 8px;\">" +
            genererLigneDetail("📅 Date", formaterDateLongue(ancienneDate)) +
            genererLigneDetail("⏰ Heure", ancienneHeure) +
            genererLigneDetail("🏫 Salle", ancienneSalle) +
            "</table>" +
            "<h3 style=\"color: #2E7D32; margin: 20px 0 10px 0;\">✅ NOUVEAU CRÉNEAU</h3>" +
            "<table style=\"width: 100%; border-collapse: collapse; margin: 0 0 20px 0; background-color: #E8F5E9; border-radius: 8px;\">" +
            genererLigneDetail("📅 Date", formaterDateLongue(nouvelleDate)) +
            genererLigneDetail("⏰ Heure", nouvelleHeure) +
            genererLigneDetail("🏫 Salle", nouvelleSalle) +
            "</table>" +
            "<p style=\"color: #718096; font-size: 14px;\">Cordialement,<br><strong>UNIV-SCHEDULER</strong></p>" +
            "</div>" +
            genererPiedPage();
        envoyerEmailHTML(email, sujet, html, null);
    }

    /**
     * Envoie un rappel de fin de réservation.
     */
    public void envoyerRappelFinReservation(String email, String nom, String salle, String heureFin) {
        String sujet = "⏰ Fin de réservation dans 5 min - " + salle;
        String html = genererEnTete() +
            "<div style=\"padding: 30px; font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">" +
            "<p style=\"font-size: 16px; color: #1A202C;\">Bonjour <strong>" + nom + "</strong>,</p>" +
            "<p style=\"color: #E65100; font-size: 14px;\">Votre réservation de la salle <strong>" + salle + "</strong> se termine à <strong>" + heureFin + "</strong> (dans 5 minutes).</p>" +
            "<p style=\"color: #4A5568;\">Si vous avez besoin de plus de temps et que la salle est libre, vous pouvez prolonger votre réservation depuis l'application.</p>" +
            "<p style=\"color: #718096; font-size: 14px; margin-top: 20px;\">Cordialement,<br><strong>UNIV-SCHEDULER</strong></p>" +
            "</div>" +
            genererPiedPage();
        envoyerEmailHTML(email, sujet, html, null);
    }


    /**
     * Envoie une notification de réservation pour une classe.
     */
    public void envoyerReservationClasse(String email, String nomEtudiant,
            String typeReservation, String classeNom, String salle,
            String date, String heureDebut, String heureFin,
            String enseignantNom, String motif) {
        String sujet = "📚 Nouveau " + typeReservation + " pour votre classe - " + classeNom;
        String html = genererEnTete() +
            "<div style=\"padding: 30px; font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">" +
            "<p style=\"font-size: 16px; color: #1A202C;\">Bonjour <strong>" + nomEtudiant + "</strong>,</p>" +
            "<p style=\"color: #4A5568;\">Un " + typeReservation.toLowerCase() + " a été programmé pour votre classe.</p>" +
            "<table style=\"width: 100%; border-collapse: collapse; margin: 20px 0; background-color: #F7FAFC; border-radius: 8px;\">" +
            genererLigneDetail("🏫 Classe", classeNom) +
            genererLigneDetail("🏛️ Salle", salle) +
            genererLigneDetail("📅 Date", formaterDateLongue(date)) +
            genererLigneDetail("⏰ Horaire", heureDebut + " - " + heureFin) +
            genererLigneDetail("👨‍🏫 Enseignant", enseignantNom) +
            genererLigneDetail("📝 Motif", motif) +
            "</table>" +
            "<p style=\"color: #718096; font-size: 14px;\">Merci de votre attention.</p>" +
            "<p style=\"color: #718096; font-size: 14px; margin-top: 20px;\">Cordialement,<br><strong>UNIV-SCHEDULER</strong></p>" +
            "</div>" +
            genererPiedPage();
        envoyerEmailHTML(email, sujet, html, null);
    }

    /**
     * Envoie une alerte de conflit au gestionnaire.
     */
    public void envoyerConflitGestionnaire(String emailGestionnaire, String typeConflit,
            String details, String salle, String date,
            String heureDebut, String heureFin, String utilisateurNom) {
        
        String sujet = "⚠️ CONFLIT DÉTECTÉ - " + typeConflit;
        String html = genererEnTete() +
            "<div style=\"padding: 30px; font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">" +
            "<p style=\"font-size: 16px; color: #1A202C;\">Bonjour,</p>" +
            "<p style=\"color: #C62828;\">Un conflit a été détecté dans l'application.</p>" +
            "<table style=\"width: 100%; border-collapse: collapse; margin: 20px 0; background-color: #F7FAFC; border-radius: 8px;\">" +
            genererLigneDetail("⚠️ Type", typeConflit) +
            genererLigneDetail("🏛️ Salle", salle) +
            genererLigneDetail("📅 Date", formaterDateLongue(date)) +
            genererLigneDetail("⏰ Horaire", heureDebut + " - " + heureFin) +
            genererLigneDetail("👤 Utilisateur", utilisateurNom) +
            genererLigneDetail("📝 Détail", details) +
            "</table>" +
            "<p style=\"color: #4A5568;\">Veuillez vous connecter à l'application pour résoudre ce conflit.</p>" +
            "<p style=\"color: #718096; font-size: 14px; margin-top: 20px;\">Cordialement,<br><strong>UNIV-SCHEDULER</strong></p>" +
            "</div>" +
            genererPiedPage();
        
        envoyerEmailHTML(emailGestionnaire, sujet, html, null);
    }


    public void envoyerResolutionConflit(String email, String nom,
            String typeConflit, String solution, String salle,
            String date, String heureDebut, String heureFin) {
        
        String sujet = "✅ Conflit résolu - " + typeConflit;
        String html = genererEnTete() +
            "<div style=\"padding: 30px; font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">" +
            "<p style=\"font-size: 16px; color: #1A202C;\">Bonjour <strong>" + nom + "</strong>,</p>" +
            "<p style=\"color: #2E7D32;\">Le conflit concernant votre demande a été résolu.</p>" +
            "<table style=\"width: 100%; border-collapse: collapse; margin: 20px 0; background-color: #F7FAFC; border-radius: 8px;\">" +
            genererLigneDetail("🔧 Solution", solution) +
            genererLigneDetail("🏛️ Salle", salle) +
            genererLigneDetail("📅 Date", formaterDateLongue(date)) +
            genererLigneDetail("⏰ Horaire", heureDebut + " - " + heureFin) +
            "</table>" +
            "<p style=\"color: #718096; font-size: 14px;\">Cordialement,<br><strong>UNIV-SCHEDULER</strong></p>" +
            "</div>" +
            genererPiedPage();
        
        envoyerEmailHTML(email, sujet, html, null);
    }
    
    


    public void envoyerAlerteDeplacementConflit(String emailGestionnaire,
            String professeurNom, String matiere, String typeCours,
            String ancienCreneau, String nouveauCreneau, String conflitDetails) {
        String sujet = "⚠️ ALERTE - Déplacement de cours conflictuel";
        String html = genererEnTete() +
            "<div style=\"padding: 30px; font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">" +
            "<p style=\"font-size: 16px; color: #1A202C;\">Bonjour,</p>" +
            "<p style=\"color: #C62828;\">Le professeur <strong>" + professeurNom + "</strong> a déplacé un cours, ce qui a créé un conflit.</p>" +
            "<table style=\"width: 100%; border-collapse: collapse; margin: 20px 0; background-color: #F7FAFC; border-radius: 8px;\">" +
            genererLigneDetail("📚 Matière", matiere + " (" + typeCours + ")") +
            genererLigneDetail("❌ Ancien", ancienCreneau) +
            genererLigneDetail("✅ Nouveau", nouveauCreneau) +
            genererLigneDetail("⚠️ Conflit", conflitDetails) +
            "</table>" +
            "<p style=\"color: #4A5568;\">Veuillez vous connecter à l'application pour résoudre ce conflit.</p>" +
            "<p style=\"color: #718096; font-size: 14px;\">Cordialement,<br><strong>UNIV-SCHEDULER</strong></p>" +
            "</div>" +
            genererPiedPage();
        envoyerEmailHTML(emailGestionnaire, sujet, html, null);
    }
    
    

    public void envoyerConflitReservation(String emailGestionnaire,
            String nomDemandeur, String salle, String date,
            String heureDebut, String heureFin, String details) {
        String sujet = "⚠️ Conflit de réservation - " + salle;
        String html = genererEnTete() +
            "<div style=\"padding: 30px; font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">" +
            "<p style=\"font-size: 16px; color: #1A202C;\">Bonjour,</p>" +
            "<p style=\"color: #C62828;\">Un conflit a été détecté lors d'une demande de réservation.</p>" +
            "<table style=\"width: 100%; border-collapse: collapse; margin: 20px 0; background-color: #F7FAFC; border-radius: 8px;\">" +
            genererLigneDetail("👤 Demandeur", nomDemandeur) +
            genererLigneDetail("🏫 Salle", salle) +
            genererLigneDetail("📅 Date", formaterDateLongue(date)) +
            genererLigneDetail("⏰ Créneau", heureDebut + " → " + heureFin) +
            genererLigneDetail("⚠️ Conflit", details) +
            "</table>" +
            "<p style=\"color: #4A5568;\">Veuillez résoudre ce conflit dans l'application SCHEDULER.</p>" +
            "<p style=\"color: #718096; font-size: 14px;\">Cordialement,<br><strong>UNIV-SCHEDULER</strong></p>" +
            "</div>" +
            genererPiedPage();
        envoyerEmailHTML(emailGestionnaire, sujet, html, null);
    }

    
    private String formaterDateLongue(String dateStr) {
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
            java.time.format.DateTimeFormatter formatter = 
                java.time.format.DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", java.util.Locale.FRENCH);
            return date.format(formatter);
        } catch (Exception e) {
            return dateStr;
        }
    }
}