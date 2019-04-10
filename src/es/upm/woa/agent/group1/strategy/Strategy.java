/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1.strategy;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;

/**
 *
 * @author ISU
 */
public abstract class Strategy extends Behaviour {
    
    public static final int LOW_PRIORITY = 0;

    protected StrategyEventDispatcher eventDispatcher; 
    
    public Strategy(Agent agent, StrategyEventDispatcher eventDispatcher) {
        super(agent);
        this.eventDispatcher = eventDispatcher;
    }
    
    public abstract int getPriority();
    
    public abstract void onEvent(StrategyEvent event);
    
    @Override
    public abstract void onStart();
    
    @Override
    public abstract int onEnd();
    
    @Override
    public abstract boolean done();
    
    @Override
    public void reset() {
        super.reset();
    }
    
}
