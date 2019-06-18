/**
 * Basic Java interface for interacting with the relevant parts of the GroupMe
 * API.
 * 
 * Created by Jacob Strieb
 */

package groupmeapi;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import static groupmearchivergui.GroupMeArchiverGUI.error;
import java.io.File;
import java.util.LinkedHashMap;

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
            error("Error getting data -- probably an invalid API key");
            return null;
        } catch (Exception ex) {
            error("An unexpected error occurred while getting data from GroupMe. "
                + "Possibly, the response was malformed");
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
            error("An unexpected error occurred while getting data from GroupMe. "
                + "Possibly, the response was malformed");
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
    public static void getMessages(ObjectNode group, String groupID, String API_KEY, File outfile) {
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
                raw = Request
                        .Get("https://api.groupme.com/v3/groups/" + groupID + "/messages?limit=100&token=" + API_KEY)
                        .execute()
                        .returnContent()
                        .asString();

                response = (ObjectNode) mapper.readTree(raw).path("response");
                messages = (ArrayNode) response.path("messages");
                messageList.addAll(messages);
            }
            
            ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
            writer.writeValue(outfile, group);
        } catch (Exception ex) {
            error("An unexpected error occurred while getting data from GroupMe. "
                + "Possibly, the response was malformed");
            ex.printStackTrace();
        }
    }

}
