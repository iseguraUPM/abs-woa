// file: GameOntology.java generated by ontology bean generator.  DO NOT EDIT, UNLESS YOU ARE REALLY SURE WHAT YOU ARE DOING!
package worldofagents.ontology;

import jade.content.onto.*;
import jade.content.schema.*;
import jade.util.leap.HashMap;
import jade.content.lang.Codec;
import jade.core.CaseInsensitiveString;
import worldofagents.ontology.Cell;

/** file: GameOntology.java
 * @author ontology bean generator
 * @version 2019/03/19, 16:00:20
 */
public class GameOntology extends jade.content.onto.Ontology  {
  //NAME
  public static final String ONTOLOGY_NAME = "game";
  // The singleton instance of this ontology
  private static ReflectiveIntrospector introspect = new ReflectiveIntrospector();
  private static Ontology theInstance = new GameOntology();
  public static Ontology getInstance() {
     return theInstance;
  }


   // VOCABULARY
    public static final String CELL_X="x";
    public static final String CELL_Y="y";
    public static final String CELL_CONTENT="content";
    public static final String CELL_OWNER="owner";
    public static final String CELL="Cell";
    public static final String CREATEUNIT="CreateUnit";
    public static final String NOTIFYNEWUNIT_LOCATION="location";
    public static final String NOTIFYNEWUNIT_NEWUNIT="newUnit";
    public static final String NOTIFYNEWUNIT="NotifyNewUnit";

  /**
   * Constructor
  */
  private GameOntology(){ 
    super(ONTOLOGY_NAME, BasicOntology.getInstance());
    try { 

    // adding Concept(s)
    ConceptSchema notifyNewUnitSchema = new ConceptSchema(NOTIFYNEWUNIT);
    add(notifyNewUnitSchema, NotifyNewUnit.class);
    ConceptSchema createUnitSchema = new ConceptSchema(CREATEUNIT);
    add(createUnitSchema, CreateUnit.class);
    ConceptSchema cellSchema = new ConceptSchema(CELL);
    add(cellSchema, Cell.class);

    // adding AgentAction(s)

    // adding AID(s)

    // adding Predicate(s)


    // adding fields
    notifyNewUnitSchema.add(NOTIFYNEWUNIT_NEWUNIT, (ConceptSchema)getSchema(BasicOntology.AID), ObjectSchema.MANDATORY);
    notifyNewUnitSchema.add(NOTIFYNEWUNIT_LOCATION, cellSchema, ObjectSchema.MANDATORY);
    cellSchema.add(CELL_OWNER, (TermSchema)getSchema(BasicOntology.INTEGER), ObjectSchema.MANDATORY);
    cellSchema.add(CELL_CONTENT, (TermSchema)getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
    cellSchema.add(CELL_Y, (TermSchema)getSchema(BasicOntology.INTEGER), ObjectSchema.MANDATORY);
    cellSchema.add(CELL_X, (TermSchema)getSchema(BasicOntology.INTEGER), ObjectSchema.MANDATORY);

    // adding name mappings

    // adding inheritance

   }catch (java.lang.Exception e) {e.printStackTrace();}
  }
  }
