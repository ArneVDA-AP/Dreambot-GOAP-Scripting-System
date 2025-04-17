package Core.Actions;

import Core.GOAP.Action;
import Core.GOAP.ActionResult;
import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile; // May need Tile for ground check
import org.dreambot.api.methods.skills.Skill; // For checking Firemaking level/XP gain
import org.dreambot.api.script.ScriptManager;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.wrappers.items.Item;
import org.dreambot.api.methods.interactive.GameObjects; // To check if fire exists after

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Action to create a fire using a Tinderbox and Logs from the inventory.
 */
public class ActionMakeFire implements Action {

    private final String TINDERBOX_NAME = "Tinderbox";
    private final String LOGS_NAME = "Logs";
    private final WorldStateKey HAS_TINDERBOX_KEY = WorldStateKey.S1_HAS_TINDERBOX;
    private final WorldStateKey HAS_LOGS_KEY = WorldStateKey.S1_HAS_LOGS;
    private final WorldStateKey IS_FIRE_LIT_KEY = WorldStateKey.S1_IS_FIRE_LIT;
    private final int FIREMAKING_ANIMATION_ID = 733; // Common firemaking animation - VERIFY

    // Internal state
    private long animationStartTime = 0;
    private long animationTimeout = 8000; // Timeout for firemaking animation

    public ActionMakeFire() {
        // No specific area needed usually, done from inventory
    }

    @Override
    public String getName() {
        return "MakeFire";
    }

    @Override
    public Map<WorldStateKey, Object> getPreconditions() {
        Map<WorldStateKey, Object> preconditions = new HashMap<>();
        // Preconditions: Must have Tinderbox and Logs
        preconditions.put(HAS_TINDERBOX_KEY, true);
        preconditions.put(HAS_LOGS_KEY, true);
        // Precondition: Not already standing on a fire (usually)
        // preconditions.put(WorldStateKey.STANDING_ON_FIRE, false); // Needs observer check
        // Precondition: Not currently animating something else
        preconditions.put(WorldStateKey.INTERACT_IS_ANIMATING, false);
        return preconditions;
    }

    @Override
    public Map<WorldStateKey, Object> getEffects() {
        Map<WorldStateKey, Object> effects = new HashMap<>();
        // Effect: Logs are consumed
        effects.put(HAS_LOGS_KEY, false);
        // Effect: A fire is lit (at player's location, observer confirms)
        effects.put(IS_FIRE_LIT_KEY, true);
        // Effect: Player is no longer animating (anticipated)
        effects.put(WorldStateKey.INTERACT_IS_ANIMATING, false);
        return effects;
    }

    @Override
    public double getCost() {
        return 1.5; // Slightly more complex than just clicking
    }

    @Override
    public boolean isApplicable(WorldState state) {
        // Check inventory for items
        if (!state.getBoolean(HAS_TINDERBOX_KEY) || !state.getBoolean(HAS_LOGS_KEY)) {
            return false;
        }
        // Check if already animating
        if (state.getBoolean(WorldStateKey.INTERACT_IS_ANIMATING)) {
            return false;
        }
        // Check if standing on an existing fire (prevents making fire on top of another)
        Tile playerTile = Players.getLocal().getTile();
        GameObject existingFire = GameObjects.closest(obj -> obj != null && obj.getName().equals("Fire") && obj.getTile().equals(playerTile));
        if (existingFire != null) {
            Logger.log(getName() + ": Already standing on a fire.");
            return false;
        }

        return true;
    }

    @Override
    public ActionResult perform(WorldState currentState) {
        Player localPlayer = Players.getLocal();
        Tile playerTile = localPlayer.getTile();

        // Check if already doing the firemaking animation
        if (localPlayer.isAnimating() && localPlayer.getAnimation() == FIREMAKING_ANIMATION_ID) {
            if (animationStartTime == 0) {
                animationStartTime = System.currentTimeMillis();
                Logger.log(getName() + ": Continuing firemaking animation.");
            }
            // Check for timeout
            if (System.currentTimeMillis() - animationStartTime > animationTimeout) {
                Logger.log(getName() + ": Animation timed out.");
                resetAnimationState();
                currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, false);
                return ActionResult.FAILURE;
            }
            // Check if fire appeared and logs are gone
            GameObject fire = GameObjects.closest(obj -> obj != null && obj.getName().equals("Fire") && obj.getTile().equals(playerTile));
            boolean logsGone = !Inventory.contains(LOGS_NAME);

            if (fire != null && logsGone) {
                Logger.log(getName() + ": Fire successfully created.");
                resetAnimationState();
                currentState.setBoolean(IS_FIRE_LIT_KEY, true); // Update state
                currentState.setBoolean(HAS_LOGS_KEY, false);
                currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, false);
                return ActionResult.SUCCESS;
            }
            // Still animating
            currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, true);
            return ActionResult.IN_PROGRESS;
        }

        // If not animating, try to start
        resetAnimationState();

        Item tinderbox = Inventory.get(TINDERBOX_NAME);
        Item logs = Inventory.get(LOGS_NAME);

        if (tinderbox == null || logs == null) {
            Logger.log(getName() + ": Missing Tinderbox or Logs.");
            return ActionResult.FAILURE; // Should have been caught by isApplicable, but safety check
        }

        Logger.log(getName() + ": Using Tinderbox on Logs.");
        if (tinderbox.useOn(logs)) {
            // Wait for animation to start
            boolean startedAnimating = Sleep.sleepUntil(() -> Players.getLocal().isAnimating() && Players.getLocal().getAnimation() == FIREMAKING_ANIMATION_ID, 4000);

            if (startedAnimating) {
                Logger.log(getName() + ": Started firemaking animation.");
                animationStartTime = System.currentTimeMillis();
                currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, true);
                return ActionResult.IN_PROGRESS;
            } else {
                Logger.log(getName() + ": Failed to start firemaking animation after interaction.");
                // Could be player moved, or interaction failed silently
                return ActionResult.FAILURE;
            }
        } else {
            Logger.log(getName() + ": Failed to use Tinderbox on Logs.");
            return ActionResult.FAILURE;
        }
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