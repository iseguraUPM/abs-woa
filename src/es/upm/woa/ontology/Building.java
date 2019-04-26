package es.upm.woa.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: Building
* @author ontology bean generator
* @version 2019/04/26, 13:38:37
*/
public class Building implements Concept {

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

}
