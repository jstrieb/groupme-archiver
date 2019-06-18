/*
 * Main file to run the GroupMe archiver GUI
 */
package groupmearchivergui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 *
 * @author Jacob Strieb
 */
public class GroupMeArchiverGUI extends Application {
    
    private static Stage stage;
    
    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        
        // Configure the window
        stage.setTitle("GroupMe Archiver");
        setUserAgentStylesheet(STYLESHEET_MODENA);
        
        // Load the layout
        Parent root = FXMLLoader.load(getClass().getResource("FXMLDocument.fxml"));
        Scene scene = new Scene(root);
        
        // Display the window
        stage.setScene(scene);
        stage.show();
    }

    
    /**
     * Show an error dialog to the user
     *
     * @param errorMessage the message that will be displayed in the main part
     * of the error dialog window
     */
    public static void error(String errorMessage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error!");
        alert.setHeaderText("Error!");
        alert.setContentText(errorMessage);
        alert.showAndWait();
    }
    
    
    /**
     * Set the static stage title to the input text
     * 
     * @param title title to set the window to 
     */
    public static void changeWindowTitle(String title) {
        if (title.equals("")) {
            stage.setTitle("GroupMe Archiver");
        } else {
            stage.setTitle("GroupMe Archiver - " + title);
        }
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
