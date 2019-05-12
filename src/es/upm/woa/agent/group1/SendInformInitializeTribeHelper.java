/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.map.GameMap;
import es.upm.woa.agent.group1.map.MapCell;
import es.upm.woa.agent.group1.map.WorldMapConfigurator;
import es.upm.woa.agent.group1.protocol.CommunicationStandard;
import es.upm.woa.agent.group1.protocol.Conversation;
import es.upm.woa.ontology.Cell;
import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.InitalizeTribe;
import es.upm.woa.ontology.ResourceAccount;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.List;
import org.apache.commons.configuration2.ex.ConfigurationException;

/**
 *
 * @author juanpamz
 */
public class SendInformInitializeTribeHelper {
    private final AgWorld groupAgent;
    private final CommunicationStandard comStandard;
    private final AID tribeAID;
    private final WorldMapConfigurator configurator;
    private final List<Unit> unitList;
    private final MapCell initialMapCell;
    
    
    
    public SendInformInitializeTribeHelper(AgWorld groupAgent
            , CommunicationStandard comStandard, AID tribeAID, WorldMapConfigurator configurator, List<Unit> unitList, MapCell initialMapCell) {
        this.groupAgent = groupAgent;
        this.comStandard = comStandard;
        this.tribeAID = tribeAID;
        this.configurator = configurator;
        this.unitList = unitList;
        this.initialMapCell = initialMapCell;
    }
    
    /**
     * Sends an inform with the initial resources
     * to every tribe that has been registered
     */
    public void initializeTribe() throws ConfigurationException {
        InitalizeTribe initializeTribe = new InitalizeTribe();
        
        TribeResources readInitialResources = configurator.getInitialResources();
        ResourceAccount resourceAccount = new ResourceAccount();
        resourceAccount.setFood(readInitialResources.getFood());
        resourceAccount.setGold(readInitialResources.getGold());
        resourceAccount.setStone(readInitialResources.getStone());
        resourceAccount.setWood(readInitialResources.getWood());

        for(Unit u : unitList){
            initializeTribe.addUnitList(u.getId());
        }
        
        Cell startingCell = new Cell();
        startingCell.setContent(initialMapCell.getContent());
        startingCell.setX(initialMapCell.getXCoord());
        startingCell.setY(initialMapCell.getYCoord());
        initializeTribe.setStartingPosition(startingCell);
        
        initializeTribe.setStartingResources(resourceAccount);


        Action initializeTribeAction = new Action(groupAgent.getAID(), initializeTribe);

        groupAgent.addBehaviour(new Conversation(groupAgent, comStandard, initializeTribeAction, GameOntology.INITALIZETRIBE) {
            @Override
            public void onStart() {
                sendMessage(tribeAID, ACLMessage.INFORM, new Conversation.SentMessageHandler() {
                });
            }

        });
        
        
    }
}
