package es.upm.woa.agent.group1;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import es.upm.woa.agent.group1.map.GraphGameMap;
import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.ontology.Group1Ontology;
import es.upm.woa.agent.group1.ontology.NotifyUnitOwnership;
import es.upm.woa.agent.group1.ontology.WhereAmI;
import es.upm.woa.agent.group1.protocol.Conversation;

import es.upm.woa.ontology.Cell;
import es.upm.woa.ontology.CreateUnit;
import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.MoveToCell;
import es.upm.woa.ontology.NotifyNewCellDiscovery;

import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import java.security.acl.Group;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;

import java.util.logging.Level;
import java.util.logging.LogRecord;


/**
 *
 * @author ISU
 */
public class AgUnit extends Agent {

    public static final String WORLD = "WORLD";
    
    private GraphGameMap knownMap;
    private Ontology gameOntology;
    private Ontology group1Ontology;
    private SLCodec codec;
    private DFAgentDescription worldAgentServiceDescription;
    private MapCell currentPosition;
    private AID ownerTribe;
    
    private Handler logHandler;

    /// NOTE: this methods must be package-private
    GraphGameMap getKnownMap() {
        return knownMap;
    }
    
    Ontology getOntology() {
        return gameOntology;
    }
    
    Codec getCodec() {
        return codec;
    }
    
    AID getWorldAID() {
        return worldAgentServiceDescription.getName();
    }
    
    MapCell currentPosition() {
        return currentPosition;
    }
    
    AID getOwnerAID() {
        return ownerTribe;
    }
    
    /// !NOTE

    @Override
    protected void setup() {
        logHandler = new ConsoleHandler();
        logHandler.setLevel(Level.FINE);
        
        initializeAgent();
        initializeUnit();
        
        if (worldAgentServiceDescription == null) {
            return;
        }
        
        startInformOwnershipBehaviour();
        //startCreateUnitBehaviour();
        //startMoveToCellBehaviour();
        //startInformNewCellDiscoveryBehaviour();
    }

    private void initializeAgent() {
        //Finds the World in the DF
        try{
            DFAgentDescription dfdWorld = new DFAgentDescription();
            ServiceDescription sdWorld = new ServiceDescription();
            sdWorld.setType(WORLD);
            dfdWorld.addServices(sdWorld);
            // It finds agents of the required type
            DFAgentDescription [] descriptions = DFService.search(this, dfdWorld);
            if (descriptions.length == 0) {
                //TODO what if the world is not found
            }
            else {
                worldAgentServiceDescription = descriptions[0];
            }
        } catch (FIPAException ex) {
            log(Level.WARNING, " the WORLD agent was not found (" + ex + ")");
        }  
    }

    private void initializeUnit() {
        gameOntology = GameOntology.getInstance();
        group1Ontology = Group1Ontology.getInstance();
        codec = new SLCodec();
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(gameOntology);
        getContentManager().registerOntology(group1Ontology);
    }
    
    private void startCreateUnitBehaviour() {
        Action createUnitAction = new Action(getAID(), new CreateUnit());
        addBehaviour(new Conversation(this, gameOntology, codec, createUnitAction, GameOntology.CREATEUNIT) {
            @Override
            public void onStart() {
                AID worldAID = (AID) worldAgentServiceDescription.getName();

                sendMessage(worldAID, ACLMessage.REQUEST
                        , new Conversation.SentMessageHandler() {

                    @Override
                    public void onSent(String conversationID) {

                        receiveResponse(conversationID, new Conversation.ResponseHandler() {

                            @Override
                            public void onAgree(ACLMessage response) {
                                log(Level.FINER, "received CreateUnit agree from "
                                        + response.getSender().getLocalName());

                                receiveResponse(conversationID, new Conversation.ResponseHandler() {

                                    @Override
                                    public void onFailure(ACLMessage response) {
                                        log(Level.WARNING, "received CreateUnit failure from "
                                        + response.getSender().getLocalName());
                                    }

                                    @Override
                                    public void onInform(ACLMessage response) {
                                        log(Level.FINER, "received CreateUnit inform from "
                                        + response.getSender().getLocalName());
                                    }

                                });
                            }

                            @Override
                            public void onNotUnderstood(ACLMessage response) {
                                log(Level.WARNING, "received CreateUnit not understood from "
                                        + response.getSender().getLocalName());
                            }

                            @Override
                            public void onRefuse(ACLMessage response) {
                                log(Level.FINE, "receive CreateUnit refuse from "
                                        + response.getSender().getLocalName());
                            }

                        });
                    }
                });

            }
            
        });
    }
    
    private void startMoveToCellBehaviour() {
        
        Cell newCellPosition = new Cell();
        //TODO by default 0,0
        newCellPosition.setX(2);
        newCellPosition.setY(2);
        
        MoveToCell moveToCell = new MoveToCell();
        moveToCell.setTarget(newCellPosition);
        
        Action moveToCellAction = new Action(getAID(), moveToCell);
        addBehaviour(new Conversation(this, gameOntology, codec, moveToCellAction, GameOntology.MOVETOCELL){
            

            @Override
            public void onStart() {
                
                AID worldAID = (AID) worldAgentServiceDescription.getName();

                sendMessage(worldAID, ACLMessage.REQUEST
                        , new Conversation.SentMessageHandler() {
                    @Override
                    public void onSent(String conversationID) {
                        log(Level.FINE,  "wants to move to cell "
                                + newCellPosition.getX() + "," +newCellPosition.getY());

                        receiveResponse(conversationID, new Conversation.ResponseHandler() {

                            @Override
                            public void onAgree(ACLMessage response) {
                                log(Level.FINER, "receive MoveToCell agree from "
                                        + response.getSender().getLocalName());

                                receiveResponse(conversationID, new Conversation.ResponseHandler() {

                                    @Override
                                    public void onFailure(ACLMessage response) {
                                        log(Level.WARNING, "receive MoveToCell failure from "
                                        + response.getSender().getLocalName());
                                    }

                                    @Override
                                    public void onInform(ACLMessage response) {
                                        
                                        log(Level.FINER, "receive MoveToCell inform from "
                                        + response.getSender().getLocalName());
                                        
                                        log(Level.FINE,  "moved to cell "
                                                + newCellPosition.getX() + ","
                                                +newCellPosition.getY());
                                    }

                                });
                            }

                            @Override
                            public void onNotUnderstood(ACLMessage response) {
                                log(Level.WARNING, "receive MoveToCell not understood from "
                                        + response.getSender().getLocalName());
                            }

                            @Override
                            public void onRefuse(ACLMessage response) {
                                log(Level.FINE, "receive MoveToCell refuse from "
                                        + response.getSender().getLocalName());
                            }

                        });
                    }
                });        
            }
        });
    }
    
    private void startInformNewCellDiscoveryBehaviour() {
        // Behaviors
        Action informNewCellDiscoveryAction = new Action(getAID(), new NotifyNewCellDiscovery());
        addBehaviour(new Conversation(this, gameOntology, codec, informNewCellDiscoveryAction, GameOntology.NOTIFYNEWCELLDISCOVERY) {
            @Override
            public void onStart() {
                listenMessages(new ResponseHandler() {
                    @Override
                    public void onInform(ACLMessage response) {
                        try {
                            ContentElement ce = getContentManager().extractContent(response);
                            if (ce instanceof Action) {
                                
                                Action agAction = (Action) ce;
                                Concept conc = agAction.getAction();
                                
                                if (conc instanceof NotifyNewCellDiscovery) {
                                    log(Level.FINER, "receive NotifyNewCellDiscovery inform from "
                                        + response.getSender().getLocalName());
                                    
                                    NotifyNewCellDiscovery newCellInfo = (NotifyNewCellDiscovery)conc;
                                    
                                    log(Level.FINER, "cell discovery at "
                                            + newCellInfo.getNewCell().getX()
                                            + ","
                                            + newCellInfo.getNewCell().getY());
                                    
                                }
                            }
                        } catch (Codec.CodecException | OntologyException ex) {
                            log(Level.WARNING, "could not receive message"
                                    + " (" + ex + ")");
                        }

                    }

                });
            }
        });
    }
    
    private void requestUnitPosition() {
        final Action whereAmIAction = new Action(getAID(), new WhereAmI());
        addBehaviour(new Conversation(this, group1Ontology, codec, whereAmIAction, Group1Ontology.WHEREAMI) {
            @Override
            public void onStart() {
                sendMessage(new AID(), ACLMessage.REQUEST, new SentMessageHandler() {
                    // TODO: complete
                });
            }
        });
    }
    
    private void startInformOwnershipBehaviour() {
        Action informOwnershipAction = new Action(getAID(), new NotifyUnitOwnership());
        addBehaviour(new Conversation(this, group1Ontology, codec, informOwnershipAction
                , Group1Ontology.NOTIFYUNITOWNERSHIP) {
            @Override
            public void onStart() {
                listenMessages(new ResponseHandler() {
                    @Override
                    public void onInform(ACLMessage response) {
                        log(Level.FINE, "Registered owner tribe: "
                                + response.getSender().getLocalName());
                        ownerTribe = response.getSender();
                    }
                });
            }
        });
    }
    
    void log(Level logLevel, String message) {
        String compMsg = getLocalName() + ": " + message;
        if (logHandler.isLoggable(new LogRecord(logLevel, compMsg))) {
            System.out.println(compMsg);
        }
    }
    
}
