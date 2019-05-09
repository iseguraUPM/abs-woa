/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import static es.upm.woa.agent.group1.AgWorld.WORLD;
import es.upm.woa.agent.group1.protocol.CommunicationStandard;
import es.upm.woa.agent.group1.protocol.Conversation;
import es.upm.woa.agent.group1.protocol.DelayTickBehaviour;
import es.upm.woa.agent.group1.protocol.Group1CommunicationStandard;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 *
 * @author Martin
 */
public class AgRegistrationDesk extends WoaAgent {
    public static final String REGISTRATION_DESK = "REGISTRATION_DESK";
    private static final int REGISTRATION_PERIOD_TICKS = 100;

    private CommunicationStandard woaComStandard;

    private WoaLogger logger;
    private WoaAgent woaAgent;
    private Collection<Tribe> registeredTribes;
    private StartGameInformer startGameInformer;
    private boolean registrationOpen;
    
    // TODO: temporal solutions before registration
    final int MAX_TRIBES = 1;
    private List<String> startingTribeNames;
    
    public AgRegistrationDesk(List<String> startingTribeNames, Collection<Tribe> registeredTribes, StartGameInformer startGameInformer){
        this.startingTribeNames = startingTribeNames;
        this.registeredTribes = registeredTribes;
        this.startGameInformer = startGameInformer;
        this.registrationOpen = true;
    }
    
    @Override
    protected void setup() {       
        logger = new WoaLogger(getAID(), new ConsoleHandler());
        logger.setLevel(Level.ALL);

        woaAgent = this;
        registeredTribes = new HashSet<>();
        
        woaComStandard = new WoaCommunicationStandard();
        woaComStandard.register(getContentManager());
  
        initializeAgent();
        
        startTribeRegistrationBehaviour();
        
        informWorldToStartGame();
                
        // TODO: temporal solutions before registration
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
 
    
    // TODO: temporal solutions before registration
    private void launchTribes(){
        startingTribeNames = new ArrayList<>();
        startingTribeNames.add("TribeA");
        startingTribeNames.add("TribeB");
        startingTribeNames.add("TribeC");
        startingTribeNames.add("TribeD");
        startingTribeNames.add("TribeE");
        startingTribeNames.add("TribeF");
        
        for (int i = 0; i < MAX_TRIBES; i++) {
            String tribeName = startingTribeNames.get(i);
            launchAgentTribe(tribeName);
        }
    }
    
    // TODO: temporal solutions before registration
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
        
        final Action  action= new Action(woaAgent.getAID(),registerTribe);
        
        this.addBehaviour(new Conversation(this, woaComStandard
               , action, GameOntology.REGISTERTRIBE) {
            @Override
            public void onStart() {
                Action action = new Action(woaAgent.getAID(), new RegisterTribe());

                listenMessages(new Conversation.ResponseHandler() {
                    @Override
                    public void onRequest(ACLMessage message) {
                        woaAgent.log(Level.FINE, "received Tribe request from"
                                + message.getSender().getLocalName());

                        if (registeredTribes.stream().anyMatch(
                                tribe -> message.getSender().equals(tribe.getAID())) || !registrationOpen) {
                            respondMessage(message, ACLMessage.REFUSE);
                            return;
                        }else{
                            Tribe newTribe = new Tribe(message.getSender());
                            registeredTribes.add(newTribe);
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
        new DelayTickBehaviour(this, REGISTRATION_PERIOD_TICKS) {
            
            @Override
            protected final void handleElapsedTimeout() {
                registrationOpen = false;
                startGameInformer.startGame();
            }
            
        };
    }
    
    @Override
    public void log(Level logLevel, String message) {
        logger.log(logLevel, message);

    }
    
}
