/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.map.finder;

import es.upm.woa.group1.map.MapCell;

/**
 *
 * @author ISU
 */
public interface MapCellEvaluator {

    boolean match(MapCell site);
    
}
