/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1.map;

/**
 *
 * @author ISU
 */
class CellTranslation {
    
    public static final int [] DOWN = {2,0};
    public static final int [] UP = {-2,0};
    public static final int [] LUP = {-1,-1};
    public static final int [] RUP = {-1,1};
    public static final int [] LDOWN = {1,-1};
    public static final int [] RDOWN = {1,1};
    
    public static final int [][] POS_OPERATORS = {UP, RUP, RDOWN, DOWN, LDOWN
            , LUP};

    private final int [] translationVector;
    
    public CellTranslation(int [] translationVector) {
        this.translationVector = translationVector;
    }
    
    public int [] translate(MapCell cell) {
        if (translationVector.length != 2) {
            return new int[]{0,0};
        }
        
        return new int[]{cell.getXCoord() + translationVector[0]
                , cell.getYCoord() + translationVector[1]};
        
    }
    
    public CellTranslation generateInverse() {
        int[] inverseVector = new int[]{translationVector[0] * -1
                , translationVector[1] * -1};
        
        return new CellTranslation(inverseVector);
    }
    
}
