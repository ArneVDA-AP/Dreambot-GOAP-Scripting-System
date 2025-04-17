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
        author = "FlaggedIP & AI Co-Lead",
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

    // In TutorialIslandGOAPScript.java

    // Make TUTORIAL_AREAS accessible if defined in Observer, or redefine here/in Constants
    // For simplicity, let's assume it's accessible via a static getter for now
    // private static final Map<String, Area> TUTORIAL_AREAS = DreamBotWorldObserver.getTutorialAreas(); // Example access

// Assume TUTORIAL_AREAS map is accessible, e.g., via a static getter or defined here
    // private static final Map<String, Area> TUTORIAL_AREAS = DreamBotWorldObserver.getTutorialAreas(); // Example access

    /** Loads all defined Action instances */
    private List<Action> loadAvailableActions() {
        List<Action> actions = new ArrayList<>();
        Logger.log("Loading available actions...");

        // --- Define Tiles (VERIFY ALL OF THESE!) ---
        Tile guideTile = new Tile(3094, 3107, 0);
        Tile startingDoorTile = new Tile(3097, 3107, 0);
        Tile survivalExpertTile = new Tile(3098, 3106, 0);
        Tile treeTile = new Tile(3101, 3098, 0);
        Tile firemakingTile = new Tile(3099, 3102, 0);
        Tile fishingSpotTile = new Tile(3103, 3093, 0);
        Tile gate1Tile = new Tile(3108, 3096, 0);
        Tile gate2Tile = new Tile(3111, 3090, 0);
        Tile chefTile = new Tile(3076, 3084, 0);
        Tile rangeTile = new Tile(3079, 3084, 0);
        Tile chefDoorExitTile = new Tile(3074, 3081, 0);
        Tile questGuideTile = new Tile(3076, 3079, 0);
        Tile ladderTile = new Tile(3088, 9506, 0); // Moved declaration up
        Tile miningInstructorTile = new Tile(3080, 9505, 0);
        Tile tinRockTile = new Tile(3077, 9502, 0);
        Tile copperRockTile = new Tile(3083, 9502, 0);
        Tile furnaceTile = new Tile(3081, 9498, 0);
        Tile anvilTile = new Tile(3081, 9499, 0);
        Tile mineGateTile = new Tile(3092, 9503, 0); // Moved declaration up
        Tile combatInstructorTile = new Tile(3095, 9503, 0);
        Tile ratGateTile = new Tile(3097, 9509, 0); // Moved declaration up
        Tile combatLadderTile = new Tile(3111, 9518, 0);
        Tile bankBoothTile = new Tile(3120, 3123, 0);
        Tile pollBoothTile = new Tile(3125, 3124, 0);
        Tile finAdvisorDoorInTile = new Tile(3125, 3126, 0);
        Tile finAdvisorTile = new Tile(3126, 3128, 0);
        Tile finAdvisorDoorOutTile = new Tile(3128, 3125, 0);
        Tile brotherBraceTile = new Tile(3124, 3118, 0);
        Tile churchDoorOutTile = new Tile(3121, 3116, 0);
        Tile magicInstructorTile = new Tile(3121, 3108, 0);
        Tile chickenTile = new Tile(3139, 3106, 0);

        // --- Define Object/Animation IDs (VERIFY ALL OF THESE!) ---
        int startingDoorId = 9398;
        int treeId = 1276;
        int firemakingAnim = 733;
        int fishingSpotId = 1518;
        int fishingAnim = 621;
        int cookingAnim = 897;
        int gateId = 9470; // Assuming same ID for multiple gates - VERIFY
        int rangeId = 114;
        int ladderDownId = 9742;
        int tinRockId = 10080;
        int copperRockId = 10079;
        int miningAnim = 625;
        int furnaceId = 24009;
        int anvilId = 2097;
        int smithingAnim = 898;
        int mineGateId = 9710; // Example ID - VERIFY & Moved declaration up
        int combatInstructorId = -1; // Find ID if needed
        int ratGateId = 9719; // Example ID - VERIFY & Moved declaration up
        int ratId = 2856;
        int combatAnimMelee = 422;
        int combatAnimRanged = 426;
        int ladderUpId = 9743; // Example ID - VERIFY
        int bankBoothId = 10355;
        int pollBoothId = 26815;
        int finDoorInId = 9721; // Example ID - VERIFY
        int finAdvisorId = -1; // Find ID if needed
        int finDoorOutId = 9722; // Example ID - VERIFY
        int brotherBraceId = -1; // Find ID if needed
        int churchDoorOutId = 9723; // Example ID - VERIFY
        int magicInstructorId = 3309; // Example ID - VERIFY
        int chickenId = 41;
        int magicCastAnim = 711;

        // --- Define Area Name Strings (Match keys in TUTORIAL_AREAS map) ---
        String guideAreaName = "Guide_Start_Area";
        String survivalAreaName = "Survival_Fishing_Area"; // Use a general name or specific if needed
        String cookingAreaName = "Survival_Cooking_Area";
        String chefAreaName = "Cooking_Range_Area";
        String questGuideAreaName = "Quest_Guide_Area";
        String miningAreaName = "Mining_Area";
        String smithingAreaName = "Mining_Smithing_Area";
        String combatAreaName = "Combat_Area";
        String bankAreaName = "Bank_Area";
        String financialAreaName = "Financial_Advisor_Area";
        String prayerAreaName = "Prayer_Area";
        String magicAreaName = "Magic_Area";


        // --- Instantiate Actions ---

        // S0: Start
        actions.add(new ActionTalkToNPC("Gielinor Guide", "S0_Start_OpenSettings", TUTORIAL_AREAS.get(guideAreaName)));
        actions.add(new ActionContinueDialogue("Gielinor Guide"));
        // TODO: Add ActionClickWidget for Settings Tab/Button if needed for stage 3
        Map<WorldStateKey, Object> openDoorEffects = createEffectsMap(WorldStateKey.S0_DOOR_OPEN, true, WorldStateKey.TUT_STAGE_NAME, "S1_Survival_TalkToExpert");
        actions.add(new ActionClickObject(startingDoorId, startingDoorTile, "Open", openDoorEffects, -1));

        // S1: Survival
        actions.add(new ActionWalkToTile(survivalExpertTile, 2, survivalAreaName));
        actions.add(new ActionTalkToNPC("Survival Expert", "S1_Survival_OpenInventory", TUTORIAL_AREAS.get(survivalAreaName)));
        actions.add(new ActionContinueDialogue("Survival Expert"));
        actions.add(new ActionOpenTab(Tab.INVENTORY, WorldStateKey.UI_INVENTORY_OPEN));
        actions.add(new ActionOpenTab(Tab.SKILLS, WorldStateKey.UI_SKILLS_TAB_OPEN));
        actions.add(new ActionCutTree(TUTORIAL_AREAS.get(cookingAreaName))); // Cut tree near fire area
        actions.add(new ActionMakeFire());
        actions.add(new ActionFishShrimp());
        actions.add(new ActionCookShrimp());
        Map<WorldStateKey, Object> openGate1Effects = createEffectsMap(WorldStateKey.TUT_STAGE_NAME, "S2_Cooking_EnterChefHouse");
        actions.add(new ActionClickObject(gateId, gate1Tile, "Open", openGate1Effects, -1)); // Open Gate 1

        // S2: Cooking
        Map<WorldStateKey, Object> openGate2Effects = createEffectsMap(WorldStateKey.TUT_STAGE_NAME, "S2_Cooking_TalkToChef");
        actions.add(new ActionClickObject(gateId, gate2Tile, "Open", openGate2Effects, -1)); // Open Gate 2
        actions.add(new ActionWalkToTile(chefTile, 2, chefAreaName));
        actions.add(new ActionTalkToNPC("Master Chef", "S2_Cooking_MakeDough", TUTORIAL_AREAS.get(chefAreaName)));
        actions.add(new ActionContinueDialogue("Master Chef"));
        actions.add(new ActionUseItemOnItem("Bucket of water", WorldStateKey.S2_HAS_BUCKET_OF_WATER, "Pot of flour", WorldStateKey.S2_HAS_FLOUR, "Bread dough", WorldStateKey.S2_HAS_DOUGH));
        Map<WorldStateKey, Object> bakeEffects = createEffectsMap(WorldStateKey.S2_HAS_DOUGH, false, WorldStateKey.S2_HAS_BREAD, true, WorldStateKey.TUT_STAGE_NAME, "S2_Cooking_ExitChefHouse");
        actions.add(new ActionUseItemOnObject("Bread dough", WorldStateKey.S2_HAS_DOUGH, rangeId, "Cook", "Bread", WorldStateKey.S2_HAS_BREAD, cookingAnim));
        actions.add(new ActionOpenTab(Tab.MUSIC, WorldStateKey.UI_MUSIC_TAB_OPEN));
        Map<WorldStateKey, Object> openChefDoorEffects = createEffectsMap(WorldStateKey.S2_CHEF_DOOR_EXIT_OPEN, true, WorldStateKey.TUT_STAGE_NAME, "S3_Quest_EnterQuestHouse");
        actions.add(new ActionClickObject("Door", chefDoorExitTile, "Open", openChefDoorEffects, -1));

        // S3: Quest/Mining/Smithing
        actions.add(new ActionWalkToTile(questGuideTile, 2, questGuideAreaName));
        actions.add(new ActionTalkToNPC("Quest Guide", "S3_Quest_OpenQuestTab", TUTORIAL_AREAS.get(questGuideAreaName)));
        actions.add(new ActionContinueDialogue("Quest Guide"));
        actions.add(new ActionOpenTab(Tab.QUEST, WorldStateKey.UI_QUEST_TAB_OPEN));
        Map<WorldStateKey, Object> climbLadderEffects = createEffectsMap(WorldStateKey.LOC_CURRENT_AREA_NAME, miningAreaName, WorldStateKey.TUT_STAGE_NAME, "S3_Mining_TalkToInstructor");
        actions.add(new ActionClickObject(ladderDownId, ladderTile, "Climb-down", climbLadderEffects, -1));
        actions.add(new ActionWalkToTile(miningInstructorTile, 2, miningAreaName));
        actions.add(new ActionTalkToNPC("Mining Instructor", "S3_Mining_MineTin", TUTORIAL_AREAS.get(miningAreaName)));
        actions.add(new ActionContinueDialogue("Mining Instructor"));
        // *** CORRECTED ActionMineOre Instantiation ***
        actions.add(new ActionMineOre(new int[]{tinRockId}, "Tin ore", WorldStateKey.S3_HAS_TIN_ORE, WorldStateKey.S3_HAS_PICKAXE, TUTORIAL_AREAS.get(miningAreaName)));
        actions.add(new ActionMineOre(new int[]{copperRockId}, "Copper ore", WorldStateKey.S3_HAS_COPPER_ORE, WorldStateKey.S3_HAS_PICKAXE, TUTORIAL_AREAS.get(miningAreaName)));
        Map<WorldStateKey, Object> smeltEffects = createEffectsMap(WorldStateKey.S3_HAS_TIN_ORE, false, WorldStateKey.S3_HAS_COPPER_ORE, false, WorldStateKey.S3_HAS_BRONZE_BAR, true, WorldStateKey.TUT_STAGE_NAME, "S3_Smithing_TalkAfterSmelting");
        actions.add(new ActionUseItemOnObject("Tin ore", WorldStateKey.S3_HAS_TIN_ORE, furnaceId, "Use", "Bronze bar", WorldStateKey.S3_HAS_BRONZE_BAR, -1));
        Map<WorldStateKey, Object> clickAnvilEffects = createEffectsMap(WorldStateKey.TUT_STAGE_NAME, "S3_Smithing_SmithDagger");
        actions.add(new ActionClickObject(anvilId, anvilTile, "Smith", clickAnvilEffects, -1));
        actions.add(new ActionSmithItem("Bronze dagger", "Bronze bar", WorldStateKey.S3_HAS_BRONZE_BAR, WorldStateKey.S3_HAS_HAMMER, WorldStateKey.S3_HAS_DAGGER, 1));
        Map<WorldStateKey, Object> openMineGateEffects = createEffectsMap(WorldStateKey.S3_MINE_GATE_OPEN, true, WorldStateKey.TUT_STAGE_NAME, "S4_Combat_TalkToInstructor");
        actions.add(new ActionClickObject(mineGateId, mineGateTile, "Open", openMineGateEffects, -1)); // Use corrected variable name

        // S4: Combat
        actions.add(new ActionWalkToTile(combatInstructorTile, 2, combatAreaName));
        actions.add(new ActionTalkToNPC("Combat Instructor", "S4_Combat_OpenEquipTab", TUTORIAL_AREAS.get(combatAreaName)));
        actions.add(new ActionContinueDialogue("Combat Instructor"));
        actions.add(new ActionOpenTab(Tab.EQUIPMENT, WorldStateKey.UI_EQUIPMENT_TAB_OPEN));
        // TODO: Add ActionClickWidget for Equip Stats Button (ID 84) - Need path
        actions.add(new ActionEquipItem("Bronze dagger", WorldStateKey.S3_HAS_DAGGER, WorldStateKey.S4_DAGGER_EQUIPPED));
        actions.add(new ActionTalkToNPC("Combat Instructor", "S4_Combat_EquipSwordShield", TUTORIAL_AREAS.get(combatAreaName))); // Talk again after dagger equip
        // TODO: Add ActionEquipItem for Sword and Shield (Need WorldStateKeys & item names confirmed)
        actions.add(new ActionOpenTab(Tab.COMBAT, WorldStateKey.UI_COMBAT_OPTIONS_OPEN));
        Map<WorldStateKey, Object> openRatGateEffects = createEffectsMap(WorldStateKey.S4_RAT_GATE_OPEN, true, WorldStateKey.TUT_STAGE_NAME, "S4_Combat_AttackRatMelee");
        actions.add(new ActionClickObject(ratGateId, ratGateTile, "Open", openRatGateEffects, -1)); // Use corrected variable name
        Map<WorldStateKey, Object> killRatMeleeEffects = createEffectsMap(WorldStateKey.S4_KILLED_RAT_MELEE, true, WorldStateKey.TUT_STAGE_NAME, "S4_Combat_TalkAfterMelee");
        actions.add(new ActionAttackNPC("Giant rat", TUTORIAL_AREAS.get(combatAreaName), killRatMeleeEffects));
        actions.add(new ActionTalkToNPC("Combat Instructor", "S4_Combat_EquipRangedAttackRat", TUTORIAL_AREAS.get(combatAreaName))); // Talk again after melee kill
        // TODO: Add ActionEquipItem for Bow and Arrows (Need WorldStateKeys)
        Map<WorldStateKey, Object> killRatRangedEffects = createEffectsMap(WorldStateKey.S4_KILLED_RAT_RANGED, true, WorldStateKey.TUT_STAGE_NAME, "S4_Combat_ExitArea");
        actions.add(new ActionAttackNPC("Giant rat", TUTORIAL_AREAS.get(combatAreaName), killRatRangedEffects));
        Map<WorldStateKey, Object> climbCombatLadderEffects = createEffectsMap(WorldStateKey.LOC_CURRENT_AREA_NAME, bankAreaName, WorldStateKey.TUT_STAGE_NAME, "S5_Financial_OpenBank");
        actions.add(new ActionClickObject(ladderUpId, combatLadderTile, "Climb-up", climbCombatLadderEffects, -1));

        // S5: Financial
        actions.add(new ActionWalkToTile(bankBoothTile, 1, bankAreaName));
        // TODO: Add ActionClickObject for Bank Booth (ID 10355?) - Need effects (UI_BANK_OPEN=true)
        // TODO: Add ActionClickObject for Poll Booth (ID 26815?) - Need effects (UI_POLL_BOOTH_OPEN=true)
        // TODO: Add ActionClickObject for Financial Door In (ID 450?) - Need effects
        actions.add(new ActionWalkToTile(finAdvisorTile, 1, financialAreaName));
        actions.add(new ActionTalkToNPC("Financial Advisor", "S5_Financial_OpenAccountTab", TUTORIAL_AREAS.get(financialAreaName)));
        actions.add(new ActionContinueDialogue("Financial Advisor"));
        // TODO: Add ActionOpenTab for Account Management (Widget 109?) - Need key
        actions.add(new ActionTalkToNPC("Financial Advisor", "S5_Financial_ExitRoom", TUTORIAL_AREAS.get(financialAreaName))); // Talk again after tab
        // TODO: Add ActionClickObject for Financial Door Out (ID 470?) - Need effects

        // S6: Prayer
        actions.add(new ActionWalkToTile(brotherBraceTile, 1, prayerAreaName));
        actions.add(new ActionTalkToNPC("Brother Brace", "S6_Prayer_OpenPrayerTab", TUTORIAL_AREAS.get(prayerAreaName)));
        actions.add(new ActionContinueDialogue("Brother Brace"));
        actions.add(new ActionOpenTab(Tab.PRAYER, WorldStateKey.UI_PRAYER_TAB_OPEN));
        actions.add(new ActionTalkToNPC("Brother Brace", "S6_Prayer_OpenFriendsList", TUTORIAL_AREAS.get(prayerAreaName))); // Talk again after tab
        actions.add(new ActionOpenTab(Tab.FRIENDS, WorldStateKey.UI_FRIENDS_TAB_OPEN)); // Covers friends/ignore
        actions.add(new ActionTalkToNPC("Brother Brace", "S6_Prayer_ExitChapel", TUTORIAL_AREAS.get(prayerAreaName))); // Talk again after lists
        // TODO: Add ActionClickObject for Church Door Out (ID 530?) - Need effects

        // S7: Magic
        actions.add(new ActionWalkToTile(magicInstructorTile, 1, magicAreaName));
        actions.add(new ActionTalkToNPC("Magic Instructor", "S7_Magic_OpenSpellbook", TUTORIAL_AREAS.get(magicAreaName)));
        actions.add(new ActionContinueDialogue("Magic Instructor"));
        actions.add(new ActionOpenTab(Tab.MAGIC, WorldStateKey.UI_MAGIC_SPELLBOOK_OPEN));
        actions.add(new ActionTalkToNPC("Magic Instructor", "S7_Magic_KillChicken", TUTORIAL_AREAS.get(magicAreaName))); // Talk again after spellbook
        Map<WorldStateKey, Object> killChickenEffects = createEffectsMap(WorldStateKey.S7_KILLED_CHICKEN, true, WorldStateKey.TUT_STAGE_NAME, "S7_Magic_ReadyToLeave");
        actions.add(new ActionCastSpellOnNPC(Normal.WIND_STRIKE, "Chicken", TUTORIAL_AREAS.get(magicAreaName), killChickenEffects));
        actions.add(new ActionTalkToNPC("Magic Instructor", "S8_Mainland", TUTORIAL_AREAS.get(magicAreaName))); // Talk again after kill (Ready to leave)
        actions.add(new ActionContinueDialogue("Magic Instructor")); // Final confirmation dialogue

        Logger.log("Loaded " + actions.size() + " available actions.");
        return actions;
    }


    // Helper to quickly create effects maps
    private Map<WorldStateKey, Object> createEffectsMap(Object... keyValues) {
        Map<WorldStateKey, Object> map = new HashMap<>();
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Must provide key-value pairs for effects map.");
        }
        for (int i = 0; i < keyValues.length; i += 2) {
            if (!(keyValues[i] instanceof WorldStateKey)) {
                throw new IllegalArgumentException("Effect map keys must be WorldStateKey enums.");
            }
            map.put((WorldStateKey) keyValues[i], keyValues[i+1]);
        }
        // Always anticipate animation stopping unless explicitly set otherwise
        if (!map.containsKey(WorldStateKey.INTERACT_IS_ANIMATING)) {
            map.put(WorldStateKey.INTERACT_IS_ANIMATING, false);
        }
        return map;
    }
    /** Determines the current goal based on the world state (Tutorial Stage ID) */
    /** Determines the current goal based on the world state (Tutorial Stage ID) */
    private void determineCurrentGoal() {
        int stageId = worldState.getInteger(WorldStateKey.TUT_STAGE_ID);
        String currentGoalName = (currentGoal != null) ? currentGoal.getName() : "None";
        Goal nextGoal = null;

        // --- Logic to map stage ID to the NEXT required goal ---
        // Based on the VarPlayer values list provided previously
        // The condition checks the *current* stage ID to determine the *next* goal.

        if (stageId < 1) nextGoal = createGoalTalkToGuide(); // Start -> Talk to Guide
        else if (stageId == 1) nextGoal = createGoalTalkToGuidePostName(); // Post Name -> Open Settings? (Needs confirmation)
        else if (stageId == 2) nextGoal = createGoalOpenSettings(); // Talked -> Open Settings
        else if (stageId >= 3 && stageId < 7) nextGoal = createGoalTalkAfterSettings(); // Settings Open -> Talk Again
        else if (stageId >= 7 && stageId < 10) nextGoal = createGoalOpenStartDoor(); // Talked -> Open Door
        else if (stageId >= 10 && stageId < 20) nextGoal = createGoalTalkToSurvivalExpert(); // Door Open -> Talk to Expert
        else if (stageId >= 20 && stageId < 30) nextGoal = createGoalOpenInventory(); // Talked -> Open Inventory
        else if (stageId >= 30 && stageId < 40) nextGoal = createGoalFishShrimp(); // Inventory Open -> Fish
        else if (stageId >= 40 && stageId < 50) nextGoal = createGoalOpenSkillsTab(); // Fished -> Open Skills
        else if (stageId >= 50 && stageId < 60) nextGoal = createGoalTalkAfterSkills(); // Skills Open -> Talk Again
        else if (stageId >= 60 && stageId < 70) nextGoal = createGoalCutTree(); // Talked -> Cut Tree
        else if (stageId >= 70 && stageId < 80) nextGoal = createGoalMakeFire(); // Cut Tree -> Make Fire
        else if (stageId >= 80 && stageId < 90) nextGoal = createGoalCookShrimp(); // Made Fire -> Cook Shrimp
        else if (stageId >= 90 && stageId < 120) nextGoal = createGoalExitSurvivalArea(); // Cooked -> Exit Area (Open Gate)
        else if (stageId >= 120 && stageId < 130) nextGoal = createGoalEnterChefHouse(); // Past Gate 1 -> Open Gate 2
        else if (stageId >= 130 && stageId < 140) nextGoal = createGoalTalkToChef(); // Entered Area -> Talk to Chef
        else if (stageId >= 140 && stageId < 150) nextGoal = createGoalMakeDough(); // Talked -> Make Dough
        else if (stageId >= 150 && stageId < 160) nextGoal = createGoalCookBread(); // Made Dough -> Cook Bread
            // Varp 145 (Music Tab) seems skipped or part of 160/170 based on list
        else if (stageId >= 160 && stageId < 170) nextGoal = createGoalExitChefHouse(); // Cooked Bread -> Exit House
        else if (stageId >= 170 && stageId < 200) nextGoal = createGoalTalkToQuestGuide(); // Exited Chef -> Talk to Quest Guide (Needs walk action first)
        else if (stageId >= 200 && stageId < 220) nextGoal = createGoalTalkToQuestGuide(); // Entered Quest House -> Talk (Redundant? Maybe walk first)
        else if (stageId >= 220 && stageId < 230) nextGoal = createGoalOpenQuestTab(); // Talked -> Open Quest Tab
        else if (stageId >= 230 && stageId < 240) nextGoal = createGoalTalkAfterQuestTab(); // Tab Open -> Talk Again
        else if (stageId >= 240 && stageId < 250) nextGoal = createGoalClimbLadder(); // Talked -> Climb Ladder
        else if (stageId >= 250 && stageId < 260) nextGoal = createGoalTalkToMiningInstructor(); // Climbed -> Talk to Instructor
            // Prospecting happens during dialogue or between 260 and 300
        else if (stageId >= 260 && stageId < 300) nextGoal = createGoalMineTin(); // Talked -> Mine Tin
        else if (stageId >= 300 && stageId < 310) nextGoal = createGoalMineCopper(); // Mined Tin -> Mine Copper
        else if (stageId >= 310 && stageId < 320) nextGoal = createGoalSmeltBar(); // Mined Copper -> Smelt Bar
        else if (stageId >= 320 && stageId < 330) nextGoal = createGoalTalkAfterSmelting(); // Smelted -> Talk Again
        else if (stageId >= 330 && stageId < 340) nextGoal = createGoalClickAnvil(); // Talked -> Click Anvil
        else if (stageId >= 340 && stageId < 350) nextGoal = createGoalSmithDagger(); // Clicked Anvil -> Smith Dagger
        else if (stageId >= 350 && stageId < 360) nextGoal = createGoalEnterCombatCave(); // Smithed -> Open Gate
        else if (stageId >= 360 && stageId < 370) nextGoal = createGoalTalkToCombatInstructor(); // Entered Cave -> Talk Instructor
        else if (stageId >= 370 && stageId < 390) nextGoal = createGoalOpenEquipTab(); // Talked -> Open Equip Tab
        else if (stageId >= 390 && stageId < 400) nextGoal = createGoalOpenEquipStats(); // Equip Tab Open -> Open Stats
        else if (stageId >= 400 && stageId < 405) nextGoal = createGoalEquipDagger(); // Stats Viewed -> Equip Dagger
        else if (stageId >= 405 && stageId < 410) nextGoal = createGoalTalkAfterDagger(); // Dagger Equipped -> Talk Again
        else if (stageId >= 410 && stageId < 420) nextGoal = createGoalEquipSwordShield(); // Talked -> Equip Sword/Shield
        else if (stageId >= 420 && stageId < 430) nextGoal = createGoalOpenCombatStyles(); // Equipped -> Open Combat Styles
        else if (stageId >= 430 && stageId < 440) nextGoal = createGoalEnterRatCage(); // Styles Open -> Enter Cage
        else if (stageId >= 440 && stageId < 450) nextGoal = createGoalAttackRatMelee(); // Entered Cage -> Attack Rat
        else if (stageId >= 450 && stageId < 460) nextGoal = createGoalWaitForRatMeleeDeath(); // Attacking -> Wait for kill
        else if (stageId >= 460 && stageId < 470) nextGoal = createGoalTalkAfterMelee(); // Killed Rat -> Exit cage & Talk
        else if (stageId >= 470 && stageId < 480) nextGoal = createGoalEquipRangedAttackRat(); // Talked -> Equip Ranged & Attack
        else if (stageId >= 480 && stageId < 490) nextGoal = createGoalWaitForRatRangedDeath(); // Attacking Ranged -> Wait for kill
        else if (stageId >= 490 && stageId < 500) nextGoal = createGoalExitCombatArea(); // Killed Ranged -> Climb Ladder
        else if (stageId >= 500 && stageId < 510) nextGoal = createGoalOpenBank(); // Climbed -> Open Bank
        else if (stageId >= 510 && stageId < 520) nextGoal = createGoalOpenPollBooth(); // Banked -> Open Poll Booth
        else if (stageId >= 520 && stageId < 530) nextGoal = createGoalTalkToAdvisor(); // Polled -> Open Door & Talk
        else if (stageId >= 530 && stageId < 531) nextGoal = createGoalOpenAccountTab(); // Talked -> Open Account Tab
        else if (stageId >= 531 && stageId < 532) nextGoal = createGoalTalkAfterAccountTab(); // Tab Open -> Talk Again
        else if (stageId >= 532 && stageId < 540) nextGoal = createGoalExitAdvisorRoom(); // Talked -> Exit Room
        else if (stageId >= 540 && stageId < 550) nextGoal = createGoalTalkToBrother(); // Exited -> Talk to Brother
        else if (stageId >= 550 && stageId < 560) nextGoal = createGoalOpenPrayerTab(); // Talked -> Open Prayer Tab
        else if (stageId >= 560 && stageId < 570) nextGoal = createGoalTalkAfterPrayerTab(); // Tab Open -> Talk Again
        else if (stageId >= 570 && stageId < 580) nextGoal = createGoalOpenFriendsList(); // Talked -> Open Friends
        else if (stageId >= 580 && stageId < 600) nextGoal = createGoalTalkAfterFriendsList(); // Lists Open -> Talk Again
        else if (stageId >= 600 && stageId < 610) nextGoal = createGoalExitChapel(); // Talked -> Exit Chapel
        else if (stageId >= 610 && stageId < 620) nextGoal = createGoalTalkToMagicInstructor(); // Exited -> Talk to Instructor
        else if (stageId >= 620 && stageId < 630) nextGoal = createGoalOpenSpellbook(); // Talked -> Open Spellbook
        else if (stageId >= 630 && stageId < 640) nextGoal = createGoalTalkAfterSpellbook(); // Spellbook Open -> Talk Again
        else if (stageId >= 640 && stageId < 650) nextGoal = createGoalKillChicken(); // Talked -> Kill Chicken
        else if (stageId >= 650 && stageId < 670) nextGoal = createGoalReadyToLeave(); // Killed Chicken -> Talk Again
        else if (stageId >= 670 && stageId < 1000) nextGoal = createGoalLeaveTutorial(); // Talked -> Confirm Departure
        else if (stageId == 1000) {
            nextGoal = null; // Tutorial Complete
            worldState.setBoolean(WorldStateKey.TUT_ISLAND_COMPLETED, true);
        }


        // Logic to set the new goal if it changed
        if (nextGoal != null && !nextGoal.getName().equals(currentGoalName)) {
            Logger.log("Setting new goal: " + nextGoal.getName() + " (Triggered by Stage ID: " + stageId + ")");
            currentGoal = nextGoal;
            currentPlan = null; // Force replan for new goal
            executionEngine.setPlan(null); // Clear engine's plan too
        } else if (nextGoal == null && currentGoal != null && stageId != 1000) {
            // Keep current goal if no new goal is determined (e.g., intermediate varp)
        } else if (stageId == 1000) {
            currentGoal = null; // No goal once complete
        }
    }

    // --- Helper methods to create Goal objects ---
    // (Includes previously defined ones for completeness)

    private Goal createGoalTalkToGuide() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 1); // Target next stage ID
        return new Goal("Goal_TalkToGuide", conditions);
    }
    private Goal createGoalTalkToGuidePostName() { // May not be needed if stage 1->2 is automatic
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 2);
        return new Goal("Goal_TalkToGuidePostName", conditions);
    }
    private Goal createGoalOpenSettings() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        // Goal could be UI_SETTINGS_TAB_OPEN, but stage ID is more direct for tutorial flow
        conditions.put(WorldStateKey.TUT_STAGE_ID, 3);
        return new Goal("Goal_OpenSettings", conditions);
    }
    private Goal createGoalTalkAfterSettings() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 7);
        return new Goal("Goal_TalkAfterSettings", conditions);
    }
    private Goal createGoalOpenStartDoor() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.S0_DOOR_OPEN, true); // Target door state
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 10); // Or target next ID
        return new Goal("Goal_OpenStartDoor", conditions);
    }
    private Goal createGoalTalkToSurvivalExpert() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 20);
        return new Goal("Goal_TalkToSurvivalExpert", conditions);
    }
    private Goal createGoalOpenInventory() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.UI_INVENTORY_OPEN, true); // Target UI state
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 30);
        return new Goal("Goal_OpenInventory", conditions);
    }
    private Goal createGoalFishShrimp() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.S1_HAS_RAW_SHRIMP, true); // Target item state
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 40);
        return new Goal("Goal_FishShrimp", conditions);
    }
    private Goal createGoalOpenSkillsTab() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.UI_SKILLS_TAB_OPEN, true);
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 50);
        return new Goal("Goal_OpenSkillsTab", conditions);
    }
    private Goal createGoalTalkAfterSkills() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 60);
        return new Goal("Goal_TalkAfterSkills", conditions);
    }
    private Goal createGoalCutTree() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.S1_HAS_LOGS, true);
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 70);
        return new Goal("Goal_CutTree", conditions);
    }
    private Goal createGoalMakeFire() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.S1_IS_FIRE_LIT, true);
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 80);
        return new Goal("Goal_MakeFire", conditions);
    }
    private Goal createGoalCookShrimp() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.S1_HAS_COOKED_SHRIMP, true);
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 90);
        return new Goal("Goal_CookShrimp", conditions);
    }
    private Goal createGoalExitSurvivalArea() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        // Goal is reaching the next area or stage ID
        conditions.put(WorldStateKey.TUT_STAGE_ID, 120);
        return new Goal("Goal_ExitSurvivalArea", conditions);
    }
    private Goal createGoalEnterChefHouse() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        // Goal is reaching the next area or stage ID
        conditions.put(WorldStateKey.TUT_STAGE_ID, 130);
        // conditions.put(WorldStateKey.LOC_CURRENT_AREA_NAME, "Cooking_Range_Area");
        return new Goal("Goal_EnterChefHouse", conditions);
    }
    private Goal createGoalTalkToChef() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 140);
        return new Goal("Goal_TalkToChef", conditions);
    }
    private Goal createGoalMakeDough() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.S2_HAS_DOUGH, true);
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 150);
        return new Goal("Goal_MakeDough", conditions);
    }
    private Goal createGoalCookBread() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.S2_HAS_BREAD, true);
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 160);
        return new Goal("Goal_CookBread", conditions);
    }
    private Goal createGoalExitChefHouse() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 170);
        // conditions.put(WorldStateKey.S2_CHEF_DOOR_EXIT_OPEN, true);
        return new Goal("Goal_ExitChefHouse", conditions);
    }
    private Goal createGoalEnterQuestHouse() { // Placeholder name
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 200); // Or being in Quest_Guide_Area
        return new Goal("Goal_EnterQuestHouse", conditions);
    }
    private Goal createGoalTalkToQuestGuide() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 220);
        return new Goal("Goal_TalkToQuestGuide", conditions);
    }
    private Goal createGoalOpenQuestTab() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.UI_QUEST_TAB_OPEN, true);
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 230);
        return new Goal("Goal_OpenQuestTab", conditions);
    }
    private Goal createGoalTalkAfterQuestTab() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 240);
        return new Goal("Goal_TalkAfterQuestTab", conditions);
    }
    private Goal createGoalClimbLadder() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.LOC_CURRENT_AREA_NAME, "Mining_Area"); // Reaching the mining area signifies success
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 250);
        return new Goal("Goal_ClimbLadder", conditions);
    }
    private Goal createGoalTalkToMiningInstructor() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 260); // Covers prospecting dialogue too
        return new Goal("Goal_TalkToMiningInstructor", conditions);
    }
    private Goal createGoalMineTin() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.S3_HAS_TIN_ORE, true);
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 300);
        return new Goal("Goal_MineTin", conditions);
    }
    private Goal createGoalMineCopper() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.S3_HAS_COPPER_ORE, true);
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 310);
        return new Goal("Goal_MineCopper", conditions);
    }
    private Goal createGoalSmeltBar() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.S3_HAS_BRONZE_BAR, true);
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 320);
        return new Goal("Goal_SmeltBar", conditions);
    }
    private Goal createGoalTalkAfterSmelting() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 330);
        return new Goal("Goal_TalkAfterSmelting", conditions);
    }
    private Goal createGoalClickAnvil() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        // Goal might be opening the smithing interface, need a key/check for that
        // Or just reaching the next stage ID
        conditions.put(WorldStateKey.TUT_STAGE_ID, 340);
        return new Goal("Goal_ClickAnvil", conditions);
    }
    private Goal createGoalSmithDagger() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.S3_HAS_DAGGER, true);
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 350);
        return new Goal("Goal_SmithDagger", conditions);
    }
    private Goal createGoalEnterCombatCave() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 360);
        // conditions.put(WorldStateKey.S3_MINE_GATE_OPEN, true);
        return new Goal("Goal_EnterCombatCave", conditions);
    }
    private Goal createGoalTalkToCombatInstructor() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 370);
        return new Goal("Goal_TalkToCombatInstructor", conditions);
    }
    private Goal createGoalOpenEquipTab() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.UI_EQUIPMENT_TAB_OPEN, true);
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 390);
        return new Goal("Goal_OpenEquipTab", conditions);
    }
    private Goal createGoalOpenEquipStats() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.UI_EQUIPMENT_STATS_OPEN, true);
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 400);
        return new Goal("Goal_OpenEquipStats", conditions);
    }
    private Goal createGoalEquipDagger() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.S4_DAGGER_EQUIPPED, true);
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 405);
        return new Goal("Goal_EquipDagger", conditions);
    }
    private Goal createGoalTalkAfterDagger() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 410);
        return new Goal("Goal_TalkAfterDagger", conditions);
    }
    private Goal createGoalEquipSwordShield() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        // Need keys for sword/shield equipped
        // conditions.put(WorldStateKey.S4_SWORD_EQUIPPED, true);
        // conditions.put(WorldStateKey.S4_SHIELD_EQUIPPED, true);
        conditions.put(WorldStateKey.TUT_STAGE_ID, 420); // Rely on stage ID for now
        return new Goal("Goal_EquipSwordShield", conditions);
    }
    private Goal createGoalOpenCombatStyles() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.UI_COMBAT_OPTIONS_OPEN, true);
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 430);
        return new Goal("Goal_OpenCombatStyles", conditions);
    }
    private Goal createGoalEnterRatCage() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.S4_RAT_GATE_OPEN, true);
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 440);
        return new Goal("Goal_EnterRatCage", conditions);
    }
    private Goal createGoalAttackRatMelee() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.S4_KILLED_RAT_MELEE, true); // Goal is killing the rat
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 460); // Or reaching the ID after kill
        return new Goal("Goal_AttackRatMelee", conditions);
    }
    private Goal createGoalWaitForRatMeleeDeath() { // Intermediate goal if needed
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 460);
        return new Goal("Goal_WaitForRatMeleeDeath", conditions);
    }
    private Goal createGoalTalkAfterMelee() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 470);
        return new Goal("Goal_TalkAfterMelee", conditions);
    }
    private Goal createGoalEquipRangedAttackRat() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        // Need keys for bow/arrow equipped
        // conditions.put(WorldStateKey.S4_BOW_EQUIPPED, true);
        // conditions.put(WorldStateKey.S4_ARROWS_EQUIPPED, true);
        conditions.put(WorldStateKey.S4_KILLED_RAT_RANGED, true); // Goal is killing the rat
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 490);
        return new Goal("Goal_EquipRangedAttackRat", conditions);
    }
    private Goal createGoalWaitForRatRangedDeath() { // Intermediate goal if needed
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 490);
        return new Goal("Goal_WaitForRatRangedDeath", conditions);
    }
    private Goal createGoalExitCombatArea() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        // Goal is being back on surface, e.g., in Bank_Area or reaching stage ID
        conditions.put(WorldStateKey.TUT_STAGE_ID, 500);
        return new Goal("Goal_ExitCombatArea", conditions);
    }
    private Goal createGoalOpenBank() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.UI_BANK_OPEN, true);
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 510);
        return new Goal("Goal_OpenBank", conditions);
    }
    private Goal createGoalOpenPollBooth() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.UI_POLL_BOOTH_OPEN, true);
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 520);
        return new Goal("Goal_OpenPollBooth", conditions);
    }
    private Goal createGoalTalkToAdvisor() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 530);
        return new Goal("Goal_TalkToAdvisor", conditions);
    }
    private Goal createGoalOpenAccountTab() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        // conditions.put(WorldStateKey.UI_ACCOUNT_MANAGEMENT_OPEN, true); // Need key/check
        conditions.put(WorldStateKey.TUT_STAGE_ID, 531);
        return new Goal("Goal_OpenAccountTab", conditions);
    }
    private Goal createGoalTalkAfterAccountTab() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 532);
        return new Goal("Goal_TalkAfterAccountTab", conditions);
    }
    private Goal createGoalExitAdvisorRoom() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 540);
        // conditions.put(WorldStateKey.S5_FINANCIAL_DOOR_OUT_OPEN, true);
        return new Goal("Goal_ExitAdvisorRoom", conditions);
    }
    private Goal createGoalTalkToBrother() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 550);
        return new Goal("Goal_TalkToBrother", conditions);
    }
    private Goal createGoalOpenPrayerTab() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.UI_PRAYER_TAB_OPEN, true);
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 560);
        return new Goal("Goal_OpenPrayerTab", conditions);
    }
    private Goal createGoalTalkAfterPrayerTab() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 570);
        return new Goal("Goal_TalkAfterPrayerTab", conditions);
    }
    private Goal createGoalOpenFriendsList() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.UI_FRIENDS_TAB_OPEN, true); // Includes ignore list
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 580); // Or maybe 600?
        return new Goal("Goal_OpenFriendsList", conditions);
    }
    private Goal createGoalTalkAfterFriendsList() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 600);
        return new Goal("Goal_TalkAfterFriendsList", conditions);
    }
    private Goal createGoalExitChapel() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 610);
        // conditions.put(WorldStateKey.S6_CHURCH_DOOR_OUT_OPEN, true);
        return new Goal("Goal_ExitChapel", conditions);
    }
    private Goal createGoalTalkToMagicInstructor() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 620);
        return new Goal("Goal_TalkToMagicInstructor", conditions);
    }
    private Goal createGoalOpenSpellbook() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.UI_MAGIC_SPELLBOOK_OPEN, true);
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 630);
        return new Goal("Goal_OpenSpellbook", conditions);
    }
    private Goal createGoalTalkAfterSpellbook() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 640);
        return new Goal("Goal_TalkAfterSpellbook", conditions);
    }
    private Goal createGoalKillChicken() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.S7_KILLED_CHICKEN, true);
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 650);
        return new Goal("Goal_KillChicken", conditions);
    }
    private Goal createGoalReadyToLeave() { // Final talk before leaving
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_STAGE_ID, 670);
        return new Goal("Goal_ReadyToLeave", conditions);
    }
    private Goal createGoalLeaveTutorial() {
        Map<WorldStateKey, Object> conditions = new HashMap<>();
        conditions.put(WorldStateKey.TUT_ISLAND_COMPLETED, true); // Final goal state
        // conditions.put(WorldStateKey.TUT_STAGE_ID, 1000);
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