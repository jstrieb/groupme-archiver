/**
 * GroupMe Archiver GUI functions
 */
package groupmearchivergui;

import com.fasterxml.jackson.databind.node.ObjectNode;
import groupmeapi.GroupMeAPI;
import static groupmearchivergui.GroupMeArchiverGUI.changeWindowTitle;
import static groupmearchivergui.GroupMeArchiverGUI.error;
import java.io.File;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

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
    private LinkedHashMap<String, String> groupList;
    private String groupID = "";
    

    /**
     *************************************************************************
     * Step 1: User enters API key and populates groups list
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
     * Handle a user clicking the "Remember API Key" checkbox with a warning
     * 
     * @param event unused
     */
    @FXML
    private void handleRememberApiKeyCheckBoxSelected(ActionEvent event) {
        if (rememberApiKeyCheckBox.isSelected()) {
            Alert warning = new Alert(Alert.AlertType.WARNING, "", ButtonType.YES, ButtonType.NO);
            warning.setHeaderText("Proceed with caution");
            warning.setContentText("API keys should be treated like passwords. "
                    + "This application does not store your keys securely. Please "
                    + "only proceed if you understand the risks. Are you sure you "
                    + "want to save your API key?");
            Optional<ButtonType> warningResult = warning.showAndWait();
            
            // Only proceed if the user acknowledges the risk of storing their key
            rememberApiKeyCheckBox.setSelected(warningResult.get() == ButtonType.YES);
        }
    }
    

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

        // Store the user's API key if necessary
        if (rememberApiKeyCheckBox.isSelected()) {
            preferences.put("API_KEY", API_KEY);
        }
        
        // Get the group list and check that the API key is valid
        groupList = GroupMeAPI.getGroups(API_KEY);
        if (groupList == null) return;
        
        // Set the group list in the panel on the left
        groupListView.getItems().addAll(groupList.keySet());
        
        // Set each group list item to have a callback that sents the global group ID accordingly
        groupListView.getSelectionModel().selectedItemProperty().addListener((observable) -> {
            if (observable == null || ((ReadOnlyObjectProperty) observable).getValue() == null) {
                handleListViewUnselection();
                return;
            }
            handleListViewSelection(((ReadOnlyObjectProperty) observable).getValue().toString());
        });
        
        // Enable the ability to select a group
        groupListView.setDisable(false);
    }
    
    

    /**
     *************************************************************************
     * Step 2: A user selects a group and their desired download options
     *************************************************************************
     */
    
    @FXML
    private Button beginArchivingButton;
    @FXML
    private Button useKeyButton;
    @FXML
    private ToggleGroup messageFormatToggleGroup;
    @FXML
    private TextField saveToFolderTextField;
    @FXML
    private VBox root;
    @FXML
    private Label statusLabel;
    
    /**
     * Update the window title, enable the additional options to download,
     * and set global variable with the Group ID to be downloaded when the user
     * selects something in the left sidebar
     * 
     * @param groupName the name of the group (key in the groupList)
     */
    @FXML
    private void handleListViewSelection(String groupName) {
        // Assume groupName in groupList otherwise something has gone horribly wrong
        groupID = groupList.get(groupName);
        changeWindowTitle("Archive \"" + groupName + "\"");
        optionsPanel.setDisable(false);
        beginArchivingButton.setDefaultButton(true);
        useKeyButton.setDefaultButton(false);
        statusLabel.setText("Archive group \"" + groupName + "\"");
    }
    
    
    /**
     * Update the window title, and disable the ability to download when there
     * are no selected options from the left sidebar
     */
    @FXML
    private void handleListViewUnselection() {
        changeWindowTitle("");
        optionsPanel.setDisable(true);
        statusLabel.setText("");
    }
    
    
    /**
     * Open a directory selection dialog so the user can pick where to download
     * the data
     * 
     * @param event unused
     */
    @FXML
    private void handleBrowseButtonAction(ActionEvent event) {
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setInitialDirectory(new File(saveToFolderTextField.getText()));
        fileChooser.setTitle("Choose a directory to put downloaded messages and media...");
        File file = fileChooser.showDialog(root.getScene().getWindow());
        saveToFolderTextField.setText(file.getAbsolutePath());
    }
    
    

    /**
     *************************************************************************
     * Step 3: Download the messages (if applicable)
     *************************************************************************
     */
    
    @FXML
    private ProgressBar mainProgressBar;
    
    /**
     * Begin the action of archiving -- handle calls to archive messages and media
     * 
     * @param event unused
     */
    @FXML
    private void handleBeginArchivingAction(ActionEvent event) {
        preferences.put("CWD", saveToFolderTextField.getText());
        ObjectNode group = GroupMeAPI.getGroupInfo(API_KEY, groupID);
        int totalCount = group.path("messages").path("count").asInt();
        
        // Make a folder for saving data
        Path groupFolderPath = Paths.get(saveToFolderTextField.getText(), group.path("name").asText()).toAbsolutePath();
        File groupFolder = groupFolderPath.toFile();
        if ((!groupFolder.exists() || !groupFolder.isDirectory()) && !groupFolder.mkdirs()) {
            error("Failed to create folder " + groupFolder.getAbsolutePath());
            return;
        }
        
        // Download the messages and save (in a separate thread so it doesn't block the UI)
        Thread downloadThread = new Thread() {
            @Override
            public void run() {
                super.run();
                
                Path messageFilePath = Paths.get(groupFolderPath.toString(), "messages.json");
                File messageFile = messageFilePath.toFile();
                GroupMeAPI.getMessages(group, groupID, API_KEY, messageFile, mainProgressBar);
            }
        };
        downloadThread.setDaemon(true);
        downloadThread.start();
        
        statusLabel.setText("Downloading " + totalCount + " messages...");
    }
    
    

    /**
     *************************************************************************
     * Step 4: Download the media (if applicable)
     *************************************************************************
     */
    
    // TODO: Implement
    
    

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
        apiKeyTextField.setText("");
        rememberApiKeyCheckBox.setSelected(false);
    }
    
    
    /**
     * Exit the application
     * 
     * @param event unused
     */
    @FXML
    private void handleCloseAction(ActionEvent event) {
        System.exit(0);
    }
    
    
    /**
     * Show a popup with information about the software
     * 
     * @param event unused
     */
    @FXML
    private void handleAboutAction(ActionEvent event) {
        Alert popup = new Alert(Alert.AlertType.INFORMATION);
        popup.setHeaderText("About GroupMe Archiver");
        popup.setContentText("This application was created in June 2019 by Jacob Strieb.");
        popup.showAndWait();
    }
    
    
    /**
     * Delete all stored preferences
     * 
     * @param event unused
     */
    @FXML
    private void handleRemoveAllPreferencesAction(ActionEvent event) {
        try {
            preferences.clear();
        } catch (BackingStoreException ex) {
        }
    }

    

    /**
     *************************************************************************
     * Miscellaneous
     *************************************************************************
     */
    
    /**
     * Run on startup 
     * 
     * @param url unused
     * @param rb unused
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Load a remembered API key from storage if possible
        API_KEY = preferences.get("API_KEY", "");
        apiKeyTextField.setText(API_KEY);
        if (!API_KEY.equals("")) {
            rememberApiKeyCheckBox.setSelected(true);
        }
        
        // Load a remembered directory from storage if possible, otherwise show CWD
        String currentWorkingDirectory = preferences.get("CWD", "");
        if (currentWorkingDirectory.equals("")) {
            currentWorkingDirectory = System.getProperty("user.home");
        }
        saveToFolderTextField.setText(currentWorkingDirectory);
    }

}
