package scheduler;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Screen;
import scheduler.dao.ConnexionBD;
import scheduler.controleur.LoginControleur;
import scheduler.service.RapportSchedulerService;
import scheduler.service.ScheduledTaskService;
import java.net.URL;

/**
 * Classe principale de l'application
 * Point d'entrée JavaFX qui initialise la base de données et charge l'interface de connexion
 */
public class Main extends Application {
    
    private ScheduledTaskService scheduledTasks;
    private RapportSchedulerService rapportScheduler;

    /**
     * Point d'entrée principal de l'application
     * @param args arguments de la ligne de commande
     */
    public static void main(String[] args) {
        launch(args);
    }
    
    /**
     * Démarre l'application
     * Teste la connexion à la base de données
     * Charge l'écran de connexion et configure la fenêtre principale
     * @param primaryStage la fenêtre principale de l'application
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            scheduledTasks = new ScheduledTaskService();
            scheduledTasks.start();
            
            rapportScheduler = new RapportSchedulerService();
            rapportScheduler.start();
            
            if (ConnexionBD.testerConnexion()) {
            }
            
            Screen screen = Screen.getPrimary();
            
            URL fxmlUrl = getClass().getResource("/fxml/Login.fxml");
            if (fxmlUrl == null) {
                System.err.println("❌ /fxml/Login.fxml introuvable dans le classpath !");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            
            LoginControleur controleur = loader.getController();
            if (controleur != null) {
                controleur.setPrimaryStage(primaryStage);
            }
            
            Scene scene = new Scene(root);
            
            URL cssUrl = getClass().getResource("/css/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.out.println("⚠️ /css/style.css non trouvé");
            }
            
            primaryStage.setTitle("UNIV-SCHEDULER - Gestion des emplois du temps");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(280);
            primaryStage.setMinHeight(400);
            primaryStage.setResizable(true);
            primaryStage.centerOnScreen();
            
            primaryStage.setOnCloseRequest(event -> {
                if (scheduledTasks != null) {
                    scheduledTasks.stop();
                }
                if (rapportScheduler != null) {
                    rapportScheduler.stop();
                }
            });
            
            primaryStage.show();
            primaryStage.toFront();
            primaryStage.requestFocus();
            System.out.println("🚀 Fenêtre principale affichée avec succès !");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur critique au démarrage de JavaFX :");
            e.printStackTrace();
        }
    }
}