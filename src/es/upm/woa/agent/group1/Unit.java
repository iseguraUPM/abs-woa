/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1;

import jade.core.AID;

/**
 *
 * @author Martin
 */
public class Unit extends WorldEntity {
    
    private boolean movingToAnotherCell;
    
    public Unit(AID pId, int pCoordX, int pCoordY) {
        super(pId, pCoordX, pCoordY);
        this.movingToAnotherCell = false;
    }
    
    public void setMovingToAnotherCell(boolean movingToAnotherCell) {
        this.movingToAnotherCell = movingToAnotherCell;
    }

    public boolean getMovingToAnotherCell() {
        return movingToAnotherCell;
    }
}
