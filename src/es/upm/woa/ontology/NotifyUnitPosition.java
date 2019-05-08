package es.upm.woa.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: NotifyUnitPosition
* @author ontology bean generator
* @version 2019/05/8, 00:40:35
*/
public class NotifyUnitPosition implements AgentAction {

   /**
* Protege name: cell
   */
   private Cell cell;
   public void setCell(Cell value) { 
    this.cell=value;
   }
   public Cell getCell() {
     return this.cell;
   }

   /**
* Protege name: tribeId
   */
   private String tribeId;
   public void setTribeId(String value) { 
    this.tribeId=value;
   }
   public String getTribeId() {
     return this.tribeId;
   }

}
