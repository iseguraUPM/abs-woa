/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent.strategy;

import es.upm.woa.group1.map.MapCell;
import jade.core.AID;
import jade.core.Agent;
import java.io.Serializable;

/**
 *
 * @author ISU
 */
public class UnitStatusMessageFactory {
    
    private static final int CHANGED_POS = 0;
    private static final int START_BUILDING = 1;
    private static final int FINISH_BUILDING = 2;
    private static final int START_UNIT = 3;
    private static final int FINISH_UNIT = 4;
    private static final int EXPLOIT_RESOURCE = 5;
    private static final int FINISH_EXPLOIT = 6;
    private static final int FINISH_EXPLORATION = 7;
    
    private Agent agent;
    
    private UnitStatusMessageFactory() {}
    
    public static UnitStatusMessageFactory getInstance(Agent agent) {
        UnitStatusMessageFactory instance = new UnitStatusMessageFactory();
        instance.agent = agent;
        
        return instance;
    }
    
    public UnitStatusMessageEnvelop envelopChangedPosition(MapCell newPosition) {
        MessageContent messageContent = new MessageContent();
        messageContent.unitAID = agent.getAID();
        messageContent.unitPositionX = newPosition.getXCoord();
        messageContent.unitPositionY = newPosition.getYCoord();
        
        return new UnitStatusMessage(CHANGED_POS, messageContent);
    }
    
    public UnitStatusMessageEnvelop envelopStartedBuilding(String buildingType) {
        MessageContent messageContent = new MessageContent();
        messageContent.unitAID = agent.getAID();
        messageContent.buildingType = buildingType;
        
        return new UnitStatusMessage(START_BUILDING, messageContent);
    }
    
    public UnitStatusMessageEnvelop envelopBuildingFailure(String buildingType) {
        MessageContent messageContent = new MessageContent();
        messageContent.unitAID = agent.getAID();
        messageContent.buildingType = buildingType;
        messageContent.operationSuccess = false;
        
        return new UnitStatusMessage(FINISH_BUILDING, messageContent);
    }
    
    public UnitStatusMessageEnvelop envelopBuildingSuccess(String buildingType) {
        MessageContent messageContent = new MessageContent();
        messageContent.unitAID = agent.getAID();
        messageContent.buildingType = buildingType;
        messageContent.operationSuccess = true;
        
        return new UnitStatusMessage(FINISH_BUILDING, messageContent);
    }
    
    public UnitStatusMessageEnvelop envelopStartedUnitCreation() {
        MessageContent messageContent = new MessageContent();
        messageContent.unitAID = agent.getAID();
        
        return new UnitStatusMessage(START_UNIT, messageContent);
    }
    
    public UnitStatusMessageEnvelop envelopUnitCreationFailure() {
        MessageContent messageContent = new MessageContent();
        messageContent.unitAID = agent.getAID();
        messageContent.operationSuccess = false;
        
        return new UnitStatusMessage(FINISH_UNIT, messageContent);
    }
    
    public UnitStatusMessageEnvelop envelopUnitCreationSuccess() {
        MessageContent messageContent = new MessageContent();
        messageContent.unitAID = agent.getAID();
        messageContent.operationSuccess = true;
        
        return new UnitStatusMessage(FINISH_UNIT, messageContent);
    }
    
    public UnitStatusMessageEnvelop envelopGainedResource(String resourceType
            , int amount) {
        MessageContent messageContent = new MessageContent();
        messageContent.unitAID = agent.getAID();
        messageContent.resourceType = resourceType;
        messageContent.resourceAmount = amount;
        
        return new UnitStatusMessage(EXPLOIT_RESOURCE, messageContent);
    }
    
    public UnitStatusMessageEnvelop envelopFinishExploiting(String resourceType) {
        MessageContent messageContent = new MessageContent();
        messageContent.unitAID = agent.getAID();
        messageContent.resourceType = resourceType;
        
        return new UnitStatusMessage(FINISH_EXPLOIT, messageContent);
    }
    
    public UnitStatusMessageEnvelop envelopFinishExploration() {
        MessageContent messageContent = new MessageContent();
        messageContent.unitAID = agent.getAID();
        
        return new UnitStatusMessage(FINISH_EXPLORATION, messageContent);
    }
    
    public static void handleMessage(UnitStatusMessageEnvelop envelop, UnitStatusHanlder handler) {
        if (envelop instanceof UnitStatusMessage) {
            getStatusMessage((UnitStatusMessage) envelop, handler);
        }
    }

    private static void getStatusMessage(UnitStatusMessage envelop, UnitStatusHanlder handler) {
        switch (envelop.getStatusId()) {
            case CHANGED_POS:
                handleChangedPosition((MessageContent) envelop.getContent(), handler);
                break;
            case START_BUILDING:
                handleStartBuilding((MessageContent) envelop.getContent(), handler);
                break;
            case FINISH_BUILDING:
                handleFinishBuilding((MessageContent) envelop.getContent(), handler);
                break;
            case START_UNIT:
                handleStartUnit((MessageContent) envelop.getContent(), handler);
                break;
            case FINISH_UNIT:
                handleFinishUnit((MessageContent) envelop.getContent(), handler);
                break;
            case EXPLOIT_RESOURCE:
                handleExploitResource((MessageContent) envelop.getContent(), handler);
                break;
            case FINISH_EXPLOIT:
                handleFinishExploitResource((MessageContent) envelop.getContent(), handler);
                break;
            case FINISH_EXPLORATION:
                handleFinishExploration((MessageContent) envelop.getContent(), handler);
                break;
            default:
                break;
        }
    }

    private static void handleChangedPosition(MessageContent content, UnitStatusHanlder handler) {
        handler.onChangedPosition(content.unitAID, content.unitPositionX, content.unitPositionY);
    }

    private static void handleStartBuilding(MessageContent content, UnitStatusHanlder handler) {
        handler.onStartedBuilding(content.unitAID, content.buildingType);
    }

    private static void handleFinishBuilding(MessageContent content, UnitStatusHanlder handler) {
        if (content.operationSuccess) {
            handler.onFinishedBuilding(content.unitAID, content.buildingType);
        }
        else {
            handler.onErrorBuilding(content.unitAID, content.buildingType);
        }
        
    }

    private static void handleStartUnit(MessageContent content, UnitStatusHanlder handler) {
        handler.onStartingUnitCreation(content.unitAID);
    }

    private static void handleFinishUnit(MessageContent content, UnitStatusHanlder handler) {
        if (content.operationSuccess) {
            handler.onFinishedUnitCreation(content.unitAID);
        }
        else {
            handler.onErrorUnitCreation(content.unitAID);
        }
    }

    private static void handleExploitResource(MessageContent content, UnitStatusHanlder handler) {
        handler.onExploitedResource(content.unitAID, content.resourceType, content.resourceAmount);
    }

    private static void handleFinishExploitResource(MessageContent messageContent, UnitStatusHanlder handler) {
        handler.onFinishedExploiting(messageContent.unitAID, messageContent.resourceType);
    }

    private static void handleFinishExploration(MessageContent messageContent, UnitStatusHanlder handler) {
        handler.onFinishedExploring(messageContent.unitAID);
    }
    
    private static class UnitStatusMessage implements UnitStatusMessageEnvelop {
        
        private final int statusId;
        private final Serializable content;
        
        public UnitStatusMessage(int statusId, Serializable content) {
            this.statusId = statusId;
            this.content = content;
        }

        @Override
        public int getStatusId() {
            return statusId;
        }

        @Override
        public Serializable getContent() {
            return content;
        }
        
    }
    
    private static class MessageContent implements Serializable {
        
        AID unitAID = null;
        int unitPositionX = 0;
        int unitPositionY = 0;
        String buildingType = null;
        boolean operationSuccess = false;
        String resourceType = null;
        int resourceAmount = 0;
        
    }
    
    
}
