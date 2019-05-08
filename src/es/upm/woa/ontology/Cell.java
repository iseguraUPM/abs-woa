package es.upm.woa.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: Cell
* @author ontology bean generator
* @version 2019/05/8, 00:40:35
*/
public class Cell implements Concept {

   /**
* Protege name: x
   */
   private int x;
   public void setX(int value) { 
    this.x=value;
   }
   public int getX() {
     return this.x;
   }

   /**
* Protege name: y
   */
   private int y;
   public void setY(int value) { 
    this.y=value;
   }
   public int getY() {
     return this.y;
   }

   /**
* Protege name: content
   */
   private CellContent content;
   public void setContent(CellContent value) { 
    this.content=value;
   }
   public CellContent getContent() {
     return this.content;
   }

}
