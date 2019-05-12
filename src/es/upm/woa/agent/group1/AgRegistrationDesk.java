/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.protocol.CommunicationStandard;
import es.upm.woa.agent.group1.protocol.Conversation;
import es.upm.woa.agent.group1.protocol.DelayTickBehaviour;
import es.upm.woa.agent.group1.protocol.WoaCommunicationStandard;
import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.RegisterTribe;

import jade.content.onto.basic.Action;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.util.Collection;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

/**
 *
 * @author Martin
 */
public class AgRegistrationDesk extends WoaAgent {
    public static final String REGISTRATION_DESK = "REGISTRATION_DESK";
    private static final int REGISTRATION_PERIOD_TICKS = 4;

    private CommunicationStandard woaComStandard;

    private WoaLogger logger;
    private final Collection<Tribe> registeredTribes;
    private final StartGameInformer startGameInformer;
    private final List<String> startingTribeNames;
    private final TribeResources initialTribeResources;
    
    final int MAX_TRIBES = 1;
    
    private boolean registrationOpen;
    
    public AgRegistrationDesk(List<String> startingTribeNames
            , TribeResources initialResources
            , Collection<Tribe> registeredTribes
            , StartGameInformer startGameInformer) {
        this.startingTribeNames = startingTribeNames;
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
                
        launchTribes();
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
 
    
    private void launchTribes(){
        
        for (int i = 0; i < MAX_TRIBES; i++) {
            String tribeName = startingTribeNames.get(i);
            launchAgentTribe(tribeName);
        }
    }
    
    private void launchAgentTribe(String tribeName) {
        try {
            ContainerController cc = getContainerController();
            AgTribe newTribe = new AgTribe();
            AgentController ac = cc.acceptNewAgent(tribeName, newTribe);
            ac.start();
        } catch (StaleProxyException ex) {
            log(Level.WARNING, "could not launch tribe " + tribeName + " (" + ex
                    + ")");
        }
    }
    
    /**
     * Start listening behaviour for RegistrationTribe agent requests.
     * Tribes who do not fill the following requirements will be refused:
     * - The tribe is already registered.
     * - Requests the registration out from the registration period
     */
    public void startTribeRegistrationBehaviour() {
        RegisterTribe registerTribe = new RegisterTribe();
        registerTribe.setTeamNumber(registeredTribes.size()+1);
        
        final Action  action = new Action(getAID(),registerTribe);
        
        this.addBehaviour(new Conversation(this, woaComStandard
               , action, GameOntology.REGISTERTRIBE) {
            @Override
            public void onStart() {
                Action action = new Action(getAID(), new RegisterTribe());

                listenMessages(new Conversation.ResponseHandler() {
                    @Override
                    public void onRequest(ACLMessage message) {
                        log(Level.FINE, "received Tribe request from"
                                + message.getSender().getLocalName());

                        if (registeredTribes.stream().anyMatch(
                                tribe -> message.getSender().equals(tribe.getAID())) || !registrationOpen) {
                            respondMessage(message, ACLMessage.REFUSE);
                        }else{
                            Tribe newTribe;
                            
                            try {
                                newTribe = new Tribe(message.getSender()
                                        , (TribeResources) initialTribeResources.clone());
                                registeredTribes.add(newTribe);
                            } catch (CloneNotSupportedException ex) {
                                log(Level.SEVERE, "Could not create new tribe ("
                                        + ex + ")");
                            }
                            
                            respondMessage(message, ACLMessage.AGREE);
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
