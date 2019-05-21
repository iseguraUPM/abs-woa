/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import jade.core.AID;

/**
 *
 * @author Martin
 */
public class Unit {
        
    private final AID id;
    private int coordX;
    private int coordY;
    
    public Unit(AID pId, int pCoordX, int pCoordY) {
        id = pId;
        coordX = pCoordX;
        coordY = pCoordY;
    }
    
    public AID getId() {
        return id;
    }
    
    public int getCoordX() {
        return coordX;
    }
    
    public int getCoordY() {
        return coordY;
    }
    
    public void setPosition(int x, int y) {
        coordX = x;
        coordY = y;
    }
    
}
