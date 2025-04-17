package Core.Actions; // New package for Action implementations

import Core.GOAP.Action;
import Core.GOAP.ActionResult;
import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area; // Optional: If action requires being in a specific area
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.script.ScriptManager; // To check if script is running
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.NPC;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Action to find and talk to a specific NPC.
 */
public class ActionTalkToNPC implements Action {

    private final String npcName;
    private final String targetStageName; // The stage this action helps achieve
    private final Area requiredArea; // Optional: Area where the NPC must be or player must be
    private long interactionTimeout = 5000; // Timeout for interaction attempt
    private long dialogueTimeout = 8000; // Timeout for dialogue to appear

    /**
     * Constructor for talking to an NPC.
     * @param npcName The exact name of the NPC to talk to.
     * @param targetStageName The name of the tutorial stage this action corresponds to (for effects).
     * @param requiredArea Optional area constraint. If null, no area check is performed.
     */
    public ActionTalkToNPC(String npcName, String targetStageName, Area requiredArea) {
        this.npcName = Objects.requireNonNull(npcName);
        this.targetStageName = Objects.requireNonNull(targetStageName); // Used for effects
        this.requiredArea = requiredArea; // Can be null
    }

    /** Simpler constructor without area constraint */
    public ActionTalkToNPC(String npcName, String targetStageName) {
        this(npcName, targetStageName, null);
    }

    @Override
    public String getName() {
        return "TalkTo_" + npcName;
    }

    @Override
    public Map<WorldStateKey, Object> getPreconditions() {
        Map<WorldStateKey, Object> preconditions = new HashMap<>();
        // Precondition: Not already in dialogue (or dialogue with someone else)
        preconditions.put(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN, false);
        // Optional: Could add area precondition if requiredArea is not null
        // if (requiredArea != null) {
        //     preconditions.put(WorldStateKey.LOC_CURRENT_AREA_NAME, requiredAreaName); // Need area name mapping
        // }
        // Precondition: Ensure we are in the correct overall stage if needed (e.g., can't talk to Chef in S0)
        // This is often better handled by the Goal definition itself.
        return preconditions;
    }

    @Override
    public Map<WorldStateKey, Object> getEffects() {
        Map<WorldStateKey, Object> effects = new HashMap<>();
        // Effect: Dialogue is open with the target NPC
        effects.put(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN, true);
        effects.put(WorldStateKey.INTERACT_NPC_NAME, npcName);
        // Effect: Assume talking advances the stage (Planner uses this)
        // Note: The actual stage change is confirmed by the Observer reading the VarPlayer
        // effects.put(WorldStateKey.TUT_STAGE_NAME, targetStageName); // Planner anticipates this
        return effects;
    }

    @Override
    public double getCost() {
        // Simple cost, could be adjusted based on distance later
        return 1.0;
    }

    @Override
    public boolean isApplicable(WorldState state) {
        // Check basic preconditions: not already in dialogue
        if (state.getBoolean(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN)) {
            return false;
        }
        // Optional: Check if player is in the required area
        if (requiredArea != null && !requiredArea.contains(Players.getLocal())) {
            return false;
        }
        // Runtime check: Can we see the NPC?
        NPC targetNpc = NPCs.closest(npc -> npc != null && npc.getName().equals(npcName));
        return targetNpc != null; // Applicable if NPC is findable nearby
    }

    @Override
    public ActionResult perform(WorldState currentState) {
        // Find the NPC
        NPC targetNpc = NPCs.closest(npc -> npc != null && npc.getName().equals(npcName));

        if (targetNpc == null) {
            Logger.log(getName() + ": NPC not found.");
            return ActionResult.FAILURE; // Cannot perform if NPC isn't there
        }

        // Optional: Walk closer if needed (though interact should handle this)
        if (!targetNpc.isOnScreen() || targetNpc.distance() > 8) {
            Logger.log(getName() + ": Walking closer to " + npcName);
            if (Walking.walk(targetNpc)) {
                Sleep.sleepUntil(() -> targetNpc.isOnScreen() && targetNpc.canReach(), 3000);
            }
        }

        // Interact
        Logger.log(getName() + ": Attempting interaction with " + npcName);
        if (targetNpc.interact("Talk-to")) {
            // Wait until dialogue is detected by the observer's logic
            boolean dialogueStarted = Sleep.sleepUntil(() -> Dialogues.inDialogue() && Dialogues.canContinue(), dialogueTimeout);

            if (dialogueStarted) {
                Logger.log(getName() + ": Dialogue started successfully with " + npcName);
                // We don't handle the *entire* dialogue here.
                // This action's goal is just to *initiate* the talk.
                // Other actions (like ActionContinueDialogue) will handle progressing it.
                // We update the WorldState optimistically for the planner,
                // but the observer will confirm the actual state next loop.
                currentState.setBoolean(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN, true);
                currentState.setString(WorldStateKey.INTERACT_NPC_NAME, npcName);
                return ActionResult.SUCCESS;
            } else {
                Logger.log(getName() + ": Failed to detect dialogue start after interacting with " + npcName);
                return ActionResult.FAILURE; // Interaction happened but dialogue didn't appear as expected
            }
        } else {
            Logger.log(getName() + ": Interaction failed with " + npcName);
            return ActionResult.FAILURE;
        }
    }
}