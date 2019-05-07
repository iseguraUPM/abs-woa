/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1.protocol;

import es.upm.woa.ontology.GameOntology;

import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;

/**
 *
 * @author ISU
 */
public class WoaCommunicationStandard implements CommunicationStandard {
    
    private final Ontology ontology; 
    private final Codec codec;
    
    public WoaCommunicationStandard() {
        this.ontology = GameOntology.getInstance();
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
