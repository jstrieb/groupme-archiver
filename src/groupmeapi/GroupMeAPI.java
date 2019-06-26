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
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.control.ProgressBar;



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
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        try {
            String url = "https://api.groupme.com/v3/groups?omit=memberships&per_page=499&token=" + API_KEY;
            InputStream inStream = new URL(url).openStream();
        
            initObjectMapper();
            JsonNode groupList = mapper.readTree(inStream).path("response");
            
            for (JsonNode group : groupList) {
                result.put(group.path("name").asText(), group.path("group_id").asText());
            }
        } catch (Exception ex) {
            Platform.runLater(() -> {
                error("An unexpected error occurred while getting data from GroupMe. "
                        + "Possibly, the response was malformed");
            });
            ex.printStackTrace();
            return null;
        }
        
        return result;
    }
    
    
    /**
     * Get a parsed JSON object with the group information to add to and export
     * 
     * @param API_KEY GroupMe API token
     * @param groupID ID of the group whose info will be retrieved
     * @return Parsed JSON object with group information
     */
    public static ObjectNode getGroupInfo(String API_KEY, String groupID) {
        try {
            String url = "https://api.groupme.com/v3/groups/" + groupID + "?token=" + API_KEY;
            InputStream inStream = new URL(url).openStream();
            
            initObjectMapper();
            ObjectNode response = (ObjectNode) mapper.readTree(inStream).path("response");
            
            return response;
        } catch (Exception ex) {
            Platform.runLater(() -> {
                error("An unexpected error occurred while getting data from GroupMe. "
                        + "Possibly, the response was malformed");
            });
            ex.printStackTrace();
            return null;
        }
    }
    
    
    /**
     * Download all of the messages associated with the group and add them to a
     * list of messages in the groupInfo node
     * 
     * @param group ObjectNode with information and messages of the group to be archived
     * @param groupID ID of the group for use in the GroupMe API
     * @param API_KEY GroupMe API Token
     * @param progressBar ProgressBar to be updated as it proceeds
     */
    public static void getMessages(ObjectNode group, String groupID, String API_KEY, ProgressBar progressBar) {
        try {
            // Get first 100 messages
            String url = "https://api.groupme.com/v3/groups/" + groupID + "/messages?limit=100&token=" + API_KEY;
            InputStream inStream = new URL(url).openStream();
            
            initObjectMapper();
            ObjectNode response = (ObjectNode) mapper.readTree(inStream).path("response");
            ArrayNode messages = (ArrayNode) response.path("messages");
            int totalCount = response.path("count").asInt();
            
            ArrayNode messageList = group.with("messages").withArray("message_list");
            messageList.addAll(messages);
            
            // Get remaining messages
            while (messageList.size() < totalCount) {
                Platform.runLater(() -> {
                    progressBar.setProgress((double) messageList.size() / totalCount);
                });
                
                String beforeId = messageList.get(messageList.size() - 1).path("id").asText();
                url = "https://api.groupme.com/v3/groups/" + groupID + "/messages?limit=100&before_id=" + beforeId + "&token=" + API_KEY;
                inStream = new URL(url).openStream();
                
                response = (ObjectNode) mapper.readTree(inStream).path("response");
                messages = (ArrayNode) response.path("messages");
                messageList.addAll(messages);
            }
            Platform.runLater(() -> {
                progressBar.setProgress((double) 1.0);
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
     * @param group JSON object with messages with media attachments to count
     * @return the number of messages with downloadable attachments
     */
    public static int countMedia(ObjectNode group) {
        ArrayNode mediaList = group.withArray("media_list");
        
        for (JsonNode message : group.with("messages").withArray("message_list")) {
            ArrayNode attachments = ((ObjectNode) message).withArray("attachments");
            for (JsonNode attachment : attachments) {
                String type = attachment.path("type").asText();
                if (type.equals("image") || type.equals("linked_image") || type.equals("video")) {
                    mediaList.add(message);
                }
            }
        }
        
        return mediaList.size();
    }
    
    
    /**
     * Download all possible media files
     * 
     * @param group object with list of messages with attachments to download
     * @param mediaFolder folder in which to save downloaded media
     * @param totalCount total number of media files to be downloaded -- used for progressBar
     * @param progressBar progress bar object to be updated with progress
     */
    public static void downloadMedia(ObjectNode group, File mediaFolder, int totalCount, ProgressBar progressBar) {
        JsonNode[] messageList = mapper.convertValue(group.with("messages").withArray("message_list"), JsonNode[].class);
        downloadMediaFromMessages(messageList, mediaFolder, totalCount, progressBar);
    }
    
    
    /**
     * Loop over the messageList downloading each message attachment to the
     * mediaFolder on a single thread
     * 
     * @param messageList list of JsonNodes, each of which is a message
     * @param mediaFolder folder into which media will be downloaded
     * @param totalCount total expected number of media files to download
     * @param progressBar ProgressBar object to be updated as media is downloaded
     */
    private static void downloadMediaFromMessages(JsonNode[] messageList, File mediaFolder, int totalCount, ProgressBar progressBar) {
        int seen = 0;
        
        for (JsonNode message : messageList) {
            if (message == null)
                continue;
            ArrayNode attachments = ((ObjectNode) message).withArray("attachments");
            for (JsonNode attachment : attachments) {
                String type = attachment.path("type").asText();
                if (type.equals("image") || type.equals("linked_image") || type.equals("video")) {
                    double progress = (double) seen / totalCount;
                    Platform.runLater(() -> {
                        progressBar.setProgress(progress);
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
        Platform.runLater(() -> {
            progressBar.setProgress((double) 1.0);
        });
    }
    
    
    /**
     * Download the media from a single message (i.e. consume from producer)
     * 
     * @param message the message whose attachments to download
     * @param mediaFolder the folder into which to save the downloaded attachments
     */
    private static void downloadMediaSingle(JsonNode message, File mediaFolder) {
        if (message == null)
            return;
        ArrayNode attachments = ((ObjectNode) message).withArray("attachments");
        for (JsonNode attachment : attachments) {
            String type = attachment.path("type").asText();
            if (type.equals("image") || type.equals("linked_image") || type.equals("video")) {
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
            }
        }
    }
    
    
    /**
     * Produce messages for threads to download and consume -- used for
     * synchronizing various consumer threads that independently download media
     */
    private static class MessageProducer {
        int index;
        JsonNode[] messageList;
        ReentrantLock lock;
        ProgressBar progressBar;
        Runnable onCompletion;
        
        public MessageProducer(JsonNode[] messageList, ProgressBar progressBar, Runnable onCompletion) {
            this.index = 0;
            this.messageList = messageList;
            this.lock = new ReentrantLock();
            this.progressBar = progressBar;
            this.onCompletion = onCompletion; // Useful for changing UI when done
        }
        
        public JsonNode produce() {
            JsonNode result = null;
            
            this.lock.lock();
            
            try {
                if (this.index < this.messageList.length) {
                    result = this.messageList[this.index++];
                } else {
                    this.onCompletion.run();
                }

                double progress = (double) this.index / this.messageList.length;
                Platform.runLater(() -> {
                    progressBar.setProgress(progress);
                });
            } finally {
                this.lock.unlock();
            }
            
            return result;
        }
    }
    
    
    /**
     * Download media from all messages that have it and save to the mediaFolder.
     * Create numThreads threads to use and have main thread use producer-consumer
     * solution to synchronize threads' downloading of files
     * 
     * @param group group object from which media will be downloaded
     * @param mediaFolder folder into which downloaded media will be saved
     * @param numThreads number of threads to use for downloading
     * @param progressBar ProgressBar object to be updated as files are downloaded
     * @param onCompletion run once the files have completed downloading
     */
    public static void downloadMediaMultithreaded(ObjectNode group, File mediaFolder, int numThreads, ProgressBar progressBar, Runnable onCompletion) {
        JsonNode[] messageList = mapper.convertValue(group.withArray("media_list"), JsonNode[].class);
        MessageProducer producer = new MessageProducer(messageList, progressBar, onCompletion);
        
        Thread[] threads = new Thread[numThreads];
        
        for (int i=0; i < numThreads; i++) {
            threads[i] = new Thread() {
                @Override
                public void run() {
                    JsonNode message = producer.produce();
                    while (message != null) {
                        downloadMediaSingle(message, mediaFolder);
                        message = producer.produce();
                    }
                }
            };
            
            threads[i].start();
        }
        
    }

}
