/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;
import java.util.logging.Logger;

/**
 *
 * @author ISU
 */
public class GameClock implements Ticker {
    
    private static final int DEFAULT_TICK_DELTA_MILLIS = 500;
    
    private static GameClock instance;
    
    private final long startMillis;
    private final long tickMillis;
    
    private GameClock(long tick) {
        startMillis = System.currentTimeMillis();
        tickMillis = tick;
    }

    public synchronized static GameClock getInstance() {
        if (instance == null) {
            Configurations config = new Configurations();
            int tickDeltaMillis = 0;
            try {
                PropertiesConfiguration woaConfig = config.properties(
                        new File(WoaDefinitions.CONFIG_FILENAME));
                
                tickDeltaMillis = woaConfig.getInt("woa.tick_millis");
            } catch (ConfigurationException ex) {
                Logger.getGlobal().warning("Could not read configuration property woa.tick_millis. Using default");
                tickDeltaMillis = DEFAULT_TICK_DELTA_MILLIS;
            }
            
            instance = new GameClock(tickDeltaMillis);
        }
        
        return instance;
    }
    
    
    @Override
    public long getCurrentTick() {
        return (System.currentTimeMillis() - startMillis) / tickMillis;
    }
    
}
