/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import java.io.Serializable;

/**
 *
 * @author ISU
 */
interface StrategyEnvelop extends Serializable {
    
    int getStrategy();
    
    int getPriority();
    
    Serializable getContent();
    
}
