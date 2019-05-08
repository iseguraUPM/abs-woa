// file: GameOntology.java generated by ontology bean generator.  DO NOT EDIT, UNLESS YOU ARE REALLY SURE WHAT YOU ARE DOING!
package es.upm.woa.ontology;

import jade.content.onto.*;
import jade.content.schema.*;
import jade.util.leap.HashMap;
import jade.content.lang.Codec;
import jade.core.CaseInsensitiveString;

/** file: GameOntology.java
 * @author ontology bean generator
 * @version 2019/05/8, 16:06:25
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
    public static final String REGISTERTRIBE_TEAMNUMBER="teamNumber";
    public static final String REGISTERTRIBE="RegisterTribe";
    public static final String NOTIFYNEWUNIT_NEWUNIT="newUnit";
    public static final String NOTIFYNEWUNIT_LOCATION="location";
    public static final String NOTIFYNEWUNIT="NotifyNewUnit";
    public static final String NOTIFYCELLDETAIL_NEWCELL="newCell";
    public static final String NOTIFYCELLDETAIL="NotifyCellDetail";
    public static final String MOVETOCELL_NEWLYARRIVEDCELL="newlyArrivedCell";
    public static final String MOVETOCELL_TARGETDIRECTION="targetDirection";
    public static final String MOVETOCELL="MoveToCell";
    public static final String NOTIFYUNITPOSITION_TRIBEID="tribeId";
    public static final String NOTIFYUNITPOSITION_CELL="cell";
    public static final String NOTIFYUNITPOSITION="NotifyUnitPosition";
    public static final String CREATEUNIT="CreateUnit";
    public static final String CREATEBUILDING_BUILDINGTYPE="buildingType";
    public static final String CREATEBUILDING="CreateBuilding";
    public static final String INITALIZETRIBE_UNITLIST="unitList";
    public static final String INITALIZETRIBE_STARTINGPOSITION="startingPosition";
    public static final String INITALIZETRIBE_STARTINGRESOURCES="startingResources";
    public static final String INITALIZETRIBE="InitalizeTribe";
    public static final String RESOURCE_RESOURCEAMOUNT="resourceAmount";
    public static final String RESOURCE_GOLDPERCENTAGE="goldPercentage";
    public static final String RESOURCE_RESOURCETYPE="resourceType";
    public static final String RESOURCE="Resource";
    public static final String RESOURCEACCOUNT_GOLD="gold";
    public static final String RESOURCEACCOUNT_FOOD="food";
    public static final String RESOURCEACCOUNT_STONE="stone";
    public static final String RESOURCEACCOUNT_WOOD="wood";
    public static final String RESOURCEACCOUNT="ResourceAccount";
    public static final String CELL_CONTENT="content";
    public static final String CELL_Y="y";
    public static final String CELL_X="x";
    public static final String CELL="Cell";
    public static final String BUILDING_OWNER="owner";
    public static final String BUILDING_TYPE="type";
    public static final String BUILDING="Building";
    public static final String EMPTY="Empty";
    public static final String CELLCONTENT="CellContent";

  /**
   * Constructor
  */
  private GameOntology(){ 
    super(ONTOLOGY_NAME, BasicOntology.getInstance());
    try { 

    // adding Concept(s)
    ConceptSchema cellContentSchema = new ConceptSchema(CELLCONTENT);
    add(cellContentSchema, es.upm.woa.ontology.CellContent.class);
    ConceptSchema emptySchema = new ConceptSchema(EMPTY);
    add(emptySchema, es.upm.woa.ontology.Empty.class);
    ConceptSchema buildingSchema = new ConceptSchema(BUILDING);
    add(buildingSchema, es.upm.woa.ontology.Building.class);
    ConceptSchema cellSchema = new ConceptSchema(CELL);
    add(cellSchema, es.upm.woa.ontology.Cell.class);
    ConceptSchema resourceAccountSchema = new ConceptSchema(RESOURCEACCOUNT);
    add(resourceAccountSchema, es.upm.woa.ontology.ResourceAccount.class);
    ConceptSchema resourceSchema = new ConceptSchema(RESOURCE);
    add(resourceSchema, es.upm.woa.ontology.Resource.class);

    // adding AgentAction(s)
    AgentActionSchema initalizeTribeSchema = new AgentActionSchema(INITALIZETRIBE);
    add(initalizeTribeSchema, es.upm.woa.ontology.InitalizeTribe.class);
    AgentActionSchema createBuildingSchema = new AgentActionSchema(CREATEBUILDING);
    add(createBuildingSchema, es.upm.woa.ontology.CreateBuilding.class);
    AgentActionSchema createUnitSchema = new AgentActionSchema(CREATEUNIT);
    add(createUnitSchema, es.upm.woa.ontology.CreateUnit.class);
    AgentActionSchema notifyUnitPositionSchema = new AgentActionSchema(NOTIFYUNITPOSITION);
    add(notifyUnitPositionSchema, es.upm.woa.ontology.NotifyUnitPosition.class);
    AgentActionSchema moveToCellSchema = new AgentActionSchema(MOVETOCELL);
    add(moveToCellSchema, es.upm.woa.ontology.MoveToCell.class);
    AgentActionSchema notifyCellDetailSchema = new AgentActionSchema(NOTIFYCELLDETAIL);
    add(notifyCellDetailSchema, es.upm.woa.ontology.NotifyCellDetail.class);
    AgentActionSchema notifyNewUnitSchema = new AgentActionSchema(NOTIFYNEWUNIT);
    add(notifyNewUnitSchema, es.upm.woa.ontology.NotifyNewUnit.class);
    AgentActionSchema registerTribeSchema = new AgentActionSchema(REGISTERTRIBE);
    add(registerTribeSchema, es.upm.woa.ontology.RegisterTribe.class);

    // adding AID(s)

    // adding Predicate(s)


    // adding fields
    buildingSchema.add(BUILDING_TYPE, (TermSchema)getSchema(BasicOntology.STRING), ObjectSchema.MANDATORY);
    buildingSchema.add(BUILDING_OWNER, (ConceptSchema)getSchema(BasicOntology.AID), ObjectSchema.MANDATORY);
    cellSchema.add(CELL_X, (TermSchema)getSchema(BasicOntology.INTEGER), ObjectSchema.MANDATORY);
    cellSchema.add(CELL_Y, (TermSchema)getSchema(BasicOntology.INTEGER), ObjectSchema.MANDATORY);
    cellSchema.add(CELL_CONTENT, cellContentSchema, ObjectSchema.OPTIONAL);
    resourceAccountSchema.add(RESOURCEACCOUNT_WOOD, (TermSchema)getSchema(BasicOntology.INTEGER), ObjectSchema.OPTIONAL);
    resourceAccountSchema.add(RESOURCEACCOUNT_STONE, (TermSchema)getSchema(BasicOntology.INTEGER), ObjectSchema.OPTIONAL);
    resourceAccountSchema.add(RESOURCEACCOUNT_FOOD, (TermSchema)getSchema(BasicOntology.INTEGER), ObjectSchema.OPTIONAL);
    resourceAccountSchema.add(RESOURCEACCOUNT_GOLD, (TermSchema)getSchema(BasicOntology.INTEGER), ObjectSchema.OPTIONAL);
    resourceSchema.add(RESOURCE_RESOURCETYPE, (TermSchema)getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
    resourceSchema.add(RESOURCE_GOLDPERCENTAGE, (TermSchema)getSchema(BasicOntology.INTEGER), ObjectSchema.OPTIONAL);
    resourceSchema.add(RESOURCE_RESOURCEAMOUNT, (TermSchema)getSchema(BasicOntology.INTEGER), ObjectSchema.OPTIONAL);
    initalizeTribeSchema.add(INITALIZETRIBE_STARTINGRESOURCES, resourceAccountSchema, ObjectSchema.MANDATORY);
    initalizeTribeSchema.add(INITALIZETRIBE_STARTINGPOSITION, cellSchema, ObjectSchema.MANDATORY);
    initalizeTribeSchema.add(INITALIZETRIBE_UNITLIST, (ConceptSchema)getSchema(BasicOntology.AID), 1, ObjectSchema.UNLIMITED);
    createBuildingSchema.add(CREATEBUILDING_BUILDINGTYPE, (TermSchema)getSchema(BasicOntology.STRING), ObjectSchema.MANDATORY);
    notifyUnitPositionSchema.add(NOTIFYUNITPOSITION_CELL, cellSchema, ObjectSchema.MANDATORY);
    notifyUnitPositionSchema.add(NOTIFYUNITPOSITION_TRIBEID, (TermSchema)getSchema(BasicOntology.STRING), ObjectSchema.MANDATORY);
    moveToCellSchema.add(MOVETOCELL_TARGETDIRECTION, (TermSchema)getSchema(BasicOntology.INTEGER), ObjectSchema.MANDATORY);
    moveToCellSchema.add(MOVETOCELL_NEWLYARRIVEDCELL, cellSchema, ObjectSchema.OPTIONAL);
    notifyCellDetailSchema.add(NOTIFYCELLDETAIL_NEWCELL, cellSchema, ObjectSchema.MANDATORY);
    notifyNewUnitSchema.add(NOTIFYNEWUNIT_LOCATION, cellSchema, ObjectSchema.MANDATORY);
    notifyNewUnitSchema.add(NOTIFYNEWUNIT_NEWUNIT, (ConceptSchema)getSchema(BasicOntology.AID), ObjectSchema.MANDATORY);
    registerTribeSchema.add(REGISTERTRIBE_TEAMNUMBER, (TermSchema)getSchema(BasicOntology.INTEGER), ObjectSchema.MANDATORY);

    // adding name mappings

    // adding inheritance
    emptySchema.addSuperSchema(cellContentSchema);
    buildingSchema.addSuperSchema(cellContentSchema);
    resourceSchema.addSuperSchema(cellContentSchema);

   }catch (java.lang.Exception e) {e.printStackTrace();}
  }
  }
