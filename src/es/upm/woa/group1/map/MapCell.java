/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.map;

import es.upm.woa.ontology.CellContent;

import java.io.Serializable;

/**
 *
 * @author ISU
 */
public interface MapCell extends Serializable {
    
    public CellContent getContent();
    
    public int getXCoord();
    
    public int getYCoord();
    
    public void setContent(CellContent content);
    
    @Override
    public String toString();
      
}
