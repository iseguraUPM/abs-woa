/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import es.upm.woa.group1.protocol.Transaction;

/**
 *
 * @author ISU
 */
public interface TransactionRecord {
    
    void addTransaction(Transaction newTransaction);
    
}
