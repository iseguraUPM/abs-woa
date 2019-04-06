package es.upm.woa.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: Building
* @author ontology bean generator
* @version 2019/04/5, 19:32:22
*/
public class Building implements Concept {

   /**
* Protege name: type
   */
   private List type = new ArrayList();
   public void addType(String elem) { 
     List oldList = this.type;
     type.add(elem);
   }
   public boolean removeType(String elem) {
     List oldList = this.type;
     boolean result = type.remove(elem);
     return result;
   }
   public void clearAllType() {
     List oldList = this.type;
     type.clear();
   }
   public Iterator getAllType() {return type.iterator(); }
   public List getType() {return type; }
   public void setType(List l) {type = l; }

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
