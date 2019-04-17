/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

/**
 *
 * @author ISU
 */
public class GameClock implements Ticker {
    
    private static final int TICK_DELTA_MILLIS = 100;
    
    private static GameClock instance;
    
    private final long startMillis;
    private final long tickMillis;
    
    private GameClock(long tick) {
        startMillis = System.currentTimeMillis();
        tickMillis = tick;
    }

    public synchronized static GameClock getInstance() {
        if (instance == null) {
            instance = new GameClock(TICK_DELTA_MILLIS);
        }
        
        return instance;
    }
    
    
    @Override
    public long getCurrentTick() {
        return (System.currentTimeMillis() - startMillis) / tickMillis;
    }
    
}
