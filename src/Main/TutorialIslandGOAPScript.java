package Main; // Or your main package

import Core.Actions.*;
import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;
import Core.GameIntegration.DreamBotWorldObserver; // Import the observer
import org.dreambot.api.methods.magic.Normal;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.tabs.Tab;
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

import static Core.GameIntegration.DreamBotWorldObserver.TUTORIAL_AREAS;

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

    // Inside loadAvailableActions() in TutorialIslandGOAPScript.java
    private List<Action> loadAvailableActions() {
        List<Action> actions = new ArrayList<>();
        Logger.log("Loading available actions..."); // Add log

        // --- Section 0 Actions ---
        Area guideArea = TUTORIAL_AREAS.get("Guide_Start_Area");
        Tile startingDoorTile = new Tile(3097, 3107, 0); // VERIFY THIS TILE
        actions.add(new ActionTalkToNPC("Gielinor Guide", "S0_Start_OpenSettings", guideArea));
        actions.add(new ActionContinueDialogue("Gielinor Guide"));
        if (startingDoorTile != null) {
            Map<WorldStateKey, Object> openDoorEffects = new HashMap<>();
            openDoorEffects.put(WorldStateKey.S0_DOOR_OPEN, true);
            openDoorEffects.put(WorldStateKey.TUT_STAGE_NAME, "S1_Survival_TalkToExpert"); // Anticipate next stage
            actions.add(new ActionOpenDoor("Door", startingDoorTile, WorldStateKey.S0_DOOR_OPEN, "S1_Survival_TalkToExpert")); // Use ActionOpenDoor specifically
        } else { Logger.log("ERROR: Starting Door Tile not set!"); }

        // --- Section 1 Actions ---
        Area survivalArea = TUTORIAL_AREAS.get("Survival_Fishing_Area"); // General survival area
        Area cookingArea = TUTORIAL_AREAS.get("Survival_Cooking_Area"); // Area with fire
        Area woodcuttingArea = null; // Define specific woodcutting area if needed
        Tile fireTile = new Tile(3098, 3102, 0); // Example fire tile - VERIFY
        int woodcuttingAnim = 879; // VERIFY
        int firemakingAnim = 733; // VERIFY
        int fishingAnim = 621; // VERIFY
        int cookingAnim = 897; // VERIFY

        actions.add(new ActionWalkToTile(survivalArea.getCenter(), 3, "Survival_Fishing_Area")); // Walk to general area
        actions.add(new ActionTalkToNPC("Survival Expert", "S1_Survival_OpenInventory", survivalArea));
        actions.add(new ActionContinueDialogue("Survival Expert"));
        actions.add(new ActionOpenTab(Tab.INVENTORY, WorldStateKey.UI_INVENTORY_OPEN)); // For ID 30
        actions.add(new ActionOpenTab(Tab.SKILLS, WorldStateKey.UI_SKILLS_TAB_OPEN)); // For ID 50
        actions.add(new ActionCutTree(woodcuttingArea)); // Add area if defined
        actions.add(new ActionMakeFire());
        actions.add(new ActionFishShrimp());
        actions.add(new ActionCookShrimp()); // Uses fire on player's tile

        // --- Section 2 Actions ---
        Area chefArea = TUTORIAL_AREAS.get("Cooking_Range_Area"); // VERIFY AREA
        Tile chefDoorExitTile = new Tile(3074, 3081, 0); // VERIFY TILE
        int rangeId = -1; // Find Range ID if needed, otherwise use name
        actions.add(new ActionWalkToTile(chefArea.getCenter(), 2, "Cooking_Range_Area"));
        actions.add(new ActionTalkToNPC("Master Chef", "S2_Cooking_MakeDough", chefArea));
        actions.add(new ActionContinueDialogue("Master Chef"));
        actions.add(new ActionUseItemOnItem("Bucket of water", WorldStateKey.S2_HAS_BUCKET_OF_WATER, "Pot of flour", WorldStateKey.S2_HAS_FLOUR, "Bread dough", WorldStateKey.S2_HAS_DOUGH));
        Map<WorldStateKey, Object> bakeEffects = new HashMap<>();
        bakeEffects.put(WorldStateKey.S2_HAS_DOUGH, false);
        bakeEffects.put(WorldStateKey.S2_HAS_BREAD, true);
        actions.add(new ActionUseItemOnObject("Bread dough", WorldStateKey.S2_HAS_DOUGH, "Range", "Cook", "Bread", WorldStateKey.S2_HAS_BREAD, cookingAnim)); // Use Name "Range"
        actions.add(new ActionOpenTab(Tab.MUSIC, WorldStateKey.UI_MUSIC_TAB_OPEN)); // For ID 145
        Map<WorldStateKey, Object> openChefDoorEffects = new HashMap<>();
        openChefDoorEffects.put(WorldStateKey.S2_CHEF_DOOR_EXIT_OPEN, true);
        openChefDoorEffects.put(WorldStateKey.TUT_STAGE_NAME, "S3_Quest_EnterQuestHouse");
        actions.add(new ActionClickObject("Door", chefDoorExitTile, "Open", openChefDoorEffects, -1)); // Open chef exit door

        // --- Section 3 Actions ---
        Area questGuideArea = TUTORIAL_AREAS.get("Quest_Guide_Area");
        Area miningArea = TUTORIAL_AREAS.get("Mining_Area");
        Area smithingArea = TUTORIAL_AREAS.get("Mining_Smithing_Area");
        Tile ladderTile = new Tile(3088, 9506, 0); // VERIFY LADDER TILE
        Tile furnaceTile = new Tile(3081, 9498, 0); // VERIFY FURNACE TILE
        Tile anvilTile = new Tile(3081, 9499, 0); // VERIFY ANVIL TILE
        int furnaceId = -1; // Find Furnace ID
        int anvilId = 2097; // Common Anvil ID - VERIFY
        int miningAnim = 625; // VERIFY
        int smithingAnim = 898; // Common smithing anim - VERIFY

        actions.add(new ActionWalkToTile(questGuideArea.getCenter(), 2, "Quest_Guide_Area"));
        actions.add(new ActionTalkToNPC("Quest Guide", "S3_Quest_OpenQuestTab", questGuideArea));
        actions.add(new ActionContinueDialogue("Quest Guide"));
        actions.add(new ActionOpenTab(Tab.QUEST, WorldStateKey.UI_QUEST_TAB_OPEN)); // For ID 230
        Map<WorldStateKey, Object> climbLadderEffects = new HashMap<>();
        climbLadderEffects.put(WorldStateKey.LOC_CURRENT_AREA_NAME, "Mining_Area"); // Now in mine
        climbLadderEffects.put(WorldStateKey.TUT_STAGE_NAME, "S3_Mining_TalkToInstructor");
        actions.add(new ActionClickObject("Ladder", ladderTile, "Climb-down", climbLadderEffects, -1)); // Climb ladder
        actions.add(new ActionWalkToTile(miningArea.getCenter(), 3, "Mining_Area")); // Walk within mine
        actions.add(new ActionTalkToNPC("Mining Instructor", "S3_Mining_MineTin", miningArea)); // Includes prospecting dialogue implicitly
        actions.add(new ActionContinueDialogue("Mining Instructor"));
        actions.add(new ActionMineOre("Tin rocks", "Tin ore", WorldStateKey.S3_HAS_TIN_ORE, WorldStateKey.S3_HAS_PICKAXE, miningArea));
        actions.add(new ActionMineOre("Copper rocks", "Copper ore", WorldStateKey.S3_HAS_COPPER_ORE, WorldStateKey.S3_HAS_PICKAXE, miningArea));
        Map<WorldStateKey, Object> smeltEffects = new HashMap<>();
        smeltEffects.put(WorldStateKey.S3_HAS_TIN_ORE, false);
        smeltEffects.put(WorldStateKey.S3_HAS_COPPER_ORE, false);
        smeltEffects.put(WorldStateKey.S3_HAS_BRONZE_BAR, true);
        // Use ore on furnace - need to decide which ore triggers the action, or make two actions
        actions.add(new ActionUseItemOnObject("Tin ore", WorldStateKey.S3_HAS_TIN_ORE, "Furnace", "Use", "Bronze bar", WorldStateKey.S3_HAS_BRONZE_BAR, -1)); // Use Tin on Furnace
        // OR actions.add(new ActionUseItemOnObject("Copper ore", WorldStateKey.S3_HAS_COPPER_ORE, "Furnace", "Use", "Bronze bar", WorldStateKey.S3_HAS_BRONZE_BAR, -1)); // Use Copper on Furnace
        Map<WorldStateKey, Object> clickAnvilEffects = new HashMap<>(); // Clicking anvil might just open interface
        // clickAnvilEffects.put(WorldStateKey.UI_SMITHING_INTERFACE_OPEN, true); // Need this key/check
        // Action for Smithing Dagger (S3)
        actions.add(new ActionSmithItem(
                "Bronze dagger", // Item to make
                "Bronze bar",    // Bar needed
                WorldStateKey.S3_HAS_BRONZE_BAR,
                WorldStateKey.S3_HAS_HAMMER,
                WorldStateKey.S3_HAS_DAGGER,
                1 // Make 1 for tutorial
        ));

        // Action for clicking Equip Stats button (S4) - ID path needs verification!
        int[] equipStatsButtonPath = {15, 10}; // EXAMPLE - FIND ACTUAL PATH
        Map<WorldStateKey, Object> viewStatsEffects = new HashMap<>();
        viewStatsEffects.put(WorldStateKey.UI_EQUIPMENT_STATS_OPEN, true);
        actions.add(new ActionClickWidget(equipStatsButtonPath, "ViewEquipmentStats", viewStatsEffects));

        // Action for clicking Dagger in Smithing Interface (S3) - ID path needs verification!
        int[] smithDaggerButtonPath = {15, 10}; // EXAMPLE - FIND ACTUAL PATH
        // Effects for clicking smithing item might be just starting animation
        Map<WorldStateKey, Object> clickSmithDaggerEffects = new HashMap<>();
        // clickSmithDaggerEffects.put(WorldStateKey.INTERACT_IS_ANIMATING, true); // Maybe? Or handled by UseItemOnObject
        actions.add(new ActionClickWidget(smithDaggerButtonPath, "ClickSmithDagger", clickSmithDaggerEffects));

        // Action for Equipping Dagger (S4)
        actions.add(new ActionEquipItem("Bronze dagger", WorldStateKey.S3_HAS_DAGGER, WorldStateKey.S4_DAGGER_EQUIPPED));

        // Action for Equipping Sword (S4) - Need key S4_HAS_SWORD
        // actions.add(new ActionEquipItem("Bronze sword", WorldStateKey.S4_HAS_SWORD, WorldStateKey.S4_SWORD_EQUIPPED)); // Need S4_SWORD_EQUIPPED key

        // Action for Equipping Shield (S4) - Need key S4_HAS_SHIELD
        // actions.add(new ActionEquipItem("Wooden shield", WorldStateKey.S4_HAS_SHIELD, WorldStateKey.S4_SHIELD_EQUIPPED)); // Need S4_SHIELD_EQUIPPED key

        // Action for Equipping Bow (S4)
        actions.add(new ActionEquipItem("Shortbow", WorldStateKey.S4_HAS_BOW, WorldStateKey.S4_BOW_EQUIPPED)); // Need S4_BOW_EQUIPPED key

        // Action for Equipping Arrows (S4) - Arrows are stackable, might need different handling or key
        actions.add(new ActionEquipItem("Bronze arrow", WorldStateKey.S4_HAS_ARROWS, WorldStateKey.S4_ARROWS_EQUIPPED)); // Need S4_ARROWS_EQUIPPED key


        // Action for attacking Rat with Melee (S4)
        Area combatArea = TUTORIAL_AREAS.get("Combat_Area"); // Use correct name
        Map<WorldStateKey, Object> killRatMeleeEffects = new HashMap<>();
        killRatMeleeEffects.put(WorldStateKey.S4_KILLED_RAT_MELEE, true); // Set completion flag
        killRatMeleeEffects.put(WorldStateKey.COMBAT_IS_IN_COMBAT, false); // Anticipate combat ends
        actions.add(new ActionAttackNPC("Giant rat", combatArea, killRatMeleeEffects));

        // Action for attacking Rat with Ranged (S4)
        Map<WorldStateKey, Object> killRatRangedEffects = new HashMap<>();
        killRatRangedEffects.put(WorldStateKey.S4_KILLED_RAT_RANGED, true);
        killRatRangedEffects.put(WorldStateKey.COMBAT_IS_IN_COMBAT, false);
        actions.add(new ActionAttackNPC("Giant rat", combatArea, killRatRangedEffects));

        // Action for Casting Wind Strike on Chicken (S7)
        Area magicArea = TUTORIAL_AREAS.get("Magic_Area"); // Use correct name
        Map<WorldStateKey, Object> killChickenEffects = new HashMap<>();
        killChickenEffects.put(WorldStateKey.S7_KILLED_CHICKEN, true);
        killChickenEffects.put(WorldStateKey.COMBAT_IS_IN_COMBAT, false);
        // Add rune consumption effects if desired for planner
        // killChickenEffects.put(WorldStateKey.S7_HAS_AIR_RUNE, false); // Example
        // killChickenEffects.put(WorldStateKey.S7_HAS_MIND_RUNE, false); // Example

        actions.add(new ActionCastSpellOnNPC(
                Normal.WIND_STRIKE, // The spell enum
                "Chicken",          // Target NPC name
                magicArea,
                killChickenEffects
        ));
        // --- TODO: Add Actions for Sections 4, 5, 6, 7, 8 ---
        // Examples:
        // ActionOpenTab(Tab.EQUIPMENT, ...)
        // ActionClickWidget(...) // For equip stats button
        // ActionEquipItem(...) // Need a dedicated action for equipping
        // ActionClickObject(...) // For rat gate
        // ActionAttackNPC(...) // Need a dedicated action for combat
        // ActionWalkToTile(...)
        // ActionClickObject("Bank booth", ...)
        // ActionClickObject("Poll booth", ...)
        // ActionOpenDoor(...) // Financial advisor doors
        // ActionTalkToNPC("Financial Advisor", ...)
        // ActionOpenTab(Tab.PRAYER, ...)
        // ActionTalkToNPC("Brother Brace", ...)
        // ActionOpenTab(Tab.FRIENDS, ...)
        // ActionOpenDoor(...) // Church door
        // ActionTalkToNPC("Magic Instructor", ...)
        // ActionOpenTab(Tab.MAGIC, ...)
        // ActionCastSpellOnNPC(...) // Need dedicated action
        // ActionTalkToNPC(...) // Final dialogue

        Logger.log("Loaded " + actions.size() + " available actions.");
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