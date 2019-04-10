/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1.strategy;

import es.upm.woa.agent.group1.AgUnit;

import jade.core.behaviours.Behaviour;
import jade.core.behaviours.SerialBehaviour;
import jade.util.leap.Collection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author ISU
 */
public class StrategicUnitBehaviour extends SerialBehaviour {
 
    private final static int LOW_PRIORITY = 9999;
    
    private final List<Strategy> strategyList;
    
    public StrategicUnitBehaviour(AgUnit agentUnit) {
        super(agentUnit);
        strategyList = new ArrayList<>();
    }
    
    public void addStrategy(Strategy strategy) {
        strategyList.add(strategy);
    }

    @Override
    protected void scheduleFirst() {
        strategyList.sort(new StrategyComparator());
    }

    @Override
    protected void scheduleNext(boolean currentDone, int currentResult) {
        if (currentDone) {
            getCurrent().reset();
            strategyList.sort(new StrategyComparator());
        }
    }

    @Override
    protected boolean checkTermination(boolean currentDone, int currentResult) {
        return false;
    }

    @Override
    protected Behaviour getCurrent() {
        if (strategyList.isEmpty()) {
            strategyList.add(new WaitStrategy(myAgent));
        }
        
        return strategyList.get(0);
    }

    @Override
    public Collection getChildren() {
        Collection behaviours = new jade.util.leap.ArrayList();
        strategyList.stream().forEach(s -> behaviours.add(s));
        
        return behaviours;
    }
    
    
    
    private class StrategyComparator implements Comparator<Strategy> {

        @Override
        public int compare(Strategy t1, Strategy t2) {
            return t1.getPriority() - t2.getPriority();
        }
        
    }
    
}
