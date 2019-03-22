/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package worldofagents;

import jade.core.AID;
import lombok.Getter;
import lombok.Setter;


/**
 *
 * @author Martin
 * An Entity is any class which corresponds to an agent which will be part of the world
 */
@Getter
@Setter
public abstract class WorldEntity {
    
    private AID entityAID;
    private int coordX;
    private int coordY;
    
    public WorldEntity(AID pAID, int pCoordX, int pCoordY){
        entityAID = pAID;
        coordX = pCoordX;
        coordY = pCoordY;
    }
    
    public boolean sameCoords(WorldEntity otherEntity) {
        return coordX == otherEntity.coordX && coordY == otherEntity.coordY;
    }
}
