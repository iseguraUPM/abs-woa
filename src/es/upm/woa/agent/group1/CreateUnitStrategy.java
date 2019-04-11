/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.map.GameMap;
import es.upm.woa.agent.group1.map.GraphGameMap;
import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.protocol.Conversation;
import es.upm.woa.agent.group1.strategy.Strategy;
import es.upm.woa.agent.group1.strategy.StrategyEvent;
import es.upm.woa.agent.group1.strategy.UnitEventDispatcher;
import es.upm.woa.ontology.Building;
import es.upm.woa.ontology.CreateUnit;
import es.upm.woa.ontology.GameOntology;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.function.Predicate;
import java.util.logging.Level;

/**
 *
 * @author ISU
 */
public class CreateUnitStrategy extends Strategy {
    
    private int priority;
    
    public CreateUnitStrategy(AgUnit agentUnit, UnitEventDispatcher eventDispatcher) {
        super(agentUnit, eventDispatcher);
    }

    @Override
    public int getPriority() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onEvent(StrategyEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void action() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean done() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onStart() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int onEnd() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void reset() {
        super.reset();
    }
    
    private void moveToNearestTownHall() {
        
    }
    
    private MapCell findNearestTownHall(MapCell source) {
        GraphGameMap knownMap = ((AgUnit) myAgent).getKnownMap();
        
        knownMap.findShortestPathTo(source, new Predicate<MapCell>() {
            @Override
            public boolean test(MapCell t) {
                if (t.getContent() instanceof Building) {
                    Building building = (Building) t.getContent();
                    
                }
                
                return false;
            }
        });
        
        return null;
    }
    
    private void startCreateUnitBehaviour() {
        AgUnit myAgUnit = (AgUnit) myAgent;
        
        Action createUnitAction = new Action(myAgUnit.getAID(), new CreateUnit());
        myAgUnit.addBehaviour(new Conversation(myAgUnit, myAgUnit.getOntology()
                , myAgUnit.getCodec(), createUnitAction, GameOntology.CREATEUNIT) {
            @Override
            public void onStart() {
                AID worldAID = myAgUnit.getWorldAID();

                sendMessage(worldAID, ACLMessage.REQUEST
                        , new Conversation.SentMessageHandler() {

                    @Override
                    public void onSent(String conversationID) {

                        receiveResponse(conversationID, new Conversation.ResponseHandler() {

                            @Override
                            public void onAgree(ACLMessage response) {
                                myAgUnit.log(Level.FINE, "received CreateUnit agree from "
                                        + response.getSender().getLocalName());

                                receiveResponse(conversationID, new Conversation.ResponseHandler() {

                                    @Override
                                    public void onFailure(ACLMessage response) {
                                        myAgUnit.log(Level.WARNING, "received CreateUnit failure from "
                                        + response.getSender().getLocalName());
                                    }

                                    @Override
                                    public void onInform(ACLMessage response) {
                                        myAgUnit.log(Level.FINE, "received CreateUnit inform from "
                                        + response.getSender().getLocalName());
                                    }

                                });
                            }

                            @Override
                            public void onNotUnderstood(ACLMessage response) {
                                myAgUnit.log(Level.WARNING, "received CreateUnit not understood from "
                                        + response.getSender().getLocalName());
                            }

                            @Override
                            public void onRefuse(ACLMessage response) {
                                myAgUnit.log(Level.FINE, "receive CreateUnit refuse from "
                                        + response.getSender().getLocalName());
                            }

                        });
                    }
                });

            }
            
        });
    }
}
