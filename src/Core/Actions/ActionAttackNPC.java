package Core.Actions;

import Core.GOAP.Action;
import Core.GOAP.ActionResult;
import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.combat.Combat; // For potentially turning auto-retaliate on/off
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area; // Optional
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.script.ScriptManager;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.Character; // For interacting character
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.methods.filter.Filter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import static Core.GameIntegration.DreamBotWorldObserver.TUTORIAL_AREAS;

/**
 * Action to find and attack a specific NPC.
 */
public class ActionAttackNPC implements Action {

    private final String npcName; // Can be null if using predicate
    private final Predicate<NPC> npcPredicate; // Can be null if using name
    private final Area combatArea; // Optional area constraint

    // Optional: State changes anticipated by the planner
    private final Map<WorldStateKey, Object> effectsMap;

    // Internal state
    private NPC currentTarget = null;
    private long combatStartTime = 0;
    private long combatTimeout = 120000; // 2 minutes timeout per fight? Adjust as needed.

    /** Constructor using NPC Name */
    public ActionAttackNPC(String npcName, Area combatArea, Map<WorldStateKey, Object> effects) {
        this(npcName, null, combatArea, effects);
    }

    /** Constructor using Predicate */
    public ActionAttackNPC(Predicate<NPC> predicate, Area combatArea, Map<WorldStateKey, Object> effects) {
        this(null, predicate, combatArea, effects);
    }

    // Private constructor
    private ActionAttackNPC(String name, Predicate<NPC> predicate, Area area, Map<WorldStateKey, Object> effects) {
        this.npcName = name;
        this.npcPredicate = predicate;
        this.combatArea = area; // Can be null
        this.effectsMap = (effects != null) ? new HashMap<>(effects) : new HashMap<>();

        if (name == null && predicate == null) {
            throw new IllegalArgumentException("Must provide NPC name or predicate.");
        }
        // Ensure effect anticipates not being in combat eventually
        if (!this.effectsMap.containsKey(WorldStateKey.COMBAT_IS_IN_COMBAT)) {
            this.effectsMap.put(WorldStateKey.COMBAT_IS_IN_COMBAT, false);
        }
    }

    @Override
    public String getName() {
        return "Attack_" + (npcName != null ? npcName : "NPC_by_Predicate");
    }

    @Override
    public Map<WorldStateKey, Object> getPreconditions() {
        Map<WorldStateKey, Object> preconditions = new HashMap<>();
        // Precondition: Not already in combat (unless target is current interactor?)
        preconditions.put(WorldStateKey.COMBAT_IS_IN_COMBAT, false);
        // Precondition: Appropriate weapon equipped (e.g., S4_DAGGER_EQUIPPED for first rat)
        // This should be handled by the Goal definition.
        if (combatArea != null) {
            preconditions.put(WorldStateKey.LOC_CURRENT_AREA_NAME, getAreaName());
        }
        return preconditions;
    }

    @Override
    public Map<WorldStateKey, Object> getEffects() {
        // Effects: Target NPC is dead (hard to guarantee, rely on observer)
        // Effect: Player is no longer in combat (anticipated)
        return effectsMap;
    }

    @Override
    public double getCost() {
        // Cost could factor in distance, NPC health/difficulty
        return 5.0; // Combat is costly/time-consuming
    }

    @Override
    public boolean isApplicable(WorldState state) {
        // Don't start attacking if already in combat with something else
        if (state.getBoolean(WorldStateKey.COMBAT_IS_IN_COMBAT)) {
            Character interacting = Players.getLocal().getInteractingCharacter();
            // Allow if already fighting the correct type of NPC
            if (interacting instanceof NPC) {
                NPC currentOpponent = (NPC) interacting;
                boolean nameMatch = npcName != null && npcName.equals(currentOpponent.getName());
                boolean predicateMatch = npcPredicate != null && npcPredicate.test(currentOpponent);
                if (nameMatch || predicateMatch) return false; // Already fighting correct target type
            }
            // Otherwise, don't start a new fight if busy
            return false;
        }

        // Check area if specified
        if (combatArea != null && !getAreaName().equals(state.getString(WorldStateKey.LOC_CURRENT_AREA_NAME))) {
            return false;
        }

        // Check if a suitable target exists
        return findTargetNPC() != null;
    }

    @Override
    public ActionResult perform(WorldState currentState) {
        Player localPlayer = Players.getLocal();

        // --- Check if already in combat ---
        if (localPlayer.isInCombat()) {
            Character interactingChar = localPlayer.getInteractingCharacter();
            if (interactingChar instanceof NPC) {
                NPC currentOpponent = (NPC) interactingChar;
                // Check if fighting the type we intended OR if our specific target is still alive
                boolean nameMatch = npcName != null && npcName.equals(currentOpponent.getName());
                boolean predicateMatch = npcPredicate != null && npcPredicate.test(currentOpponent);
                boolean specificTargetMatch = currentTarget != null && currentTarget.equals(currentOpponent);

                if (nameMatch || predicateMatch || specificTargetMatch) {
                    // We are fighting the correct type or the specific target
                    if (combatStartTime == 0) combatStartTime = System.currentTimeMillis(); // Start timer if just entered combat

                    // Check timeout
                    if (System.currentTimeMillis() - combatStartTime > combatTimeout) {
                        Logger.log(getName() + ": Combat timed out against " + currentOpponent.getName());
                        resetCombatState();
                        currentState.setBoolean(WorldStateKey.COMBAT_IS_IN_COMBAT, false);
                        return ActionResult.FAILURE;
                    }

                    // Check if opponent is dead or no longer interacting
                    if (currentOpponent.getHealthPercent() <= 0 || !currentOpponent.exists() || !currentOpponent.equals(localPlayer.getInteractingCharacter())) {
                        Logger.log(getName() + ": Target " + currentOpponent.getName() + " defeated or interaction ended.");
                        resetCombatState();
                        currentState.setBoolean(WorldStateKey.COMBAT_IS_IN_COMBAT, false);
                        // Apply effects manually here as confirmation
                        currentState.applyEffects(getEffects());
                        return ActionResult.SUCCESS;
                    }

                    // Still fighting
                    currentState.setBoolean(WorldStateKey.COMBAT_IS_IN_COMBAT, true);
                    return ActionResult.IN_PROGRESS;
                }
            }
            // In combat, but not with the right target? Let current fight finish or fail?
            // For simplicity, let's fail this action if in combat with wrong target.
            Logger.log(getName() + ": In combat with unexpected target.");
            resetCombatState(); // Reset just in case
            return ActionResult.FAILURE;
        }

        // --- If not in combat, find and attack target ---
        resetCombatState(); // Ensure timer is reset

        currentTarget = findTargetNPC(); // Find closest valid target
        if (currentTarget == null) {
            Logger.log(getName() + ": No suitable target NPC found.");
            return ActionResult.FAILURE;
        }

        // Walk if needed
        if (!currentTarget.isOnScreen() || currentTarget.distance() > 10) { // Increase distance for combat
            Logger.log(getName() + ": Walking to target " + currentTarget.getName());
            if (Walking.walk(currentTarget)) {
                Sleep.sleepUntil(currentTarget::isOnScreen, 4000);
            }
        }

        Logger.log(getName() + ": Interacting 'Attack' with " + currentTarget.getName());
        if (currentTarget.interact("Attack")) {
            // Wait for combat state to change
            boolean startedCombat = Sleep.sleepUntil(() -> Players.getLocal().isInCombat(), 5000);

            if (startedCombat) {
                Logger.log(getName() + ": Successfully initiated combat with " + currentTarget.getName());
                combatStartTime = System.currentTimeMillis(); // Start timer
                currentState.setBoolean(WorldStateKey.COMBAT_IS_IN_COMBAT, true);
                return ActionResult.IN_PROGRESS;
            } else {
                Logger.log(getName() + ": Failed to confirm combat start after attacking.");
                // Target might have died, moved, or interaction failed
                currentTarget = null; // Clear target
                return ActionResult.FAILURE;
            }
        } else {
            Logger.log(getName() + ": Interaction 'Attack' failed on " + currentTarget.getName());
            currentTarget = null;
            return ActionResult.FAILURE;
        }
    }

    /** Finds the closest valid NPC target based on name or predicate */
    private NPC findTargetNPC() {
        // Define the filter using DreamBot's Filter interface
        Filter<NPC> filter = npc -> {
            if (npc == null || !npc.exists() || npc.isInCombat() || npc.getHealthPercent() <= 0 || !npc.hasAction("Attack")) {
                return false; // Basic validity checks
            }
            if (combatArea != null && !combatArea.contains(npc)) {
                return false; // Check area constraint
            }
            // Apply specific name or predicate check
            if (npcPredicate != null) {
                return npcPredicate.test(npc); // Use custom predicate logic
            }
            if (npcName != null) {
                return npcName.equals(npc.getName()); // Use name
            }
            return false; // Should have name or predicate if we reach here
        };

        // Call the closest method that accepts DreamBot's Filter
        return NPCs.closest(filter);
    }

    private String getAreaName() {
        // Crude way to get area name string if needed for preconditions
        if (combatArea != null) {
            for (Map.Entry<String, Area> entry : TUTORIAL_AREAS.entrySet()) { // Assumes TUTORIAL_AREAS is accessible
                if (entry.getValue().equals(combatArea)) return entry.getKey();
            }
        }
        return "UnknownCombatArea";
    }

    private void resetCombatState() {
        currentTarget = null;
        combatStartTime = 0;
    }

    @Override
    public void onAbort() {
        resetCombatState();
        Logger.log(getName() + ": Aborted.");
    }
}