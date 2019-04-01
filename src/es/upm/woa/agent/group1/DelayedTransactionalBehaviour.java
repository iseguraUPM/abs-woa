/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import jade.core.Agent;

/**
 *
 * @author ISU
 */
public abstract class DelayedTransactionalBehaviour extends DelayBehaviour
        implements Transaction {

    public DelayedTransactionalBehaviour(Agent agent, long timeout) {
        super(agent, timeout);
    }

    @Override
    public abstract void rollback();
    
}
