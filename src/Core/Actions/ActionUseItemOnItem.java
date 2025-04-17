package Core.Actions;

import Core.GOAP.Action;
import Core.GOAP.ActionResult;
import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.interactive.Players; // Potentially check animation
import org.dreambot.api.script.ScriptManager;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.items.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Action to use one inventory item on another inventory item.
 * Example: Using water on flour to make dough.
 */
public class ActionUseItemOnItem implements Action {

    private final String itemToUseName;
    private final String itemUsedOnName;
    private final WorldStateKey hasItemToUseKey;
    private final WorldStateKey hasItemUsedOnKey;
    private final WorldStateKey hasResultItemKey; // Key for the resulting item (e.g., dough)
    private final String resultItemName; // Name of the resulting item

    // Optional: Animation check
    private final int expectedAnimationId; // Set to -1 if no specific animation expected
    private long animationStartTime = 0;
    private long animationTimeout = 5000;

    public ActionUseItemOnItem(String itemToUseName, WorldStateKey hasItemToUseKey,
                               String itemUsedOnName, WorldStateKey hasItemUsedOnKey,
                               String resultItemName, WorldStateKey hasResultItemKey,
                               int animationId) {
        this.itemToUseName = Objects.requireNonNull(itemToUseName);
        this.hasItemToUseKey = Objects.requireNonNull(hasItemToUseKey);
        this.itemUsedOnName = Objects.requireNonNull(itemUsedOnName);
        this.hasItemUsedOnKey = Objects.requireNonNull(hasItemUsedOnKey);
        this.resultItemName = Objects.requireNonNull(resultItemName);
        this.hasResultItemKey = Objects.requireNonNull(hasResultItemKey);
        this.expectedAnimationId = animationId;
    }

    // Constructor without animation check
    public ActionUseItemOnItem(String itemToUseName, WorldStateKey hasItemToUseKey,
                               String itemUsedOnName, WorldStateKey hasItemUsedOnKey,
                               String resultItemName, WorldStateKey hasResultItemKey) {
        this(itemToUseName, hasItemToUseKey, itemUsedOnName, hasItemUsedOnKey, resultItemName, hasResultItemKey, -1);
    }


    @Override
    public String getName() {
        return "Use_" + itemToUseName + "_On_" + itemUsedOnName;
    }

    @Override
    public Map<WorldStateKey, Object> getPreconditions() {
        Map<WorldStateKey, Object> preconditions = new HashMap<>();
        // Preconditions: Must have both items
        preconditions.put(hasItemToUseKey, true);
        preconditions.put(hasItemUsedOnKey, true);
        // Precondition: Should not already have the result (usually handled by Goal)
        // preconditions.put(hasResultItemKey, false);
        preconditions.put(WorldStateKey.INTERACT_IS_ANIMATING, false); // Don't interrupt
        return preconditions;
    }

    @Override
    public Map<WorldStateKey, Object> getEffects() {
        Map<WorldStateKey, Object> effects = new HashMap<>();
        // Effect: Consumed items are gone
        effects.put(hasItemToUseKey, false);
        effects.put(hasItemUsedOnKey, false);
        // Effect: Result item is present
        effects.put(hasResultItemKey, true);
        effects.put(WorldStateKey.INTERACT_IS_ANIMATING, false); // Anticipate animation ends
        return effects;
    }

    @Override
    public double getCost() {
        return 1.0; // Simple inventory interaction
    }

    @Override
    public boolean isApplicable(WorldState state) {
        // Check inventory for both items and not animating
        return state.getBoolean(hasItemToUseKey) &&
                state.getBoolean(hasItemUsedOnKey) &&
                !state.getBoolean(WorldStateKey.INTERACT_IS_ANIMATING);
    }

    @Override
    public ActionResult perform(WorldState currentState) {
        Item itemToUse = Inventory.get(itemToUseName);
        Item itemUsedOn = Inventory.get(itemUsedOnName);

        if (itemToUse == null || itemUsedOn == null) {
            Logger.log(getName() + ": Missing required items in inventory.");
            // Update state if items unexpectedly missing
            currentState.setBoolean(hasItemToUseKey, itemToUse != null);
            currentState.setBoolean(hasItemUsedOnKey, itemUsedOn != null);
            return ActionResult.FAILURE;
        }

        Logger.log(getName() + ": Attempting to use " + itemToUseName + " on " + itemUsedOnName);
        if (itemToUse.useOn(itemUsedOn)) {
            // Wait for items to be consumed or result item to appear, or animation
            boolean successConditionMet = Sleep.sleepUntil(() -> {
                boolean itemsConsumed = !Inventory.contains(itemToUseName) || !Inventory.contains(itemUsedOnName);
                boolean resultAppeared = Inventory.contains(resultItemName);
                boolean isAnimating = expectedAnimationId != -1 && Players.getLocal().isAnimating() && Players.getLocal().getAnimation() == expectedAnimationId;
                // Success if result appears OR if items are consumed (even if no result item, like lighting logs)
                // OR if expected animation starts
                return resultAppeared || itemsConsumed || isAnimating;
            }, animationTimeout);

            if (successConditionMet) {
                // Double check final state
                boolean resultExists = Inventory.contains(resultItemName);
                boolean stillAnimating = expectedAnimationId != -1 && Players.getLocal().isAnimating() && Players.getLocal().getAnimation() == expectedAnimationId;

                if (resultExists) {
                    Logger.log(getName() + ": Successfully created " + resultItemName);
                    currentState.setBoolean(hasItemToUseKey, Inventory.contains(itemToUseName));
                    currentState.setBoolean(hasItemUsedOnKey, Inventory.contains(itemUsedOnName));
                    currentState.setBoolean(hasResultItemKey, true);
                    currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, false); // Assume animation finished if item appeared
                    return ActionResult.SUCCESS;
                } else if (stillAnimating) {
                    Logger.log(getName() + ": Started animation...");
                    currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, true);
                    return ActionResult.IN_PROGRESS; // Let animation complete
                } else {
                    // Items might have been consumed without result or animation (e.g. failed attempt?)
                    // Or animation finished but result didn't appear?
                    Logger.log(getName() + ": Interaction occurred, but result item '" + resultItemName + "' not found and not animating.");
                    currentState.setBoolean(hasItemToUseKey, Inventory.contains(itemToUseName));
                    currentState.setBoolean(hasItemUsedOnKey, Inventory.contains(itemUsedOnName));
                    currentState.setBoolean(hasResultItemKey, false);
                    currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, false);
                    return ActionResult.FAILURE; // Treat as failure if result isn't there
                }
            } else {
                Logger.log(getName() + ": Timed out waiting for result/animation after using items.");
                return ActionResult.FAILURE;
            }
        } else {
            Logger.log(getName() + ": Failed to execute useOn interaction.");
            return ActionResult.FAILURE;
        }
    }
    @Override
    public void onAbort() {
        Logger.log(getName() + ": Aborted.");
    }
}