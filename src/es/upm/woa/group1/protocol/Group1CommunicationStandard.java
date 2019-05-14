/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.protocol;

import es.upm.woa.group1.ontology.Group1Ontology;

import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;

/**
 *
 * @author ISU
 */
public class Group1CommunicationStandard implements CommunicationStandard {
    
    private final Ontology ontology; 
    private final Codec codec;
    
    public Group1CommunicationStandard() {
        this.ontology = Group1Ontology.getInstance();
        this.codec = new SLCodec();
    }

    @Override
    public Ontology getOntology() {
        return ontology;
    }

    @Override
    public Codec getCodec() {
        return codec;
    }

    @Override
    public void register(ContentManager contentManager) {
        contentManager.registerLanguage(codec);
        contentManager.registerOntology(ontology);
    }
    
}
