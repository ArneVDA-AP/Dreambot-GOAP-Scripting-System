package Core.Actions;

import Core.GOAP.Action;
import Core.GOAP.ActionResult;
import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
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

/**
 * Action to find and interact with a specific GameObject using a given action string.
 * Example: Clicking a ladder ("Climb-down"), clicking an anvil ("Smith").
 */
public class ActionClickObject implements Action {

    private final String objectName; // Can be null if using ID or predicate
    private final int objectId;      // Can be -1 if using name or predicate
    private final Predicate<GameObject> objectPredicate; // Can be null if using name/ID
    private final String interaction; // The action string to click (e.g., "Climb-down", "Smith")
    private final Tile specificTile; // Optional: Exact tile for targeting specific instance

    // Optional: State changes anticipated by the planner
    private final Map<WorldStateKey, Object> effectsMap;

    // Optional: Animation check
    private final int expectedAnimationId; // Set to -1 if not applicable
    private long animationStartTime = 0;
    private long animationTimeout = 8000; // Default timeout

    /** Constructor using Object Name */
    public ActionClickObject(String objName, String interaction, Map<WorldStateKey, Object> effects, int animId) {
        this(objName, -1, null, null, interaction, effects, animId);
    }

    /** Constructor using Object ID */
    public ActionClickObject(int objId, String interaction, Map<WorldStateKey, Object> effects, int animId) {
        this(null, objId, null, null, interaction, effects, animId);
    }

    /** Constructor using Object Name and specific Tile */
    public ActionClickObject(String objName, Tile tile, String interaction, Map<WorldStateKey, Object> effects, int animId) {
        this(objName, -1, tile, null, interaction, effects, animId);
    }

    /** Constructor using Object ID and specific Tile */
    public ActionClickObject(int objId, Tile tile, String interaction, Map<WorldStateKey, Object> effects, int animId) {
        this(null, objId, tile, null, interaction, effects, animId);
    }

    /** Constructor using Predicate */
    public ActionClickObject(Predicate<GameObject> predicate, String interaction, Map<WorldStateKey, Object> effects, int animId) {
        this(null, -1, null, predicate, interaction, effects, animId);
    }


    // Private master constructor
    private ActionClickObject(String name, int id, Tile tile, Predicate<GameObject> predicate,
                              String interaction, Map<WorldStateKey, Object> effects, int animId) {
        this.objectName = name;
        this.objectId = id;
        this.specificTile = tile;
        this.objectPredicate = predicate;
        this.interaction = Objects.requireNonNull(interaction, "Interaction string cannot be null");
        this.effectsMap = (effects != null) ? new HashMap<>(effects) : new HashMap<>();
        this.expectedAnimationId = animId;

        if (name == null && id <= 0 && predicate == null && tile == null) {
            throw new IllegalArgumentException("Must provide object name, ID, Tile, or predicate.");
        }
        // Ensure INTERACT_IS_ANIMATING effect is included if an animation is expected
        if (animId != -1 && !this.effectsMap.containsKey(WorldStateKey.INTERACT_IS_ANIMATING)) {
            this.effectsMap.put(WorldStateKey.INTERACT_IS_ANIMATING, false); // Anticipate animation ends
        } else if (!this.effectsMap.containsKey(WorldStateKey.INTERACT_IS_ANIMATING)){
            // If no animation expected, ensure effect doesn't claim we start animating
            // This might be redundant if preconditions already check this
        }
    }

    @Override
    public String getName() {
        String targetDesc = objectName != null ? objectName : (objectId > 0 ? "ID_" + objectId : (specificTile != null ? "Tile_" + specificTile.getX()+"_"+specificTile.getY() : "Object"));
        return interaction + "_" + targetDesc;
    }

    @Override
    public Map<WorldStateKey, Object> getPreconditions() {
        Map<WorldStateKey, Object> preconditions = new HashMap<>();
        // Basic preconditions - can be expanded by specific goal requirements
        preconditions.put(WorldStateKey.INTERACT_IS_ANIMATING, false);
        preconditions.put(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN, false);
        return preconditions;
    }

    @Override
    public Map<WorldStateKey, Object> getEffects() {
        // Return the effects provided during construction
        return effectsMap;
    }

    @Override
    public double getCost() {
        GameObject obj = findObject();
        double distance = (obj != null) ? Players.getLocal().distance(obj) : 10.0; // Estimate distance if obj not found yet
        return 1.0 + (distance / 5.0); // Base cost + distance
    }

    @Override
    public boolean isApplicable(WorldState state) {
        if (state.getBoolean(WorldStateKey.INTERACT_IS_ANIMATING) || state.getBoolean(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN)) {
            return false;
        }
        // Check if the object exists and has the required action
        GameObject obj = findObject();
        return obj != null && obj.hasAction(interaction);
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
            // If animation is the primary indicator of success (like smithing/smelting)
            // We might need to wait until it *stops* to return SUCCESS.
            // Or, if the goal is just to *start* the animation, we could return SUCCESS sooner.
            // Let's assume for now success is when animation *stops* or a state changes.
            // Check if expected effects occurred (this is complex without specific keys)
            // For now, just continue if animating.
            currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, true);
            return ActionResult.IN_PROGRESS;
        } else if (animationStartTime != 0) {
            // We were animating, but now we're not - assume success if animation was expected
            if (expectedAnimationId != -1) {
                Logger.log(getName() + ": Animation finished.");
                resetAnimationState();
                // Manually apply effects to current state as confirmation
                currentState.applyEffects(getEffects());
                currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, false);
                return ActionResult.SUCCESS;
            }
        }

        // If not animating (or no animation expected), try to interact
        resetAnimationState();

        GameObject targetObject = findObject();
        if (targetObject == null) {
            Logger.log(getName() + ": Target object not found.");
            return ActionResult.FAILURE;
        }
        if (!targetObject.hasAction(interaction)) {
            Logger.log(getName() + ": Target object does not have action '" + interaction + "'.");
            return ActionResult.FAILURE;
        }

        // Walk if needed
        if (!targetObject.isOnScreen() || targetObject.distance() > 8) {
            Logger.log(getName() + ": Walking to target object at " + targetObject.getTile());
            if (Walking.walk(targetObject)) {
                Sleep.sleepUntil(targetObject::isOnScreen, 3000);
            } else {
                Logger.log(getName() + ": Walking failed.");
                return ActionResult.FAILURE;
            }
        }

        Logger.log(getName() + ": Interacting '" + interaction + "' with " + targetObject.getName());
        if (targetObject.interact(interaction)) {
            // Wait for animation start OR player position change (e.g., after climbing ladder)
            Tile startingTile = localPlayer.getTile();
            boolean conditionMet = Sleep.sleepUntil(() -> {
                boolean isAnimating = expectedAnimationId != -1 && Players.getLocal().isAnimating() && Players.getLocal().getAnimation() == expectedAnimationId;
                boolean positionChanged = !Players.getLocal().getTile().equals(startingTile);
                return isAnimating || positionChanged;
            }, 5000); // Adjust timeout as needed

            if (conditionMet) {
                boolean stillAnimating = expectedAnimationId != -1 && Players.getLocal().isAnimating() && Players.getLocal().getAnimation() == expectedAnimationId;
                boolean positionChanged = !Players.getLocal().getTile().equals(startingTile);

                if (stillAnimating) {
                    Logger.log(getName() + ": Started animation...");
                    animationStartTime = System.currentTimeMillis();
                    currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, true);
                    return ActionResult.IN_PROGRESS;
                } else if (positionChanged) {
                    Logger.log(getName() + ": Position changed after interaction (e.g., climbed ladder).");
                    currentState.applyEffects(getEffects()); // Apply effects as success confirmed
                    currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, false);
                    return ActionResult.SUCCESS;
                } else {
                    // Interaction likely succeeded instantly without animation/movement
                    Logger.log(getName() + ": Interaction likely succeeded (no animation/movement detected).");
                    currentState.applyEffects(getEffects());
                    currentState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, false);
                    return ActionResult.SUCCESS;
                }
            } else {
                Logger.log(getName() + ": Timed out waiting for animation/position change after interaction.");
                // Check if maybe the state *did* change despite no animation/movement
                GameObject postInteractionObject = findObject();
                if (postInteractionObject == null || !postInteractionObject.hasAction(interaction)) {
                    Logger.log(getName() + ": Object state changed after timeout, assuming success.");
                    currentState.applyEffects(getEffects());
                    return ActionResult.SUCCESS;
                }
                return ActionResult.FAILURE;
            }
        } else {
            Logger.log(getName() + ": Interaction '" + interaction + "' failed.");
            return ActionResult.FAILURE;
        }
    }

    /** Helper method to find the specific GameObject */
    private GameObject findObject() {
        Predicate<GameObject> basePredicate = obj -> obj != null && (objectId <= 0 || obj.getID() == objectId) && (objectName == null || obj.getName().equals(objectName)) && (specificTile == null || obj.getTile().equals(specificTile));

        if (objectPredicate != null) {
            // Combine the provided predicate with the base checks
            Predicate<GameObject> combined = basePredicate.and(objectPredicate);
            return GameObjects.closest(obj -> combined.test(obj)); // Use lambda directly with closest
        } else {
            return GameObjects.closest(obj -> basePredicate.test(obj)); // Use lambda directly with closest
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