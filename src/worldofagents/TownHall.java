/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package worldofagents;

import java.util.UUID;

/**
 *
 * @author Martin
 */
public class TownHall extends WorldEntity{
    
    private String id;
    
    public TownHall(String pId, int pCoordX, int pCoordY){
        super(pId, pCoordX, pCoordY);
        id = pId;
    }
}
