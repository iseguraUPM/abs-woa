package es.upm.woa.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: NotifyNewCellDiscovery
* @author ontology bean generator
* @version 2019/04/8, 12:20:37
*/
public class NotifyNewCellDiscovery implements AgentAction {

   /**
* Protege name: newCell
   */
   private Cell newCell;
   public void setNewCell(Cell value) { 
    this.newCell=value;
   }
   public Cell getNewCell() {
     return this.newCell;
   }

}
