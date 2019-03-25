/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package worldofagents.objects;

import jade.core.AID;


/**
 *
 * @author Martin
 * An Entity is any class which corresponds to an agent which will be part of the world
 */
public abstract class WorldEntity {
    
    private AID id;
    private int coordX;
    private int coordY;
    
    public WorldEntity(AID pId, int pCoordX, int pCoordY){
        id = pId;
        coordX = pCoordX;
        coordY = pCoordY;
    }
    
    public AID getId(){
        return id;
    }
    
    public int getCoordX(){
        return coordX;
    }
    
    public int getCoordY(){
        return coordY;
    }
    
    public boolean sameCoords(WorldEntity otherEntity) {
        return coordX == otherEntity.coordX && coordY == otherEntity.coordY;
    }
}
