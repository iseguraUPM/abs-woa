/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1.protocol;

import es.upm.woa.agent.group1.GameClock;

import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;

/**
 *
 * @author https://www.iro.umontreal.ca/~vaucher/Agents/Jade/primer6.html#6.5
 */
public class DelayTickBehaviour extends SimpleBehaviour {
   private final long timeout;
   private long wakeupTime;
   private boolean finished = false;
   
   public DelayTickBehaviour(Agent a, long tickTimeout) {
      super(a);
      this.timeout = tickTimeout;
   }
   
   @Override
   public void onStart() {
      wakeupTime = GameClock.getInstance().getCurrentTick() + timeout;
   }
      
   @Override
   public void action() 
   {
      long dt = wakeupTime - GameClock.getInstance().getCurrentTick();
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
