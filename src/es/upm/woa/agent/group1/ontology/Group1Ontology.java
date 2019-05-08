// file: Group1Ontology.java generated by ontology bean generator.  DO NOT EDIT, UNLESS YOU ARE REALLY SURE WHAT YOU ARE DOING!
package es.upm.woa.agent.group1.ontology;

import jade.content.onto.*;
import jade.content.schema.*;
import jade.util.leap.HashMap;
import jade.content.lang.Codec;
import jade.core.CaseInsensitiveString;

/** file: Group1Ontology.java
 * @author ontology bean generator
 * @version 2019/05/8, 17:42:14
 */
public class Group1Ontology extends jade.content.onto.Ontology  {
  //NAME
  public static final String ONTOLOGY_NAME = "Group1";
  // The singleton instance of this ontology
  private static ReflectiveIntrospector introspect = new ReflectiveIntrospector();
  private static Ontology theInstance = new Group1Ontology();
  public static Ontology getInstance() {
     return theInstance;
  }


   // VOCABULARY
    public static final String WHEREAMI_XCOORD="xCoord";
    public static final String WHEREAMI_YCOORD="yCoord";
    public static final String WHEREAMI="WhereAmI";
    public static final String NOTIFYUNITOWNERSHIP="NotifyUnitOwnership";
    public static final String SHAREMAPDATA_KNOWNMAP="knownMap";
    public static final String SHAREMAPDATA="ShareMapData";

  /**
   * Constructor
  */
  private Group1Ontology(){ 
    super(ONTOLOGY_NAME, BasicOntology.getInstance());
    try { 

    // adding Concept(s)

    // adding AgentAction(s)
    AgentActionSchema shareMapDataSchema = new AgentActionSchema(SHAREMAPDATA);
    add(shareMapDataSchema, es.upm.woa.agent.group1.ontology.ShareMapData.class);
    AgentActionSchema notifyUnitOwnershipSchema = new AgentActionSchema(NOTIFYUNITOWNERSHIP);
    add(notifyUnitOwnershipSchema, es.upm.woa.agent.group1.ontology.NotifyUnitOwnership.class);
    AgentActionSchema whereAmISchema = new AgentActionSchema(WHEREAMI);
    add(whereAmISchema, es.upm.woa.agent.group1.ontology.WhereAmI.class);

    // adding AID(s)

    // adding Predicate(s)


    // adding fields
    shareMapDataSchema.add(SHAREMAPDATA_KNOWNMAP, (TermSchema)getSchema(BasicOntology.SET), ObjectSchema.MANDATORY);
    whereAmISchema.add(WHEREAMI_YCOORD, (TermSchema)getSchema(BasicOntology.INTEGER), ObjectSchema.OPTIONAL);
    whereAmISchema.add(WHEREAMI_XCOORD, (TermSchema)getSchema(BasicOntology.INTEGER), ObjectSchema.OPTIONAL);

    // adding name mappings

    // adding inheritance

   }catch (java.lang.Exception e) {e.printStackTrace();}
  }
  }
