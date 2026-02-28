package scheduler.util;

import scheduler.modele.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import java.util.List;

/**
 * Utilitaire de construction et d'affichage des emplois du temps.
 * Fournit des méthodes statiques pour générer des grilles EDT
 * au style professionnel (type grandes universités françaises).
 *
 * <p>Chaque cellule de cours affiche :</p>
 * <ul>
 *   <li>Un bandeau coloré selon le type (CM=bleu, TD=vert, TP=orange, EXAMEN=rouge)</li>
 *   <li>Le nom de la matière en gras</li>
 *   <li>Le grade et nom abrégé de l'enseignant (ex. "Dr. Ndiaye")</li>
 *   <li>La salle</li>
 *   <li>La classe concernée (pour l'EDT enseignant)</li>
 * </ul>
 *
 * <p>Une légende identique est affichée sur tous les EDT.</p>
 */
public final class EdtAffichageUtil {

    /** Jours affichés dans la grille (Lundi → Samedi). */
    public static final String[] JOURS = {"Lundi","Mardi","Mercredi","Jeudi","Vendredi","Samedi"};

    /** Créneaux horaires (une ligne = 1h). */
    public static final String[] HEURES = {
        "07:30","08:30","09:30","10:30","11:30",
        "12:30","13:30","14:30","15:30","16:30","17:30","18:30"
    };

    private EdtAffichageUtil() {}

    //  CONSTRUCTION DE LA GRILLE

    /**
     * Construit et remplit la grille GridPane avec les créneaux fournis.
     *
     * @param grid         GridPane cible (FXML ou créé dynamiquement)
     * @param creneaux     Liste des créneaux à afficher
     * @param coursCache   Cache cours (id → Cours)
     * @param matiereCache Cache matières (id → Matiere)
     * @param salleCache   Cache salles (id → Salle)
     * @param ensCache     Cache enseignants (id → Enseignant)
     * @param classeCache  Cache classes (id → Classe)
     * @param modeEnseignant true = afficher la classe, false = afficher le prof
     */
    public static void construireGrille(
            GridPane grid,
            List<Creneau> creneaux,
            java.util.Map<Integer,Cours>     coursCache,
            java.util.Map<Integer,Matiere>   matiereCache,
            java.util.Map<Integer,Salle>     salleCache,
            java.util.Map<Integer,Enseignant> ensCache,
            java.util.Map<Integer,Classe>    classeCache,
            boolean modeEnseignant) {

        grid.getChildren().clear();
        grid.getColumnConstraints().clear();
        grid.getRowConstraints().clear();
        grid.setHgap(1);
        grid.setVgap(1);
        grid.setStyle("-fx-background-color:#E8EBFF;");

        // Colonnes
        // Col 0 : horaires
        ColumnConstraints colHeure = new ColumnConstraints(72, 72, 72);
        grid.getColumnConstraints().add(colHeure);
        for (int j = 0; j < JOURS.length; j++) {
            ColumnConstraints cc = new ColumnConstraints(140, 170, Double.MAX_VALUE);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }

        // Lignes
        // Ligne 0 : en-têtes jours
        RowConstraints rowHeader = new RowConstraints(36, 36, 36);
        grid.getRowConstraints().add(rowHeader);
        for (int i = 0; i < HEURES.length; i++) {
            RowConstraints rc = new RowConstraints(85, 90, 90);
            grid.getRowConstraints().add(rc);
        }

        // En-tête de coin pour les horaires
        Label coin = new Label("Horaire");
        coin.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        coin.setAlignment(Pos.CENTER);
        coin.setStyle("-fx-background-color:#1A1F3A;-fx-text-fill:white;"
            + "-fx-font-weight:800;-fx-font-size:11px;-fx-padding:4;");
        grid.add(coin, 0, 0);

        // En-têtes des jours de cours
        for (int j = 0; j < JOURS.length; j++) {
            Label h = new Label(JOURS[j]);
            h.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            h.setAlignment(Pos.CENTER);
            h.setStyle("-fx-background-color:#1A1F3A;-fx-text-fill:white;"
                + "-fx-font-weight:800;-fx-font-size:12px;-fx-padding:4;");
            grid.add(h, j + 1, 0);
        }

        // En-têtes horaires et initialisation des cellules vides
        for (int i = 0; i < HEURES.length; i++) {
            String hDebut = HEURES[i];
            String hFin   = i < HEURES.length - 1 ? HEURES[i + 1] : "19:30";
            Label hl = new Label(hDebut + "\n" + hFin);
            hl.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            hl.setAlignment(Pos.CENTER);
            hl.setStyle("-fx-background-color:#EEF0FF;-fx-padding:4;"
                + "-fx-alignment:center;-fx-font-weight:700;-fx-font-size:11px;"
                + "-fx-border-color:#B8C4FF;-fx-border-width:0 1 1 0;");
            grid.add(hl, 0, i + 1);

            for (int j = 0; j < JOURS.length; j++) {
                Pane vide = new Pane();
                vide.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                vide.setStyle("-fx-background-color:white;"
                    + "-fx-border-color:#E8EBFF;-fx-border-width:0 1 1 0;");
                grid.add(vide, j + 1, i + 1);
            }
        }

        // Remplissage des créneaux
        if (creneaux == null || creneaux.isEmpty()) {
            Label vide = new Label("Aucun cours programmé sur cette période");
            vide.setStyle("-fx-font-size:14px;-fx-text-fill:#A0AABE;");
            vide.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            vide.setAlignment(Pos.CENTER);
            GridPane.setColumnSpan(vide, JOURS.length);
            GridPane.setRowSpan(vide, HEURES.length);
            grid.add(vide, 1, 1, JOURS.length, HEURES.length);
            return;
        }

        for (Creneau c : creneaux) {
            try {
                int col = resolveColonne(c.getJour());
                if (col < 0) continue;

                int row     = resolveRow(c.getHeureDebut());
                int rowSpan = Math.max(1, resolveRow(c.getHeureFin()) - row);
                if (row < 0) continue;

                Cours cours = coursCache != null ? coursCache.get(c.getCoursId()) : null;
                Matiere mat = (cours != null && matiereCache != null)
                    ? matiereCache.get(cours.getMatiereId()) : null;
                Salle salle = (c.getSalleId() != null && salleCache != null)
                    ? salleCache.get(c.getSalleId()) : null;
                Enseignant ens = (cours != null && ensCache != null)
                    ? ensCache.get(cours.getEnseignantId()) : null;
                Classe classe = (cours != null && classeCache != null)
                    ? classeCache.get(cours.getClasseId()) : null;

                VBox cell = creerCellule(c, cours, mat, salle, ens, classe, modeEnseignant);
                cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

                // Retirer la cellule vide si elle existe
                grid.getChildren().removeIf(node -> {
                    Integer nc = GridPane.getColumnIndex(node);
                    Integer nr = GridPane.getRowIndex(node);
                    return nc != null && nc == col && nr != null && nr == row + 1
                        && (node instanceof Pane) && !(node instanceof VBox);
                });

                grid.add(cell, col, row + 1, 1, rowSpan);
            } catch (Exception ex) {
                // Ignorer les créneaux mal formés
            }
        }
    }

    //  CELLULE DE COURS

    /**
     * Crée une cellule visuelle pour un créneau de cours.
     *
     * @param c              Créneau
     * @param cours          Cours associé (peut être null)
     * @param mat            Matière (peut être null)
     * @param salle          Salle (peut être null)
     * @param ens            Enseignant (peut être null)
     * @param classe         Classe (peut être null)
     * @param modeEnseignant true = afficher la classe, false = afficher le prof
     * @return Nœud JavaFX représentant la cellule
     */
    public static VBox creerCellule(Creneau c, Cours cours, Matiere mat,
                                     Salle salle, Enseignant ens, Classe classe,
                                     boolean modeEnseignant) {
        boolean annule = "annule".equals(c.getStatut());
        String type    = cours != null ? cours.getTypeCours() : "CM";

        VBox box = new VBox(3);
        box.setPadding(new Insets(4, 6, 4, 6));
        box.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        box.setCursor(javafx.scene.Cursor.HAND);
        box.setStyle(getCouleurStyle(type, annule));

        // Bandeau type en haut
        Label tagType = new Label(annule ? "ANNULÉ" : type);
        tagType.setStyle("-fx-background-color:" + getCouleurTag(type, annule) + ";"
            + "-fx-text-fill:white;-fx-font-size:10px;-fx-font-weight:900;"
            + "-fx-padding:1 6;-fx-background-radius:3;");
        tagType.setMaxWidth(Double.MAX_VALUE);
        tagType.setAlignment(Pos.CENTER);

        // Matière en gras
        String nomMat = mat != null ? mat.getNom() : (cours != null ? "Cours #" + cours.getId() : "?");
        Label lblMat = new Label(nomMat);
        lblMat.setStyle("-fx-font-weight:800;-fx-font-size:12px;-fx-wrap-text:true;"
            + "-fx-text-fill:" + (annule ? "#757575" : "#1A202C") + ";");
        lblMat.setWrapText(true);
        lblMat.setMaxWidth(Double.MAX_VALUE);

        // Enseignant ou classe
        String ligne2;
        if (modeEnseignant) {
            ligne2 = classe != null ? classe.getIntitule() : "";
        } else {
            ligne2 = ens != null ? ens.getAffichageCourt() : "";
        }
        Label lblLine2 = new Label(ligne2);
        lblLine2.setStyle("-fx-font-size:11px;-fx-text-fill:#1A1F3A;-fx-font-weight:600;");
        lblLine2.setMaxWidth(Double.MAX_VALUE);

        // Salle
        String nomSalle = salle != null ? salle.getNumero() : (c.getSalleId() != null ? "S."+c.getSalleId() : "-");
        Label lblSalle = new Label(nomSalle);
        lblSalle.setStyle("-fx-font-size:10px;-fx-text-fill:#7B83C4;");
        lblSalle.setMaxWidth(Double.MAX_VALUE);

        box.getChildren().addAll(tagType, lblMat, lblLine2, lblSalle);

        // Tooltip détaillé
        String tooltipTxt = nomMat + "\n"
            + "Type : " + type + "\n"
            + "Horaire : " + c.getHeureDebut() + " - " + c.getHeureFin() + "\n"
            + (salle != null ? "Salle : " + salle.getDescription() + "\n" : "")
            + (ens   != null ? "Prof : " + ens.getAffichageComplet() + "\n" : "")
            + (classe != null ? "Classe : " + classe.getIntitule() : "");
        Tooltip.install(box, new Tooltip(tooltipTxt));

        return box;
    }

    //  LÉGENDE STANDARD

    /**
     * Construit la légende standard des types de cours.
     * Identique sur tous les EDT de l'application.
     *
     * @param container Conteneur VBox qui recevra la légende
     */
    public static void construirelegende(VBox container) {
        if (container == null) return;
        container.getChildren().clear();
        container.setSpacing(8);
        container.setPadding(new Insets(12));
        container.setStyle("-fx-background-color:white;-fx-background-radius:10;"
            + "-fx-border-color:#E8EBFF;-fx-border-radius:10;-fx-border-width:1;");

        Label titre = new Label("Légende");
        titre.setStyle("-fx-font-weight:800;-fx-font-size:13px;-fx-text-fill:#4361EE;");
        container.getChildren().add(titre);

        String[][] items = {
            {"CM",     "#4361EE", "Cours Magistral"},
            {"TD",     "#F4A261", "Travaux Dirigés"},
            {"TP",     "#2DC653", "Travaux Pratiques"},
            {"EXAMEN", "#E53E3E", "Examen / Contrôle"},
            {"ANNULÉ", "#7B83C4", "Cours annulé"},
        };
        for (String[] it : items) {
            HBox ligne = new HBox(8);
            ligne.setAlignment(Pos.CENTER_LEFT);
            Pane carre = new Pane();
            carre.setPrefSize(14, 14);
            carre.setMaxSize(14, 14);
            carre.setStyle("-fx-background-color:" + it[1] + ";-fx-background-radius:3;");
            Label lbl = new Label(it[2]);
            lbl.setStyle("-fx-font-size:12px;-fx-text-fill:#1A1F3A;");
            ligne.getChildren().addAll(carre, lbl);
            container.getChildren().add(ligne);
        }
    }

    //  RÉSOLUTION POSITION

    /**
     * Résout l'index de colonne à partir du jour (nom ou date).
     *
     * @param jour Jour en texte ("Lundi") ou date ISO ("2025-10-07")
     * @return Index colonne (0=Lundi … 5=Samedi) ou -1 si invalide
     */
    public static int resolveColonne(String jour) {
        if (jour == null) return -1;
        // Essayer par nom
        for (int j = 0; j < JOURS.length; j++) {
            if (JOURS[j].equalsIgnoreCase(jour)) return j;
        }
        // Essayer par date ISO
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(jour);
            int dow = date.getDayOfWeek().getValue() - 1; // 0=Lundi
            return (dow >= 0 && dow < JOURS.length) ? dow : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Résout l'index de ligne à partir d'une heure.
     *
     * @param heure Heure au format HH:mm
     * @return Index de ligne (0 = première tranche) ou 0 si invalide
     */
    public static int resolveRow(String heure) {
        if (heure == null) return 0;
        try {
            int h = Integer.parseInt(heure.split(":")[0]);
            int m = Integer.parseInt(heure.split(":")[1]);
            double total = h + m / 60.0;
            // HEURES commence à 07:30 avec pas de 1h
            int row = (int) Math.round((total - 7.5));
            return Math.max(0, Math.min(row, HEURES.length - 1));
        } catch (Exception e) {
            return 0;
        }
    }

    //  COULEURS

    /**
     * Retourne le style CSS de fond de la cellule selon le type de cours.
     *
     * @param type   Type de cours (CM, TD, TP, EXAMEN)
     * @param annule true si le cours est annulé
     * @return Chaîne de style JavaFX inline
     */
    public static String getCouleurStyle(String type, boolean annule) {
        if (annule) return "-fx-background-color:rgba(158,158,158,0.1);"
            + "-fx-border-color:#7B83C4;-fx-border-width:0 0 0 4;";
        switch (type == null ? "" : type.toUpperCase()) {
            case "CM":     return "-fx-background-color:rgba(21,101,192,0.10);"
                               + "-fx-border-color:#1565C0;-fx-border-width:0 0 0 4;";
            case "TD":     return "-fx-background-color:rgba(46,125,50,0.10);"
                               + "-fx-border-color:#2E7D32;-fx-border-width:0 0 0 4;";
            case "TP":     return "-fx-background-color:rgba(230,81,0,0.10);"
                               + "-fx-border-color:#E65100;-fx-border-width:0 0 0 4;";
            case "EXAMEN": return "-fx-background-color:rgba(198,40,40,0.10);"
                               + "-fx-border-color:#C62828;-fx-border-width:0 0 0 4;";
            default:       return "-fx-background-color:rgba(21,101,192,0.08);"
                               + "-fx-border-color:#1565C0;-fx-border-width:0 0 0 4;";
        }
    }

    /**
     * Retourne la couleur hexadécimale du bandeau type.
     *
     * @param type   Type de cours
     * @param annule true si annulé
     * @return Couleur hex
     */
    public static String getCouleurTag(String type, boolean annule) {
        if (annule) return "#7B83C4";
        switch (type == null ? "" : type.toUpperCase()) {
            case "CM":     return "#4361EE";
            case "TD":     return "#F4A261";
            case "TP":     return "#2DC653";
            case "EXAMEN": return "#E53E3E";
            default:       return "#4361EE";
        }
    }
}
