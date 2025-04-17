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
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.script.ScriptManager;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.wrappers.items.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate; // For flexible object finding
import java.util.List;
import java.util.Comparator;

/**
 * Action to use an inventory item on a specific GameObject.
 * Example: Using dough on a range, ore on a furnace.
 */
public class ActionUseItemOnObject implements Action {

    private final String itemName;
    private final WorldStateKey hasItemKey;
    private final String objectName; // Can be null if using ID or predicate
    private final int objectId;      // Can be -1 if using name or predicate
    private final Predicate<GameObject> objectPredicate; // Can be null if using name/ID
    private final String resultItemName; // Can be null if item is just consumed
    private final WorldStateKey hasResultKey; // Can be null
    private final String actionName; // The interaction name, e.g., "Use", "Smelt"

    // Optional: Animation check
    private final int expectedAnimationId; // Set to -1 if not applicable
    private long animationStartTime = 0;
    private long animationTimeout = 10000; // Default timeout

    /** Simplified constructor using Item Name and Object Name */
    public ActionUseItemOnObject(String itemName, WorldStateKey hasItemKey,
                                 String objectName, String actionName,
                                 String resultItemName, WorldStateKey hasResultKey,
                                 int animationId) {
        this(itemName, hasItemKey, objectName, -1, null, actionName, resultItemName, hasResultKey, animationId);
    }

    /** Constructor using Item Name and Object ID */
    public ActionUseItemOnObject(String itemName, WorldStateKey hasItemKey,
                                 int objectId, String actionName,
                                 String resultItemName, WorldStateKey hasResultKey,
                                 int animationId) {
        this(itemName, hasItemKey, null, objectId, null, actionName, resultItemName, hasResultKey, animationId);
    }

    /** Flexible constructor using a Predicate for the object */
    public ActionUseItemOnObject(String itemName, WorldStateKey hasItemKey,
                                 Predicate<GameObject> objectPredicate, String actionName,
                                 String resultItemName, WorldStateKey hasResultKey,
                                 int animationId) {
        this(itemName, hasItemKey, null, -1, objectPredicate, actionName, resultItemName, hasResultKey, animationId);
    }

    // Private master constructor
    private ActionUseItemOnObject(String item, WorldStateKey itemKey, String objName, int objId, Predicate<GameObject> predicate,
                                  String action, String resultName, WorldStateKey resultKey, int animId) {
        this.itemName = Objects.requireNonNull(item);
        this.hasItemKey = Objects.requireNonNull(itemKey);
        this.objectName = objName;
        this.objectId = objId;
        this.objectPredicate = predicate;
        this.actionName = Objects.requireNonNull(action); // Action verb is required
        this.resultItemName = resultName; // Can be null
        this.hasResultKey = resultKey;     // Can be null
        this.expectedAnimationId = animId;

        if (objName == null && objId <= 0 && predicate == null) {
            throw new IllegalArgumentException("Must provide object name, ID, or predicate.");
        }
    }

    @Override
    public String getName() {
        String targetDesc = objectName != null ? objectName : (objectId > 0 ? "ID_" + objectId : "Object");
        return "Use_" + itemName + "_On_" + targetDesc;
    }

    @Override
    public Map<WorldStateKey, Object> getPreconditions() {
        Map<WorldStateKey, Object> preconditions = new HashMap<>();
        preconditions.put(hasItemKey, true); // Must have the item to use
        preconditions.put(WorldStateKey.INTERACT_IS_ANIMATING, false);
        // Optional: Add area precondition if needed
        return preconditions;
    }

    @Override
    public Map<WorldStateKey, Object> getEffects() {
        Map<WorldStateKey, Object> effects = new HashMap<>();
        effects.put(hasItemKey, false); // Item is consumed
        if (hasResultKey != null) {
            effects.put(hasResultKey, true); // Result item is gained
        }
        effects.put(WorldStateKey.INTERACT_IS_ANIMATING, false);
        return effects;
    }

    @Override
    public double getCost() {
        // Could add distance cost
        return 1.8; // Slightly more complex than item-on-item
    }

    @Override
    public boolean isApplicable(WorldState state) {
        if (!state.getBoolean(hasItemKey) || state.getBoolean(WorldStateKey.INTERACT_IS_ANIMATING)) {
            return false;
        }
        // Check if the target object exists nearby
        return findObject() != null;
    }

    @Override
    public ActionResult perform(WorldState currentState) {
        Player localPlayer = Players.getLocal();

        // Check if already performing the relevant animation
        if (expectedAnimationId != -1 && localPlayer.isAnimating() && localPlayer.getAnimation() == expectedAnimationId) {
            if (animationStartTime == 0) animationStartTime = System.currentTimeMillis();

            if (System.currentTimeMillis() - animationStartTime > animationTimeout) {
                Logger.log(getName() + ": Animation timed out.");
                resetAnimationState();
                currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, false);
                return ActionResult.FAILURE;
            }

            // Check if result item appeared (if applicable) or source item disappeared
            boolean resultExists = hasResultKey != null && Inventory.contains(resultItemName);
            boolean sourceGone = !Inventory.contains(itemName);

            if (resultExists || sourceGone) {
                Logger.log(getName() + ": Action completed (result/consumption detected).");
                resetAnimationState();
                currentState.setBoolean(hasItemKey, Inventory.contains(itemName)); // Update source state
                if(hasResultKey != null) currentState.setBoolean(hasResultKey, resultExists); // Update result state
                currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, false);
                return ActionResult.SUCCESS;
            }
            // Still animating
            currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, true);
            return ActionResult.IN_PROGRESS;
        }

        // If not animating, try to start
        resetAnimationState();

        Item itemToUse = Inventory.get(itemName);
        GameObject targetObject = findObject();

        if (itemToUse == null) {
            Logger.log(getName() + ": Item '" + itemName + "' not found in inventory.");
            currentState.setBoolean(hasItemKey, false); // Correct state
            return ActionResult.FAILURE;
        }
        if (targetObject == null) {
            Logger.log(getName() + ": Target object not found.");
            return ActionResult.FAILURE;
        }

        // Walk if needed
        if (!targetObject.isOnScreen() || targetObject.distance() > 7) {
            Logger.log(getName() + ": Walking to target object at " + targetObject.getTile());
            if (Walking.walk(targetObject)) {
                Sleep.sleepUntil(targetObject::isOnScreen, 3000);
            }
        }

        Logger.log(getName() + ": Using " + itemName + " on " + targetObject.getName() + " (Action: " + actionName + ")");
        if (itemToUse.useOn(targetObject)) { // Use the item on the object
            // Wait for animation or item change
            boolean conditionMet = Sleep.sleepUntil(() -> {
                boolean isAnimating = expectedAnimationId != -1 && Players.getLocal().isAnimating() && Players.getLocal().getAnimation() == expectedAnimationId;
                boolean resultAppeared = hasResultKey != null && Inventory.contains(resultItemName);
                boolean sourceGone = !Inventory.contains(itemName);
                return isAnimating || resultAppeared || sourceGone;
            }, animationTimeout);

            if (conditionMet) {
                boolean resultExists = hasResultKey != null && Inventory.contains(resultItemName);
                boolean stillAnimating = expectedAnimationId != -1 && Players.getLocal().isAnimating() && Players.getLocal().getAnimation() == expectedAnimationId;

                if (resultExists || !Inventory.contains(itemName)) { // Success if result or consumption
                    Logger.log(getName() + ": Action likely successful (result/consumption detected).");
                    currentState.setBoolean(hasItemKey, Inventory.contains(itemName));
                    if(hasResultKey != null) currentState.setBoolean(hasResultKey, resultExists);
                    currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, false);
                    return ActionResult.SUCCESS;
                } else if (stillAnimating) {
                    Logger.log(getName() + ": Started animation...");
                    animationStartTime = System.currentTimeMillis();
                    currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, true);
                    return ActionResult.IN_PROGRESS;
                } else {
                    Logger.log(getName() + ": Interaction occurred, but no state change/animation detected.");
                    return ActionResult.FAILURE; // Treat as failure if nothing happened
                }
            } else {
                Logger.log(getName() + ": Timed out waiting for result/animation after using item on object.");
                return ActionResult.FAILURE;
            }
        } else {
            Logger.log(getName() + ": Failed to execute useOn interaction for item on object.");
            return ActionResult.FAILURE;
        }
    }
    /**
     * Helper to find the target GameObject based on provided criteria
     */
//    private GameObject findObject() {
//        Filter<GameObject> filter = null;
//
//        if (objectPredicate != null) {
//            // Convert Predicate to Filter
//            filter = object -> objectPredicate.test(object);
//        } else if (objectId > 0) {
//            filter = object -> object != null && object.getID() == objectId;
//        } else if (objectName != null && !objectName.isEmpty()) {
//            filter = object -> object != null && objectName.equals(object.getName());
//        }
//
//        if (filter != null) {
//            return GameObjects.closest(filter);  // Now valid
//        }
//
//        Logger.log("Error in findObject: No valid object identifier (ID, Name, or Predicate) provided.");
//        return null;
//    }


    /** Helper to find the target GameObject based on provided criteria */
    private GameObject findObject() {
        // Use DreamBot's Filter interface
        Filter<GameObject> filter = null;

        if (objectPredicate != null) {
            // Convert the standard Java Predicate to DreamBot's Filter
            filter = obj -> obj != null && objectPredicate.test(obj); // Use the provided predicate logic
        } else if (objectId > 0) {
            // Create a Filter lambda for ID check
            filter = obj -> obj != null && obj.getID() == objectId;
        } else if (objectName != null) {
            // Create a Filter lambda for Name check
            filter = obj -> obj != null && obj.getName().equals(objectName);
        }

        if (filter != null) {
            // Call the closest method that accepts DreamBot's Filter
            return GameObjects.closest(filter);
        }

        Logger.log("Error in ActionUseItemOnObject: No valid object identifier (ID, Name, or Predicate) provided.");
        return null; // Invalid parameters or no object found
    }
    private void resetAnimationState() {
        animationStartTime = 0;
    }

    @Override
    public void onAbort() {
        resetAnimationState();
        Logger.log(getName() + ": Aborted.");
    }
}