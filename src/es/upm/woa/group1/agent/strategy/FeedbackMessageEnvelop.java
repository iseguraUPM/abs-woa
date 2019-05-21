/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent.strategy;

import java.io.Serializable;

/**
 *
 * @author ISU
 */
public interface FeedbackMessageEnvelop {
    
    int getStatusId();
    
    Serializable getContent();
    
}
