/**
 * Basic Java interface for interacting with the relevant parts of the GroupMe
 * API.
 * 
 * Created by Jacob Strieb
 */

package groupmeapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import static groupmearchivergui.GroupMeArchiverGUI.error;
import java.util.LinkedHashMap;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Request;


public class GroupMeAPI {
    
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
        
            ObjectMapper mapper = new ObjectMapper();
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

}
