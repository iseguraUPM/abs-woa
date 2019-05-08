/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.agent.group1.map;

import java.util.Arrays;

/**
 *
 * @author ISU
 */
public class CellTranslation {
    
    public static final int [] V_DOWN = {2,0};
    public static final int [] V_UP = {-2,0};
    public static final int [] V_LUP = {-1,-1};
    public static final int [] V_RUP = {-1,1};
    public static final int [] V_LDOWN = {1,-1};
    public static final int [] V_RDOWN = {1,1};
    
    public enum TranslateDirection {
        
        UP(1), RUP(2), RDOWN(3), DOWN(4), LDOWN(5), LUP(6);
        
        public final int translationCode;
        
        private TranslateDirection(int translationCode) {
            this.translationCode = translationCode;
        }
    }
    
    private final TranslateDirection direction;
    
    public CellTranslation(TranslateDirection direction) {
        this.direction = direction;
    }
    
    public int getTranslationCode() {
        return direction.translationCode;
    }
    
    /**
     * 
     * @return inverse translate operation
     */
    public CellTranslation generateInverse() {
        int[] vector = getTranslationVector(direction);
        
        int[] inverseVector = {vector[0] * -1, vector[1] * -1};
        
        return new CellTranslation(getTranslateDirection(inverseVector));
    }
    
    private TranslateDirection getTranslateDirection(int [] vector) {
        if (Arrays.equals(vector, V_UP)) {
            return TranslateDirection.UP;
        }
        else if (Arrays.equals(vector, V_RUP)) {
            return TranslateDirection.RUP;
        }
        else if (Arrays.equals(vector, V_RDOWN)) {
            return TranslateDirection.RDOWN;
        }
        else if (Arrays.equals(vector, V_DOWN)) {
            return TranslateDirection.DOWN;
        }
        else if (Arrays.equals(vector, V_LDOWN)) {
            return TranslateDirection.LDOWN;
        }
        else if (Arrays.equals(vector, V_LUP)) {
            return TranslateDirection.LUP;
        }
        else {
            return null;
        }
    }
    
    private int [] getTranslationVector(TranslateDirection direction) {
        switch (direction) {
            case UP:
                return V_UP;
            case RUP:
                return V_RUP;
            case RDOWN:
                return V_RDOWN;
            case DOWN:
                return V_DOWN;
            case LDOWN:
                return V_LDOWN;
            case LUP:
                return V_LUP;
            default:
                return null;
        } 
    }
    
    @Override
    public String toString() {
        switch (direction) {
            case UP:
                return "UP";
            case RUP:
                return "RIGHT and UP";
            case RDOWN:
                return "RIGHT and DOWN";
            case DOWN:
                return "DOWN";
            case LDOWN:
                return "LEFT and DOWN";
            case LUP:
                return "LEFT and UP";
            default:
                throw new AssertionError(direction.name());
        }
    }
    
}
