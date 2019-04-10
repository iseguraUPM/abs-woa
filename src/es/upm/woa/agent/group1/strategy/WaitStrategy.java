/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1.strategy;

import jade.core.Agent;

/**
 *
 * @author ISU
 */
class WaitStrategy extends Strategy {

    public WaitStrategy(Agent agent) {
        super(agent, new StrategyEventDispatcher() {
            @Override
            public void subscribe(Strategy subscriber) {

            }

            @Override
            public void dispatch(StrategyEvent event) {

            }
        });
    }

    @Override
    public void action() {
        block();
    }

    @Override
    public boolean done() {
        return false;
    }

    @Override
    public int getPriority() {
        return LOW_PRIORITY;
    }

    @Override
    public void onEvent(StrategyEvent event) {
    }

    @Override
    public void onStart() {

    }

    @Override
    public int onEnd() {
        return 0;
    }

}
