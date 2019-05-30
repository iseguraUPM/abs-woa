/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.upm.woa.group1.agent;

import es.upm.woa.group1.map.finder.TownHallSiteEvaluator;
import es.upm.woa.group1.agent.strategy.StrategyFactory;
import es.upm.woa.group1.WoaDefinitions;
import es.upm.woa.group1.agent.strategy.Strategy;
import es.upm.woa.group1.agent.strategy.StrategyEnvelop;
import es.upm.woa.group1.agent.strategy.UnitStatusHanlder;
import es.upm.woa.group1.map.MapCell;
import es.upm.woa.group1.map.PathfinderGameMap;
import es.upm.woa.group1.map.finder.ForestResourceEvaluator;
import es.upm.woa.group1.map.finder.MapCellEvaluator;
import es.upm.woa.group1.map.finder.OreResourceEvaluator;
import es.upm.woa.group1.map.finder.OtherBuildingSiteEvaluator;
import es.upm.woa.group1.map.finder.TribeBuildingEvaluator;

import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javafx.util.Pair;

/**
 *
 * @author ISU
 */
final class TribeStrategyBehaviour extends SimpleBehaviour implements UnitStatusHanlder {

    private static final int EXPLORE_PHASE_TICKS = 30;

    private final WoaAgent agent;
    private final Ticker ticker;
    private final SendAssignStrategyHelper strategyHelper;
    private final PathfinderGameMap graphMap;
    private final int resourceCapUpgrade;
    private final Collection<Unit> unitCollection;
    private final TribeResources tribeResources;
    private final MapCellFinder mapCellFinder;

    private long elapsedTicks;

    private final Set<Unit> workerUnits;
    private final Set<Unit> miners;
    private final Set<Unit> lumberjacks;
    private final Set<Unit> farmers;
    private Unit townHallBuilder;
    private Unit farmBuilder;
    private Unit unitBuilder;
    private Unit storeBuilder;
    private int builtTownHalls;
    private int builtFarms;
    private int builtStores;
    private int builtUnits;
    private boolean mapExplored;
    
    private int needGold;
    private int needWood;
    private int needStone;
    private int needFood;
    
    private int needTownHalls;
    private int needFarms;
    private int needStores;
    private int needUnits;

    private long startStrategyElapsedTicks;

    public TribeStrategyBehaviour(WoaAgent agent, Ticker ticker,
            SendAssignStrategyHelper strategyHelper,
            PathfinderGameMap graphMap,
            int resourceCapUpgrade,
            Collection<Unit> unitCollection,
            TribeResources resourceAccount,
            MapCellFinder mapCellFinder) {
        super(agent);
        this.agent = agent;
        this.ticker = ticker;
        this.strategyHelper = strategyHelper;
        this.graphMap = graphMap;
        this.resourceCapUpgrade = resourceCapUpgrade;
        this.unitCollection = unitCollection;
        this.tribeResources = resourceAccount;
        this.mapCellFinder = mapCellFinder;

        this.elapsedTicks = ticker.getCurrentTick();

        this.workerUnits = new HashSet<>();
        this.miners = new HashSet<>();
        this.lumberjacks = new HashSet<>();
        this.farmers = new HashSet<>();
        this.townHallBuilder = null;
        this.farmBuilder = null;
        this.unitBuilder = null;
        this.builtTownHalls = 0;
        this.builtFarms = 0;
        this.builtStores = 0;
        this.builtUnits = 0;
        this.mapExplored = false;

        this.needGold = 0;
        this.needFood = 0;
        this.needStone = 0;
        this.needWood = 0;
        
        this.needTownHalls = 0;
        this.needFarms = 0;
        this.needStores = 0;
        this.needUnits = 0;
    }

    @Override
    public void action() {
        if (elapsedTicks < ticker.getCurrentTick()) {
            performRound();
            elapsedTicks = ticker.getCurrentTick();
        }
    }

    @Override
    public boolean done() {
        return false;
    }

    private void performRound() {
        checkNewUnits();
        checkFinishedJobs();
        checkResources();
        assignJobs();
        sendStrategies();
    }

    private void checkNewUnits() {
        if (workerUnits.isEmpty()) {
            startStrategyElapsedTicks = ticker.getCurrentTick();
            needTownHalls = 1;
        }
        
        if (unitCollection.size() > workerUnits.size()) {
            assignExplorerStrategy();
        }
    }

    private void assignExplorerStrategy() {
        unitCollection.stream()
                .filter(unit -> !workerUnits.contains(unit))
                .forEach(unit -> {
                    workerUnits.add(unit);
                    // NOTE: one in three units will be a backtracker
                    boolean backTracker = workerUnits.size() % 3 == 0;
                    
                    strategyHelper.unicastStrategy(unit.getId(),
                            StrategyFactory.envelopFreeExploreStrategy(Strategy.MID_PRIORITY, backTracker));
                });
    }

    private void checkFinishedJobs() {
        workerUnits.stream().filter(unit -> !unit.isBusy())
                .forEach(unit -> {
                    removeJob(unit);
                });
    }

    private void checkResources() {
        if (startStrategyElapsedTicks + EXPLORE_PHASE_TICKS <= ticker.getCurrentTick()) {
            if (builtTownHalls < needTownHalls && townHallBuilder == null) {
                needGold = Math.max(0, WoaDefinitions.TOWN_HALL_GOLD_COST - tribeResources.getGold());
                needStone = Math.max(0, WoaDefinitions.TOWN_HALL_STONE_COST - tribeResources.getStone());
                needWood = Math.max(0, WoaDefinitions.TOWN_HALL_WOOD_COST - tribeResources.getWood());
                needFood = 0;
            } else if (builtFarms < needFarms && farmBuilder == null) {
                needGold = Math.max(0, WoaDefinitions.FARM_GOLD_COST - tribeResources.getGold());
                needStone = Math.max(0, WoaDefinitions.FARM_STONE_COST - tribeResources.getStone());
                needWood = Math.max(0, WoaDefinitions.FARM_WOOD_COST - tribeResources.getWood());
                needFood = 0;
            } else if (builtUnits < needUnits && unitBuilder == null) {
                needWood = 0;
                needStone = 0;
                needGold = Math.max(0, WoaDefinitions.UNIT_GOLD_COST - tribeResources.getGold());
                needFood = Math.max(0, WoaDefinitions.UNIT_FOOD_COST - tribeResources.getFood());
            } else if (builtStores < needStores && storeBuilder == null) {
                needGold = Math.max(0, WoaDefinitions.STORE_GOLD_COST - tribeResources.getGold());
                needStone = Math.max(0, WoaDefinitions.STORE_STONE_COST - tribeResources.getStone());
                needWood = Math.max(0, WoaDefinitions.STORE_WOOD_COST - tribeResources.getWood());
                needFood = 0;
            } else {
                needFood = 0;
                needGold = 0;
                needWood = 0;
                needStone = 0;
            }
        }
    }

    private void assignJobs() {
        int requiredFreeUnits = 1;
        if (mapExplored) {
            requiredFreeUnits = 0;
        }
        
        Collection<Unit> freeUnits = findFreeUnits(workerUnits);
        assignBuilders(freeUnits, requiredFreeUnits);
        assignRemainingJobs(freeUnits, requiredFreeUnits);
    }

    private void assignRemainingJobs(Collection<Unit> freeUnits, int requiredFreeUnits) {
        List<Pair<Integer, Collection<Unit>>> requiredJobs = new ArrayList<>();
        if (builtFarms > 0) {
            requiredJobs.add(new Pair(needFood, farmers));
        }
        requiredJobs.add(new Pair(Math.max(needGold, needStone), miners));
        requiredJobs.add(new Pair(needWood, lumberjacks));

        while (freeUnits.size() > requiredFreeUnits) {
            int currentFree = freeUnits.size();

            requiredJobs.stream().sorted((Pair<Integer, Collection<Unit>> p1
                    , Pair<Integer, Collection<Unit>> p2) -> {
                if (p1.getValue().size() == p2.getValue().size()) {
                    return p2.getKey() - p1.getKey();
                }
                else {
                    return p1.getValue().size() - p2.getValue().size();
                }
            }).filter(pair -> pair.getKey() > 0).forEach(pair -> {
                if (freeUnits.size() > 0) {
                    changeOneWorkerJob(freeUnits, pair.getValue());
                }
            });

            if (currentFree == freeUnits.size()) {
                break;
            }
        }
    }

    private void assignBuilders(Collection<Unit> freeUnits, int requiredFreeUnits) {
        
        if (builtTownHalls < needTownHalls && tribeResources.canAffordTownHall()
                && townHallBuilder == null
                && freeUnits.size() > requiredFreeUnits) {
            assignTownHallBuilder(freeUnits);
        } else if (builtTownHalls > 0 && builtFarms < needFarms
                && tribeResources.canAffordFarm()
                && farmBuilder == null
                && freeUnits.size() > requiredFreeUnits) {
            assignFarmBuilder(freeUnits);
        } else if (builtTownHalls > 0 && builtUnits < needUnits
                && tribeResources.canAffordUnit()
                && unitBuilder == null
                && freeUnits.size() > requiredFreeUnits) {
            assignUnitBuilder(freeUnits);
        } else if (builtTownHalls > 0 && builtStores < needStores
                && tribeResources.canAffordUnit()
                && storeBuilder == null
                && freeUnits.size() > requiredFreeUnits) {
            assignStoreBuilder(freeUnits);
        }
    }

    private void removeJob(Unit unit) {
        farmers.remove(unit);
        miners.remove(unit);
        lumberjacks.remove(unit);
        unit.setFree();
    }

    private void changeOneWorkerJob(Collection<Unit> from,
            Collection<Unit> to) {
        try {
            Unit candidate;
            if (to == farmers) {
                candidate = findClosestUnit(from, new TribeBuildingEvaluator(agent.getAID(), WoaDefinitions.FARM));
            } else if (to == miners) {
                candidate = findClosestUnit(from, new OreResourceEvaluator());
            } else {
                candidate = findClosestUnit(from, new ForestResourceEvaluator());
            }

            from.remove(candidate);
            to.add(candidate);
        } catch (NoSuchElementException ex) {
            // Still busy
        }
    }

    private void assignTownHallBuilder(Collection<Unit> freeWorkers) {
        try {
            Unit builder = findClosestUnit(freeWorkers, new TownHallSiteEvaluator(graphMap));

            townHallBuilder = builder;
            builder.setBusy();

            strategyHelper.unicastStrategy(builder.getId(),
                    StrategyFactory
                            .envelopCreateBuildingStrategy(Strategy.HIGH_PRIORITY,
                                    WoaDefinitions.TOWN_HALL));
        } catch (NoSuchElementException ex) {
            agent.log(Level.WARNING, "Could not find a town hall builder");
        }
    }

    private void assignUnitBuilder(Collection<Unit> freeWorkers) {
        try {
            Unit builder = findClosestUnit(freeWorkers
                    , new TribeBuildingEvaluator(agent.getAID()
                            , WoaDefinitions.TOWN_HALL));

            unitBuilder = builder;
            builder.setBusy();

            strategyHelper.unicastStrategy(builder.getId(),
                    StrategyFactory
                            .envelopCreateUnitStrategy(Strategy.HIGH_PRIORITY));
        } catch (NoSuchElementException ex) {
            agent.log(Level.WARNING, "Could not find a unit builder");
        }
    }

    private void assignFarmBuilder(Collection<Unit> freeWorkers) {
        try {
            Unit builder = findClosestUnit(freeWorkers
                    , new OtherBuildingSiteEvaluator(graphMap, agent.getAID()));

            farmBuilder = builder;
            builder.setBusy();

            strategyHelper.unicastStrategy(builder.getId(),
                    StrategyFactory
                            .envelopCreateBuildingStrategy(Strategy.HIGH_PRIORITY
                                    , WoaDefinitions.FARM));
        } catch (NoSuchElementException ex) {
            agent.log(Level.WARNING, "Could not find a farm builder");
        }
    }
    
    private void assignStoreBuilder(Collection<Unit> freeWorkers) {
        try {
            Unit builder = findClosestUnit(freeWorkers
                    , new OtherBuildingSiteEvaluator(graphMap, agent.getAID()));

            storeBuilder = builder;
            builder.setBusy();

            strategyHelper.unicastStrategy(builder.getId(),
                    StrategyFactory
                            .envelopCreateBuildingStrategy(Strategy.HIGH_PRIORITY
                                    , WoaDefinitions.STORE));
        } catch (NoSuchElementException ex) {
            agent.log(Level.WARNING, "Could not find a store builder");
        }
    }


    @Override
    public void onChangedPosition(AID unitAID, int xCoord, int yCoord) {
        try {
            Unit movedUnit = unitCollection.stream().filter(unit -> unit.getId()
                    .equals(unitAID)).findAny().get();
            movedUnit.setPosition(xCoord, yCoord);
            agent.log(Level.FINER, unitAID.getLocalName() + " changed position");
        } catch (NoSuchElementException ex) {
            agent.log(Level.WARNING, "Could not change position of unknown unit: "
                    + unitAID.getLocalName());
        }
    }

    @Override
    public void onStartedBuilding(AID unitAID, String buildingType) {
        switch (buildingType) {
            case WoaDefinitions.TOWN_HALL:
                tribeResources.purchaseTownHall();
                break;
            case WoaDefinitions.STORE:
                tribeResources.purchaseStore();
                break;
            case WoaDefinitions.FARM:
                tribeResources.purchaseFarm();
                break;
            default:
                agent.log(Level.WARNING, "Cannot purchase unknown building: "
                        + buildingType);
                break;
        }
        agent.log(Level.FINE, unitAID.getLocalName() + " started "
                + buildingType + " construction");
    }

    @Override
    public void onStartingUnitCreation(AID unitAID) {
        tribeResources.purchaseUnit();
        agent.log(Level.FINE, unitAID.getLocalName()
                + " started creating a unit");
    }

    @Override
    public void onExploitedResource(AID unitAID, String resourceType, int amount) {
        int currentAmount;
        switch (resourceType) {
            case WoaDefinitions.GOLD:
                tribeResources.addGold(amount);
                currentAmount = tribeResources.getGold();
                break;
            case WoaDefinitions.FOOD:
                tribeResources.addFood(amount);
                currentAmount = tribeResources.getFood();
                break;
            case WoaDefinitions.WOOD:
                tribeResources.addWood(amount);
                currentAmount = tribeResources.getWood();
                break;
            case WoaDefinitions.STONE:
                tribeResources.addStone(amount);
                currentAmount = tribeResources.getStone();
                break;
            default:
                return;
        }
        agent.log(Level.FINE, unitAID.getLocalName() + " gained " + amount
                + " of " + resourceType + " (" + currentAmount + ", "
                + tribeResources.getStorageSpaceLeft() + ")");
    }

    @Override
    public void onFinishedBuilding(AID unitAID, String buildingType) {
        switch (buildingType) {
            case WoaDefinitions.TOWN_HALL:
                unassignTownHallBuilder();
                builtTownHalls++;
                needFarms++;
                break;
            case WoaDefinitions.FARM:
                unassignFarmBuilder();
                builtFarms++;
                needUnits++;
                break;
            case WoaDefinitions.STORE:
                unassignStoreBuilder();
                tribeResources.upgradeStorageSpace(resourceCapUpgrade);
                needFarms++;
                builtStores++;
                break;
            default:
                agent.log(Level.WARNING, "Finished construction of unknown building: " + buildingType);
                return;
        }

        agent.log(Level.FINE, unitAID.getLocalName() + " finished construction of " + buildingType);
    }

    @Override
    public void onErrorBuilding(AID unitAID, String buildingType) {
        switch (buildingType) {
            case WoaDefinitions.TOWN_HALL:
                unassignTownHallBuilder();
                tribeResources.refundTownHall();
                break;
            case WoaDefinitions.STORE:
                unassignStoreBuilder();
                tribeResources.refundStore();
                break;
            case WoaDefinitions.FARM:
                unassignFarmBuilder();
                tribeResources.refundFarm();
                break;
            default:
                agent.log(Level.WARNING, "Cannot refund unknown building: "
                        + buildingType);
                break;
        }
        agent.log(Level.FINE, unitAID.getLocalName() + " could not build " + buildingType + ". Resources refunded");
    }

    private void unassignTownHallBuilder() {
        if (townHallBuilder != null) {
            townHallBuilder.setFree();
            townHallBuilder = null;
        }
    }
    
    private void unassignStoreBuilder() {
        if (storeBuilder != null) {
            storeBuilder.setFree();
            storeBuilder = null;
        }
    }

    private void unassignFarmBuilder() {
        if (farmBuilder != null) {
            farmBuilder.setFree();
            farmBuilder = null;
        }
    }

    @Override
    public void onFinishedUnitCreation(AID unitAID) {
        unassignUnitBuilder();
        builtUnits++;
        needStores++;
        agent.log(Level.FINE, unitAID.getLocalName() + " finished creation of new unit");
    }

    private void unassignUnitBuilder() {
        if (unitBuilder != null) {
            unitBuilder.setFree();
            unitBuilder = null;
        }
    }

    @Override
    public void onErrorUnitCreation(AID unitAID) {
        unassignUnitBuilder();
        tribeResources.refundUnit();
        agent.log(Level.FINE, unitAID.getLocalName() + " could not create unit. Resources refunded");
    }

    @Override
    public void onFinishedExploiting(AID unitAID, String resourceType) {
        switch (resourceType) {
            case WoaDefinitions.ORE:
                setUnitFinishedJob(miners, unitAID);
                break;
            case WoaDefinitions.FOREST:
                setUnitFinishedJob(lumberjacks, unitAID);
                break;
            case WoaDefinitions.FARM:
                setUnitFinishedJob(farmers, unitAID);
                break;
            default:
                agent.log(Level.WARNING, "Unknown resource type: " + resourceType);
                break;
        }

        agent.log(Level.FINE, unitAID.getLocalName()
                + " finished exploiting for " + resourceType);
    }

    @Override
    public void onFinishedExploring(AID unitAID) {
        mapExplored = true;
        agent.log(Level.FINE, unitAID.getLocalName() + " finished exploring");
    }

    private void setUnitFinishedJob(Collection<Unit> workers,
            AID unitAID) {
        if (workers.isEmpty()) {
            return;
        }

        try {
            Unit worker = workers.stream()
                    .filter(unit -> unit.getId()
                    .equals(unitAID)).findAny().get();
            worker.setFree();
        } catch (NoSuchElementException ex) {
            agent.log(Level.WARNING, "Could not find worker unit");
        }
    }

    private void sendStrategies() {
        int requiredOre = roundUpDivision(Math.max(needGold, needStone), miners.size());
        int requiredWood = roundUpDivision(needWood, lumberjacks.size());
        int requiredFood = roundUpDivision(needFood, farmers.size());

        assignStrategies(miners, StrategyFactory
                .envelopExploitResourceStrategy(Strategy.MID_PRIORITY - 1,
                        WoaDefinitions.ORE, requiredOre));

        assignStrategies(lumberjacks, StrategyFactory
                .envelopExploitResourceStrategy(Strategy.MID_PRIORITY - 1,
                        WoaDefinitions.FOREST, requiredWood));

        assignStrategies(farmers, StrategyFactory
                .envelopExploitResourceStrategy(Strategy.MID_PRIORITY - 1,
                        WoaDefinitions.FARM, requiredFood));
    }

    private int roundUpDivision(float num, int divisor) {
        return (int) Math.floor(num / divisor);
    }

    private void assignStrategies(Collection<Unit> units, StrategyEnvelop strategyEnvelop) {
        Collection<Unit> freeUnits = findFreeUnits(units);
        if (!freeUnits.isEmpty()) {
            freeUnits.stream().forEach(unit -> unit.setBusy());
            strategyHelper.multicastStrategy(collectAIDs(freeUnits),
                    strategyEnvelop);
        }
    }

    private Collection<Unit> findFreeUnits(Collection<Unit> units) {
        Collection<Unit> freeUnits = units.stream()
                .filter(unit -> !unit.isBusy())
                .collect(Collectors.toSet());
        return freeUnits;
    }

    private AID[] collectAIDs(Collection<? extends Unit> units) {
        return units.stream().map(unit -> unit.getId())
                .collect(Collectors.toList())
                .toArray(new AID[units.size()]);
    }

    private Unit findClosestUnit(Collection<Unit> from, MapCellEvaluator evaluator) throws NoSuchElementException {
        return from.stream().min(new CellDistanceComparator(evaluator)).get();
    }
    
    private class CellDistanceComparator implements Comparator<Unit> {

        private final MapCellEvaluator evaluator;

        public CellDistanceComparator(MapCellEvaluator evaluator) {
            this.evaluator = evaluator;
        }

        @Override
        public int compare(Unit unit1, Unit unit2) {
            MapCell unitPosition = graphMap.getCellAt(unit1.getCoordX(), unit1.getCoordY());

            MapCell siteCandidate = mapCellFinder
                    .findMatchingSiteCloseTo(unitPosition, evaluator);
            
            if (siteCandidate == null) {
                return Integer.MAX_VALUE;
            }

            if (unitPosition.equals(siteCandidate)) {
                return 0;
            }

            int distance
                    = graphMap.findShortestPath(unitPosition,
                            siteCandidate).size();

            if (distance == 0) {
                return Integer.MAX_VALUE;
            } else {
                return distance;
            }
        }

    }

}
