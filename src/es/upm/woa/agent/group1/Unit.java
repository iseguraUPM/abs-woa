/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.protocol.Transaction;
import jade.core.AID;

/**
 *
 * @author Martin
 */
public class Unit extends WorldEntity {
    
    private boolean isBuilding;
    
    private Transaction currentTransaction;
    
    public Unit(AID pId, int pCoordX, int pCoordY) {
        super(pId, pCoordX, pCoordY);
        isBuilding = false;
    }
    
    public void setIsBuilding(boolean building) {
        this.isBuilding = building;
    }

    public boolean getIsBuilding() {
        return isBuilding;
    }
    
    /**
     * 
     * @param currentTransaction
     */
    public void setCurrentTransaction(Transaction currentTransaction) {
        this.currentTransaction = currentTransaction;
    }
    
    /**
     * rollback current transaction and set it to null
     * 
     */
    public void refundUnitTransaction(){
        if(currentTransaction != null){
            currentTransaction.rollback();  
        }
        currentTransaction = null;
    }
    
}
