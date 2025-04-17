package Core.Actions;

import Core.GOAP.Action;
import Core.GOAP.ActionResult;
import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area; // Use Area
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.script.ScriptManager;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.wrappers.items.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Action to fish for Raw shrimps using a Small fishing net.
 */
public class ActionFishShrimp implements Action {

    private final String NET_NAME = "Small fishing net";
    private final String SHRIMP_NAME = "Raw shrimps";
    private final String FISHING_SPOT_NAME = "Fishing spot"; // Name of the NPC/Object
    private final WorldStateKey HAS_NET_KEY = WorldStateKey.S1_HAS_FISHING_NET;
    private final WorldStateKey HAS_SHRIMP_KEY = WorldStateKey.S1_HAS_RAW_SHRIMP;
    private final WorldStateKey AREA_KEY = WorldStateKey.LOC_CURRENT_AREA_NAME;
    private final String REQUIRED_AREA_NAME = "Survival_Fishing_Area"; // Name from our Area map
    private final int FISHING_ANIMATION_ID = 621; // Common Net fishing animation - VERIFY

    // Internal state
    private long animationStartTime = 0;
    private long animationTimeout = 60000; // Fishing can take a while, allow 60s timeout
    private int initialShrimpCount = -1;

    public ActionFishShrimp() {
        // Assumes area check is handled by preconditions/applicability
    }

    @Override
    public String getName() {
        return "FishShrimp";
    }

    @Override
    public Map<WorldStateKey, Object> getPreconditions() {
        Map<WorldStateKey, Object> preconditions = new HashMap<>();
        // Preconditions: Must have net, be in the fishing area, have inventory space
        preconditions.put(HAS_NET_KEY, true);
        preconditions.put(AREA_KEY, REQUIRED_AREA_NAME);
        // preconditions.put(WorldStateKey.INV_SPACE > 0); // Checked in isApplicable
        preconditions.put(WorldStateKey.INTERACT_IS_ANIMATING, false);
        return preconditions;
    }

    @Override
    public Map<WorldStateKey, Object> getEffects() {
        Map<WorldStateKey, Object> effects = new HashMap<>();
        // Effect: Player has shrimp
        effects.put(HAS_SHRIMP_KEY, true);
        // Effect: Player is not animating (anticipated)
        effects.put(WorldStateKey.INTERACT_IS_ANIMATING, false);
        // Effect: Inventory space might decrease (hard to predict exact amount)
        // effects.put(WorldStateKey.INV_SPACE, someLowerValue); // Less reliable effect
        return effects;
    }

    @Override
    public double getCost() {
        return 3.0; // Fishing involves waiting, higher cost
    }

    @Override
    public boolean isApplicable(WorldState state) {
        // Check inventory for net and space
        if (!state.getBoolean(HAS_NET_KEY) || state.getInteger(WorldStateKey.INV_SPACE) <= 0) {
            return false;
        }
        // Check if already animating
        if (state.getBoolean(WorldStateKey.INTERACT_IS_ANIMATING)) {
            return false;
        }
        // Check if in the correct area
        if (!REQUIRED_AREA_NAME.equals(state.getString(AREA_KEY))) {
            return false;
        }
        // Runtime check: Is there a fishing spot nearby?
        // Fishing spots are NPCs in OSRS
        return NPCs.closest(FISHING_SPOT_NAME) != null;
    }

    @Override
    public ActionResult perform(WorldState currentState) {
        Player localPlayer = Players.getLocal();

        // Check if already fishing
        if (localPlayer.isAnimating() && localPlayer.getAnimation() == FISHING_ANIMATION_ID) {
            if (animationStartTime == 0 || initialShrimpCount == -1) {
                animationStartTime = System.currentTimeMillis();
                initialShrimpCount = Inventory.count(SHRIMP_NAME);
                Logger.log(getName() + ": Continuing fishing animation. Initial shrimp count: " + initialShrimpCount);
            }

            // Check for timeout (e.g., spot moved or depleted)
            if (System.currentTimeMillis() - animationStartTime > animationTimeout) {
                Logger.log(getName() + ": Fishing animation timed out or spot depleted.");
                resetAnimationState();
                currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, false);
                return ActionResult.FAILURE; // Fail if we time out
            }

            // Check if we caught a shrimp
            int currentShrimpCount = Inventory.count(SHRIMP_NAME);
            if (currentShrimpCount > initialShrimpCount) {
                Logger.log(getName() + ": Successfully caught shrimp (" + initialShrimpCount + " -> " + currentShrimpCount + ").");
                resetAnimationState();
                currentState.setBoolean(HAS_SHRIMP_KEY, true); // Update state
                currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, false);
                // Check if inventory is now full
                if (Inventory.isFull()) {
                    currentState.setInteger(WorldStateKey.INV_SPACE, 0);
                }
                return ActionResult.SUCCESS; // Goal is usually just to catch *one* for tutorial
            }

            // Check if inventory is full - if so, we can't fish more, action fails for planning purposes
            if (Inventory.isFull()) {
                Logger.log(getName() + ": Inventory full, cannot fish more.");
                resetAnimationState();
                currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, false); // Not animating anymore
                currentState.setInteger(WorldStateKey.INV_SPACE, 0);
                return ActionResult.FAILURE; // Cannot achieve goal of getting *more* shrimp if full
            }


            // Still animating, haven't timed out or caught shrimp yet
            currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, true);
            return ActionResult.IN_PROGRESS;
        }

        // If not animating, try to start
        resetAnimationState();

        // Find necessary items/objects
        Item fishingNet = Inventory.get(NET_NAME);
        // Fishing spots are NPCs
        NPC fishingSpot = NPCs.closest(spot -> spot != null && spot.getName().equals(FISHING_SPOT_NAME) && spot.hasAction("Net")); // Ensure it has the "Net" action

        if (fishingNet == null) {
            Logger.log(getName() + ": Fishing net not found in inventory.");
            return ActionResult.FAILURE; // Should be caught by isApplicable
        }
        if (fishingSpot == null) {
            Logger.log(getName() + ": No suitable fishing spot found nearby.");
            return ActionResult.FAILURE;
        }

        // Walk if needed
        if (!fishingSpot.isOnScreen() || fishingSpot.distance() > 5) {
            Logger.log(getName() + ": Walking to fishing spot.");
            if (Walking.walk(fishingSpot)) {
                Sleep.sleepUntil(fishingSpot::isOnScreen, 3000);
            }
        }

        Logger.log(getName() + ": Interacting 'Net' with Fishing spot.");
        // Interact with the spot directly using the "Net" action
        if (fishingSpot.interact("Net")) {
            // Wait for animation to start
            boolean startedAnimating = Sleep.sleepUntil(() -> Players.getLocal().isAnimating() && Players.getLocal().getAnimation() == FISHING_ANIMATION_ID, 5000);

            if (startedAnimating) {
                Logger.log(getName() + ": Started fishing animation.");
                animationStartTime = System.currentTimeMillis();
                initialShrimpCount = Inventory.count(SHRIMP_NAME);
                currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, true);
                return ActionResult.IN_PROGRESS;
            } else {
                Logger.log(getName() + ": Failed to start fishing animation after interaction.");
                // Spot might have moved, or interaction failed
                return ActionResult.FAILURE;
            }
        } else {
            Logger.log(getName() + ": Interaction 'Net' failed on Fishing spot.");
            return ActionResult.FAILURE;
        }
    }

    private void resetAnimationState() {
        animationStartTime = 0;
        initialShrimpCount = -1;
    }

    @Override
    public void onAbort() {
        resetAnimationState();
        Logger.log(getName() + ": Aborted.");
    }
}