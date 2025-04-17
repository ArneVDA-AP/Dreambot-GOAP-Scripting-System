package Core.Actions;

import Core.GOAP.Action;
import Core.GOAP.ActionResult;
import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.impl.Walking; // If needed for walking to object
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Action to open a specific door or gate GameObject.
 */
public class ActionOpenDoor implements Action {

    private final String objectName; // e.g., "Door", "Gate"
    private final Tile objectTile;   // Exact tile of the object for precise targeting
    private final int objectId;      // Optional: ID for more specific targeting
    private final String targetStageName; // Stage name this action helps achieve
    private final WorldStateKey openStateKey; // The WorldStateKey representing this door's open state

    private long interactionTimeout = 5000;

    /**
     * Constructor using name and exact tile.
     * @param objectName Name of the door/gate.
     * @param objectTile Exact tile location.
     * @param openStateKey The WorldStateKey that will be true when this door is open.
     * @param targetStageName The stage name achieved after opening this door.
     */
    public ActionOpenDoor(String objectName, Tile objectTile, WorldStateKey openStateKey, String targetStageName) {
        this(objectName, objectTile, -1, openStateKey, targetStageName); // Use -1 for invalid ID
    }

    /**
     * Constructor using ID and exact tile.
     * @param objectId ID of the door/gate.
     * @param objectTile Exact tile location.
     * @param openStateKey The WorldStateKey that will be true when this door is open.
     * @param targetStageName The stage name achieved after opening this door.
     */
    public ActionOpenDoor(int objectId, Tile objectTile, WorldStateKey openStateKey, String targetStageName) {
        this(null, objectTile, objectId, openStateKey, targetStageName); // Use null for name if using ID
    }

    // Private constructor for internal use
    private ActionOpenDoor(String name, Tile tile, int id, WorldStateKey key, String stage) {
        this.objectName = name;
        this.objectTile = Objects.requireNonNull(tile, "Object Tile cannot be null");
        this.objectId = id;
        this.openStateKey = Objects.requireNonNull(key, "Open State Key cannot be null");
        this.targetStageName = Objects.requireNonNull(stage, "Target Stage Name cannot be null");

        if (name == null && id <= 0) {
            throw new IllegalArgumentException("Must provide either a valid object name or ID for ActionOpenDoor");
        }
    }


    @Override
    public String getName() {
        return "Open_" + (objectName != null ? objectName : "ID_" + objectId) + "_At_" + objectTile;
    }

    @Override
    public Map<WorldStateKey, Object> getPreconditions() {
        Map<WorldStateKey, Object> preconditions = new HashMap<>();
        // Precondition: The door must be closed
        preconditions.put(openStateKey, false);
        // Precondition: Dialogue should be closed (usually)
        preconditions.put(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN, false);
        // Optional: Add readiness state if needed (like TUT_S0_READY_FOR_DOOR)
        // preconditions.put(WorldStateKey.TUT_S0_READY_FOR_DOOR, true);
        return preconditions;
    }

    @Override
    public Map<WorldStateKey, Object> getEffects() {
        Map<WorldStateKey, Object> effects = new HashMap<>();
        // Effect: The door is now open
        effects.put(openStateKey, true);
        // Effect: Player is now in the next section (anticipated by planner)
        effects.put(WorldStateKey.TUT_STAGE_NAME, targetStageName);
        // Could also set TUT_STAGE_ID if the target varp value is known
        // effects.put(WorldStateKey.TUT_STAGE_ID, targetStageVarpValue);
        return effects;
    }

    @Override
    public double getCost() {
        // Cost might depend on distance to the door
        double distance = Players.getLocal().distance(objectTile);
        return 1.0 + (distance / 5.0); // Base cost + cost for distance
    }

    @Override
    public boolean isApplicable(WorldState state) {
        // Check preconditions from state
        if (state.getBoolean(openStateKey)) return false; // Already open
        if (state.getBoolean(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN)) return false; // In dialogue

        // Optional: Check readiness state if applicable
        // if (!state.getBoolean(WorldStateKey.TUT_S0_READY_FOR_DOOR)) return false;

        // Runtime check: Is the object actually present?
        GameObject door = findDoor();
        return door != null && door.hasAction("Open"); // Check if present and has "Open" action
    }

    @Override
    public ActionResult perform(WorldState currentState) {
        GameObject door = findDoor();

        if (door == null) {
            Logger.log(getName() + ": Door/Gate object not found at " + objectTile);
            return ActionResult.FAILURE;
        }

        if (!door.hasAction("Open")) {
            Logger.log(getName() + ": Door/Gate already open or action unavailable.");
            // If it's already open according to the game, update state and succeed
            if (!currentState.getBoolean(openStateKey)) {
                currentState.setBoolean(openStateKey, true); // Correct our state
            }
            return ActionResult.SUCCESS;
        }

        // Walk closer if needed
        if (!door.isOnScreen() || door.distance() > 6) {
            Logger.log(getName() + ": Walking closer to door at " + objectTile);
            if (Walking.walk(door)) {
                Sleep.sleepUntil(door::isOnScreen, 3000);
            }
        }

        Logger.log(getName() + ": Interacting 'Open' with door at " + objectTile);
        if (door.interact("Open")) {
            // Wait for the door state to change (either object disappears, changes ID, or loses "Open" action)
            // Or wait for player to potentially start moving through it
            boolean opened = Sleep.sleepUntil(() -> {
                GameObject updatedDoor = findDoor(); // Re-check the door
                return updatedDoor == null || !updatedDoor.hasAction("Open");
                // Could also add: || Players.getLocal().isMoving()
            }, interactionTimeout);

            if (opened) {
                Logger.log(getName() + ": Door opened successfully.");
                currentState.setBoolean(openStateKey, true); // Update state
                return ActionResult.SUCCESS;
            } else {
                Logger.log(getName() + ": Failed to confirm door opened after interaction.");
                return ActionResult.FAILURE; // Timeout or state didn't change
            }
        } else {
            Logger.log(getName() + ": Interaction 'Open' failed.");
            return ActionResult.FAILURE;
        }
    }

    /** Helper method to find the specific door/gate object */
    private GameObject findDoor() {
        if (objectId > 0) {
            // Prioritize finding by ID and Tile
            return GameObjects.closest(obj -> obj != null && obj.getID() == objectId && obj.getTile().equals(objectTile));
        } else if (objectName != null) {
            // Fallback to finding by Name and Tile
            return GameObjects.closest(obj -> obj != null && obj.getName().equals(objectName) && obj.getTile().equals(objectTile));
        }
        return null; // Invalid parameters
    }
}