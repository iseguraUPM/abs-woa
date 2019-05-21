/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.map;

/**
 *
 * @author ISU
 */
public interface LocationFinder {
    
    /**
     * Finds a map cell site that matches the conditions.
     * Starts looking for the closest possible sites to the zero site.
     * This method does not take into account the unknown surrounding cells
     * of the candidates.
     * @param zero point where to find the closest construction site
     * @param evaluator
     * @return the closest cell that matches conditions
     * or null if it could not be found
     */
    MapCell findMatchingSiteCloseTo(MapCell zero, MapCellEvaluator evaluator);
    
    /**
     * Finds a map cell site that matches the conditions.
     * This method does not take into account the unknown surrounding cells
     * of the candidates.
     * @param evaluator
     * @return a cell that matches the conditions or null if it could not be found
     */
    MapCell findMatchingSite(MapCellEvaluator evaluator);
    
}
