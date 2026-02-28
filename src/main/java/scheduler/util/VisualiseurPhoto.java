package scheduler.util;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import scheduler.service.ImageUploadService;

/**
 * Modale de visualisation haute définition pour photos de salles, bâtiments, UFR ou preuves de signalements.
 */
public class VisualiseurPhoto {

    public static void afficher(String titre, String sousTitre, String photoUrl, String details) {
        afficher(null, titre, sousTitre, photoUrl, details);
    }

    public static void afficher(Window parent, String titre, String sousTitre, String photoUrl, String details) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (parent != null) dialog.initOwner(parent);
        dialog.setTitle(titre != null ? titre : "Visualisation Photo");

        VBox root = new VBox(15);
        root.setStyle("-fx-background-color: #FBF8F3; -fx-padding: 20;");
        root.setAlignment(Pos.CENTER);

        // En-tête
        VBox header = new VBox(4);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lblTitre = new Label(titre != null ? titre : "Détails du lieu");
        lblTitre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #3D261A;");
        Label lblSousTitre = new Label(sousTitre != null ? sousTitre : "");
        lblSousTitre.setStyle("-fx-font-size: 13px; -fx-text-fill: #6B4226;");
        header.getChildren().addAll(lblTitre, lblSousTitre);

        // Image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(520);
        imageView.setFitHeight(340);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        Image img = null;
        if (photoUrl != null && !photoUrl.trim().isEmpty()) {
            img = ImageUploadService.chargerImage(photoUrl, 520, 340);
        }
        if (img == null) {
            // Icône par défaut si pas d'image
            ImageView icone = IconHelper.getIcon("room.png", 64);
            if (icone != null) img = icone.getImage();
        }

        if (img != null) {
            imageView.setImage(img);
        }

        VBox cadrePhoto = new VBox(imageView);
        cadrePhoto.setAlignment(Pos.CENTER);
        cadrePhoto.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-color: #EBDBC8; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");
        cadrePhoto.setMaxWidth(540);

        // Détails complémentaires
        Label lblDetails = new Label(details != null ? details : "");
        lblDetails.setStyle("-fx-font-size: 12px; -fx-text-fill: #3D261A; -fx-wrap-text: true;");
        lblDetails.setMaxWidth(520);

        // Bouton Fermer
        Button btnFermer = new Button("Fermer");
        btnFermer.setStyle("-fx-background-color: #3D261A; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 24; -fx-background-radius: 6; -fx-cursor: hand;");
        btnFermer.setOnAction(e -> dialog.close());

        HBox btnBox = new HBox(btnFermer);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(header, cadrePhoto, lblDetails, btnBox);

        Scene scene = new Scene(root, 580, 520);
        dialog.setScene(scene);
        dialog.setResizable(true);
        dialog.setMinWidth(400);
        dialog.setMinHeight(400);
        dialog.showAndWait();
    }
}
