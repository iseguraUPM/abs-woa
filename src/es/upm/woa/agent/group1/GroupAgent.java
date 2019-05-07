/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import es.upm.woa.agent.group1.map.GameMap;
import es.upm.woa.agent.group1.map.MapCell;

import jade.content.lang.Codec;
import jade.content.onto.Ontology;

/**
 *
 * @author ISU
 */
abstract class GroupAgent extends WoaAgent {
    
    abstract Ontology getOntology();
    
    abstract Codec getCodec();
    
    abstract GameMap getKnownMap();
    
    abstract void onCellDiscovered(MapCell newCell);
    
    abstract void onCellUpdated(MapCell updatedCell);
    
    abstract void onUnitPassby(MapCell cell, String tribeId);
    
}
