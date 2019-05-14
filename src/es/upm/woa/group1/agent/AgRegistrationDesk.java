/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import es.upm.woa.group1.protocol.CommunicationStandard;
import es.upm.woa.group1.protocol.Conversation;
import es.upm.woa.group1.protocol.DelayTickBehaviour;
import es.upm.woa.group1.protocol.WoaCommunicationStandard;
import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.RegisterTribe;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;

import jade.content.onto.basic.Action;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.Collection;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

/**
 *
 * @author Martin
 */
public class AgRegistrationDesk extends WoaAgent {
    public static final String REGISTRATION_DESK = "REGISTRATION DESK";
    private static final int REGISTRATION_PERIOD_TICKS = 4;

    private CommunicationStandard woaComStandard;

    private WoaLogger logger;
    private final Collection<Tribe> registeredTribes;
    private final StartGameInformer startGameInformer;
    private final TribeResources initialTribeResources;
    
    private boolean registrationOpen;
    
    public AgRegistrationDesk(TribeResources initialResources
            , Collection<Tribe> registeredTribes
            , StartGameInformer startGameInformer) {
        this.initialTribeResources = initialResources;
        this.registeredTribes = registeredTribes;
        this.startGameInformer = startGameInformer;
        this.registrationOpen = true;
    }
    
    @Override
    protected void setup() {       
        logger = new WoaLogger(getAID(), new ConsoleHandler());
        logger.setLevel(Level.ALL);
        
        woaComStandard = new WoaCommunicationStandard();
        woaComStandard.register(getContentManager());
  
        initializeAgent();
        
        startTribeRegistrationBehaviour();
        
        informWorldToStartGame();

    }
    
    private void initializeAgent() {
        try {
            // Creates its own description
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setName(this.getName());
            sd.setType(REGISTRATION_DESK);
            dfd.addServices(sd);
            // Registers its description in the DF
            DFService.register(this, dfd);
            log(Level.INFO, "registered in the DF");
        } catch (FIPAException ex) {
            log(Level.WARNING, "could not register in the DF (" + ex + ")");
        }
    }
    

    
    /**
     * Start listening behaviour for RegistrationTribe agent requests.
     * Tribes who do not fill the following requirements will be refused:
     * - The tribe is already registered.
     * - Requests the registration out from the registration period
     */
    public void startTribeRegistrationBehaviour() {
        this.addBehaviour(new Conversation(this, woaComStandard
               , GameOntology.REGISTERTRIBE) {
            @Override
            public void onStart() {

                listenMessages(new Conversation.ResponseHandler() {
                    @Override
                    public void onRequest(ACLMessage message) {
                        handleRequest(message);
                    }

                    private void handleRequest(ACLMessage message) {
                        try {
                            ContentElement content = getContentManager().extractContent(message);
                            Action action = (Action) content;
                            
                            RegisterTribe registerTribe = (RegisterTribe) action.getAction();
                            int tribeNumber = registerTribe.getTeamNumber();
                            
                            log(Level.FINE, "received Tribe request from"
                                    + message.getSender().getLocalName());
                            
                            if (!canRegister(message, tribeNumber)) {
                                respondMessage(message, ACLMessage.REFUSE, action);
                            } else {
                                Tribe newTribe;
                                
                                try {
                                    newTribe = new Tribe(tribeNumber, message.getSender()
                                            , (TribeResources) initialTribeResources.clone());
                                    registeredTribes.add(newTribe);
                                    log(Level.INFO, "Registered tribe number "
                                            + newTribe.getTribeNumber());
                                } catch (CloneNotSupportedException ex) {
                                    log(Level.SEVERE, "Could not create new tribe ("
                                            + ex + ")");
                                }
                                
                                respondMessage(message, ACLMessage.AGREE, action);
                            }
                        } catch (Codec.CodecException | OntologyException | ClassCastException ex) {
                            log(Level.WARNING, "Could not extract content from RegisterTribe message");
                        }
                    }
                    
                    @Override
                    public void onNotUnderstood(ACLMessage response) {
                        log(Level.WARNING, "received RegisterTribe not understood from "
                                + response.getSender().getLocalName());
                    }
                });
            }
        });
    }

    private boolean canRegister(ACLMessage message, int teamNumber) {
        if (!registrationOpen)
            return false;
        
        if (registeredTribes.stream().anyMatch(
                tribe -> {
                    return message.getSender().equals(tribe.getAID());
                }))
        {
            log(Level.WARNING, "Tribe " + message.getSender().getLocalName() + " was already registered");
            return false;
        }
        
        if (registeredTribes.stream().anyMatch(
                tribe -> {
                    return tribe.getTribeNumber() == teamNumber;
                }))
        {
            log(Level.WARNING, "Tribe number " + teamNumber + " was already registered");
            return false;
        }
        
        return true;
    }
    
    private void informWorldToStartGame(){
        DelayTickBehaviour behaviour = new DelayTickBehaviour(this, REGISTRATION_PERIOD_TICKS) {
            
            @Override
            protected final void handleElapsedTimeout() {
                registrationOpen = false;
                startGameInformer.startGame();
            }
            
        };
        addBehaviour(behaviour);
    }
    
    @Override
    public void log(Level logLevel, String message) {
        logger.log(logLevel, message);

    }    
}
