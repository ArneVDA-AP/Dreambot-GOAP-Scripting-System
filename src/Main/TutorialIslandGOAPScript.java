package Main; // Or your main package

import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;
import Core.GameIntegration.DreamBotWorldObserver; // Import the observer
import org.dreambot.api.methods.world.World;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.Category;
import org.dreambot.api.utilities.Logger; // Use DreamBot Logger
import org.dreambot.api.utilities.Sleep;
import Core.GOAP.*; // Import GOAP classes
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue; // Import Queue
import java.util.LinkedList; // Import LinkedList

@ScriptManifest(name = "GOAP Tutorial Island",
        description = "Automates Tutorial Island using GOAP", // Updated description
        author = "YourName & AI Co-Lead",
        version = 0.2, // Incremented version
        category = Category.UTILITY)
public class TutorialIslandGOAPScript extends AbstractScript {

    private WorldState worldState;
    private DreamBotWorldObserver worldObserver;
    private Planner planner;
    private ExecutionEngine executionEngine;
    private List<Action> availableActions;
    private Goal currentGoal;
    private Plan currentPlan;

    // State tracking for logging
    private String previousStageName = "";
    private String previousAreaName = "";
    private boolean previousDialogueState = false;
    private boolean previousAnimatingState = false;

    @Override
    public void onStart() {
        Logger.log("Starting GOAP Tutorial Island Script...");
        worldState = new WorldState();
        worldObserver = new DreamBotWorldObserver(this);
        planner = new Planner(); // Instantiate Planner
        executionEngine = new ExecutionEngine(); // Instantiate Engine
        availableActions = loadAvailableActions(); // Load all possible actions
        currentGoal = null; // Will be determined in onLoop
        currentPlan = null; // No plan initially

        previousStageName = "INITIALIZING";
        previousAreaName = "INITIALIZING";
        previousDialogueState = true;
        previousAnimatingState = true;
        Logger.log("Core GOAP components initialized.");
    }

    @Override
    public int onLoop() {
        if (worldObserver == null || worldState == null || planner == null || executionEngine == null || availableActions == null) {
            Logger.log("Core component(s) not initialized, stopping.");
            return -1;
        }

        // 1. Observe World State
        worldObserver.updateWorldState(worldState);
        logStateChanges(); // Use helper for logging

        // 2. Determine Current Goal (Crucial Step)
        determineCurrentGoal();
        if (currentGoal == null) {
            if (worldState.getBoolean(WorldStateKey.TUT_ISLAND_COMPLETED)) {
                Logger.log("Tutorial Island Completed! Stopping script.");
                return -1; // Stop script
            } else {
                Logger.log("Could not determine current goal, waiting...");
                return 1000; // Wait and retry
            }
        }

        // 3. Plan if needed
        // Need a plan if we don't have one OR if the engine signals a replan is needed
        boolean needsPlan = (currentPlan == null || currentPlan.isEmpty()) && !executionEngine.isExecuting();
        // We'll handle REPLAN_NEEDED status from the engine later

        if (needsPlan) {
            Logger.log("Needing new plan for goal: " + currentGoal.getName());
            currentPlan = planner.plan(worldState, currentGoal, availableActions);
            if (currentPlan != null && !currentPlan.isEmpty()) {
                executionEngine.setPlan(currentPlan);
                Logger.log("Planner generated new plan: " + currentPlan);
            } else {
                Logger.log("Planner failed to find a plan for goal: " + currentGoal.getName() + ". Waiting...");
                currentPlan = null; // Ensure plan is null if planner failed
                return 2000; // Wait before retrying planning
            }
        }

        // 4. Execute Step if engine has a plan/action
        if (executionEngine.isExecuting()) {
            ExecutionEngine.EngineStatus status = executionEngine.executeNextStep(worldState);

            // Handle REPLAN_NEEDED status
            if (status == ExecutionEngine.EngineStatus.REPLAN_NEEDED) {
                Logger.log("Execution Engine requested replan. Clearing current plan.");
                currentPlan = null; // Clear plan so planner runs next loop
                // Optional: Add logic here to handle persistent failures (e.g., blacklist action)
            } else if (status == ExecutionEngine.EngineStatus.PLAN_COMPLETE) {
                Logger.log("Execution Engine completed the plan for goal: " + currentGoal.getName());
                currentPlan = null; // Clear plan, goal determination will run next loop
                currentGoal = null; // Clear goal to force redetermination
            }
        } else if (currentPlan != null && currentPlan.isEmpty() && !executionEngine.isExecuting()) {
            // Plan became empty, but engine didn't report PLAN_COMPLETE? Treat as complete.
            Logger.log("Plan queue empty, assuming plan complete for goal: " + currentGoal.getName());
            currentPlan = null;
            currentGoal = null;
        }


        // 5. Return sleep time
        return 600; // Standard loop delay
    }

    /** Loads all defined Action instances */
    private List<Action> loadAvailableActions() {
        List<Action> actions = new ArrayList<>();
        // TODO: Instantiate ALL specific Action classes needed for Tutorial Island here
        // Example:
        // actions.add(new ActionTalkToNPC("Gielinor Guide"));
        // actions.add(new ActionOpenDoor(DOOR_ID, DOOR_TILE));
        // actions.add(new ActionClickWidget(SETTINGS_WIDGET_ID));
        // actions.add(new ActionWalkToArea(TUTORIAL_AREAS.get("Survival_Fishing_Area")));
        // actions.add(new ActionCutTree());
        // ... etc ...
        Logger.log("Loaded " + actions.size() + " available actions."); // Log count once implemented
        return actions;
    }

    /** Determines the current goal based on the world state (Tutorial Stage ID) */
    private void determineCurrentGoal() {
        int stageId = worldState.getInteger(WorldStateKey.TUT_STAGE_ID);
        String currentGoalName = (currentGoal != null) ? currentGoal.getName() : "None";
        Goal nextGoal = null;

        // --- Logic to map stage ID to the NEXT required goal ---
        // This requires defining Goal objects for each step completion
        // Example: If stage ID is 1 (Talked to Guide), the next goal is Open Settings (Stage 3)
        if (stageId < 3 && !currentGoalName.equals("Goal_OpenSettings")) {
            nextGoal = createGoalOpenSettings();
        } else if (stageId >= 3 && stageId < 7 && !currentGoalName.equals("Goal_TalkAfterSettings")) {
            nextGoal = createGoalTalkAfterSettings();
        } else if (stageId >= 7 && stageId < 10 && !currentGoalName.equals("Goal_OpenStartDoor")) {
            nextGoal = createGoalOpenStartDoor();
        } else if (stageId >= 10 && stageId < 20 && !currentGoalName.equals("Goal_TalkToSurvivalExpert")) {
            nextGoal = createGoalTalkToSurvivalExpert();
        }
        // ... Add logic for ALL tutorial stages based on the VarP values ...
        else if (stageId >= 670 && stageId < 1000 && !currentGoalName.equals("Goal_LeaveTutorial")) {
            nextGoal = createGoalLeaveTutorial();
        } else if (stageId == 1000) {
            nextGoal = null; // Tutorial Complete
            worldState.setBoolean(WorldStateKey.TUT_ISLAND_COMPLETED, true);
        }


        if (nextGoal != null && !nextGoal.equals(currentGoal)) {
            Logger.log("Setting new goal: " + nextGoal.getName() + " (Triggered by Stage ID: " + stageId + ")");
            currentGoal = nextGoal;
            currentPlan = null; // Force replan for new goal
            executionEngine.setPlan(null); // Clear engine's plan too
        } else if (nextGoal == null && currentGoal != null && stageId != 1000) {
            // Keep current goal if no new goal is determined and tutorial isn't done
            // This might happen if the varp value is between defined stages
        } else if (stageId == 1000) {
            currentGoal = null; // No goal once complete
        }
    }

    // --- Helper methods to create Goal objects ---
    // (Define these based on WorldStateKeys needed for each step)
    private Goal createGoalOpenSettings() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 3); // Target VarP value
        // Or conditions.put(WorldStateKey.UI_SETTINGS_OPEN, true); // If we add UI check
        return new Goal("Goal_OpenSettings", conditions);
    }
    private Goal createGoalTalkAfterSettings() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 7);
        return new Goal("Goal_TalkAfterSettings", conditions);
    }
    private Goal createGoalOpenStartDoor() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.S0_DOOR_OPEN, true);
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 10); // Also check varp?
        return new Goal("Goal_OpenStartDoor", conditions);
    }
    private Goal createGoalTalkToSurvivalExpert() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 20);
        return new Goal("Goal_TalkToSurvivalExpert", conditions);
    }
    // ... Create goal methods for ALL other tutorial steps ...
    private Goal createGoalLeaveTutorial() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_ISLAND_COMPLETED, true);
        return new Goal("Goal_LeaveTutorial", conditions);
    }


    /** Helper method for logging state changes */
    private void logStateChanges() {
        String currentStage = worldState.getString(WorldStateKey.TUT_STAGE_NAME);
        String currentArea = worldState.getString(WorldStateKey.LOC_CURRENT_AREA_NAME);
        boolean isDialogueOpen = worldState.getBoolean(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN);
        boolean isAnimating = worldState.getBoolean(WorldStateKey.INTERACT_IS_ANIMATING);
        int currentStageId = worldState.getInteger(WorldStateKey.TUT_STAGE_ID);

        boolean stageChanged = !previousStageName.equals(currentStage);
        boolean areaChanged = !previousAreaName.equals(currentArea);
        boolean dialogueChanged = previousDialogueState != isDialogueOpen;
        boolean animatingChanged = previousAnimatingState != isAnimating;

        if (stageChanged || areaChanged || dialogueChanged || animatingChanged) {
            Logger.log("===== State Change Detected =====");
            Logger.log(String.format("Stage: %s (ID: %d) | Area: %s | Dialogue: %s",
                    currentStage, currentStageId, currentArea, isDialogueOpen));
            Logger.log(String.format("Inv Space: %d | Walking: %s | Animating: %s",
                    worldState.getInteger(WorldStateKey.INV_SPACE),
                    worldState.getBoolean(WorldStateKey.LOC_IS_WALKING),
                    isAnimating));
            Logger.log("=================================");

            previousStageName = currentStage;
            previousAreaName = currentArea;
            previousDialogueState = isDialogueOpen;
            previousAnimatingState = isAnimating;
        }
    }

    @Override
    public void onExit() {
        Logger.log("Stopping GOAP Tutorial Island Script.");
    }
}