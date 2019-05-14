/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.gui;

import java.io.IOException;

/**
 *
 * @author ISU
 */
public class WoaGUIFactory {
    
    private static WoaGUIFactory instance;
    
    private WoaGUI guiInstance;
    
    private WoaGUIFactory() {
    }
    
    public static WoaGUIFactory getInstance() {
        if (instance == null) {
            instance = new WoaGUIFactory();
        }
        
        return instance;
    }
    
    public WoaGUI getGUI() throws IOException {
        if (guiInstance == null) {
            guiInstance = HttpWoaGUI.getInstance();
        }
        
        if (guiInstance == null) {
            throw new IOException("Could not connect to GUI endpoint");
        }
        
        return guiInstance;
    }
    
}
