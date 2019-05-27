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
class TribeUnit implements Unit {
    
    private final AID id;
    private int coordX;
    private int coordY;
    private boolean busy;
    
    public TribeUnit(AID pId, int pCoordX, int pCoordY) {
        id = pId;
        coordX = pCoordX;
        coordY = pCoordY;
        busy = false;
    }
    
    @Override
    public AID getId() {
        return id;
    }
    
    @Override
    public int getCoordX() {
        return coordX;
    }
    
    @Override
    public int getCoordY() {
        return coordY;
    }
    
    @Override
    public void setPosition(int x, int y) {
        coordX = x;
        coordY = y;
    }

    @Override
    public boolean isBusy() {
        return busy;
    }

    @Override
    public void setBusy() {
        busy = true;
    }

    @Override
    public void setFree() {
        busy = false;
    }
    
}
