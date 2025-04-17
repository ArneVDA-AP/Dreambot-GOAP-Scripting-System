package Core.Actions;

import Core.GOAP.Action;
import Core.GOAP.ActionResult;
import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.script.ScriptManager;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Action to walk to a specific destination Tile.
 */
public class ActionWalkToTile implements Action {

    private final Tile destinationTile;
    private final int acceptanceRadius; // How close is close enough?
    private final String targetAreaName; // Optional: Name of area we are walking to (for effects)

    // Internal state for IN_PROGRESS
    private boolean walkingInitiated = false;
    private long walkStartTime = 0;
    private long walkTimeout = 30000; // Max time to attempt walking before failing

    /**
     * Constructor for walking to a tile.
     * @param destinationTile The target tile.
     * @param acceptanceRadius The distance within which the destination is considered reached.
     * @param targetAreaName Optional name of the area for WorldState effect (can be null).
     */
    public ActionWalkToTile(Tile destinationTile, int acceptanceRadius, String targetAreaName) {
        this.destinationTile = Objects.requireNonNull(destinationTile, "Destination Tile cannot be null");
        this.acceptanceRadius = Math.max(1, acceptanceRadius); // Ensure at least 1
        this.targetAreaName = targetAreaName; // Can be null
    }

    /** Simpler constructor with default radius 3 and no area name */
    public ActionWalkToTile(Tile destinationTile) {
        this(destinationTile, 3, null);
    }

    @Override
    public String getName() {
        return "WalkTo_" + destinationTile.getX() + "_" + destinationTile.getY() + "_" + destinationTile.getZ();
    }

    @Override
    public Map<WorldStateKey, Object> getPreconditions() {
        Map<WorldStateKey, Object> preconditions = new HashMap<>();
        // Precondition: Not already walking (handled by perform/IN_PROGRESS)
        // preconditions.put(WorldStateKey.LOC_IS_WALKING, false);
        // Precondition: Not already at the destination (handled by isApplicable)
        return preconditions;
    }

    @Override
    public Map<WorldStateKey, Object> getEffects() {
        Map<WorldStateKey, Object> effects = new HashMap<>();
        // Effect: Player is no longer walking (anticipated)
        effects.put(WorldStateKey.LOC_IS_WALKING, false);
        // Effect: Player is at the destination (or in the target area)
        if (targetAreaName != null) {
            effects.put(WorldStateKey.LOC_CURRENT_AREA_NAME, targetAreaName);
        }
        // Setting the exact tile might be too specific if radius > 0
        // effects.put(WorldStateKey.LOC_CURRENT_TILE, destinationTile);
        return effects;
    }

    @Override
    public double getCost() {
        // Cost based on distance
        double distance = Players.getLocal().distance(destinationTile);
        return 1.0 + (distance / 3.0); // Base cost + distance factor
    }

    @Override
    public boolean isApplicable(WorldState state) {
        // Only applicable if we are not already at the destination (within radius)
        return Players.getLocal().distance(destinationTile) > acceptanceRadius;
    }

    @Override
    public ActionResult perform(WorldState currentState) {
        Tile playerPos = Players.getLocal().getTile();

        // Check if already arrived (might happen between loops)
        if (playerPos.distance(destinationTile) <= acceptanceRadius) {
            Logger.log(getName() + ": Already at destination.");
            resetWalkState();
            currentState.setBoolean(WorldStateKey.LOC_IS_WALKING, false); // Update state
            return ActionResult.SUCCESS;
        }

        // Initiate walking if not already started or if player stopped moving unexpectedly
        if (!walkingInitiated || !Players.getLocal().isMoving()) {
            Logger.log(getName() + ": Initiating walk to " + destinationTile);
            if (Walking.walk(destinationTile)) {
                walkingInitiated = true;
                walkStartTime = System.currentTimeMillis();
                // Brief sleep to allow movement to start
                Sleep.sleep(Calculations.random(300, 600));
                // Check immediately if we are now moving
                if (Players.getLocal().isMoving()) {
                    currentState.setBoolean(WorldStateKey.LOC_IS_WALKING, true); // Update state
                    return ActionResult.IN_PROGRESS;
                } else {
                    // Failed to start moving after walk command
                    Logger.log(getName() + ": Failed to initiate movement after walk command.");
                    resetWalkState();
                    return ActionResult.FAILURE;
                }
            } else {
                Logger.log(getName() + ": Walking.walk() command failed.");
                resetWalkState();
                return ActionResult.FAILURE;
            }
        }

        // If walking was initiated, check progress
        if (walkingInitiated) {
            // Check for timeout
            if (System.currentTimeMillis() - walkStartTime > walkTimeout) {
                Logger.log(getName() + ": Walk timed out after " + walkTimeout + "ms.");
                resetWalkState();
                currentState.setBoolean(WorldStateKey.LOC_IS_WALKING, false); // Update state
                return ActionResult.FAILURE;
            }

            // Check if still moving towards destination
            if (Players.getLocal().isMoving()) {
                currentState.setBoolean(WorldStateKey.LOC_IS_WALKING, true); // Update state
                // Still moving, check if destination reached during this check
                if (Players.getLocal().distance(destinationTile) <= acceptanceRadius) {
                    Logger.log(getName() + ": Reached destination while checking progress.");
                    resetWalkState();
                    currentState.setBoolean(WorldStateKey.LOC_IS_WALKING, false); // Update state
                    return ActionResult.SUCCESS;
                }
                // Otherwise, still in progress
                return ActionResult.IN_PROGRESS;
            } else {
                // Stopped moving, but not at destination? Might be stuck or finished last step.
                if (Players.getLocal().distance(destinationTile) <= acceptanceRadius) {
                    Logger.log(getName() + ": Reached destination (detected after stopping).");
                    resetWalkState();
                    currentState.setBoolean(WorldStateKey.LOC_IS_WALKING, false); // Update state
                    return ActionResult.SUCCESS;
                } else {
                    Logger.log(getName() + ": Stopped moving but not at destination. Potentially stuck.");
                    // Could retry walking once, or just fail. Let's fail for now.
                    resetWalkState();
                    currentState.setBoolean(WorldStateKey.LOC_IS_WALKING, false); // Update state
                    return ActionResult.FAILURE;
                }
            }
        }

        // Should not be reached, but default to failure
        Logger.log(getName() + ": Reached unexpected state in perform().");
        resetWalkState();
        return ActionResult.FAILURE;
    }

    private void resetWalkState() {
        walkingInitiated = false;
        walkStartTime = 0;
    }

    @Override
    public void onAbort() {
        // Reset internal state if the action is aborted by the engine
        resetWalkState();
        Logger.log(getName() + ": Aborted.");
    }
}