/**
 * Basic Java interface for interacting with the relevant parts of the GroupMe
 * API.
 * 
 * Created by Jacob Strieb
 */

package groupmeapi;

import static groupmearchivergui.GroupMeArchiverGUI.error;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.control.ProgressBar;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Request;


public class GroupMeAPI {
    
    private static ObjectMapper mapper = null;
    
    private static void initObjectMapper() {
        if (mapper == null) {
            mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
    }
    
    /**
     * Return a list of groups of which the API_KEY-holding user is a member
     * 
     * TODO: Implement for >= 500 groups (i.e. multiple pages of data) -- loop
     *       until there is an empty response list
     * 
     * @param API_KEY GroupMe API token
     * @return Map of group names to group IDs
     */
    public static LinkedHashMap<String, String> getGroups(String API_KEY) {
        // Get the group list from GroupMe
        String raw;
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        try {
            raw = Request
                    .Get("https://api.groupme.com/v3/groups?omit=memberships&per_page=499&token=" + API_KEY)
                    .execute()
                    .returnContent()
                    .asString();
        
            initObjectMapper();
            JsonNode groupList = mapper.readTree(raw).path("response");
            
            for (JsonNode group : groupList) {
                result.put(group.path("name").asText(), group.path("group_id").asText());
            }
        } catch (HttpResponseException ex) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    error("Error getting data -- probably an invalid API key");
                }
            });
            return null;
        } catch (Exception ex) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    error("An unexpected error occurred while getting data from GroupMe. "
                        + "Possibly, the response was malformed");
                }
            });
            ex.printStackTrace();
            return null;
        }
        
        return result;
    }
    
    
    /**
     * Get a parsed JSON object with the group information to add to and export
     * @param API_KEY GroupMe API token
     * @param groupID ID of the group whose info will be retrieved
     * @return Parsed JSON object with group information
     */
    public static ObjectNode getGroupInfo(String API_KEY, String groupID) {
        String raw;
        try {
            raw = Request
                    .Get("https://api.groupme.com/v3/groups/" + groupID + "?token=" + API_KEY)
                    .execute()
                    .returnContent()
                    .asString();
            
            initObjectMapper();
            ObjectNode response = (ObjectNode) mapper.readTree(raw).path("response");
            
            return response;
        } catch (Exception ex) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    error("An unexpected error occurred while getting data from GroupMe. "
                        + "Possibly, the response was malformed");
                }
            });
            ex.printStackTrace();
            return null;
        }
    }
    
    
    /**
     * Download all of the messages associated with the group and add them to a
     * list of messages in the groupInfo node
     * @param group
     * @return 
     */
    public static void getMessages(ObjectNode group, String groupID, String API_KEY, ProgressBar progressBar) {
        String raw;
        try {
            // Get first 100 messages
            raw = Request
                    .Get("https://api.groupme.com/v3/groups/" + groupID + "/messages?limit=100&token=" + API_KEY)
                    .execute()
                    .returnContent()
                    .asString();
            
            initObjectMapper();
            ObjectNode response = (ObjectNode) mapper.readTree(raw).path("response");
            ArrayNode messages = (ArrayNode) response.path("messages");
            int totalCount = response.path("count").asInt();
            
            ArrayNode messageList = group.with("messages").withArray("message_list");
            messageList.addAll(messages);
            
            // Get remaining messages
            while (messageList.size() < totalCount) {
                // TODO: Do this the right way, whatever that is (respect main thread and whatnot)
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setProgress((double) messageList.size() / totalCount);
                    }
                });
                
                String beforeId = messageList.get(messageList.size() - 1).path("id").asText();
                try {
                    raw = Request
                            .Get("https://api.groupme.com/v3/groups/" + groupID + "/messages?limit=100&before_id=" + beforeId + "&token=" + API_KEY)
                            .execute()
                            .returnContent()
                            .asString();
                } catch (HttpResponseException ex) {
                    int status = ex.getStatusCode();
                    if (!(300 <= status && status < 400)) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                error("An unexpected error occurred while getting data from GroupMe. "
                                    + "Possibly, the response was malformed");
                            }
                        });
                        ex.printStackTrace();
                        return;
                    }
                }

                response = (ObjectNode) mapper.readTree(raw).path("response");
                messages = (ArrayNode) response.path("messages");
                messageList.addAll(messages);
            }
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    progressBar.setProgress((double) 1.0);
                }
            });
        } catch (Exception ex) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    error("An unexpected error occurred while getting data from GroupMe. "
                        + "Possibly, the response was malformed");
                }
            });
            ex.printStackTrace();
        }
    }
    
    
    /**
     * Write an object node out to a file as JSON
     * 
     * @param node node whose data will be written
     * @param outfile file to write to
     */
    public static void writeObjectNode(ObjectNode node, File outfile) {
        // Write the files
        ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
        try {
            writer.writeValue(outfile, node);
        } catch (Exception ex) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    error("An unexpected error occurred while saving the file.");
                }
            });
            ex.printStackTrace();
        }
    }
    
    
    /**
     * Count the number of messages with downloadable media attachments
     * 
     * TODO: Make this more efficient by enabling counting while messages are downloaded
     * 
     * @param group JSON object with messages with media attachments to count
     * @return the number of messages with downloadable attachments
     */
    public static int countMedia(ObjectNode group) {
        int result = 0;
        
        for (JsonNode message : group.with("messages").withArray("message_list")) {
            ArrayNode attachments = ((ObjectNode) message).withArray("attachments");
            for (JsonNode attachment : attachments) {
                String type = attachment.path("type").asText();
                if (type.equals("image") || type.equals("linked_image") || type.equals("video")) {
                    result++;
                }
            }
        }
        
        return result;
    }
    
    
    /**
     * Download all possible media files
     * 
     * @param group object with list of messages with attachments to download
     * @param mediaFolder folder in which to save downloaded media
     * @param progressBar progress bar object to be updated with progress
     */
    public static void downloadMedia(ObjectNode group, File mediaFolder, int totalCount, ProgressBar progressBar) {
        // int totalCount = group.with("messages").path("count").asInt();
        int seen = 0;
        
        for (JsonNode message : group.with("messages").withArray("message_list")) {
            ArrayNode attachments = ((ObjectNode) message).withArray("attachments");
            for (JsonNode attachment : attachments) {
                String type = attachment.path("type").asText();
                if (type.equals("image") || type.equals("linked_image") || type.equals("video")) {
                    double progress = (double) seen / totalCount;
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setProgress(progress);
                        }
                    });
                    
                    // Get filename
                    String url = attachment.path("url").asText();
                    String fileName = url.split("/")[url.split("/").length - 1];
                    fileName = message.path("created_at").asText() + "." + fileName;
                    // Add the correct file extension (remove the 'e' from 'jpeg' if applicable)
                    for (String s : new String[]{"gif", "jpeg", "png"}) {
                        fileName = (fileName.contains(s) ? fileName + "." + s.replace("e", "") : fileName);
                    }
                    
                    // Download
                    try {
                        InputStream in = new URL(url).openStream();
                        Files.copy(in, Paths.get(mediaFolder.getAbsolutePath(), fileName));
                    } catch (FileAlreadyExistsException ex) {
                        // Pass
                    } catch (MalformedURLException ex) {
                        Logger.getLogger(GroupMeAPI.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(GroupMeAPI.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (Exception ex) {
                        // Pass
                    }
                    
                    seen++;
                }
            }
        }
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                progressBar.setProgress((double) 1.0);
            }
        });
    }

}
