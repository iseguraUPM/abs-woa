/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1.map;

import jade.content.Concept;

/**
 *
 * @author ISU
 */
public interface MapCell {
    
    public Concept getContent();
    
    public int getXCoord();
    
    public int getYCoord();
    
    public void setContent(Concept content);
  
}
