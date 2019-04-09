package es.upm.woa.agent.group1;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import es.upm.woa.agent.group1.map.GameMap;
import es.upm.woa.agent.group1.map.MapCellFactory;
import es.upm.woa.agent.group1.protocol.Conversation;

import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.NotifyNewCellDiscovery;
import es.upm.woa.ontology.NotifyNewUnit;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;

import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ISU
 */
public class AgTribe extends Agent {

    private Ontology ontology;
    private Codec codec;
    private Collection<Unit> units;
    private GameMap knownMap;

    @Override
    protected void setup() {
        
        initializeAgent();
        initializeTribe();
        
        startInformNewUnitBehaviour();
        startInformNewCellDiscoveryBehaviour();
    }

    private void startInformNewUnitBehaviour() {
        // Behaviors
        Action informNewUnitAction = new Action(getAID(), new NotifyNewUnit());
        addBehaviour(new Conversation(this, ontology, codec, informNewUnitAction, "NotifyNewUnit") {
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
                                
                                if (conc instanceof NotifyNewUnit) {
                                    log(Level.FINE, "receive NotifyNewUnit inform from "
                                        + response.getSender().getLocalName());
                                    NotifyNewUnit newUnitInfo = (NotifyNewUnit) conc;
                                    log(Level.FINE, "new unit created at " + 
                                            + newUnitInfo.getLocation().getX()
                                            + ", "
                                            + newUnitInfo.getLocation().getY());
                                    addUnit(newUnitInfo);
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
    
    
    private void startInformNewCellDiscoveryBehaviour() {
        // Behaviors
        Action informNewCellDiscoveryAction = new Action(getAID(), new NotifyNewCellDiscovery());
        addBehaviour(new Conversation(this, ontology, codec, informNewCellDiscoveryAction, "NotifyNewCellDiscovery") {
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
                                    log(Level.FINE, "receive NotifyNewCellDiscovery inform from "
                                        + response.getSender().getLocalName());
                                    NotifyNewCellDiscovery newCellInfo = (NotifyNewCellDiscovery)conc;
                                    
                                    processNewCell(newCellInfo);
                                    
                                }
                            }
                        } catch (Codec.CodecException | OntologyException ex) {
                            log(Level.WARNING, "could not receive message (" + ex + ")");
                        }

                    }

                    private void processNewCell(NotifyNewCellDiscovery newCellInfo) {
                        boolean cellAdded = knownMap.addCell(MapCellFactory
                                .getInstance().buildCell(newCellInfo.getNewCell()));
                        if (cellAdded) {
                            log(Level.FINER, "cell discovery at "
                                    + newCellInfo.getNewCell().getX()
                                    + ", "
                                    + newCellInfo.getNewCell().getY());
                        }
                    }

                });
            }
        });
    }

    private void initializeAgent() {
        
    }

    private void initializeTribe() {
        ontology = GameOntology.getInstance();
        codec = new SLCodec();
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);

        units = new HashSet<>();
        knownMap = new TribeMap();
    }

    public Ontology getOntology() {
        return ontology;
    }

    public Codec getCodec() {
        return codec;
    }

    public void addUnit(NotifyNewUnit newUnitInfo) {
        units.add(new Unit(newUnitInfo.getNewUnit(), newUnitInfo.getLocation().getX(), newUnitInfo.getLocation().getY()));
    }
    
    public GameMap getKnownMap() {        
        return knownMap;
    }
    
    private void log(Level logLevel, String message) {
        String compMsg = getLocalName() + ": " + message;
        Logger.getLogger("WOAGROUP1").log(logLevel, compMsg);
    }

}
