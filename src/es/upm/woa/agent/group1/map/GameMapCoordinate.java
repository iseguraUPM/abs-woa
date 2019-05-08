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
public class GameMapCoordinate {

    public static final int[][] POS_OPERATORS = {CellTranslation.V_UP
            , CellTranslation.V_RUP, CellTranslation.V_RDOWN, CellTranslation.V_DOWN
            , CellTranslation.V_LDOWN, CellTranslation.V_LUP};
    
    /**
     * 
     * @param width of the map
     * @param height of the map
     * @param x coordinate (up down)
     * @param y coordinate (left right)
     * @return if the position is inside the map bounds and the coordinates
     *  are both EVEN or both ODD.
     */
    public static boolean isCorrectPosition(int width, int height, int x, int y) {
        return x >= 1 && y >= 1
                && x <= height && y <= width
                && (x + y) % 2 == 0;
    }
    
    /**
     * Apply translation to a CORRECT position inside the map bounds.
     * @param width of the map
     * @param height of the map
     * @param x coordinate (up down)
     * @param y coordinate (left right)
     * @param translationVector to apply
     * @return the final position or null if the translation vector is not valid
     *  (valid vectors = {2,0} {-2,0} {1,1} {-1,1} {-1,-1} {1,-1})
     */
    public static int [] applyTranslation(int width, int height, int x, int y, int [] translationVector) {
        if (translationVector.length != 2) {
            return null;
        }
        
        if (Math.abs(translationVector[0]) + Math.abs(translationVector[1]) != 2) {
            return null;
        }
        else if (Math.abs(translationVector[0]) == 2 && translationVector[1] != 0) {
            return null;
        }
        
        
        return correctPosition(width, height, x % 2 == 0, x + translationVector[0]
                , y + translationVector[1]);
    }
    
    
    private static int [] correctPosition(int width, int height, boolean sourceIsOdd, int x, int y) {
        int[] pos = new int[] {x, y};
        
        if (x >= 1 && y >= 1
                && x <= height && y <= width) {
            return pos;
        }
        
        // Case: position outside square map by the lower right corner
        if (x > height && y > width) {
            pos[0] = 1;
            pos[1] = 1;
            return pos;
        }
        
        // Case: position outside square map by the upper left corner
        if (x < 1 && y < 1) {
            pos[0] = width;
            pos[1] = height;
            return pos;
        }
        
        if (x < 1 && y % 2 == 0) {
            pos[0] = closestLowerEven(height);
        }
        else if (x < 1) {
            pos[0] = closestLowerOdd(height);
        }
        
        if (x > height && y % 2 == 0) {
            pos[0] = 2;
        }
        else if (x > height) {
            pos[0] = 1;
        }
        
        if (y < 1 && x % 2 == 0) {
            pos[1] = closestLowerEven(width);
        }
        else if (y < 1) {
            pos[1] = closestLowerOdd(width);
        }
        
        if (y > width && x % 2 == 0) {
            pos[1] = 2;
        }
        else if (y > width) {
            pos[1] = 1;
        }
        
        return pos;
    }
    
    private static int closestLowerEven(int number) {
        if (number % 2 == 0) {
            return number;
        }
        else {
            return number - 1;
        }
    }
    
    private static int closestLowerOdd(int number) {
        if (number % 2 != 0) {
            return number;
        }
        else {
            return number - 1;
        }
    }
    
}
