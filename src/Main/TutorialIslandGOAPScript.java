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
        category = Category.UTILITY) // Or Category.TUTORIAL
public class TutorialIslandGOAPScript extends AbstractScript {

    private WorldState worldState;
    private DreamBotWorldObserver worldObserver;

    @Override
    public void onStart() {
        Logger.log("Starting GOAP Tutorial Island Script (Observer Test)...");
        worldState = new WorldState();
        worldObserver = new DreamBotWorldObserver(this); // Pass script reference
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

        // 2. Log key values for verification
        // --- Choose several important keys to monitor ---
        Logger.log("-------------------- World State Update --------------------");
        Logger.log("Current Section: " + worldState.getString(WorldStateKey.TUT_CURRENT_SECTION));
        Logger.log("Current Area: " + worldState.getString(WorldStateKey.LOC_CURRENT_AREA));
        Logger.log("Dialogue Open? " + worldState.getBoolean(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN));
        Logger.log("Inventory Space: " + worldState.getInteger(WorldStateKey.INV_SPACE));
        Logger.log("Has Axe? " + worldState.getBoolean(WorldStateKey.S1_HAS_AXE)); // Example item
        Logger.log("Is Walking? " + worldState.getBoolean(WorldStateKey.LOC_IS_WALKING));
        Logger.log("----------------------------------------------------------");


        // 3. Return sleep time
        // No planning or execution yet, just observe
        return 1000; // Update state every second
    }

    @Override
    public void onExit() {
        Logger.log("Stopping GOAP Tutorial Island Script.");
    }
}