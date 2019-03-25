package worldofagents;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import worldofagents.objects.Unit;
import worldofagents.ontology.GameOntology;
import worldofagents.ontology.NotifyNewUnit;

/**
 *
 * @author ISU
 */
public class AgTribe extends Agent {
    
    public static final String TRIBE = "TRIBE";
    private Ontology ontology;
    private Codec codec;
    private Collection<Unit> units;

    @Override
    protected void setup() {
        try {
            initializeAgent();
            initializeTribe();
        } catch (FIPAException ex) {
            Logger.getLogger(AgTribe.class.getName()).log(Level.SEVERE, null, ex);
        }

//	BEHAVIOURS ****************************************************************
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive(MessageTemplate.and(
                    MessageTemplate.MatchLanguage(codec.getName()),
                    MessageTemplate.MatchOntology(ontology.getName())
                ));

                if (msg != null) {
                    try {
                        if(msg.getPerformative() == ACLMessage.INFORM){
                            ContentElement ce = getContentManager().extractContent(msg);
                            if (ce instanceof Action){

                                Action agAction = (Action) ce;
                                Concept conc = agAction.getAction();

                                if (conc instanceof NotifyNewUnit){
                                    System.out.println(getLocalName()+": received inform request from "+(msg.getSender()).getLocalName());
                                    NotifyNewUnit newUnitInfo = (NotifyNewUnit) conc;
                                    System.out.println(getLocalName() + ": new unit '"
                                            + newUnitInfo.getNewUnit().getName() + "' at "
                                            + newUnitInfo.getLocation().getX() + " "
                                            + newUnitInfo.getLocation().getY());
                                    addUnit(newUnitInfo);
                                }
                            }
                        }
                    } catch (Codec.CodecException | OntologyException e) {
                        e.printStackTrace();
                    }         

                }else {
                    // If no message arrives
                    block();
                }
            }
        });

    }
 
    
    private void initializeAgent() throws FIPAException {
        // Creates its own description
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setName(this.getName());
        sd.setType(TRIBE);
        dfd.addServices(sd);
        // Registers its description in the DF
        DFService.register(this, dfd);
        System.out.println(getLocalName() + ": registered in the DF");
        dfd = null;
        sd = null;
    }
    
    private void initializeTribe() {
        ontology = GameOntology.getInstance();
        codec = new SLCodec();
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);
            
        units = new HashSet<>();
        
    }
    
    public Ontology getOntology() {
        return ontology;
    }
    
    public Codec getCodec() {
        return codec;
    }
    
    public void addUnit(NotifyNewUnit newUnitInfo){
        units.add(new Unit(newUnitInfo.getNewUnit(), newUnitInfo.getLocation().getX(),  newUnitInfo.getLocation().getY()));
    }

}
