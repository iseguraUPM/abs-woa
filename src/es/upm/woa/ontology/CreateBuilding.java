package es.upm.woa.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: CreateBuilding
* @author ontology bean generator
* @version 2019/05/8, 00:40:35
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
