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
    
    private Transaction currentTransaction;
    
    public Unit(AID pId, int pCoordX, int pCoordY) {
        super(pId, pCoordX, pCoordY);
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
    public void rollbackCurrentTransaction() {
        if(currentTransaction != null){
            currentTransaction.rollback();  
        }
        currentTransaction = null;
    }
    
}
