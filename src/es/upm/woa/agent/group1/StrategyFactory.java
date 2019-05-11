/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.protocol.CommunicationStandard;
import es.upm.woa.agent.group1.strategy.Strategy;

import jade.core.AID;
import jade.domain.FIPAAgentManagement.UnexpectedArgument;

import java.io.Serializable;

/**
 *
 * @author ISU
 */
class StrategyFactory {
    
    private static final int FREE_EXPLORE = 0;
    private static final int CREATE_UNIT = 1;
    private static final int GOTO = 2;
    private static final int CREATE_BUILDING = 3;
    
    private WoaAgent woaAgent;
    private CommunicationStandard comStandard;
    private GraphGameMap graphKnownMap;
    private AID worldAID;
    
    private PositionedAgentUnit agentUnit;
    private MapCellFinder constructionSiteFinder;
    
    private StrategyFactory() {}
    
    public static StrategyFactory getInstance(WoaAgent woaAgent
            , CommunicationStandard comStandard, GraphGameMap graphKnownMap
            , AID worldAID, PositionedAgentUnit agentUnit
            , MapCellFinder constructionSiteFinder) {
        StrategyFactory instance = new StrategyFactory();
        instance.woaAgent = woaAgent;
        instance.comStandard = comStandard;
        instance.graphKnownMap = graphKnownMap;
        instance.worldAID = worldAID;
        instance.agentUnit = agentUnit;
        instance.constructionSiteFinder = constructionSiteFinder;
        
        return instance;
    }
    
    /**
     * 
     * @param envelope
     * @return strategy defined by envelope
     * @throws UnexpectedArgument if the envelope defines an incorrect
     * strategy
     */
    public Strategy getStrategy(StrategyEnvelop envelope)
            throws UnexpectedArgument {
        switch (envelope.getStrategy()) {
            case FREE_EXPLORE:
                return getFreeExploreStrategy(envelope);
            case CREATE_UNIT:
                return getCreateUnitStrategy(envelope);
            case GOTO:
                return getGoToStrategy(envelope);
            case CREATE_BUILDING:
                return getCreateBuildingStrategy(envelope);
            default:
                throw new UnexpectedArgument();
        }
    }
    
    private Strategy getFreeExploreStrategy(StrategyEnvelop envelope) {
        return new FreeExploreStrategy(woaAgent, comStandard, graphKnownMap
                , worldAID, agentUnit);
    }

    private Strategy getCreateUnitStrategy(StrategyEnvelop envelope) {
        return new CreateUnitStrategy(woaAgent, comStandard, graphKnownMap
                , worldAID, agentUnit);
    }
    
    private Strategy getCreateBuildingStrategy(StrategyEnvelop envelope)
            throws UnexpectedArgument {
        if (envelope.getContent() instanceof CreateBuildingRequest) {
            CreateBuildingRequest request = (CreateBuildingRequest)
                    envelope.getContent();
            if (request.constructionSite != null) {
                return new CreateBuildingStrategy(woaAgent, comStandard
                        , graphKnownMap, worldAID, agentUnit
                        , request.buildingType, request.constructionSite);
            }
            else {
                return new CreateBuildingStrategy(woaAgent, comStandard
                        , graphKnownMap, worldAID, agentUnit, request.buildingType
                        , constructionSiteFinder);
            }
        }
        else {
            throw new UnexpectedArgument("Could not find map cell argument");
        }
    }
    
    private Strategy getGoToStrategy(StrategyEnvelop envelope)
            throws UnexpectedArgument {
        if (envelope.getContent() instanceof MapCell) {
            return new GoToStrategy(woaAgent, comStandard, graphKnownMap
                    , worldAID, (MapCell) envelope.getContent(), agentUnit);
        }
        else {
            throw new UnexpectedArgument("Could not find map cell argument");  
        }
    }
    
    public static StrategyEnvelop envelopFreeExploreStrategy(int priority) {
        return new Envelop(FREE_EXPLORE, priority);
    }
    
    public static StrategyEnvelop envelopCreateUnitStrategy(int priority) {
        return new Envelop(CREATE_UNIT, priority);
    }
    
    public static StrategyEnvelop envelopGoToStrategy(int priority
            , MapCell destination) {
        return new Envelop(GOTO, priority, destination);
    }
    
    public static StrategyEnvelop envelopCreateBuildingStrategy(int priority
            , String buildingType, MapCell destination) {
        CreateBuildingRequest request = new CreateBuildingRequest();
        request.buildingType = buildingType;
        request.constructionSite = destination;
        
        return new Envelop(CREATE_BUILDING, priority, request);
    }
    
    public static StrategyEnvelop envelopCreateBuildingStrategy(int priority
            , String buildingType) {
        return envelopCreateBuildingStrategy(priority, buildingType, null);
    }
    
    private static class CreateBuildingRequest implements Serializable {
        
        String buildingType;
        MapCell constructionSite;
        
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
