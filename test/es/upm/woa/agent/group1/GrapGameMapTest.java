package es.upm.woa.agent.group1;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import es.upm.woa.agent.group1.map.CellTranslation;
import es.upm.woa.agent.group1.map.MapCell;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author ISU
 */
public class GrapGameMapTest {
    
    private GraphGameMap gameMap;
    
    public GrapGameMapTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        gameMap = GraphGameMap.getInstance();
    }
    
    @After
    public void tearDown() {
    }
    
    @Test
    public void findSameShortestPathTest() {
        MapCell source = new EmptyMapCell(1, 1);
        
        gameMap.addCell(source);
        
        List<CellTranslation> shortestPath = gameMap.findShortestPath(source, source);
        
        assertEquals(shortestPath, Arrays.asList(new MapCell[]{}));
    }
    
    @Test
    public void findSimpleShortestPathTest() {
        MapCell source = new EmptyMapCell(1, 1);
        MapCell target = new EmptyMapCell(2, 2);
        
        CellTranslation expectedTranslation
                = new CellTranslation(CellTranslation.TranslateDirection.RDOWN);
        
        gameMap.addCell(source);
        gameMap.addCell(target);
        gameMap.connectPath(source, target, expectedTranslation);
        
        List<CellTranslation> shortestPath = gameMap.findShortestPath(source, target);
        
        
        assertEquals(shortestPath, Arrays.asList(new CellTranslation[]{expectedTranslation}));
    }
    
    @Test
    public void findNoShortestPathTest() {
        MapCell source = new EmptyMapCell(1, 1);
        MapCell target = new EmptyMapCell(4, 4);
        
        gameMap.addCell(source);
        gameMap.addCell(target);
        
        List<CellTranslation> shortestPath = gameMap.findShortestPath(source, target);
        
        assertTrue(shortestPath.isEmpty());
    }
    

    
}
