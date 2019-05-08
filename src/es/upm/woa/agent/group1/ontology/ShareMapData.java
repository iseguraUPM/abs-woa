package es.upm.woa.agent.group1.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: ShareMapData
* @author ontology bean generator
* @version 2019/05/8, 17:42:14
*/
public class ShareMapData implements AgentAction {

   /**
* Protege name: knownMap
   */
   private Object knownMap;
   public void setKnownMap(Object value) { 
    this.knownMap=value;
   }
   public Object getKnownMap() {
     return this.knownMap;
   }

}
