/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1.strategy;



/**
 *
 * @author ISU
 */
public class StrategyEvent {
    
    private final int type;
    
    public static final StrategyEvent WAIT = new StrategyEvent(0);
    public static final StrategyEvent CREATE_UNIT = new StrategyEvent(1);
    
    private StrategyEvent(int type) {
        this.type = type;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof StrategyEvent) {
            StrategyEvent other = (StrategyEvent) o;
            return type == other.type;
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + this.type;
        return hash;
    }
    
}


