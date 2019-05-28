/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

/**
 *
 * @author ISU
 */
class TribeClock implements Ticker {
    
    private static final int DEFAULT_TICK_DELTA_MILLIS = 500;
    
    private final long startMillis;
    private final long tickMillis;
    
    public TribeClock() {
        startMillis = System.currentTimeMillis();
        tickMillis = DEFAULT_TICK_DELTA_MILLIS;
    }
    
    
    @Override
    public long getCurrentTick() {
        return (System.currentTimeMillis() - startMillis) / tickMillis;
    }
    
}
