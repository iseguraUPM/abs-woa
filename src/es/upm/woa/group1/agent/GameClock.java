/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import es.upm.woa.group1.map.WoaConfigurator;
import java.util.logging.Level;

import org.apache.commons.configuration2.ex.ConfigurationException;

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
            int tickDeltaMillis;
            try {
                tickDeltaMillis = WoaConfigurator.getInstance().getTickMillis();
            } catch (ConfigurationException ex) {
                Logger.getGlobal().log(Level.WARNING
                        , "Could not read configuration property"
                                + " woa.tick_millis. Using default: {0}"
                        , DEFAULT_TICK_DELTA_MILLIS);
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
