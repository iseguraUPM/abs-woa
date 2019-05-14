/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.protocol;

import jade.core.Agent;

/**
 * This modification of the DelayTickBehaviour behaves as a transaction. It executes
 * commit once the elapsed time is over.
 * @author ISU
 */
public abstract class DelayedTransactionalBehaviour extends DelayTickBehaviour
        implements Transaction {

    public DelayedTransactionalBehaviour(Agent agent, long timeout) {
        super(agent, timeout);
    }
    
    @Override
    protected final void handleElapsedTimeout() {
        commit();
    }

    @Override
    public abstract void rollback();
    
}
