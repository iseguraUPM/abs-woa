package es.upm.woa.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: Cell
* @author ontology bean generator
* @version 2019/04/26, 13:38:37
*/
public class Cell implements Concept {

   /**
* Protege name: content
   */
   private Object content;
   public void setContent(Object value) { 
    this.content=value;
   }
   public Object getContent() {
     return this.content;
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
* Protege name: x
   */
   private int x;
   public void setX(int value) { 
    this.x=value;
   }
   public int getX() {
     return this.x;
   }

}
