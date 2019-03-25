package es.upm.woa.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: Cell
* @author ontology bean generator
* @version 2019/03/19, 16:00:20
*/
public class Cell implements Concept {

   /**
* Protege name: owner
   */
   private int owner;
   public void setOwner(int value) { 
    this.owner=value;
   }
   public int getOwner() {
     return this.owner;
   }

   /**
* Protege name: content
   */
   private String content;
   public void setContent(String value) { 
    this.content=value;
   }
   public String getContent() {
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
