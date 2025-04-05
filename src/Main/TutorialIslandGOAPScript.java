package Main; // Or your main package

import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;
import Core.GameIntegration.DreamBotWorldObserver; // Import the observer
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.Category;
import org.dreambot.api.utilities.Logger; // Use DreamBot Logger
import org.dreambot.api.utilities.Sleep;

@ScriptManifest(name = "GOAP Tutorial Island",
        description = "Automates Tutorial Island using GOAP (Observer Test)",
        author = "YourName & AI Co-Lead",
        version = 0.1,
        category = Category.UTILITY)
public class TutorialIslandGOAPScript extends AbstractScript {

    private WorldState worldState;
    private DreamBotWorldObserver worldObserver;

    // Variables to track previous state for change detection
    private String previousStageName = "";
    private String previousAreaName = "";
    private boolean previousDialogueState = false; // Initialize based on expected start

    @Override
    public void onStart() {
        Logger.log("Starting GOAP Tutorial Island Script (Observer Test)...");
        worldState = new WorldState();
        worldObserver = new DreamBotWorldObserver(this); // Pass script reference
        // Initialize previous states to force first log
        previousStageName = "INITIALIZING";
        previousAreaName = "INITIALIZING";
        previousDialogueState = true; // Force initial log if dialogue starts false
        Logger.log("Observer and WorldState initialized.");
    }

    @Override
    public int onLoop() {
        if (worldObserver == null || worldState == null) {
            Logger.log("Observer or WorldState not initialized, stopping.");
            return -1; // Stop script if initialization failed
        }

        // 1. Update the World State from the game
        worldObserver.updateWorldState(worldState);

        // 2. Get current key states
        String currentStage = worldState.getString(WorldStateKey.TUT_STAGE_NAME);
        String currentArea = worldState.getString(WorldStateKey.LOC_CURRENT_AREA_NAME);
        boolean isDialogueOpen = worldState.getBoolean(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN);
        int currentStageId = worldState.getInteger(WorldStateKey.TUT_STAGE_ID); // Get the ID too

        // 3. Log only if key states have changed
        boolean stageChanged = !previousStageName.equals(currentStage);
        boolean areaChanged = !previousAreaName.equals(currentArea);
        boolean dialogueChanged = previousDialogueState != isDialogueOpen;

        if (stageChanged || areaChanged || dialogueChanged) {
            Logger.log("===== State Change Detected =====");
            Logger.log(String.format("Stage: %s (ID: %d) | Area: %s | Dialogue: %s",
                    currentStage,
                    currentStageId, // Log the ID
                    currentArea,
                    isDialogueOpen));
            // Log a few other potentially relevant details on change
            Logger.log(String.format("Inv Space: %d | Walking: %s | Animating: %s",
                    worldState.getInteger(WorldStateKey.INV_SPACE),
                    worldState.getBoolean(WorldStateKey.LOC_IS_WALKING),
                    worldState.getBoolean(WorldStateKey.INTERACT_IS_ANIMATING)));
            Logger.log("=================================");

            // Update previous states
            previousStageName = currentStage;
            previousAreaName = currentArea;
            previousDialogueState = isDialogueOpen;
        } else {
            // Optional: Log a less frequent heartbeat message if needed
            // if (System.currentTimeMillis() % 15000 < 3000) { // Log approx every 15s
            //     Logger.log(String.format("Heartbeat - Stage: %s | Area: %s", currentStage, currentArea));
            // }
        }


        // 4. Return sleep time (increased)
        return 3000; // Update and check state every 3 seconds
    }

    @Override
    public void onExit() {
        Logger.log("Stopping GOAP Tutorial Island Script.");
    }
}