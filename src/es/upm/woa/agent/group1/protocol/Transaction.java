/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1.protocol;

/**
 *
 * @author ISU
 */
public interface Transaction {
    
    void commit();
    
    void rollback();
    
}