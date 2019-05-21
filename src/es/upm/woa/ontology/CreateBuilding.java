package es.upm.woa.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: CreateBuilding
* @author ontology bean generator
* @version 2019/05/21, 11:04:57
*/
public class CreateBuilding implements AgentAction {

   /**
* Protege name: buildingType
   */
   private String buildingType;
   public void setBuildingType(String value) { 
    this.buildingType=value;
   }
   public String getBuildingType() {
     return this.buildingType;
   }

}
