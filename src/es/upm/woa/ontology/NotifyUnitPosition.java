package es.upm.woa.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: NotifyUnitPosition
* @author ontology bean generator
* @version 2019/04/26, 13:38:37
*/
public class NotifyUnitPosition implements AgentAction {

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

}
