/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1;

import jade.core.AID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 *
 * @author ISU
 */
public class WoaLogger {
    
    private final AID agentAid;
    private final Handler logHandler;
    
    public WoaLogger(AID agentAid, Handler logHandler) {
        this.agentAid = agentAid;
        this.logHandler = logHandler;
    }
    
    public void setLevel(Level logLevel) {
        logHandler.setLevel(logLevel);
    }
    
    public void log(Level logLevel, String message) {
        if (logHandler.isLoggable(new LogRecord(logLevel, ""))) {
            printLog(logLevel, message);
        }
    }

    private void printLog(Level logLevel, String message) {
        boolean error = false;
        StringBuilder sb = new StringBuilder();
        if (logLevel.equals(Level.SEVERE)) {
            error = true;
        }
        else if (logLevel.equals(Level.WARNING)) {
            error = true;
        }
        else if (logLevel.equals(Level.FINE)) {
            sb.append("  ");
        }
        else if (logLevel.equals(Level.FINER)) {
            sb.append("    ");
        }
        else if (logLevel.equals(Level.FINEST)) {
            sb.append("      ");
        }
        composeMessage(sb, logLevel, message);
        
        if (error) {
            System.err.println(sb.toString());
        }
        else {
            System.out.println(sb.toString());
        }
    }

    private void composeMessage(StringBuilder sb, Level logLevel, String message) {
        sb.append("[").append(logLevel.getName()).append("] ");
        sb.append(agentAid.getLocalName()).append(": ").append(message);
    }
    
}
