/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import jade.content.Concept;
import jade.core.AID;

/**
 *
 * @author ISU
 */
public interface MapCell {
    
    public AID getOwner();
    
    public Concept getContent();
    
    public int getXCoord();
    
    public int getYCoord();
  
}
