package scheduler.service;

import scheduler.dao.*;
import scheduler.modele.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

/**
* Service métier pour la gestion des emplois du temps.
* Inclut la validation avec notification email aux étudiants et enseignants.
*/
public class EmploiDuTempsService {
    private static final Logger logger = LoggerFactory.getLogger(EmploiDuTempsService.class);

    private EmploiDuTempsDAO     edtDAO;
    private EmploiDuTempsCreneauDAO edtCreneauDAO;
    private CreneauDAO           creneauDAO;
    private CoursDAO             coursDAO;
    private UtilisateurDAO       utilisateurDAO;
    private ClasseDAO            classeDAO;
    private NotificationService  notificationService;
    private EmailService         emailService;

    public EmploiDuTempsService() {
        this.edtDAO             = new EmploiDuTempsDAO();
        this.edtCreneauDAO      = new EmploiDuTempsCreneauDAO();
        this.creneauDAO         = new CreneauDAO();
        this.coursDAO           = new CoursDAO();
        this.utilisateurDAO     = new UtilisateurDAO();
        this.classeDAO          = new ClasseDAO();
        this.notificationService = new NotificationService();
        this.emailService        = new EmailService();
    }

    
    public void creerEmploiDuTemps(EmploiDuTemps edt, List<Integer> creneauIds) throws SQLException {
        edt.mettreAJourStatut();
        edtDAO.ajouter(edt);
        for (int creneauId : creneauIds) {
            edtCreneauDAO.ajouterCreneau(edt.getId(), creneauId);
        }
    }

    public EmploiDuTemps trouverParId(int id) throws SQLException {
        EmploiDuTemps edt = edtDAO.trouverParId(id);
        if (edt != null) edt.mettreAJourStatut();
        return edt;
    }

    public List<EmploiDuTemps> listerParClasse(int classeId) throws SQLException {
        List<EmploiDuTemps> liste = edtDAO.listerParClasse(classeId);
        liste.forEach(EmploiDuTemps::mettreAJourStatut);
        return liste;
    }

    public List<EmploiDuTemps> listerTous() throws SQLException {
        List<EmploiDuTemps> liste = edtDAO.listerTous();
        liste.forEach(EmploiDuTemps::mettreAJourStatut);
        return liste;
    }

    public List<EmploiDuTemps> listerActifs() throws SQLException {
        return edtDAO.listerActifs();
    }

    public void supprimerEmploiDuTemps(int id) throws SQLException {
        edtCreneauDAO.supprimerTousCreneaux(id);
        edtDAO.supprimer(id);
    }

    public void mettreAJourStatutsExpires() throws SQLException {
        for (EmploiDuTemps edt : listerTous()) {
            if (!"termine".equals(edt.getStatut()) && edt.estTermine()) {
                edtDAO.marquerTermine(edt.getId());
            }
        }
    }

    
    /**
     * Valide un emploi du temps et envoie les notifications.
     * @param id identifiant de l'emploi du temps
     * @param pdfContent PDF à joindre (peut être null)
     * @param titre titre de l'EDT
     * @param envoyerEtudiants envoyer aux étudiants ?
     * @param envoyerProfs envoyer aux enseignants ?
     */
    public void validerEmploiDuTemps(int id, byte[] pdfContent,
            String titre, boolean envoyerEtudiants, boolean envoyerProfs) throws SQLException {

        edtDAO.valider(id);

        EmploiDuTemps edt = edtDAO.trouverParId(id);
        if (edt == null) {
            logger.warn("EDT #{} non trouvé", id);
            return;
        }

        String periode = edt.getPeriodeType() + " du " + edt.getPeriodeDebut() + " au " + edt.getPeriodeFin();

        Classe classe = classeDAO.trouverParId(edt.getClasseId());
        if (classe == null) {
            logger.warn("Classe #{} non trouvée pour EDT #{}", edt.getClasseId(), id);
            return;
        }
        String nomClasse = classe.getIntitule();

        List<Integer> creneauIds = edtCreneauDAO.listerCreneauxParEdt(id);
        Set<Integer> enseignantIds = new HashSet<>();
        for (int creneauId : creneauIds) {
            Creneau creneau = creneauDAO.trouverParId(creneauId);
            if (creneau == null) continue;
            Cours cours = coursDAO.trouverParId(creneau.getCoursId());
            if (cours != null) enseignantIds.add(cours.getEnseignantId());
        }

        if (envoyerEtudiants) {
            List<Etudiant> etudiants = new ArrayList<>();
            List<Utilisateur> tousUtilisateurs = utilisateurDAO.listerTous();
            for (Utilisateur u : tousUtilisateurs) {
                if (u instanceof Etudiant && u.isEstValide()) {
                    Etudiant e = (Etudiant) u;
                    if (e.getClasseId() == edt.getClasseId()) {
                        etudiants.add(e);
                    }
                }
            }
            
            logger.info("📧 Envoi à {} étudiants", etudiants.size());
            
            for (Etudiant e : etudiants) {
                String nomComplet = e.getPrenom() + " " + e.getNom();
                
                notificationService.ajouterNotification(e.getId(),
                        "📅 L'emploi du temps «" + titre + "» (" + periode + ") pour votre classe a été validé.");

                try {
                    emailService.envoyerNotificationValidationEDT(
                            e.getEmail(), nomComplet, titre, nomClasse, periode);
                    logger.info("✅ Email envoyé à étudiant {}", e.getEmail());
                } catch (Exception ex) {
                    logger.warn("Impossible d'envoyer l'email à l'étudiant {}: {}", e.getEmail(), ex.getMessage());
                }
            }
        }

        if (envoyerProfs) {
            logger.info("📧 Envoi à {} enseignants", enseignantIds.size());
            
            for (int ensId : enseignantIds) {
                Utilisateur u = utilisateurDAO.trouverParId(ensId);
                if (u == null || !u.isEstValide()) continue;

                String nomComplet = u.getPrenom() + " " + u.getNom();

                notificationService.ajouterNotification(u.getId(),
                        "📅 L'emploi du temps «" + titre + "» (" + periode + ") pour la classe " + nomClasse + " a été validé.");

                // Email
                try {
                    emailService.envoyerNotificationValidationEDT(
                            u.getEmail(), nomComplet, titre, nomClasse, periode);
                    logger.info("✅ Email envoyé à enseignant {}", u.getEmail());
                } catch (Exception ex) {
                    logger.warn("Impossible d'envoyer l'email à l'enseignant {}: {}", u.getEmail(), ex.getMessage());
                }
            }
        }

        logger.info("✅ EDT #{} validé - {} étudiants et {} enseignants notifiés.",
                id, envoyerEtudiants ? "les" : "0", envoyerProfs ? enseignantIds.size() : 0);
    }
    

    /**
     * Met à jour le statut d'un EDT spécifique.
     */
    public void mettreAJourStatut(int edtId) throws SQLException {
        EmploiDuTemps edt = edtDAO.trouverParId(edtId);
        if (edt == null) return;
        
        String ancienStatut = edt.getStatut();
        edt.mettreAJourStatut();
        
        if (!ancienStatut.equals(edt.getStatut())) {
            edtDAO.marquerTermine(edtId);
            logger.info("EDT #{} changé de statut: {} → {}", edtId, ancienStatut, edt.getStatut());
        }
    }
    
    

    /**
     * Met à jour les statuts de tous les EDT (à appeler au démarrage).
     */
    public void mettreAJourTousStatuts() throws SQLException {
        List<EmploiDuTemps> tous = listerTous();
        for (EmploiDuTemps edt : tous) {
            String ancienStatut = edt.getStatut();
            edt.mettreAJourStatut();
            if (!ancienStatut.equals(edt.getStatut())) {
                if ("termine".equals(edt.getStatut())) {
                    edtDAO.marquerTermine(edt.getId());
                }
            }
        }
        logger.info("✅ Mise à jour des statuts EDT terminée. {} EDT traités.", tous.size());
    }

    /**
     * Valide un EDT et envoie automatiquement les notifications.
     */
    public void validerEmploiDuTemps(int id) throws SQLException {
        validerEmploiDuTemps(id, null, "Emploi du temps", true, true);
    }
}