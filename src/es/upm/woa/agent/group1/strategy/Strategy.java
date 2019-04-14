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
public abstract class Strategy extends Behaviour implements StrategyEventListener {
    
    public static final int HIGH_PRIORITY = 0;
    public static final int MID_PRIORITY = 50;
    public static final int LOW_PRIORITY = 100;

    protected StrategyEventDispatcher eventDispatcher; 
    
    public Strategy(Agent agent, StrategyEventDispatcher eventDispatcher) {
        super(agent);
        this.eventDispatcher = eventDispatcher;
    }
    
    public abstract int getPriority();
    
    @Override
    public abstract void onStart();
    
    @Override
    public final int onEnd() { return 0; }
    
    @Override
    public abstract boolean done();
    
    @Override
    public final void reset() {
        super.reset();
        resetStrategy();
    }
    
    protected abstract void resetStrategy();
    
}
