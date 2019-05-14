/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.protocol;

import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.onto.Ontology;

/**
 *
 * @author ISU
 */
public interface CommunicationStandard {
    
    public Ontology getOntology();
    
    public Codec getCodec();
    
    public void register(ContentManager contentManager);
    
}
