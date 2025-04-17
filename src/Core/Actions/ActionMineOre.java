package Core.Actions;

import Core.GOAP.Action;
import Core.GOAP.ActionResult;
import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.filter.Filter;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area; // Optional
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.script.ScriptManager;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import static Core.GameIntegration.DreamBotWorldObserver.TUTORIAL_AREAS;

/**
 * Action to find and mine a specific type of ore rock.
 */
public class ActionMineOre implements Action {

    private final String rockName; // e.g., "Tin rocks", "Copper rocks"
    private final String oreItemName; // e.g., "Tin ore", "Copper ore"
    private final WorldStateKey hasOreKey;
    private final WorldStateKey hasPickaxeKey;
    private final Area miningArea; // Optional area constraint
    private final int[] rockIds; // Optional: Specific IDs for the rocks

    // Common mining animation IDs (Bronze pickaxe might be 625?) - VERIFY
    private final int MINING_ANIMATION_ID = 625;

    // Internal state
    private long animationStartTime = 0;
    private long animationTimeout = 20000; // Mining can take longer if contested/low level
    private int initialOreCount = -1;
    private GameObject targetRock = null; // Track the specific rock being mined

    /** Constructor using rock name */
    public ActionMineOre(String rockName, String oreItemName, WorldStateKey hasOreKey, WorldStateKey hasPickaxeKey, Area miningArea) {
        this(rockName, oreItemName, hasOreKey, hasPickaxeKey, miningArea, null);
    }

    /** Constructor using rock IDs */
    public ActionMineOre(int[] rockIds, String oreItemName, WorldStateKey hasOreKey, WorldStateKey hasPickaxeKey, Area miningArea) {
        this(null, oreItemName, hasOreKey, hasPickaxeKey, miningArea, rockIds);
    }

    // Private constructor
    private ActionMineOre(String rName, String oName, WorldStateKey hOK, WorldStateKey hPK, Area area, int[] rIds) {
        this.rockName = rName;
        this.oreItemName = Objects.requireNonNull(oName);
        this.hasOreKey = Objects.requireNonNull(hOK);
        this.hasPickaxeKey = Objects.requireNonNull(hPK);
        this.miningArea = area; // Can be null
        this.rockIds = rIds;

        if (rName == null && (rIds == null || rIds.length == 0)) {
            throw new IllegalArgumentException("Must provide rock name or IDs for ActionMineOre");
        }
    }


    @Override
    public String getName() {
        return "Mine_" + (rockName != null ? rockName : "OreByID");
    }

    @Override
    public Map<WorldStateKey, Object> getPreconditions() {
        Map<WorldStateKey, Object> preconditions = new HashMap<>();
        preconditions.put(hasPickaxeKey, true); // Must have pickaxe
        // preconditions.put(hasOreKey, false); // Goal usually handles this
        preconditions.put(WorldStateKey.INTERACT_IS_ANIMATING, false);
        if (miningArea != null) {
            preconditions.put(WorldStateKey.LOC_CURRENT_AREA_NAME, getAreaName()); // Ensure in correct area
        }
        // preconditions.put(WorldStateKey.INV_SPACE > 0); // Checked in isApplicable
        return preconditions;
    }

    @Override
    public Map<WorldStateKey, Object> getEffects() {
        Map<WorldStateKey, Object> effects = new HashMap<>();
        effects.put(hasOreKey, true); // Gain ore
        effects.put(WorldStateKey.INTERACT_IS_ANIMATING, false); // Anticipate animation ends
        return effects;
    }

    @Override
    public double getCost() {
        // Could add distance cost
        return 2.5; // Skilling action cost
    }

    @Override
    public boolean isApplicable(WorldState state) {
        if (!state.getBoolean(hasPickaxeKey) || state.getBoolean(WorldStateKey.INTERACT_IS_ANIMATING) || state.getInteger(WorldStateKey.INV_SPACE) <= 0) {
            return false;
        }
        if (miningArea != null && !getAreaName().equals(state.getString(WorldStateKey.LOC_CURRENT_AREA_NAME))) {
            return false;
        }
        // Check if a suitable rock exists nearby
        return findRock() != null;
    }

    @Override
    public ActionResult perform(WorldState currentState) {
        Player localPlayer = Players.getLocal();

        // Check if already mining the correct type of rock
        if (localPlayer.isAnimating() && localPlayer.getAnimation() == MINING_ANIMATION_ID) {
            if (animationStartTime == 0 || initialOreCount == -1) {
                animationStartTime = System.currentTimeMillis();
                initialOreCount = Inventory.count(oreItemName);
                Logger.log(getName() + ": Continuing mining animation. Initial ore count: " + initialOreCount);
            }

            // Check for timeout (rock depleted, player moved)
            if (System.currentTimeMillis() - animationStartTime > animationTimeout) {
                Logger.log(getName() + ": Mining animation timed out or rock depleted.");
                resetMiningState();
                currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, false);
                return ActionResult.FAILURE;
            }

            // Check if we received ore
            int currentOreCount = Inventory.count(oreItemName);
            if (currentOreCount > initialOreCount) {
                Logger.log(getName() + ": Successfully obtained " + oreItemName + " (" + initialOreCount + " -> " + currentOreCount + ").");
                resetMiningState();
                currentState.setBoolean(hasOreKey, true); // Update state
                currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, false);
                if (Inventory.isFull()) currentState.setInteger(WorldStateKey.INV_SPACE, 0);
                return ActionResult.SUCCESS; // Tutorial usually needs one
            }

            // Check if inventory is full
            if (Inventory.isFull()) {
                Logger.log(getName() + ": Inventory full, cannot mine more.");
                resetMiningState();
                currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, false);
                currentState.setInteger(WorldStateKey.INV_SPACE, 0);
                return ActionResult.FAILURE;
            }

            // Check if the rock we were mining still exists and is valid
            if (targetRock != null && !targetRock.exists()) {
                Logger.log(getName() + ": Target rock depleted while mining.");
                resetMiningState();
                currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, false);
                return ActionResult.FAILURE; // Need to find a new rock
            }


            // Still animating
            currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, true);
            return ActionResult.IN_PROGRESS;
        }

        // If not animating, try to start
        resetMiningState();

        targetRock = findRock(); // Find the closest valid rock
        if (targetRock == null) {
            Logger.log(getName() + ": No suitable rocks found nearby.");
            return ActionResult.FAILURE;
        }

        // Walk if needed
        if (!targetRock.isOnScreen() || targetRock.distance() > 6) {
            Logger.log(getName() + ": Walking to rock at " + targetRock.getTile());
            if (Walking.walk(targetRock)) {
                Sleep.sleepUntil(targetRock::isOnScreen, 3000);
            }
        }

        Logger.log(getName() + ": Interacting 'Mine' with " + targetRock.getName());
        if (targetRock.interact("Mine")) {
            // Wait for animation to start
            boolean startedAnimating = Sleep.sleepUntil(() -> Players.getLocal().isAnimating() && Players.getLocal().getAnimation() == MINING_ANIMATION_ID, 5000);

            if (startedAnimating) {
                Logger.log(getName() + ": Started mining animation.");
                animationStartTime = System.currentTimeMillis();
                initialOreCount = Inventory.count(oreItemName);
                currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, true);
                return ActionResult.IN_PROGRESS;
            } else {
                Logger.log(getName() + ": Failed to start mining animation after interaction.");
                targetRock = null; // Clear target rock as interaction failed
                return ActionResult.FAILURE;
            }
        } else {
            Logger.log(getName() + ": Interaction 'Mine' failed.");
            targetRock = null;
            return ActionResult.FAILURE;
        }
    }

    /** Finds the closest suitable rock based on constructor parameters */
    private GameObject findRock() {
        // Use DreamBot's Filter interface
        Filter<GameObject> filter = obj -> {
            if (obj == null) return false;
            boolean nameMatch = rockName != null && obj.getName().equals(rockName);
            boolean idMatch = false;
            if (rockIds != null) {
                for (int id : rockIds) {
                    if (obj.getID() == id) {
                        idMatch = true;
                        break;
                    }
                }
            }
            boolean areaMatch = miningArea == null || miningArea.contains(obj);
            // Ensure it's actually a mineable rock (has "Mine" action)
            return (nameMatch || idMatch) && areaMatch && obj.hasAction("Mine");
        };

        // Call the closest method that accepts DreamBot's Filter
        return GameObjects.closest(filter);
    }

    private String getAreaName() {
        // Helper to get a string name for the area if needed for preconditions
        // This is crude; a better way would be to pass the area name string directly
        if (miningArea != null) {
            for (Map.Entry<String, Area> entry : TUTORIAL_AREAS.entrySet()) { // Assumes TUTORIAL_AREAS is accessible
                if (entry.getValue().equals(miningArea)) return entry.getKey();
            }
        }
        return "UnknownMiningArea";
    }

    private void resetMiningState() {
        animationStartTime = 0;
        initialOreCount = -1;
        targetRock = null; // Clear the specific rock target
    }

    @Override
    public void onAbort() {
        resetMiningState();
        Logger.log(getName() + ": Aborted.");
    }
}