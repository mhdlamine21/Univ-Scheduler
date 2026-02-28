package scheduler.util;

/**
 * Classe de constantes pour l'application UNIV-SCHEDULER.
 * Centralise toutes les valeurs constantes utilisées dans l'application.
 */
public final class Constantes {
    
    private Constantes() {
    }
    
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_GESTIONNAIRE = "gestionnaire";
    public static final String ROLE_ENSEIGNANT = "enseignant";
    public static final String ROLE_ETUDIANT = "etudiant";
    
    public static final String TYPE_ETUDIANT_NORMAL = "normal";
    public static final String TYPE_ETUDIANT_RESPONSABLE = "responsable";
    
    public static final String STATUT_DISPONIBLE = "disponible";
    public static final String STATUT_INDISPONIBLE = "indisponible";
    
    public static final String STATUT_DEMANDE_EN_ATTENTE = "en_attente";
    public static final String STATUT_DEMANDE_VALIDEE = "validee";
    public static final String STATUT_DEMANDE_REFUSEE = "refusee";
    
    public static final String STATUT_SIGNALEMENT_EN_ATTENTE = "en_attente";
    public static final String STATUT_SIGNALEMENT_EN_COURS = "en_cours";
    public static final String STATUT_SIGNALEMENT_RESOLU = "resolu";
    
    public static final String STATUT_RESERVATION_CONFIRMEE = "confirmee";
    public static final String STATUT_RESERVATION_EN_COURS = "en_cours";
    public static final String STATUT_RESERVATION_TERMINEE = "terminee";
    public static final String STATUT_RESERVATION_ANNULEE = "annulee";
    
    public static final String STATUT_CRENEAU_PLANIFIE = "planifie";
    public static final String STATUT_CRENEAU_ANNULE = "annule";
    public static final String STATUT_CRENEAU_DEPLACE = "deplace";
    
    public static final String TYPE_COURS_CM = "CM";
    public static final String TYPE_COURS_TD = "TD";
    public static final String TYPE_COURS_TP = "TP";
    
    public static final String TYPE_SALLE_TD = "TD";
    public static final String TYPE_SALLE_TP = "TP";
    public static final String TYPE_SALLE_AMPHI = "Amphi";
    public static final String TYPE_SALLE_AUTRE = "Autre";
    
    public static final String PERIODE_HEBDOMADAIRE = "hebdomadaire";
    public static final String PERIODE_MENSUEL = "mensuel";
    public static final String PERIODE_SEMESTRIEL = "semestriel";
    
    public static final int CAPACITE_SALLE_DEFAUT = 30;
    public static final int DUREE_COURS_DEFAUT = 90;
    public static final int NB_ETAGES_DEFAUT = 1;
    public static final int NB_GROUPES_DEFAUT = 1;
    public static final int EFFECTIF_CLASSE_DEFAUT = 30;
    
    public static final String ERREUR_CHAMPS_OBLIGATOIRES = "Veuillez remplir tous les champs obligatoires";
    public static final String ERREUR_EMAIL_INVALIDE = "Format d'email invalide";
    public static final String ERREUR_MOT_DE_PASSE_INCORRECT = "Mot de passe incorrect";
    public static final String ERREUR_UTILISATEUR_NON_TROUVE = "Utilisateur non trouvé";
    public static final String ERREUR_CONNEXION_BD = "Erreur de connexion à la base de données";
    public static final String ERREUR_SUPPRESSION_IMPOSSIBLE = "Suppression impossible car des éléments sont liés";
    
    public static final String FORMAT_DATE = "dd/MM/yyyy";
    public static final String FORMAT_DATE_BD = "yyyy-MM-dd";
    public static final String FORMAT_HEURE = "HH:mm";
    public static final String FORMAT_DATE_HEURE = "dd/MM/yyyy HH:mm";
    
    public static final String FXML_LOGIN = "/fxml/Login.fxml";
    public static final String FXML_INSCRIPTION = "/fxml/Inscription.fxml";
    public static final String FXML_TABLEAU_BORD_ADMIN = "/fxml/TableauBordAdmin.fxml";
    public static final String FXML_TABLEAU_BORD_GESTIONNAIRE = "/fxml/TableauBordGestionnaire.fxml";
    public static final String FXML_TABLEAU_BORD_ENSEIGNANT = "/fxml/TableauBordEnseignant.fxml";
    public static final String FXML_TABLEAU_BORD_ETUDIANT = "/fxml/TableauBordEtudiant.fxml";
    public static final String FXML_PROFIL = "/fxml/Profil.fxml";
    public static final String FXML_CHANGER_MOT_DE_PASSE = "/fxml/ChangerMotDePasse.fxml";
    
    public static final String CSS_STYLE = "/css/style.css";
    
    public static final int NB_MAX_TENTATIVES_LOGIN = 3;
    public static final int LONGUEUR_MIN_MOT_DE_PASSE = 8;
    public static final int DELAI_BLOCAGE_MINUTES = 5;
    
    public static final int RAPPEL_FIN_RESERVATION_MINUTES = 5;
}