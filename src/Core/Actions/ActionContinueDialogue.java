package Core.Actions;

import Core.GOAP.Action;
import Core.GOAP.ActionResult;
import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;

import java.util.HashMap;
import java.util.Map;

/**
 * Action to continue the current dialogue by clicking the continue prompt
 * or pressing spacebar.
 */
public class ActionContinueDialogue implements Action {

    private final String expectedNpc; // Optional: Only continue if talking to specific NPC
    private long dialogueTimeout = 3000; // Timeout for the continue action itself

    /**
     * Constructor to continue dialogue with any NPC.
     */
    public ActionContinueDialogue() {
        this.expectedNpc = null; // Continue any dialogue
    }

    /**
     * Constructor to continue dialogue only if talking to a specific NPC.
     * @param npcName The name of the NPC expected to be in dialogue with.
     */
    public ActionContinueDialogue(String npcName) {
        this.expectedNpc = npcName;
    }


    @Override
    public String getName() {
        return "ContinueDialogue" + (expectedNpc != null ? "_"+expectedNpc : "");
    }

    @Override
    public Map<WorldStateKey, Object> getPreconditions() {
        Map<WorldStateKey, Object> preconditions = new HashMap<>();
        // Precondition: Dialogue must be open and potentially advanceable
        preconditions.put(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN, true);
        if (expectedNpc != null) {
            preconditions.put(WorldStateKey.INTERACT_NPC_NAME, expectedNpc);
        }
        return preconditions;
    }

    @Override
    public Map<WorldStateKey, Object> getEffects() {
        Map<WorldStateKey, Object> effects = new HashMap<>();
        // Effect: Dialogue is potentially still open, or closed if this was the last step.
        // It's hard for the planner to know the exact outcome reliably.
        // We might assume it *could* close the dialogue.
        // effects.put(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN, false); // Planner anticipates this possibility
        // effects.put(WorldStateKey.INTERACT_NPC_NAME, null);
        // More importantly, it might enable the *next* step (e.g., make door openable)
        // This state change (like TUT_S0_READY_FOR_DOOR) should be an effect of *this* action
        // if continuing dialogue is what enables the next step.
        // Example (Needs context from Goal/Stage):
        // if ("TalkAfterSettings".equals(targetStageName)) { // Hypothetical target stage name
        //     effects.put(WorldStateKey.TUT_S0_READY_FOR_DOOR, true);
        // }
        return effects; // Effects are tricky here, often minimal for simple continue
    }

    @Override
    public double getCost() {
        return 0.5; // Slightly lower cost than initiating talk or walking
    }

    @Override
    public boolean isApplicable(WorldState state) {
        // Check if dialogue is open and potentially with the correct NPC
        boolean dialogueOpen = state.getBoolean(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN);
        if (!dialogueOpen) return false;

        if (expectedNpc != null && !expectedNpc.equals(state.getString(WorldStateKey.INTERACT_NPC_NAME))) {
            return false; // Dialogue open, but wrong NPC
        }

        // Check if dialogue can actually be continued
        return Dialogues.canContinue();
    }

    @Override
    public ActionResult perform(WorldState currentState) {
        if (!Dialogues.canContinue()) {
            Logger.log(getName() + ": Cannot continue dialogue right now.");
            // This might happen if waiting for options or if dialogue closed between checks.
            // Returning IN_PROGRESS might cause a loop if options appear.
            // Returning FAILURE might be safer to trigger replan/reassessment.
            return ActionResult.FAILURE;
        }

        Logger.log(getName() + ": Attempting to continue dialogue...");
        if (Dialogues.continueDialogue()) {
            // Wait briefly to allow the dialogue state to potentially update
            Sleep.sleep(Calculations.random(400, 700));
            // Check if dialogue is *still* continuable or completely closed
            boolean stillInDialogue = Dialogues.inDialogue(); // Use broader check after continuing

            if (stillInDialogue && Dialogues.canContinue()) {
                Logger.log(getName() + ": Dialogue advanced, more continues available.");
                return ActionResult.IN_PROGRESS; // Still more dialogue to continue
            } else if (!stillInDialogue) {
                Logger.log(getName() + ": Dialogue finished.");
                currentState.setBoolean(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN, false); // Update state
                currentState.setString(WorldStateKey.INTERACT_NPC_NAME, null);
                return ActionResult.SUCCESS; // Dialogue sequence completed
            } else {
                // Dialogue window still open, but cannot continue (e.g., options appeared)
                Logger.log(getName() + ": Dialogue advanced, but cannot continue (options?).");
                // SUCCESS might be appropriate here, letting another action handle options.
                return ActionResult.SUCCESS;
            }
        } else {
            Logger.log(getName() + ": Failed to execute continueDialogue().");
            return ActionResult.FAILURE;
        }
    }
}