/**
 * GroupMe Archiver GUI functions
 */
package groupmearchivergui;

import groupmeapi.GroupMeAPI;
import static groupmearchivergui.GroupMeArchiverGUI.error;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * The following code is organized based on the desired flow of user
 * interaction. As such, it is broken down into steps that the user will follow
 * to use the program.
 *
 * @author Jacob Strieb
 */
public class FXMLDocumentController implements Initializable {

    /**
     *************************************************************************
     * Global Variables
     *************************************************************************
     */
    private String API_KEY = "";
    private Preferences preferences = Preferences.userRoot().node(this.getClass().getName());
    

    /**
     *************************************************************************
     * Step 1: User enters API key
     *************************************************************************
     */
    @FXML
    private TextField apiKeyTextField;
    @FXML
    private CheckBox rememberApiKeyCheckBox;
    @FXML
    private ListView groupListView;
    @FXML
    private VBox optionsPanel;

    /**
     * Handle a user entering an API key and pressing the button by listing all
     * of the user's groups in the list panel on the left
     *
     * @param event unused
     */
    @FXML
    private void handleApiKeyButtonAction(ActionEvent event) {
        API_KEY = apiKeyTextField.getText();

        if (API_KEY == null || API_KEY.equals("")) {
            error("No API key entered");
            return;
        }

        if (rememberApiKeyCheckBox.isSelected()) {
            // Only proceed if the user acknowledges the risk of storing their key
            Alert warning = new Alert(Alert.AlertType.WARNING, "", ButtonType.YES, ButtonType.NO);
            warning.setHeaderText("Proceed with caution");
            warning.setContentText("API keys should be treated like passwords. "
                    + "This application does not store your keys securely. Please "
                    + "only proceed if you understand the risks. Are you sure you "
                    + "want to save your API key?");
            Optional<ButtonType> warningResult = warning.showAndWait();

            if (warningResult.get() == ButtonType.YES) {
                preferences.put("API_KEY", API_KEY);
            }
        }
        
        GroupMeAPI.getGroups(API_KEY);
        // TODO: Get the group list and check that the API key is valid
        // TODO: Set the group list in the panel on the left
        
        optionsPanel.setDisable(false);
    }
    
    

    /**
     *************************************************************************
     * Menu Items
     *************************************************************************
     */
    
    /**
     * Remove a stored API key by overwriting it with the empty string
     * 
     * @param event unused
     */
    @FXML
    private void handleRemoveApiKeyMenuItem(ActionEvent event) {
        preferences.put("API_KEY", "");
    }

    

    /**
     *************************************************************************
     * Miscellaneous
     *************************************************************************
     */
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Load a remembered API key from storage if possible
        apiKeyTextField.setText(preferences.get("API_KEY", ""));
    }

}
