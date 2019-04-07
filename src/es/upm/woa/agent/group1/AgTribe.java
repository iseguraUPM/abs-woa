package es.upm.woa.agent.group1;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import es.upm.woa.ontology.Cell;
import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import es.upm.woa.ontology.GameOntology;
import es.upm.woa.ontology.NotifyNewCellDiscovery;
import es.upm.woa.ontology.NotifyNewUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author ISU
 */
public class AgTribe extends Agent {

    private Ontology ontology;
    private Codec codec;
    private Collection<Unit> units;
    //private Collection<Cell> discoveredCells;
    private Map<ArrayList<Integer>, Cell> discoveredCells; 

    @Override
    protected void setup() {
        
        initializeAgent();
        initializeTribe();
        
        startInformNewUnitBehaviour();
        startInformNewCellDiscoveryBehaviour();
    }

    private void startInformNewUnitBehaviour() {
        // Behaviors
        Action informNewUnitAction = new Action(getAID(), new NotifyNewUnit());
        addBehaviour(new Conversation(this, ontology, codec, informNewUnitAction, "NotifyNewUnit") {
            @Override
            public void onStart() {
                listenMessages(new ResponseHandler() {
                    @Override
                    public void onInform(ACLMessage response) {
                        try {
                            ContentElement ce = getContentManager().extractContent(response);
                            if (ce instanceof Action) {
                                
                                Action agAction = (Action) ce;
                                Concept conc = agAction.getAction();
                                
                                if (conc instanceof NotifyNewUnit) {
                                    System.out.println(getLocalName() + ": received inform"
                                            + " request from " + response.getSender().getLocalName());
                                    NotifyNewUnit newUnitInfo = (NotifyNewUnit) conc;
                                    System.out.println(getLocalName() + ": new unit '"
                                            + newUnitInfo.getNewUnit().getLocalName()+ "' at "
                                            + newUnitInfo.getLocation().getX() + " "
                                            + newUnitInfo.getLocation().getY());
                                    addUnit(newUnitInfo);
                                }
                            }
                        } catch (Codec.CodecException | OntologyException ex) {
                            Logger.getLogger(AgTribe.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }

                });
            }
        });
    }
    
    
    private void startInformNewCellDiscoveryBehaviour() {
        // Behaviors
        Action informNewCellDiscoveryAction = new Action(getAID(), new NotifyNewCellDiscovery());
        addBehaviour(new Conversation(this, ontology, codec, informNewCellDiscoveryAction, "NotifyNewCellDiscovery") {
            @Override
            public void onStart() {
                listenMessages(new ResponseHandler() {
                    @Override
                    public void onInform(ACLMessage response) {
                        try {
                            ContentElement ce = getContentManager().extractContent(response);
                            if (ce instanceof Action) {
                                
                                Action agAction = (Action) ce;
                                Concept conc = agAction.getAction();
                                
                                if (conc instanceof NotifyNewCellDiscovery) {
                                    System.out.println(getLocalName() + ": received inform"
                                            + " request from " + response.getSender().getLocalName());
                                    NotifyNewCellDiscovery newCellInfo = (NotifyNewCellDiscovery)conc;
                                    ArrayList<Integer> coords = new ArrayList<>();
                                    coords.add(newCellInfo.getNewCell().getX());
                                    coords.add(newCellInfo.getNewCell().getY());
                                    if(discoveredCells.get(coords) == null){
                                        System.out.println(getLocalName() + ": cell discovered at "
                                            + newCellInfo.getNewCell().getX() + ", "
                                            + newCellInfo.getNewCell().getY() + " ");
                                        addDiscoveredCell(coords, newCellInfo.getNewCell());
                                    }
                                    
                                }
                            }
                        } catch (Codec.CodecException | OntologyException ex) {
                            Logger.getLogger(AgTribe.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }

                });
            }
        });
    }

    private void initializeAgent() {
        
    }

    private void initializeTribe() {
        ontology = GameOntology.getInstance();
        codec = new SLCodec();
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);

        units = new HashSet<>();
        discoveredCells = new HashMap<>();

    }

    public Ontology getOntology() {
        return ontology;
    }

    public Codec getCodec() {
        return codec;
    }

    public void addUnit(NotifyNewUnit newUnitInfo) {
        units.add(new Unit(newUnitInfo.getNewUnit(), newUnitInfo.getLocation().getX(), newUnitInfo.getLocation().getY()));
    }
    
    public void addDiscoveredCell(ArrayList<Integer> coords, Cell cell) {        
        discoveredCells.put(coords, cell);
        System.out.println(discoveredCells);
    }

}
