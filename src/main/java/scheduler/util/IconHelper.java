package scheduler.util;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.InputStream;

/**
 * Utilitaire pour manipuler les icônes officielles de l'application
 * et créer des boutons d'action homogènes et prestigieux.
 */
public class IconHelper {

    /**
     * Charge une icône PNG depuis le pack officiel avec redimensionnement automatique.
     */
    public static ImageView getIcon(String iconName, int size) {
        String path = "/images/icons/" + (iconName.endsWith(".png") ? iconName : iconName + ".png");
        InputStream is = IconHelper.class.getResourceAsStream(path);
        if (is != null) {
            Image img = new Image(is, size, size, true, true);
            ImageView iv = new ImageView(img);
            iv.setFitWidth(size);
            iv.setFitHeight(size);
            return iv;
        }
        return null;
    }

    /**
     * Crée un bouton d'action net pour les tableaux avec icône et infobulle.
     */
    public static Button createActionButton(String label, String iconName, String tooltipText, String bgHex, String textHex) {
        Button btn = new Button(label);
        ImageView icon = getIcon(iconName, 14);
        if (icon != null) {
            btn.setGraphic(icon);
        }
        btn.setAlignment(Pos.CENTER);
        btn.setStyle(String.format(
            "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 11px; -fx-font-weight: bold; " +
            "-fx-padding: 5 10; -fx-background-radius: 5px; -fx-cursor: hand;",
            bgHex, textHex
        ));
        if (tooltipText != null && !tooltipText.isEmpty()) {
            btn.setTooltip(new Tooltip(tooltipText));
        }
        return btn;
    }

    public static Button createIconButton(String iconName, String tooltipText, String bgHex) {
        Button btn = new Button();
        ImageView icon = getIcon(iconName, 14);
        if (icon != null) {
            btn.setGraphic(icon);
        }
        btn.setAlignment(Pos.CENTER);
        btn.setStyle(String.format(
            "-fx-background-color: %s; -fx-padding: 5 8; -fx-background-radius: 6px; -fx-cursor: hand; " +
            "-fx-border-color: rgba(61, 38, 26, 0.15); -fx-border-radius: 6px;",
            bgHex
        ));
        if (tooltipText != null && !tooltipText.isEmpty()) {
            btn.setTooltip(new Tooltip(tooltipText));
        }
        return btn;
    }
}
