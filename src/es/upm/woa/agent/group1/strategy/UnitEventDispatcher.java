/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1.strategy;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author ISU
 */
public class UnitEventDispatcher implements StrategyEventDispatcher {
    
    private final Collection<Strategy> subscribers;
    
    public UnitEventDispatcher() {
        subscribers = new ArrayList<>();
    }
    
    @Override
    public void subscribe(Strategy subscriber) {
        subscribers.add(subscriber);
    }
    
    @Override
    public void dispatch(StrategyEvent event) {
        subscribers.forEach(subscriber -> subscriber.onEvent(event));
    }
    
}
