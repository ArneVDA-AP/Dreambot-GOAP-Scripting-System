package Core.Actions;

import Core.GOAP.Action;
import Core.GOAP.ActionResult;
import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.skills.Skill; // For checking Cooking level/XP gain
import org.dreambot.api.script.ScriptManager;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.wrappers.items.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Action to cook Raw shrimps on a fire or range.
 */
public class ActionCookShrimp implements Action {

    private final String RAW_SHRIMP_NAME = "Raw shrimps";
    private final String COOKED_SHRIMP_NAME = "Shrimps";
    private final String FIRE_NAME = "Fire"; // Or "Cooking range"
    private final WorldStateKey HAS_RAW_KEY = WorldStateKey.S1_HAS_RAW_SHRIMP;
    private final WorldStateKey HAS_COOKED_KEY = WorldStateKey.S1_HAS_COOKED_SHRIMP;
    private final WorldStateKey IS_FIRE_LIT_KEY = WorldStateKey.S1_IS_FIRE_LIT; // Or check for range nearby
    private final WorldStateKey AREA_KEY = WorldStateKey.LOC_CURRENT_AREA_NAME;
    private final String REQUIRED_AREA_NAME = "Survival_Cooking_Area"; // Area where fire/range is
    private final int COOKING_ANIMATION_ID = 897; // Common cooking animation ID - VERIFY

    // Internal state
    private long animationStartTime = 0;
    private long animationTimeout = 10000; // Cooking is usually quick per item
    private int initialRawCount = -1;
    private int initialCookedCount = -1;

    public ActionCookShrimp() {
        // Assumes area check is handled by preconditions/applicability
    }

    @Override
    public String getName() {
        return "CookShrimp";
    }

    @Override
    public Map<WorldStateKey, Object> getPreconditions() {
        Map<WorldStateKey, Object> preconditions = new HashMap<>();
        // Preconditions: Must have raw shrimp, be near a cooking source (fire/range)
        preconditions.put(HAS_RAW_KEY, true);
        preconditions.put(IS_FIRE_LIT_KEY, true); // Or a key indicating near range
        preconditions.put(AREA_KEY, REQUIRED_AREA_NAME);
        preconditions.put(WorldStateKey.INTERACT_IS_ANIMATING, false);
        return preconditions;
    }

    @Override
    public Map<WorldStateKey, Object> getEffects() {
        Map<WorldStateKey, Object> effects = new HashMap<>();
        // Effect: Raw shrimp are gone
        effects.put(HAS_RAW_KEY, false);
        // Effect: Cooked shrimp are present
        effects.put(HAS_COOKED_KEY, true);
        // Effect: Fire might be gone if it was temporary
        // effects.put(IS_FIRE_LIT_KEY, false); // Depends on fire type
        effects.put(WorldStateKey.INTERACT_IS_ANIMATING, false);
        return effects;
    }

    @Override
    public double getCost() {
        return 1.5;
    }

    @Override
    public boolean isApplicable(WorldState state) {
        // Check inventory
        if (!state.getBoolean(HAS_RAW_KEY)) return false;
        // Check if animating
        if (state.getBoolean(WorldStateKey.INTERACT_IS_ANIMATING)) return false;
        // Check if in the right area
        if (!REQUIRED_AREA_NAME.equals(state.getString(AREA_KEY))) return false;
        // Check if a cooking source is available nearby
        return findCookingSource() != null;
    }

    @Override
    public ActionResult perform(WorldState currentState) {
        Player localPlayer = Players.getLocal();

        // Check if already cooking
        if (localPlayer.isAnimating() && localPlayer.getAnimation() == COOKING_ANIMATION_ID) {
            if (animationStartTime == 0 || initialCookedCount == -1) {
                animationStartTime = System.currentTimeMillis();
                initialRawCount = Inventory.count(RAW_SHRIMP_NAME);
                initialCookedCount = Inventory.count(COOKED_SHRIMP_NAME);
                Logger.log(getName() + ": Continuing cooking animation. Initial counts - Raw: " + initialRawCount + ", Cooked: " + initialCookedCount);
            }

            // Check for timeout
            if (System.currentTimeMillis() - animationStartTime > animationTimeout) {
                Logger.log(getName() + ": Cooking animation timed out.");
                resetAnimationState();
                currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, false);
                return ActionResult.FAILURE;
            }

            // Check if shrimp was cooked (raw count decreased OR cooked count increased)
            int currentRawCount = Inventory.count(RAW_SHRIMP_NAME);
            int currentCookedCount = Inventory.count(COOKED_SHRIMP_NAME);

            if (currentRawCount < initialRawCount || currentCookedCount > initialCookedCount) {
                Logger.log(getName() + ": Successfully cooked shrimp.");
                resetAnimationState();
                currentState.setBoolean(HAS_COOKED_KEY, true); // Update state
                currentState.setBoolean(HAS_RAW_KEY, Inventory.contains(RAW_SHRIMP_NAME)); // Update raw state
                currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, false);
                // Check if fire still exists (important for subsequent cooks)
                currentState.setBoolean(IS_FIRE_LIT_KEY, findCookingSource() != null);
                return ActionResult.SUCCESS; // Tutorial usually needs one successful cook
            }

            // Still animating
            currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, true);
            return ActionResult.IN_PROGRESS;
        }

        // If not animating, try to start
        resetAnimationState();

        Item rawShrimp = Inventory.get(RAW_SHRIMP_NAME);
        GameObject cookingSource = findCookingSource();

        if (rawShrimp == null) {
            Logger.log(getName() + ": No raw shrimp found.");
            return ActionResult.FAILURE;
        }
        if (cookingSource == null) {
            Logger.log(getName() + ": No cooking source (fire/range) found nearby.");
            currentState.setBoolean(IS_FIRE_LIT_KEY, false); // Update state if fire isn't found
            return ActionResult.FAILURE;
        }

        Logger.log(getName() + ": Using " + RAW_SHRIMP_NAME + " on " + cookingSource.getName());
        if (rawShrimp.useOn(cookingSource)) {
            // Wait for cooking animation to start
            boolean startedAnimating = Sleep.sleepUntil(() -> Players.getLocal().isAnimating() && Players.getLocal().getAnimation() == COOKING_ANIMATION_ID, 4000);

            if (startedAnimating) {
                Logger.log(getName() + ": Started cooking animation.");
                animationStartTime = System.currentTimeMillis();
                initialRawCount = Inventory.count(RAW_SHRIMP_NAME);
                initialCookedCount = Inventory.count(COOKED_SHRIMP_NAME);
                currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, true);
                return ActionResult.IN_PROGRESS;
            } else {
                Logger.log(getName() + ": Failed to start cooking animation after interaction.");
                // Fire might have gone out, or interaction failed
                currentState.setBoolean(IS_FIRE_LIT_KEY, findCookingSource() != null); // Update fire state
                return ActionResult.FAILURE;
            }
        } else {
            Logger.log(getName() + ": Failed to use shrimp on cooking source.");
            return ActionResult.FAILURE;
        }
    }

    /** Finds the nearest usable cooking source (Fire on player tile, or Range) */
    private GameObject findCookingSource() {
        Tile playerTile = Players.getLocal().getTile();
        // Prioritize fire on current tile
        GameObject fire = GameObjects.closest(obj -> obj != null && obj.getName().equals(FIRE_NAME) && obj.getTile().equals(playerTile) && obj.hasAction("Cook"));
        if (fire != null) {
            return fire;
        }
        // TODO: Add check for "Cooking range" if needed later
        // GameObject range = GameObjects.closest("Cooking range");
        // if (range != null && range.canReach()) return range;
        return null;
    }


    private void resetAnimationState() {
        animationStartTime = 0;
        initialRawCount = -1;
        initialCookedCount = -1;
    }

    @Override
    public void onAbort() {
        resetAnimationState();
        Logger.log(getName() + ": Aborted.");
    }
}