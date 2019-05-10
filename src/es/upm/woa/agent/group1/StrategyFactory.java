/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.protocol.CommunicationStandard;
import es.upm.woa.agent.group1.strategy.Strategy;
import jade.core.AID;
import java.io.Serializable;
import java.util.NoSuchElementException;

/**
 *
 * @author ISU
 */
class StrategyFactory {
    
    private static final int FREE_EXPLORE = 0;
    private static final int CREATE_UNIT = 1;
    private static final int CREATE_BUILDING = 2;
    
    private WoaAgent woaAgent;
    private CommunicationStandard comStandard;
    private GraphGameMap graphKnownMap;
    private AID worldAID;
    
    private PositionedAgentUnit agentUnit;
    
    private StrategyFactory() {}
    
    public static StrategyFactory getInstance(WoaAgent woaAgent
            , CommunicationStandard comStandard, GraphGameMap graphKnownMap
            , AID worldAID, PositionedAgentUnit agentUnit) {
        StrategyFactory instance = new StrategyFactory();
        instance.woaAgent = woaAgent;
        instance.comStandard = comStandard;
        instance.graphKnownMap = graphKnownMap;
        instance.worldAID = worldAID;
        instance.agentUnit = agentUnit;
        
        return instance;
    }
    
    /**
     * 
     * @param envelope
     * @return strategy defined by envelope
     * @throws NoSuchElementException if the envelope defines an incorrect
     * strategy
     */
    public Strategy getStrategy(StrategyEnvelop envelope) {
        switch (envelope.getStrategy()) {
            case FREE_EXPLORE:
                return createFreeExploreStrategy(envelope);
            case CREATE_UNIT:
                return createCreateUnitStrategy(envelope);
            default:
                throw new NoSuchElementException();
        }
    }
    
    private Strategy createFreeExploreStrategy(StrategyEnvelop envelope) {
        return new FreeExploreStrategy(woaAgent, comStandard, graphKnownMap
                , worldAID, agentUnit);
    }

    private Strategy createCreateUnitStrategy(StrategyEnvelop envelope) {
        return new CreateUnitStrategy(woaAgent, comStandard, graphKnownMap
                , worldAID, agentUnit);
    }
    
    public static StrategyEnvelop envelopFreeExploreStrategy(int priority) {
        return new Envelop(FREE_EXPLORE, priority);
    }
    
    public static StrategyEnvelop envelopCreateUnitStrategy(int priority) {
        return new Envelop(CREATE_UNIT, priority);
    }
    
    private static class Envelop implements StrategyEnvelop {
        
        private final int strategyCode;
        private final int priority;
        private final Serializable object;
        
        public Envelop(int s, int p) {
            strategyCode = s;
            priority = p;
            object = null;
        }

        public Envelop(int s, int p, Serializable o) {
            strategyCode = s;
            priority = p;
            object = o;
        }

        @Override
        public int getStrategy() {
            return strategyCode;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public Serializable getContent() {
            return object;
        }
        
    }
    
}
