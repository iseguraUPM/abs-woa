/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import es.upm.woa.group1.map.CellTranslation;
import es.upm.woa.group1.map.MapCell;
import java.io.Serializable;

/**
 *
 * @author ISU
 */
class NewGraphConnection implements Serializable {
    
    public MapCell source;
    public CellTranslation direction;
    public MapCell target;

    public NewGraphConnection() {
    }
    
}
