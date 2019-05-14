/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.gui;

import es.upm.woa.group1.WoaDefinitions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import es.upm.woa.group1.map.WoaConfigurator;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ISU
 */
class HttpWoaGUI implements WoaGUI {
    
    private final static String URI_START_GAME = "/api/start";
    private final static String URI_NEW_AGENT = "/api/agent/create";
    private final static String URI_MOVE_AGENT = "/api/agent/move";
    private final static String URI_KILL_AGENT = "/api/agent/die";
    private final static String URI_START_ACTION_AGENT = "/api/agent/start";
    private final static String URI_STOP_ACTION_AGENT = "/api/agent/cancel";
    private final static String URI_GAIN_RESOURCE = "/api/resource/gain";
    private final static String URI_LOSE_RESOURCE = "/api/resource/lose";
    private final static String URI_DEPLETE_RESOURCE = "/api/resource/deplete";
    private final static String URI_CREATE_BUILDING = "/api/building/create";
    private final static String URI_FINISH_GAME = "/api/end";
    
    private final URL serverUrl;
    
    private HttpWoaGUI(URL serverUrl) {
        this.serverUrl = serverUrl;
    }
    
    /**
     * Retrieve HttpWoaGUI instance
     * @return new instance or null if files could not be accessed
     */
    public static HttpWoaGUI getInstance() {
        String address = "";
        
        try {
            address = WoaConfigurator.getInstance().getGuiEndpoint();
            
        } catch (ConfigurationException ex) {
            Logger.getGlobal().log(Level.WARNING, "Could not read"
                    + " configuration gui endpoint");
        }
        
        try {
            URL serverUrl = new URL("http://" + address);
            testConnection(serverUrl);
            return new HttpWoaGUI(serverUrl);
        } catch (MalformedURLException ex) {
            Logger.getGlobal().log(Level.SEVERE, "Invalid server address: {0}"
                    , "http://" + address);
        } catch (IOException ex) {
            Logger.getGlobal().log(Level.SEVERE, "Could not connect to server: {0}"
                    , "http://" + address);
        }
        
        return null;
    }

    private static void testConnection(URL serverUrl1) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) serverUrl1.openConnection();
        conn.connect();
        conn.disconnect();
    }
    
    private HttpURLConnection connectTo(String resourceURI)
            throws MalformedURLException, IOException {
        URL completeURL = new URL(serverUrl, resourceURI);
        HttpURLConnection connection = (HttpURLConnection)
                completeURL.openConnection();
        return connection;
    }
    
    private void sendJson(HttpURLConnection connection, String jsonString) throws IOException {
        byte[] out = jsonString.getBytes(StandardCharsets.UTF_8);
        int length = out.length;
        
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setFixedLengthStreamingMode(length);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.connect();
        
        try (OutputStream os = connection.getOutputStream()) {
            os.write(out);
        } finally {
            connection.disconnect();
        }
    }

    @Override
    public void apiStartGame(String[] playerIds, String jsonMapData) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        
        ObjectNode root = factory.objectNode();
        ArrayNode playerIdsNode = root.putArray("players");
        for (String playerId : playerIds) {
            playerIdsNode.add(playerId);
        }
        
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode mapDataNode = mapper.readTree(jsonMapData);
            root.set("map", mapDataNode);
            
            
        } catch (IOException ex) {
            Logger.getGlobal().log(Level.WARNING
                    , "Could not parse json map data");
            return;
        }
        
        sendJsonObjectTo(URI_START_GAME, root);
    }

    private void sendJsonObjectTo(String resourceURI, ObjectNode root) {
        try {
            HttpURLConnection connection = connectTo(resourceURI);
            sendJson(connection, root.toString());
        } catch (MalformedURLException ex) {
            Logger.getGlobal().log(Level.WARNING
                    , "Could not form URL ({0})", ex);
        } catch (IOException ex) {
            Logger.getGlobal().log(Level.WARNING
                    , "Could not send data to GUI endpoint");
        }
    }

    @Override
    public void apiCreateAgent(String playerId, String newAgentId, int xPos, int yPos) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        
        ObjectNode root = factory.objectNode();
        root.put("player_id", playerId);
        root.put("agent_id", newAgentId);
        
        ObjectNode coordinatesNode = root.putObject("tile");
        coordinatesNode.put("x",xPos).put("y", yPos);
        
        sendJsonObjectTo(URI_NEW_AGENT, root);
    }

    @Override
    public void apiMoveAgent(String agentId, int xPos, int yPos) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        
        ObjectNode root = factory.objectNode();
        root.put("agent_id", agentId);
        
        ObjectNode coordinatesNode = root.putObject("tile");
        coordinatesNode.put("x",xPos).put("y", yPos);
        
        sendJsonObjectTo(URI_MOVE_AGENT, root);
    }

    @Override
    public void apiAgentDies(String agentId) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        
        ObjectNode root = factory.objectNode();
        root.put("agent_id", agentId);
        
        sendJsonObjectTo(URI_KILL_AGENT, root);
    }

    @Override
    public void apiStartAction(String agentId, String actionType) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        
        ObjectNode root = factory.objectNode();
        root.put("agent_id", agentId).put("type", actionType);
        
        sendJsonObjectTo(URI_START_ACTION_AGENT, root);
    }

    @Override
    public void apiCancelAction(String agentId) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        
        ObjectNode root = factory.objectNode();
        root.put("agent_id", agentId);
        
        sendJsonObjectTo(URI_STOP_ACTION_AGENT, root);
    }

    @Override
    public void apiGainResource(String playerId, String agentId, String resourceType, int amount) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        
        ObjectNode root = factory.objectNode();
        root.put("player_id", playerId).put("agent_id", agentId);
        root.put("resource", resourceType).put("amount", amount);
        
        
        sendJsonObjectTo(URI_GAIN_RESOURCE, root);
    }

    @Override
    public void apiLoseResource(String playerId, String agentId, String resourceType, int amount) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        
        ObjectNode root = factory.objectNode();
        root.put("player_id", playerId).put("agent_id", agentId);
        root.put("resource", resourceType).put("amount", amount);
        
        
        sendJsonObjectTo(URI_LOSE_RESOURCE, root);
    }

    @Override
    public void apiDepleteResource(int xPos, int yPos) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        
        ObjectNode root = factory.objectNode();
        root.putObject("tile").put("x", xPos).put("y", yPos);
        
        sendJsonObjectTo(URI_DEPLETE_RESOURCE, root);
    }

    @Override
    public void apiCreateBuilding(String playerId, String buildingType) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        
        ObjectNode root = factory.objectNode();
        root.put("player_id", playerId).put("type", buildingType);        
        
        sendJsonObjectTo(URI_CREATE_BUILDING, root);
    }

    @Override
    public void apiEndGame() {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        
        sendJsonObjectTo(URI_FINISH_GAME, factory.objectNode());
    }
    
}
