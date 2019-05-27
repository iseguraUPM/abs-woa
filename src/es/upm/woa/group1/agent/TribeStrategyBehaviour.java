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

import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;

import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 *
 * @author ISU
 */
final class TribeStrategyBehaviour extends SimpleBehaviour implements UnitStatusHanlder {

    private static final int FIRST_PHASE = 0;
    private static final int SECOND_PHASE = 1;
    private static final int THIRD_PHASE = 2;
    private static final int FOURTH_PHASE = 3;
    private static final int FIFTH_PHASE = 3;

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
    private int phase;

    private final Set<Unit> workingUnits;
    private final Set<Unit> exporers;
    private final Set<Unit> miners;
    private final Set<Unit> lumberjacks;
    private final Set<Unit> farmers;
    private final Set<Unit> builders;
    private int builtTownHalls;
    private int builtFarms;
    private int builtStores;

    private long phaseStartElapsedTicks;

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
        this.phase = FIRST_PHASE;

        this.workingUnits = new HashSet<>();
        this.exporers = new HashSet<>();
        this.miners = new HashSet<>();
        this.lumberjacks = new HashSet<>();
        this.farmers = new HashSet<>();
        this.builders = new HashSet<>();
        this.builtTownHalls = 0;
        this.builtFarms = 0;
        this.builtStores = 0;
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
        checkPhase();
        switch (phase) {
            case FIRST_PHASE:
                performFirstPhase();
                break;
            case SECOND_PHASE:
                performSecondPhase();
                break;
            case THIRD_PHASE:
                performThirdPhase();
                break;
            default:
                break;
        }
        assignJobs();
    }

    private void checkPhase() {
        /*if (phase == FIRST_PHASE && tribeResources.canAffordTownHall()) {
            phase = SECOND_PHASE;
        } else if (phase == SECOND_PHASE && builtTownHalls > 0) {
            phase = THIRD_PHASE;
        }
        else if (phase == THIRD_PHASE && tribeResources.canAffordFarm()) {
            phase = FOURTH_PHASE;
        }
        else if (phase == FOURTH_PHASE && builtFarms > 0) {
            phase = FIFTH_PHASE;
        }*/
    }

    private void performFirstPhase() {
        if (workingUnits.isEmpty()) {
            workingUnits.addAll(unitCollection);
            phaseStartElapsedTicks = ticker.getCurrentTick();
        }

        if (exporers.isEmpty()) {
            exporers.addAll(workingUnits);
        }

        if (phaseStartElapsedTicks + EXPLORE_PHASE_TICKS
                <= ticker.getCurrentTick() && miners.isEmpty()
                && lumberjacks.isEmpty()) {
            randomChangeJob(exporers, miners);
            randomChangeJob(exporers, lumberjacks);
        }
    }

    protected void randomChangeJob(Collection<Unit> from,
             Collection<Unit> to) {
        Unit candidate = from.stream().findAny().get();
        from.remove(candidate);
        candidate.setFree();
        to.add(candidate);
    }

    private void performSecondPhase() {
        if (builtTownHalls == 0 && builders.isEmpty()) {
            try {
                assignTownHallConstructor();
            } catch (NoSuchElementException ex) {
                agent.log(Level.WARNING, "No construction site found");
            }
        }
        else if (builtTownHalls == 0 && !builders.isEmpty()) {
            repeatTownHallConstruction();
        }
    }
    
    private void performThirdPhase() {
        if (!builders.isEmpty()) {
            try {
                Unit builder = builders.stream().filter(unit
                        -> !unit.isBusy()).findAny().get();
                removeJob(builder);
                miners.add(builder);
            } catch (NoSuchElementException ex) {
                // Still busy
            }
        }
    }

    private AID[] collectAIDs(Collection<? extends Unit> units) {
        return units.stream().map(unit -> unit.getId())
                .collect(Collectors.toList())
                .toArray(new AID[units.size()]);
    }

    private void assignTownHallConstructor() {
        Unit builder
                = workingUnits.stream().min((Unit t, Unit t1) -> {
                    MapCell unitPosition = graphMap.getCellAt(t.getCoordX(), t.getCoordY());

                    MapCell siteCandidate = mapCellFinder
                            .findMatchingSiteCloseTo(unitPosition,
                                     new TownHallSiteEvaluator(graphMap));

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
                }).get();

        removeJob(builder);
        builder.setBusy();
        builders.add(builder);

        strategyHelper.unicastStrategy(builder.getId(),
                 StrategyFactory
                        .envelopCreateBuildingStrategy(Strategy.HIGH_PRIORITY,
                                 WoaDefinitions.TOWN_HALL));
    }

    private void removeJob(Unit unit) {
        exporers.remove(unit);
        farmers.remove(unit);
        miners.remove(unit);
        lumberjacks.remove(unit);
        unit.setFree();
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
                + tribeResources.getStorageSpaceLeft()+ ")");
    }

    @Override
    public void onFinishedBuilding(AID unitAID, String buildingType) {
        setUnitFinishedJob(builders, unitAID);
        switch (buildingType) {
            case WoaDefinitions.TOWN_HALL:
                builtTownHalls++;
                break;
            case WoaDefinitions.FARM:
                builtFarms++;
                break;
            case WoaDefinitions.STORE:
                tribeResources.upgradeStorageSpace(resourceCapUpgrade);
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
        setUnitFinishedJob(builders, unitAID);
        switch (buildingType) {
            case WoaDefinitions.TOWN_HALL:
                tribeResources.refundTownHall();
                break;
            case WoaDefinitions.STORE:
            //break;
            case WoaDefinitions.FARM:
                throw new UnsupportedOperationException("Building " + buildingType + " refund implementation");
            //break;
            default:
                agent.log(Level.WARNING, "Cannot refund unknown building: "
                        + buildingType);
                break;
        }
        agent.log(Level.FINE, unitAID.getLocalName() + " could not build " + buildingType + ". Resources refunded");
    }

    @Override
    public void onFinishedUnitCreation(AID unitAID) {
        agent.log(Level.FINE, unitAID.getLocalName() + " finished creation of new unit");
    }

    @Override
    public void onErrorUnitCreation(AID unitAID) {
        tribeResources.refundUnit();
        setUnitFinishedJob(builders, unitAID);
        agent.log(Level.FINE, unitAID.getLocalName() + " could not create unit. Resources refunded");
    }

    @Override
    public void onFinishedExploiting(AID unitAID, String resourceType) {
        setUnitFinishedJob(miners, unitAID);
        agent.log(Level.FINE, unitAID.getLocalName()
                + " finished exploiting for " + resourceType);
    }

    @Override
    public void onFinishedExploring(AID unitAID) {
        setUnitFinishedJob(exporers, unitAID);
        agent.log(Level.FINE, unitAID.getLocalName() + " finished exploring");
    }

    protected void setUnitFinishedJob(Collection<Unit> workers,
             AID unitAID) {
        if (workers.isEmpty()) {
            return;
        }

        try {
            Unit builder = workers.stream()
                    .filter(unit -> unit.getId()
                    .equals(unitAID)).findAny().get();
            builder.setFree();
        } catch (NoSuchElementException ex) {
            agent.log(Level.WARNING, "Could not find builder unit");
        }
    }

    private void assignJobs() {
        assignStrategies(exporers, StrategyFactory
                .envelopFreeExploreStrategy(Strategy.MID_PRIORITY));

        assignStrategies(miners, StrategyFactory
                .envelopExploitResourceStrategy(Strategy.MID_PRIORITY - 1,
                         WoaDefinitions.ORE));

        assignStrategies(lumberjacks, StrategyFactory
                .envelopExploitResourceStrategy(Strategy.MID_PRIORITY - 1,
                         WoaDefinitions.FOREST));

        assignStrategies(farmers, StrategyFactory
                .envelopExploitResourceStrategy(Strategy.MID_PRIORITY - 1,
                         WoaDefinitions.FARM));
    }

    protected void assignStrategies(Collection<Unit> units, StrategyEnvelop strategyEnvelop) {
        Collection<Unit> freeUnits = units.stream()
                .filter(unit -> !unit.isBusy())
                .collect(Collectors.toSet());
        if (!freeUnits.isEmpty()) {
            freeUnits.stream().forEach(unit -> unit.setBusy());
            strategyHelper.multicastStrategy(collectAIDs(freeUnits),
                     strategyEnvelop);
        }
    }

    private void repeatTownHallConstruction() {
        try {
            Unit builder = builders.stream().filter(unit
                    -> !unit.isBusy()).findAny().get();
            builder.setBusy();
            strategyHelper.unicastStrategy(builder.getId(),
                 StrategyFactory
                        .envelopCreateBuildingStrategy(Strategy.HIGH_PRIORITY,
                                 WoaDefinitions.TOWN_HALL));
        } catch (NoSuchElementException ex) {
            // Still building
        }
    }

}
