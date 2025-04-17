package Core.Actions;

import Core.GOAP.Action;
import Core.GOAP.ActionResult;
import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.combat.Combat;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.filter.Filter;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.magic.Magic; // Import Magic class
import org.dreambot.api.methods.magic.Normal; // Import Normal spell enum (or other spellbooks if needed)
import org.dreambot.api.methods.magic.Spell; // Import Spell interface
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.script.ScriptManager;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.Character;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.interactive.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import static Core.GameIntegration.DreamBotWorldObserver.TUTORIAL_AREAS;

/**
 * Action to cast a specific Normal spell on a target NPC.
 */
public class ActionCastSpellOnNPC implements Action {

    private final Spell spellToCast;
    private final String targetNpcName; // Can be null if using predicate
    private final Predicate<NPC> targetNpcPredicate; // Can be null if using name
    private final Area combatArea; // Optional area constraint

    // Keys for required runes (example for Wind Strike)
    private final WorldStateKey hasAirRuneKey = WorldStateKey.S7_HAS_AIR_RUNE;
    private final WorldStateKey hasMindRuneKey = WorldStateKey.S7_HAS_MIND_RUNE;

    // Optional: State changes anticipated by the planner
    private final Map<WorldStateKey, Object> effectsMap;

    // Internal state
    private NPC currentTarget = null;
    private long combatStartTime = 0;
    private long combatTimeout = 60000; // Timeout for spell combat

    /** Constructor using Spell enum and NPC Name */
    public ActionCastSpellOnNPC(Spell spell, String npcName, Area combatArea, Map<WorldStateKey, Object> effects) {
        this(spell, npcName, null, combatArea, effects);
    }

    /** Constructor using Spell enum and NPC Predicate */
    public ActionCastSpellOnNPC(Spell spell, Predicate<NPC> predicate, Area combatArea, Map<WorldStateKey, Object> effects) {
        this(spell, null, predicate, combatArea, effects);
    }

    // Private constructor
    private ActionCastSpellOnNPC(Spell spell, String name, Predicate<NPC> predicate, Area area, Map<WorldStateKey, Object> effects) {
        this.spellToCast = Objects.requireNonNull(spell, "Spell cannot be null");
        this.targetNpcName = name;
        this.targetNpcPredicate = predicate;
        this.combatArea = area; // Can be null
        this.effectsMap = (effects != null) ? new HashMap<>(effects) : new HashMap<>();

        if (name == null && predicate == null) {
            throw new IllegalArgumentException("Must provide NPC name or predicate.");
        }
        // Ensure effect anticipates not being in combat eventually
        if (!this.effectsMap.containsKey(WorldStateKey.COMBAT_IS_IN_COMBAT)) {
            this.effectsMap.put(WorldStateKey.COMBAT_IS_IN_COMBAT, false);
        }
        // Anticipate rune consumption (optional, observer is more reliable)
        // effectsMap.put(hasAirRuneKey, false);
        // effectsMap.put(hasMindRuneKey, false);
    }


    @Override
    public String getName() {
        return "Cast_" + spellToCast.toString() + "_On_" + (targetNpcName != null ? targetNpcName : "NPC_by_Predicate");
    }

    @Override
    public Map<WorldStateKey, Object> getPreconditions() {
        Map<WorldStateKey, Object> preconditions = new HashMap<>();
        // Precondition: Have required runes (specific keys needed based on spell)
        // Example for Wind Strike:
        preconditions.put(hasAirRuneKey, true);
        preconditions.put(hasMindRuneKey, true);
        // Precondition: Not in combat (unless fighting target already)
        preconditions.put(WorldStateKey.COMBAT_IS_IN_COMBAT, false);
        // Precondition: Magic tab might need to be open (or handled by castSpellOn)
        // preconditions.put(WorldStateKey.UI_MAGIC_SPELLBOOK_OPEN, true);
        if (combatArea != null) {
            preconditions.put(WorldStateKey.LOC_CURRENT_AREA_NAME, getAreaName());
        }
        return preconditions;
    }

    @Override
    public Map<WorldStateKey, Object> getEffects() {
        // Return the effects provided during construction
        return effectsMap;
    }

    @Override
    public double getCost() {
        return 6.0; // Magic combat might involve more steps/time
    }

    @Override
    public boolean isApplicable(WorldState state) {
        // Check runes
        // Example for Wind Strike:
        if (!state.getBoolean(hasAirRuneKey) || !state.getBoolean(hasMindRuneKey)) {
            return false;
        }
        // Check if already in combat with wrong target
        if (state.getBoolean(WorldStateKey.COMBAT_IS_IN_COMBAT)) {
            Character interacting = Players.getLocal().getInteractingCharacter();
            if (interacting instanceof NPC) {
                NPC currentOpponent = (NPC) interacting;
                boolean nameMatch = targetNpcName != null && targetNpcName.equals(currentOpponent.getName());
                boolean predicateMatch = targetNpcPredicate != null && targetNpcPredicate.test(currentOpponent);
                if (!nameMatch && !predicateMatch) return false; // Fighting wrong target
            } else {
                return false; // Fighting player?
            }
        }
        // Check area if specified
        if (combatArea != null && !getAreaName().equals(state.getString(WorldStateKey.LOC_CURRENT_AREA_NAME))) {
            return false;
        }
        // Check if target exists
        return findTargetNPC() != null;
    }

    @Override
    public ActionResult perform(WorldState currentState) {
        Player localPlayer = Players.getLocal();

        // --- Check if already in combat with the correct target ---
        if (localPlayer.isInCombat()) {
            Character interactingChar = localPlayer.getInteractingCharacter();
            if (interactingChar instanceof NPC) {
                NPC currentOpponent = (NPC) interactingChar;
                boolean nameMatch = targetNpcName != null && targetNpcName.equals(currentOpponent.getName());
                boolean predicateMatch = targetNpcPredicate != null && targetNpcPredicate.test(currentOpponent);
                boolean specificTargetMatch = currentTarget != null && currentTarget.equals(currentOpponent);

                if (nameMatch || predicateMatch || specificTargetMatch) {
                    // Monitor existing combat
                    if (combatStartTime == 0) combatStartTime = System.currentTimeMillis();
                    if (System.currentTimeMillis() - combatStartTime > combatTimeout) {
                        Logger.log(getName() + ": Combat timed out against " + currentOpponent.getName());
                        resetCombatState();
                        currentState.setBoolean(WorldStateKey.COMBAT_IS_IN_COMBAT, false);
                        return ActionResult.FAILURE;
                    }
                    if (currentOpponent.getHealthPercent() <= 0 || !currentOpponent.exists() || !currentOpponent.equals(localPlayer.getInteractingCharacter())) {
                        Logger.log(getName() + ": Target " + currentOpponent.getName() + " defeated or interaction ended.");
                        resetCombatState();
                        currentState.setBoolean(WorldStateKey.COMBAT_IS_IN_COMBAT, false);
                        currentState.applyEffects(getEffects());
                        return ActionResult.SUCCESS;
                    }
                    currentState.setBoolean(WorldStateKey.COMBAT_IS_IN_COMBAT, true);
                    return ActionResult.IN_PROGRESS; // Continue monitoring
                }
            }
            Logger.log(getName() + ": In combat with unexpected target.");
            return ActionResult.FAILURE; // Fail if fighting wrong thing
        }

        // --- If not in combat, find target and cast spell ---
        resetCombatState();

        currentTarget = findTargetNPC();
        if (currentTarget == null) {
            Logger.log(getName() + ": No suitable target NPC found.");
            return ActionResult.FAILURE;
        }

        // Check runes again just before casting
        // Example for Wind Strike:
        if (!Inventory.contains("Air rune") || !Inventory.contains("Mind rune")) {
            Logger.log(getName() + ": Missing required runes.");
            currentState.setBoolean(hasAirRuneKey, Inventory.contains("Air rune"));
            currentState.setBoolean(hasMindRuneKey, Inventory.contains("Mind rune"));
            return ActionResult.FAILURE;
        }

        // Walk if needed
        if (!currentTarget.isOnScreen() || currentTarget.distance() > 10) {
            Logger.log(getName() + ": Walking to target " + currentTarget.getName());
            if (Walking.walk(currentTarget)) {
                Sleep.sleepUntil(currentTarget::isOnScreen, 4000);
            }
        }

        Logger.log(getName() + ": Casting " + spellToCast.toString() + " on " + currentTarget.getName());
        if (Magic.castSpellOn(spellToCast, currentTarget)) {
            // Wait for combat state to change or target health to drop
            boolean combatStarted = Sleep.sleepUntil(() -> Players.getLocal().isInCombat() || (currentTarget.exists() && currentTarget.getHealthPercent() < 100), 5000);

            if (combatStarted) {
                Logger.log(getName() + ": Successfully cast spell and initiated combat/damage.");
                combatStartTime = System.currentTimeMillis();
                currentState.setBoolean(WorldStateKey.COMBAT_IS_IN_COMBAT, true);
                return ActionResult.IN_PROGRESS; // Monitor the fight
            } else {
                Logger.log(getName() + ": Failed to confirm combat start/damage after casting.");
                currentTarget = null;
                return ActionResult.FAILURE;
            }
        } else {
            Logger.log(getName() + ": Magic.castSpellOn() failed.");
            // Check if spellbook needed opening
            if (!Tabs.isOpen(Tab.MAGIC)) {
                Logger.log(getName() + ": Magic tab wasn't open, attempting to open.");
                // Could return FAILURE and let ActionOpenTab handle it, or try opening here.
                // For simplicity, let's fail and assume planner will insert ActionOpenTab if needed.
                return ActionResult.FAILURE;
            }
            currentTarget = null;
            return ActionResult.FAILURE;
        }
    }

    /** Finds the closest valid NPC target based on name or predicate */
    private NPC findTargetNPC() {
        Filter<NPC> filter = npc -> {
            if (npc == null || !npc.exists() || npc.isInCombat() || npc.getHealthPercent() <= 0 || !npc.hasAction("Attack")) {
                return false;
            }
            if (combatArea != null && !combatArea.contains(npc)) {
                return false;
            }
            if (targetNpcPredicate != null) {
                return targetNpcPredicate.test(npc);
            }
            if (targetNpcName != null) {
                return targetNpcName.equals(npc.getName());
            }
            return false;
        };
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