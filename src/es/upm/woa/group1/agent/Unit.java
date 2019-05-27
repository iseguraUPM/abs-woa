/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import jade.core.AID;

/**
 *
 * @author ISU
 */
public interface Unit {

    int getCoordX();

    int getCoordY();

    AID getId();

    void setPosition(int x, int y);
    
    boolean isBusy();
    
    void setBusy();
    
    void setFree();
    
}
