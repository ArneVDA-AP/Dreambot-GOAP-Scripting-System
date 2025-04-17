package Core.Actions;

import Core.GOAP.Action;
import Core.GOAP.ActionResult;
import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area; // Optional for area constraint
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.script.ScriptManager;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Action to find and chop a nearby tree to obtain logs.
 */
public class ActionCutTree implements Action {

    private final String TREE_NAME = "Tree"; // Standard tree name
    private final String AXE_NAME = "Bronze axe"; // Axe needed for tutorial
    private final WorldStateKey HAS_LOGS_KEY = WorldStateKey.S1_HAS_LOGS;
    private final WorldStateKey HAS_AXE_KEY = WorldStateKey.S1_HAS_AXE;
    private final Area woodcuttingArea; // Optional area constraint

    private final String LOGS_ITEM_NAME = "Logs"; // Define the item name
    private final int WOODCUTTING_ANIMATION_ID = 879; // Placeholder - VERIFY THIS ID

    // Internal state for IN_PROGRESS
    private long animationStartTime = 0;
    private long animationTimeout = 15000; // Timeout if animation doesn't start/finish
    private int initialLogCount = -1; // Track inventory changes

    /**
     * Constructor for cutting trees.
     * @param woodcuttingArea Optional area where trees should be located. Can be null.
     */
    public ActionCutTree(Area woodcuttingArea) {
        this.woodcuttingArea = woodcuttingArea;
    }

    /** Simpler constructor without area constraint */
    public ActionCutTree() {
        this(null);
    }

    @Override
    public String getName() {
        return "CutTree";
    }

    @Override
    public Map<WorldStateKey, Object> getPreconditions() {
        Map<WorldStateKey, Object> preconditions = new HashMap<>();
        // Precondition: Must have an axe
        preconditions.put(HAS_AXE_KEY, true);
        // Precondition: Not already have logs (or goal is to get more) - Goal usually handles this
        // preconditions.put(HAS_LOGS_KEY, false);
        // Precondition: Not currently animating something else
        preconditions.put(WorldStateKey.INTERACT_IS_ANIMATING, false);
        // Optional: Precondition to be in the woodcutting area
        // if (woodcuttingArea != null) { preconditions.put(WorldStateKey.LOC_CURRENT_AREA_NAME, "Survival_Woodcutting_Area"); }
        return preconditions;
    }

    @Override
    public Map<WorldStateKey, Object> getEffects() {
        Map<WorldStateKey, Object> effects = new HashMap<>();
        // Effect: Player now has logs
        effects.put(HAS_LOGS_KEY, true);
        // Effect: Player is no longer animating (anticipated)
        effects.put(WorldStateKey.INTERACT_IS_ANIMATING, false);
        return effects;
    }

    @Override
    public double getCost() {
        // Could add distance cost to nearest tree later
        return 2.0; // Slightly higher cost than just walking/talking
    }

    @Override
    public boolean isApplicable(WorldState state) {
        // Check preconditions from state
        if (!state.getBoolean(HAS_AXE_KEY)) return false;
        if (state.getBoolean(WorldStateKey.INTERACT_IS_ANIMATING)) return false; // Don't interrupt other animations

        // Optional Area Check
        if (woodcuttingArea != null && !woodcuttingArea.contains(Players.getLocal())) {
            return false;
        }

        // Runtime check: Is there a tree nearby?
        return GameObjects.closest(TREE_NAME) != null;
    }

    @Override
    public ActionResult perform(WorldState currentState) {
        Player localPlayer = Players.getLocal();

        // Check if already animating woodcutting
        // Use the verified animation ID
        if (localPlayer.isAnimating() && localPlayer.getAnimation() == WOODCUTTING_ANIMATION_ID) {
            // If we just started animating (or re-checking), record time and initial count
            if (animationStartTime == 0 || initialLogCount == -1) {
                animationStartTime = System.currentTimeMillis();
                // *** CORRECTED LOG COUNT CHECK ***
                initialLogCount = Inventory.count(LOGS_ITEM_NAME); // Use item name
                Logger.log(getName() + ": Started/Continuing animation. Initial log count: " + initialLogCount);
            }

            // Check for timeout
            if (System.currentTimeMillis() - animationStartTime > animationTimeout) {
                Logger.log(getName() + ": Animation timed out.");
                resetAnimationState();
                currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, false); // Update state on timeout
                return ActionResult.FAILURE;
            }

            // Check if we received logs
            // *** CORRECTED LOG COUNT CHECK ***
            int currentLogCount = Inventory.count(LOGS_ITEM_NAME); // Use item name
            if (currentLogCount > initialLogCount) {
                Logger.log(getName() + ": Successfully obtained logs (" + initialLogCount + " -> " + currentLogCount + ").");
                resetAnimationState();
                currentState.setBoolean(HAS_LOGS_KEY, true); // Update state flag
                currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, false);
                return ActionResult.SUCCESS;
            }

            // Still animating, haven't timed out or gotten logs yet
            currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, true);
            return ActionResult.IN_PROGRESS;
        }

        // If not animating woodcutting, try to start
        resetAnimationState(); // Reset timer if we are not animating WC

        GameObject tree = GameObjects.closest(obj -> obj != null && obj.getName().equals(TREE_NAME) && obj.hasAction("Chop down") && (woodcuttingArea == null || woodcuttingArea.contains(obj)));

        if (tree == null) {
            Logger.log(getName() + ": No suitable tree found nearby.");
            return ActionResult.FAILURE;
        }

        // Walk if needed (logic remains the same)
        if (!tree.isOnScreen() || tree.distance() > 6) {
            // ... walking logic ...
        }

        Logger.log(getName() + ": Interacting 'Chop down' with tree.");
        if (tree.interact("Chop down")) {
            // Wait a bit for the animation to potentially start
            // Use the verified animation ID
            boolean startedAnimating = Sleep.sleepUntil(() -> Players.getLocal().isAnimating() && Players.getLocal().getAnimation() == WOODCUTTING_ANIMATION_ID, 4000);

            if (startedAnimating) {
                Logger.log(getName() + ": Started chopping animation.");
                // Set initial state for tracking within the IN_PROGRESS block next loop
                animationStartTime = System.currentTimeMillis();
                initialLogCount = Inventory.count(LOGS_ITEM_NAME); // Record initial count
                currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, true);
                return ActionResult.IN_PROGRESS;
            } else {
                Logger.log(getName() + ": Failed to start chopping animation after interaction.");
                return ActionResult.FAILURE;
            }
        } else {
            Logger.log(getName() + ": Interaction 'Chop down' failed.");
            return ActionResult.FAILURE;
        }
    }

    private void resetAnimationState() {
        animationStartTime = 0;
        initialLogCount = -1; // Reset log count tracking
    }

    @Override
    public void onAbort() {
        resetAnimationState();
        Logger.log(getName() + ": Aborted.");
    }
}