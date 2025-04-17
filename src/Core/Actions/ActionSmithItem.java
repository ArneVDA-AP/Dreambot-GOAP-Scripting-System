package Core.Actions;

import Core.GOAP.Action;
import Core.GOAP.ActionResult;
import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.widget.helpers.Smithing; // Import Smithing helper
import org.dreambot.api.script.ScriptManager;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.wrappers.items.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Action to smith an item using the Smithing interface (e.g., making a dagger on an anvil).
 * Uses the high-level Smithing helper class.
 */
public class ActionSmithItem implements Action {

    private final String itemName; // Item to smith (e.g., "Bronze dagger")
    private final String barItemName; // Bar required (e.g., "Bronze bar")
    private final WorldStateKey hasBarKey;
    private final WorldStateKey hasHammerKey;
    private final WorldStateKey hasResultKey; // Key for the smithed item
    private final int amountToMake; // 1 for single, or use -1/Integer.MAX_VALUE for makeAll

    // Optional: Animation check
    private final int expectedAnimationId = 898; // Common smithing anim - VERIFY
    private long animationStartTime = 0;
    private long animationTimeout = 15000; // Allow time for smithing
    private int initialResultCount = -1;

    /**
     * Constructor for smithing a specific amount.
     */
    public ActionSmithItem(String itemName, String barItemName, WorldStateKey hasBarKey, WorldStateKey hasHammerKey, WorldStateKey hasResultKey, int amount) {
        this.itemName = Objects.requireNonNull(itemName);
        this.barItemName = Objects.requireNonNull(barItemName);
        this.hasBarKey = Objects.requireNonNull(hasBarKey);
        this.hasHammerKey = Objects.requireNonNull(hasHammerKey);
        this.hasResultKey = Objects.requireNonNull(hasResultKey);
        this.amountToMake = amount;
    }

    /**
     * Constructor for using Make All.
     */
    public ActionSmithItem(String itemName, String barItemName, WorldStateKey hasBarKey, WorldStateKey hasHammerKey, WorldStateKey hasResultKey) {
        this(itemName, barItemName, hasBarKey, hasHammerKey, hasResultKey, -1); // Use -1 to signify makeAll
    }


    @Override
    public String getName() {
        return "Smith_" + itemName + (amountToMake == -1 ? "_All" : "_x" + amountToMake);
    }

    @Override
    public Map<WorldStateKey, Object> getPreconditions() {
        Map<WorldStateKey, Object> preconditions = new HashMap<>();
        preconditions.put(hasBarKey, true); // Must have bars
        preconditions.put(hasHammerKey, true); // Must have hammer
        // preconditions.put(WorldStateKey.NEAR_ANVIL, true); // Need observer check or area precondition
        preconditions.put(WorldStateKey.INTERACT_IS_ANIMATING, false);
        return preconditions;
    }

    @Override
    public Map<WorldStateKey, Object> getEffects() {
        Map<WorldStateKey, Object> effects = new HashMap<>();
        effects.put(hasBarKey, false); // Bars are consumed (partially or fully)
        effects.put(hasResultKey, true); // Result item is gained
        effects.put(WorldStateKey.INTERACT_IS_ANIMATING, false);
        return effects;
    }

    @Override
    public double getCost() {
        return 2.0; // Skilling action
    }

    @Override
    public boolean isApplicable(WorldState state) {
        // Check inventory and animation state
        // Note: Smithing helper might handle anvil proximity check internally
        return state.getBoolean(hasBarKey) &&
                state.getBoolean(hasHammerKey) &&
                !state.getBoolean(WorldStateKey.INTERACT_IS_ANIMATING);
    }

    @Override
    public ActionResult perform(WorldState currentState) {
        Player localPlayer = Players.getLocal();

        // Check if already smithing
        if (localPlayer.isAnimating() && localPlayer.getAnimation() == expectedAnimationId) {
            if (animationStartTime == 0 || initialResultCount == -1) {
                animationStartTime = System.currentTimeMillis();
                initialResultCount = Inventory.count(itemName); // Track result item count
                Logger.log(getName() + ": Continuing smithing animation. Initial result count: " + initialResultCount);
            }

            if (System.currentTimeMillis() - animationStartTime > animationTimeout) {
                Logger.log(getName() + ": Smithing animation timed out (maybe out of bars?).");
                resetAnimationState();
                currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, false);
                // Check if we actually made *any* before timeout
                return Inventory.count(itemName) > initialResultCount ? ActionResult.SUCCESS : ActionResult.FAILURE;
            }

            // Check if we successfully created the item(s)
            int currentResultCount = Inventory.count(itemName);
            boolean barsRemaining = Inventory.contains(barItemName);

            if (currentResultCount > initialResultCount) {
                // Successfully made at least one
                // If making a specific amount, check if done
                if (amountToMake != -1 && currentResultCount >= initialResultCount + amountToMake) {
                    Logger.log(getName() + ": Successfully smithed required amount (" + amountToMake + ").");
                    resetAnimationState();
                    updateState(currentState);
                    return ActionResult.SUCCESS;
                }
                // If making all, check if out of bars
                if (amountToMake == -1 && !barsRemaining) {
                    Logger.log(getName() + ": Successfully smithed all available bars.");
                    resetAnimationState();
                    updateState(currentState);
                    return ActionResult.SUCCESS;
                }
                // Otherwise, still making more or animation continuing
                currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, true);
                return ActionResult.IN_PROGRESS;
            }

            // If still animating but no result yet, or waiting for next cycle
            currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, true);
            return ActionResult.IN_PROGRESS;

        } else if (animationStartTime != 0) {
            // Stopped animating, assume finished or interrupted
            Logger.log(getName() + ": Smithing animation stopped.");
            resetAnimationState();
            updateState(currentState); // Update state based on final inventory
            // Return success if we made at least one, even if interrupted
            return Inventory.count(itemName) > initialResultCount ? ActionResult.SUCCESS : ActionResult.FAILURE;
        }

        // If not animating, try to start
        resetAnimationState();

        if (!Inventory.contains(barItemName) || !Inventory.contains("Hammer")) {
            Logger.log(getName() + ": Missing bars or hammer.");
            return ActionResult.FAILURE;
        }

        Logger.log(getName() + ": Attempting to smith " + (amountToMake == -1 ? "all" : amountToMake) + " " + itemName);
        boolean interactionSent;
        if (amountToMake == -1) {
            interactionSent = Smithing.makeAll(itemName);
        } else {
            interactionSent = Smithing.make(itemName, amountToMake);
        }

        if (interactionSent) {
            // Wait briefly for animation to start
            boolean startedAnimating = Sleep.sleepUntil(() -> Players.getLocal().isAnimating() && Players.getLocal().getAnimation() == expectedAnimationId, 4000);
            if (startedAnimating) {
                Logger.log(getName() + ": Started smithing animation.");
                animationStartTime = System.currentTimeMillis();
                initialResultCount = Inventory.count(itemName);
                currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, true);
                return ActionResult.IN_PROGRESS;
            } else {
                // Smithing.make might return true even if interface didn't open or animation didn't start immediately
                // Could be successful if it made just one very quickly? Check inventory.
                if (Inventory.count(itemName) > initialResultCount || !Inventory.contains(barItemName)) {
                    Logger.log(getName() + ": Smithing likely completed instantly or animation not detected.");
                    updateState(currentState);
                    return ActionResult.SUCCESS;
                }
                Logger.log(getName() + ": Failed to start smithing animation after command.");
                return ActionResult.FAILURE;
            }
        } else {
            Logger.log(getName() + ": Smithing." + (amountToMake == -1 ? "makeAll" : "make") + "() command failed.");
            return ActionResult.FAILURE;
        }
    }

    private void resetAnimationState() {
        animationStartTime = 0;
        initialResultCount = -1;
    }

    private void updateState(WorldState currentState) {
        currentState.setBoolean(hasBarKey, Inventory.contains(barItemName));
        currentState.setBoolean(hasResultKey, Inventory.contains(itemName));
        currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, false);
        if (Inventory.isFull()) currentState.setInteger(WorldStateKey.INV_SPACE, 0);
    }

    @Override
    public void onAbort() {
        resetAnimationState();
        Logger.log(getName() + ": Aborted.");
    }
}