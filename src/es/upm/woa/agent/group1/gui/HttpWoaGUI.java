/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1.gui;

import es.upm.woa.agent.group1.WoaDefinitions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;


/**
 *
 * @author ISU
 */
public class HttpWoaGUI implements WoaGUI {
    
    private final URL serverUrl;
    
    private HttpWoaGUI(URL serverUrl) {
        this.serverUrl = serverUrl;
    }
    
    public static HttpWoaGUI getInstance() {
        Configurations config = new Configurations();
        String address = "";
        int port = 0;
        
        FileInputStream fis = null;
        try {
            PropertiesConfiguration woaConfig = config.properties(
                    new File(WoaDefinitions.CONFIG_FILENAME));

            address = woaConfig.getString("woa.interface_endpoint");
            port = woaConfig.getInt("woa.interface_port");
            
            String mapConfigPath = woaConfig.getString("woa.map_directory");
            String mapConfigFilename = woaConfig.getString("woa.map_filename");
            
            // TODO: map data from outside this class
            
            File mapConfiguration = new File(mapConfigPath + mapConfigFilename);
            fis = new FileInputStream(mapConfiguration);
            byte[] data = new byte[(int) mapConfiguration.length()];
            fis.read(data);
            fis.close();
            
            String mapConfigurationJson = new String(data, "UTF-8");
            
        } catch (ConfigurationException ex) {
            Logger.getGlobal().log(Level.WARNING, "Could not read configuration data ({0})", ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getGlobal().log(Level.SEVERE, "Could not decode map data ({0})", ex);
        } catch (IOException ex) {
            Logger.getGlobal().log(Level.SEVERE, "Could not read map data ({0})", ex);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ex) {
                    Logger.getGlobal().log(Level.SEVERE, "Could not close stream");
                }
            }
        }
        
        try {
            URL serverUrl = new URL("http://" + address + ":" + port);
            return new HttpWoaGUI(serverUrl);
        } catch (MalformedURLException ex) {
            Logger.getGlobal().log(Level.SEVERE, "Invalid server address ({0})", ex);
        }
        
        return null;
    }

    @Override
    public void apiStartGame(String[] playerIds, String jsonMapData) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void apiCreateAgent(String playerId, String newAgentId, int xPos, int yPos) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void apiMoveAgent(String agentId, int xPos, int yPos) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void apiAgentDies(String agentId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void apiStartAction(String agentId, String actionType) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void apiCancelAction(String agentId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void apiGainResource(String playerId, String agentId, String resourceType, int amount) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void apiLoseResource(String playerId, String agentId, String resourceType, int amount) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void apiDepleteResource(int xPos, int yPos) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void apiCreateBuilding(String playerId, String buildingType) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void apiEndGame() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
