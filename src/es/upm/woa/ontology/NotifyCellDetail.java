package es.upm.woa.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: NotifyCellDetail
* @author ontology bean generator
* @version 2019/05/21, 11:04:57
*/
public class NotifyCellDetail implements AgentAction {

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
