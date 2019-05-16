/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import es.upm.woa.group1.WoaDefinitions;
import es.upm.woa.group1.WoaLogger;
import jade.core.Agent;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

/**
 *
 * @author ISU
 */
public class AgPlatform extends WoaAgent {
    
    private WoaLogger logger;
    
    private final String AGENT_TRIBE_NAME_PREFIX = "Tribe";
    private final int MAX_TRIBES = 2;
    
    private Collection<AgentController> agents;
    
    @Override
    public void setup() {
        logger = new WoaLogger(getAID(), new ConsoleHandler());
        logger.setLevel(Level.INFO);
        log(Level.INFO, "Preparing platform...");
        
        agents = new ArrayList<>();
        
        launchAgentWorld();
        launchTribes();
    }
    
    @Override
    public void takeDown() {
        log(Level.INFO, "Taking down...");
        agents.forEach((AgentController agent) -> {
            try {
                agent.kill();
            } catch (StaleProxyException ex) {
                try {
                    log(Level.WARNING, "Could not terminate agent " + agent.getName());
                } catch (StaleProxyException ex1) {
                    log(Level.WARNING, "Could not terminate agent");
                }
            }
        });
        agents.clear();
    }

    private void launchAgentWorld() {
        log(Level.INFO, "Launching world...");
        try {
            ContainerController cc = getContainerController();
            AgWorld agentWorld = new AgWorld();
            AgentController ac = cc.acceptNewAgent("Agent World", agentWorld);
            agents.add(ac);
            ac.start();
            log(Level.FINE, "Launched world...");
        } catch (StaleProxyException ex) {
            log(Level.SEVERE, "Could not launch agent world");
        }
    }
    
        
    private void launchTribes() {
        log(Level.INFO, "Launching tribes...");
        Set<Integer> successfulTribes = new HashSet<>();
        for (int i = 1; i <= MAX_TRIBES; i++) {
            String tribeClassPath = MessageFormat
                        .format(WoaDefinitions.AGENT_CLASS_PATH_TEMPLATE, i)
                        .concat(WoaDefinitions.AGENT_TRIBE_CLASS_NAME);
                String tribeName = AGENT_TRIBE_NAME_PREFIX + i;
            try {
                launchAgentTribe(tribeClassPath, tribeName);
                successfulTribes.add(i);
                log(Level.FINE, "Launched " + tribeName);
            } catch (ClassNotFoundException ex) {
                log(Level.WARNING, "Could not find " + tribeName
                        + " class at " + tribeClassPath);
            }
        }
        
        if (successfulTribes.contains(1) && successfulTribes.size() == 1) {
            launchGroup1TribesOnly();
        }
    }

    private void launchGroup1TribesOnly() {
        log(Level.INFO, "Group1 only mode: launching remaining tribes");
        String tribeClassPath = MessageFormat
                .format(WoaDefinitions.AGENT_CLASS_PATH_TEMPLATE, 1)
                .concat(WoaDefinitions.AGENT_TRIBE_CLASS_NAME);
        for (int i = 2; i <= MAX_TRIBES; i++) {
            String tribeName = AGENT_TRIBE_NAME_PREFIX + i;
            try {
                launchAgentTribe(tribeClassPath, tribeName);
            } catch (ClassNotFoundException ex) {
                log(Level.SEVERE, "Could not find " + tribeName
                        + " class at " + tribeClassPath);
            }
        }
    }
    
    private void launchAgentTribe(String tribeClassPath, String tribeName
    ) throws ClassNotFoundException {
        try {
            Class<?> tribeClass = Class.forName(tribeClassPath);
            ContainerController cc = getContainerController();
            Agent newTribe = (Agent) tribeClass.newInstance();
            AgentController ac = cc.acceptNewAgent(tribeName, newTribe);
            agents.add(ac);
            ac.start();
        } catch (StaleProxyException ex) {
            log(Level.WARNING, "Could not launch tribe " + tribeName);
        } catch (InstantiationException
                | IllegalAccessException ex) {
            log(Level.WARNING, "Could not instantiate tribe " + tribeName);        
        }
    }
        
    @Override
    public void log(Level logLevel, String message) {
        logger.log(logLevel, message);
    }
    
}
