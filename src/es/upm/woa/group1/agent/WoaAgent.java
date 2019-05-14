/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import jade.core.Agent;
import java.util.logging.Level;

/**
 *
 * @author ISU
 */
public abstract class WoaAgent extends Agent {
    
    public abstract void log(Level logLevel, String message);
    
}
