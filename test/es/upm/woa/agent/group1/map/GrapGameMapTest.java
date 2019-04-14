package es.upm.woa.agent.group1.map;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

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
        gameMap = GraphGameMap.getInstance(6, 6);
    }
    
    @After
    public void tearDown() {
    }
    
    @Test
    public void findSameShortestPathTest() {
        MapCell source = new EmptyMapCell(1, 1);
        
        gameMap.addCell(source);
        
        List<MapCell> shortestPath = gameMap.findShortestPath(source, source);
        
        assertEquals(shortestPath, Arrays.asList(new MapCell[]{source}));
    }
    
    @Test
    public void findSimpleShortestPathTest() {
        MapCell source = new EmptyMapCell(1, 1);
        MapCell target = new EmptyMapCell(2, 2);
        
        gameMap.addCell(source);
        gameMap.addCell(target);
        
        List<MapCell> shortestPath = gameMap.findShortestPath(source, target);
        
        assertEquals(shortestPath, Arrays.asList(new MapCell[]{source, target}));
    }
    
    @Test
    public void findWrappedShortestPathTest() {
        MapCell source = new EmptyMapCell(1, 1);
        MapCell target = new EmptyMapCell(6, 6);
        
        gameMap.addCell(source);
        gameMap.addCell(target);
        
        List<MapCell> shortestPath = gameMap.findShortestPath(source, target);
        
        assertEquals(shortestPath, Arrays.asList(new MapCell[]{source, target}));
    }
    
    @Test
    public void findNoShortestPathTest() {
        MapCell source = new EmptyMapCell(1, 1);
        MapCell target = new EmptyMapCell(4, 4);
        
        gameMap.addCell(source);
        gameMap.addCell(target);
        
        List<MapCell> shortestPath = gameMap.findShortestPath(source, target);
        
        assertTrue(shortestPath.isEmpty());
    }
    
    @Test
    public void findComplexShortestPathTest() {
        MapCell source = new EmptyMapCell(1, 1);
        MapCell target = new EmptyMapCell(3, 3);
        
        MapCell inWay1 = new EmptyMapCell(6, 6);
        MapCell inWay2 = new EmptyMapCell(1, 5);
        MapCell inWay3 = new EmptyMapCell(2, 4);
        
        gameMap.addCell(source);
        gameMap.addCell(target);
        gameMap.addCell(inWay1);
        gameMap.addCell(inWay2);
        gameMap.addCell(inWay3);
        
        List<MapCell> shortestPath = gameMap.findShortestPath(source, target);
        
        assertEquals(shortestPath, Arrays.asList(new MapCell[]{source
                , inWay1, inWay2, inWay3, target}));
        
        
    }
    
    @Test
    public void findComplexCompetingShortestPathTest() {
        MapCell source = new EmptyMapCell(2, 2);
        MapCell target = new EmptyMapCell(4, 4);
        
        MapCell inWay1 = new EmptyMapCell(6, 6);
        MapCell inWay2 = new EmptyMapCell(5, 1);
        MapCell inWay3 = new EmptyMapCell(1, 5);
        MapCell inWay4 = new EmptyMapCell(2, 4);
        MapCell inWay5 = new EmptyMapCell(6, 2);
        MapCell inWay6 = new EmptyMapCell(5, 3);
        MapCell inWay7 = new EmptyMapCell(3, 5);
        
        
        gameMap.addCell(source);
        gameMap.addCell(target);
        gameMap.addCell(inWay1);
        gameMap.addCell(inWay2);
        gameMap.addCell(inWay3);
        gameMap.addCell(inWay4);
        gameMap.addCell(inWay5);
        gameMap.addCell(inWay6);
        gameMap.addCell(inWay7);
        
        List<MapCell> shortestPath = gameMap.findShortestPath(source, target);
        
        assertEquals(shortestPath, Arrays.asList(new MapCell[]{source
                , inWay5, inWay6, target}));
        
        
    }
    
}
