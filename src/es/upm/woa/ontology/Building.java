package es.upm.woa.ontology;

import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: Building
* @author ontology bean generator
* @version 2019/05/8, 00:40:35
*/
public class Building extends CellContent{ 

   /**
* Protege name: type
   */
   private String type;
   public void setType(String value) { 
    this.type=value;
   }
   public String getType() {
     return this.type;
   }

   /**
* Protege name: owner
   */
   private AID owner;
   public void setOwner(AID value) { 
    this.owner=value;
   }
   public AID getOwner() {
     return this.owner;
   }

}
