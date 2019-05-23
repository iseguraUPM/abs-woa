/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent.strategy;

import es.upm.woa.group1.agent.CreateUnitRequestHandler;
import es.upm.woa.group1.agent.WoaAgent;
import es.upm.woa.group1.map.finder.LocationFinder;
import es.upm.woa.group1.map.MapCell;
import es.upm.woa.group1.map.PathfinderGameMap;
import es.upm.woa.group1.protocol.CommunicationStandard;

import jade.core.AID;
import jade.domain.FIPAAgentManagement.UnexpectedArgument;

import java.io.Serializable;
import es.upm.woa.group1.agent.CreateBuildingRequestHandler;
import es.upm.woa.group1.agent.ExploitResourceRequestHandler;
import es.upm.woa.group1.agent.ExplorationRequestHandler;

/**
 *
 * @author ISU
 */
public class StrategyFactory {
    
    private static final int FREE_EXPLORE = 0;
    private static final int CREATE_UNIT = 1;
    private static final int GOTO = 2;
    private static final int CREATE_BUILDING = 3;
    private static final int EXPLOIT_RESOURCE = 4;
    
    private WoaAgent woaAgent;
    private CommunicationStandard comStandard;
    private PathfinderGameMap graphKnownMap;
    private AID worldAID;
    
    private PositionedAgentUnit agentUnit;
    private LocationFinder locationFinder;
    
    private CreateBuildingRequestHandler constructionRequestHandler;
    private CreateUnitRequestHandler createUnitRequestHandler;
    private ExploitResourceRequestHandler exploitResourceRequestHandler;
    private ExplorationRequestHandler explorationRequestHandler;
    
    private StrategyFactory() {}
    
    public static StrategyFactory getInstance(WoaAgent woaAgent
            , CommunicationStandard comStandard
            , PathfinderGameMap graphKnownMap
            , AID worldAID, PositionedAgentUnit agentUnit
            , LocationFinder constructionSiteFinder
            , CreateBuildingRequestHandler constructionRequestHandler
            , CreateUnitRequestHandler createUnitRequestHandler
            , ExploitResourceRequestHandler exploitResourceRequestHandler
            , ExplorationRequestHandler explorationRequestHandler) {
        StrategyFactory instance = new StrategyFactory();
        instance.woaAgent = woaAgent;
        instance.comStandard = comStandard;
        instance.graphKnownMap = graphKnownMap;
        instance.worldAID = worldAID;
        instance.agentUnit = agentUnit;
        instance.locationFinder = constructionSiteFinder;
        
        instance.constructionRequestHandler = constructionRequestHandler;
        instance.createUnitRequestHandler = createUnitRequestHandler;
        instance.exploitResourceRequestHandler = exploitResourceRequestHandler;
        instance.explorationRequestHandler = explorationRequestHandler;
        
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
            case EXPLOIT_RESOURCE:
                return getExploitResourceStrategy(envelope);
            default:
                throw new UnexpectedArgument();
        }
    }
    
    private Strategy getFreeExploreStrategy(StrategyEnvelop envelope) {
        return new FreeExploreStrategy(envelope.getPriority()
                , woaAgent, comStandard, graphKnownMap
                , worldAID, agentUnit, explorationRequestHandler);
    }

    private Strategy getCreateUnitStrategy(StrategyEnvelop envelope) {
        return new CreateUnitStrategy(envelope.getPriority(), woaAgent
                , comStandard, graphKnownMap
                , worldAID, agentUnit, createUnitRequestHandler);
    }
    
    private Strategy getCreateBuildingStrategy(StrategyEnvelop envelope)
            throws UnexpectedArgument {
        if (envelope.getContent() instanceof CreateBuildingRequest) {
            CreateBuildingRequest request = (CreateBuildingRequest)
                    envelope.getContent();
            if (request.constructionSite != null) {
                return new CreateBuildingStrategy(
                        envelope.getPriority(), woaAgent, comStandard
                        , graphKnownMap, worldAID, agentUnit
                        , request.buildingType, constructionRequestHandler
                        , request.constructionSite);
            }
            else {
                return new CreateBuildingStrategy(envelope.getPriority()
                        , woaAgent, comStandard
                        , graphKnownMap, worldAID, agentUnit, request.buildingType
                        , constructionRequestHandler
                        , locationFinder);
            }
        }
        else {
            throw new UnexpectedArgument("Could not find create building argument");
        }
    }
    
    private Strategy getExploitResourceStrategy(StrategyEnvelop envelop) 
        throws UnexpectedArgument {
        if (envelop.getContent() instanceof ExploitResourceRequest) {
            ExploitResourceRequest request
                    = (ExploitResourceRequest) envelop.getContent();
            return new ExploitResourceStrategy(envelop.getPriority(), woaAgent
                    , comStandard, graphKnownMap, worldAID, agentUnit
                    , request.resourceType, exploitResourceRequestHandler
                    , locationFinder);
        }
        else {
            throw new UnexpectedArgument("Could not find exploit resource argument");
        }
    }
    
    private Strategy getGoToStrategy(StrategyEnvelop envelope)
            throws UnexpectedArgument {
        if (envelope.getContent() instanceof MapCell) {
            return new GoToStrategy(envelope.getPriority()
                    , woaAgent, comStandard, graphKnownMap
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
    
    /**
     * 
     * @param priority
     * @param buildingType can be Town Hall, Farm or Store
     * @param destination where to build
     * @return the strategy envelop to send
     */
    public static StrategyEnvelop envelopCreateBuildingStrategy(int priority
            , String buildingType, MapCell destination) {
        CreateBuildingRequest request = new CreateBuildingRequest();
        request.buildingType = buildingType;
        request.constructionSite = destination;
        
        return new Envelop(CREATE_BUILDING, priority, request);
    }
    
    /**
     * 
     * @param priority
     * @param buildingType can be Town Hall, Farm or Store
     * @return the strategy envelop to send
     */
    public static StrategyEnvelop envelopCreateBuildingStrategy(int priority
            , String buildingType) {
        return envelopCreateBuildingStrategy(priority, buildingType, null);
    }
    
    /**
     * 
     * @param priority
     * @param resourceType can be Farm, Ore or Forest
     * @return the strategy envelop to send
     */
    public static StrategyEnvelop envelopExploitResourceStrategy(int priority
            , String resourceType) {
        ExploitResourceRequest request = new ExploitResourceRequest();
        request.resourceType = resourceType;
        
        return new Envelop(EXPLOIT_RESOURCE, priority, request);
    }
    
    private static class CreateBuildingRequest implements Serializable {
        
        String buildingType;
        MapCell constructionSite;
        
    }
    
    private static class ExploitResourceRequest implements Serializable {
        String resourceType;
    }
    
    private static class Envelop implements StrategyEnvelop {
        
        private final int strategyCode;
        private final int priority;
        private final Serializable object;
        
        public Envelop(int s, int p) {
            strategyCode = s;
            priority = p;
            object = new Serializable() {};
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
