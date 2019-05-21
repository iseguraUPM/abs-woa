/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent.strategy;

import jade.core.Agent;

/**
 *
 * @author ISU
 */
public class WaitStrategy extends Strategy {
    
    private static final int WAIT_PRIORITY = LOW_PRIORITY - 10;

    public WaitStrategy(Agent agent) {
        super(agent);
     
    }

    @Override
    public void action() {
        block();
    }

    @Override
    public boolean done() {
        return true;
    }

    @Override
    public int getPriority() {
        return WAIT_PRIORITY;
    }

    @Override
    protected void resetStrategy() {
        
    }

    @Override
    public void onStart() {
        
    }

    @Override
    public boolean isOneShot() {
        return false;
    }

}
