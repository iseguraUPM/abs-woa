/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1.protocol;

import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;

/**
 *
 * @author https://www.iro.umontreal.ca/~vaucher/Agents/Jade/primer6.html#6.5
 */
public class DelayBehaviour extends SimpleBehaviour 
{
   private final long timeout;
   private long wakeupTime;
   private boolean finished = false;
   
   public DelayBehaviour(Agent a, long timeout) {
      super(a);
      this.timeout = timeout;
   }
   
   @Override
   public void onStart() {
      wakeupTime = System.currentTimeMillis() + timeout;
   }
      
   @Override
   public void action() 
   {
      long dt = wakeupTime - System.currentTimeMillis();
      if (dt <= 0) {
         finished = true;
         handleElapsedTimeout();
      } else 
         block(dt);
         
   } //end of action
   
   protected void handleElapsedTimeout() // by default do nothing !
      { } 
            
   @Override
   public boolean done() { return finished; }
}
