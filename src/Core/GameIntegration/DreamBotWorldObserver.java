package Core.GameIntegration;

import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.equipment.Equipment;
import org.dreambot.api.methods.container.impl.equipment.EquipmentSlot;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.prayer.Prayer;
import org.dreambot.api.methods.prayer.Prayers;
import org.dreambot.api.methods.settings.PlayerSettings;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;
import org.dreambot.api.methods.widget.Widgets;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.items.Item;
import org.dreambot.api.wrappers.widgets.WidgetChild; // Keep for potential future use

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Responsible for observing the live OSRS game state via the DreamBot API
 * and updating the GOAP agent's WorldState accordingly.
 * Uses data derived from research for Tutorial Island specifics.
 */
public class DreamBotWorldObserver {

    private final AbstractScript script;
    private static final int TUTORIAL_ISLAND_VARP = 281;

    // --- Object IDs (from research - VERIFY IN GAME) ---
    private static final int STARTING_DOOR_ID = 2;   // Research suggests ID 2
    private static final int SURVIVAL_FIRE_ID = -1; // Research suggests ID 70, but name/action check might be safer
    private static final int CHEF_EXIT_DOOR_ID = 160; // Research suggests ID 160
    private static final int MINE_EXIT_GATE_ID = 305; // Research suggests ID 305
    private static final int RAT_PEN_GATE_ID = 370; // Research suggests ID 370
    private static final int FIN_DOOR_IN_ID = 450; // Research suggests ID 450
    private static final int FIN_DOOR_OUT_ID = 470; // Research suggests ID 470
    private static final int CHURCH_EXIT_DOOR_ID = 530; // Research suggests ID 530

    // --- Widget IDs (from research - VERIFY IN GAME) ---
    private static final int WIDGET_ID_EQUIP_STATS = 84;
    private static final int WIDGET_ID_POLL_BOOTH = 345;
    private static final int WIDGET_ID_ACC_MANAGEMENT = 109;
    // private static final int WIDGET_ID_NPC_CHAT_BOX = 162; // Keep for reference
    // private static final int WIDGET_CHILD_ID_CONTINUE = 58; // Keep for reference

    // --- Areas (Loaded from your verified definitions - ENSURE THESE ARE ACCURATE) ---
    public static final Map<String, Area> TUTORIAL_AREAS = loadTutorialAreas();

    // --- VarPlayer Mapping (Using TreeMap and specific values - VALIDATE TRIGGER POINTS) ---
    private static final TreeMap<Integer, String> VARP_TO_STAGE_NAME = createVarpMap();


    // --- Constructor ---
    public DreamBotWorldObserver(AbstractScript script) {
        this.script = script;
        if (this.script == null) {
            System.err.println("CRITICAL: DreamBotWorldObserver initialized without a valid script reference!");
        }
    }

    // --- Area Definitions ---
    private static Map<String, Area> loadTutorialAreas() {
        Map<String, Area> areas = new HashMap<>();
        // Load ALL areas - VERIFY PLACEHOLDERS
        areas.put("Guide_Start_Area", new Area(3094, 3107, 3102, 3112));
        areas.put("Survival_Fishing_Area", new Area(3100, 3094, 3106, 3099));
        areas.put("Survival_Cooking_Area", new Area(3086, 3092, 3092, 3097));
        areas.put("Cooking_Range_Area", new Area(3072, 3082, 3080, 3086)); // Placeholder - VERIFY
        areas.put("Quest_Guide_Area", new Area(3084, 3119, 3090, 3124));
        areas.put("Mining_Area", new Area(3074, 9503, 3081, 9508, 0));
        areas.put("Mining_Smithing_Area", new Area(3076, 9497, 3082, 9504, 0));
        areas.put("Combat_Area", new Area(3104, 9503, 3116, 9513, 0)); // Placeholder - VERIFY
        areas.put("Bank_Area", new Area(3121, 3123, 3125, 3127));
        areas.put("Prayer_Area", new Area(3121, 3117, 3126, 3120)); // Placeholder - VERIFY
        areas.put("Magic_Area", new Area(3138, 3084, 3145, 3090)); // Placeholder - VERIFY
        areas.put("Mainland_Spawn", new Area(3221, 3218, 3223, 3220));
        areas.put("Financial_Advisor_Area", new Area(3124, 3127, 3128, 3130));
        return Collections.unmodifiableMap(areas);
    }

    // --- VarPlayer Mapping ---
    private static TreeMap<Integer, String> createVarpMap() {
        TreeMap<Integer, String> map = new TreeMap<>();
        // Populate with the detailed map from previous step (0, 1, 2, 3, 7, 10...)
        map.put(0, "S0_Start_CharCreation");
        map.put(1, "S0_Start_TalkToGuide");
        map.put(2, "S0_Start_TalkToGuide_PostName");
        map.put(3, "S0_Start_OpenSettings");
        map.put(7, "S0_Start_TalkAfterSettings");
        map.put(10, "S0_Start_DoorOpened");
        map.put(20, "S1_Survival_TalkToExpert");
        map.put(30, "S1_Survival_OpenInventory");
        map.put(40, "S1_Survival_FishShrimp");
        map.put(50, "S1_Survival_OpenSkillsTab");
        map.put(60, "S1_Survival_TalkAfterSkills");
        map.put(70, "S1_Survival_CutTree");
        map.put(80, "S1_Survival_MakeFire");
        map.put(90, "S1_Survival_CookShrimp");
        map.put(120, "S1_Survival_ExitArea");
        map.put(130, "S2_Cooking_EnterChefHouse");
        map.put(140, "S2_Cooking_TalkToChef");
        map.put(150, "S2_Cooking_MakeDough");
        map.put(160, "S2_Cooking_CookBread");
        map.put(170, "S2_Cooking_ExitChefHouse");
        map.put(200, "S3_Quest_EnterQuestHouse");
        map.put(220, "S3_Quest_TalkToGuide");
        map.put(230, "S3_Quest_OpenQuestTab");
        map.put(240, "S3_Quest_TalkAfterQuestTab");
        map.put(250, "S3_Quest_ClimbLadder");
        map.put(260, "S3_Mining_TalkToInstructor");
        map.put(300, "S3_Mining_MineTin");
        map.put(310, "S3_Mining_MineCopper");
        map.put(320, "S3_Smithing_SmeltBar");
        map.put(330, "S3_Smithing_TalkAfterSmelting");
        map.put(340, "S3_Smithing_ClickAnvil");
        map.put(350, "S3_Smithing_SmithDagger");
        map.put(360, "S3_Smithing_EnterCombatCave");
        map.put(370, "S4_Combat_TalkToInstructor");
        map.put(390, "S4_Combat_OpenEquipTab");
        map.put(400, "S4_Combat_OpenEquipStats");
        map.put(405, "S4_Combat_EquipDagger");
        map.put(410, "S4_Combat_TalkAfterDagger");
        map.put(420, "S4_Combat_EquipSwordShield");
        map.put(430, "S4_Combat_OpenCombatStyles");
        map.put(440, "S4_Combat_EnterRatCage");
        map.put(450, "S4_Combat_AttackRatMelee");
        map.put(460, "S4_Combat_KilledRatMelee");
        map.put(470, "S4_Combat_TalkAfterMelee");
        map.put(480, "S4_Combat_EquipRangedAttackRat");
        map.put(490, "S4_Combat_KilledRatRanged");
        map.put(500, "S4_Combat_ExitArea");
        map.put(510, "S5_Financial_OpenBank");
        map.put(520, "S5_Financial_OpenPollBooth");
        map.put(530, "S5_Financial_TalkToAdvisor");
        map.put(531, "S5_Financial_OpenAccountTab");
        map.put(532, "S5_Financial_TalkAfterAccountTab");
        map.put(540, "S5_Financial_ExitRoom");
        map.put(550, "S6_Prayer_TalkToBrother");
        map.put(560, "S6_Prayer_OpenPrayerTab");
        map.put(570, "S6_Prayer_TalkAfterPrayerTab");
        map.put(580, "S6_Prayer_OpenFriendsList");
        map.put(600, "S6_Prayer_TalkAfterFriendsList");
        map.put(610, "S6_Prayer_ExitChapel");
        map.put(620, "S7_Magic_TalkToInstructor");
        map.put(630, "S7_Magic_OpenSpellbook");
        map.put(640, "S7_Magic_TalkAfterSpellbook");
        map.put(650, "S7_Magic_KillChicken");
        map.put(670, "S7_Magic_ReadyToLeave");
        map.put(1000, "S8_Mainland");
        return map;
    }

    // --- Main Update Method ---
    public void updateWorldState(WorldState worldState) {
        if (script == null || worldState == null) return;

        // Determine Location FIRST
        Tile playerTile = Players.getLocal().getTile();
        String currentAreaName = "Unknown";
        for (Map.Entry<String, Area> entry : TUTORIAL_AREAS.entrySet()) {
            if (entry.getValue().contains(playerTile)) {
                currentAreaName = entry.getKey();
                break;
            }
        }
        worldState.setString(WorldStateKey.LOC_CURRENT_AREA_NAME, currentAreaName);
        worldState.setBoolean(WorldStateKey.LOC_IS_WALKING, Players.getLocal().isMoving());

        // Determine Tutorial Progress SECOND
        int tutorialProgressVar = PlayerSettings.getConfig(TUTORIAL_ISLAND_VARP);
        worldState.setInteger(WorldStateKey.TUT_STAGE_ID, tutorialProgressVar);
        Map.Entry<Integer, String> stageEntry = VARP_TO_STAGE_NAME.floorEntry(tutorialProgressVar);
        String stageName = (stageEntry != null) ? stageEntry.getValue() : "UnknownStage_" + tutorialProgressVar;
        worldState.setString(WorldStateKey.TUT_STAGE_NAME, stageName);
        worldState.setBoolean(WorldStateKey.TUT_ISLAND_COMPLETED, tutorialProgressVar == 1000);

        // Update Object States THIRD
        updateObjectStates(worldState, stageName, currentAreaName);

        // Update Other States

        // Interaction
        boolean npcDialogueActive = Dialogues.inDialogue() && Dialogues.canContinue();
        worldState.setBoolean(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN, npcDialogueActive);
        if (!npcDialogueActive) {
            worldState.setString(WorldStateKey.INTERACT_NPC_NAME, null);
        }
        worldState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, Players.getLocal().isAnimating());

        // UI Checks
        worldState.setBoolean(WorldStateKey.UI_INVENTORY_OPEN, Tabs.isOpen(Tab.INVENTORY));
        worldState.setBoolean(WorldStateKey.UI_SKILLS_TAB_OPEN, Tabs.isOpen(Tab.SKILLS));
        worldState.setBoolean(WorldStateKey.UI_MUSIC_TAB_OPEN, Tabs.isOpen(Tab.MUSIC));
        worldState.setBoolean(WorldStateKey.UI_EQUIPMENT_TAB_OPEN, Tabs.isOpen(Tab.EQUIPMENT));
        worldState.setBoolean(WorldStateKey.UI_EQUIPMENT_STATS_OPEN, WIDGET_ID_EQUIP_STATS != -1 && Widgets.getWidget(WIDGET_ID_EQUIP_STATS) != null && Widgets.getWidget(WIDGET_ID_EQUIP_STATS).isVisible());
        worldState.setBoolean(WorldStateKey.UI_COMBAT_OPTIONS_OPEN, Tabs.isOpen(Tab.COMBAT));
        worldState.setBoolean(WorldStateKey.UI_QUEST_TAB_OPEN, Tabs.isOpen(Tab.QUEST));
        worldState.setBoolean(WorldStateKey.UI_PRAYER_TAB_OPEN, Tabs.isOpen(Tab.PRAYER));
        worldState.setBoolean(WorldStateKey.UI_FRIENDS_TAB_OPEN, Tabs.isOpen(Tab.FRIENDS));
        worldState.setBoolean(WorldStateKey.UI_MAGIC_SPELLBOOK_OPEN, Tabs.isOpen(Tab.MAGIC));
        worldState.setBoolean(WorldStateKey.UI_BANK_OPEN, Bank.isOpen());
        worldState.setBoolean(WorldStateKey.UI_POLL_BOOTH_OPEN, WIDGET_ID_POLL_BOOTH != -1 && Widgets.getWidget(WIDGET_ID_POLL_BOOTH) != null && Widgets.getWidget(WIDGET_ID_POLL_BOOTH).isVisible());
        // TODO: Add check for Account Management (Widget 109) if needed

        // Inventory
        worldState.setInteger(WorldStateKey.INV_SPACE, Inventory.getEmptySlots());
        worldState.setInteger(WorldStateKey.INV_COINS, Inventory.count("Coins"));
        worldState.setBoolean(WorldStateKey.S1_HAS_AXE, Inventory.contains("Bronze axe"));
        worldState.setBoolean(WorldStateKey.S1_HAS_TINDERBOX, Inventory.contains("Tinderbox"));
        worldState.setBoolean(WorldStateKey.S1_HAS_LOGS, Inventory.contains("Logs"));
        worldState.setBoolean(WorldStateKey.S1_HAS_FISHING_NET, Inventory.contains("Small fishing net"));
        worldState.setBoolean(WorldStateKey.S1_HAS_RAW_SHRIMP, Inventory.contains("Raw shrimps"));
        worldState.setBoolean(WorldStateKey.S1_HAS_COOKED_SHRIMP, Inventory.contains("Shrimps"));
        worldState.setBoolean(WorldStateKey.S2_HAS_POT, Inventory.contains("Pot"));
        worldState.setBoolean(WorldStateKey.S2_HAS_FLOUR, Inventory.contains("Pot of flour"));
        worldState.setBoolean(WorldStateKey.S2_HAS_BUCKET, Inventory.contains("Bucket"));
        worldState.setBoolean(WorldStateKey.S2_HAS_BUCKET_OF_WATER, Inventory.contains("Bucket of water"));
        worldState.setBoolean(WorldStateKey.S2_HAS_DOUGH, Inventory.contains("Bread dough"));
        worldState.setBoolean(WorldStateKey.S2_HAS_BREAD, Inventory.contains("Bread"));
        worldState.setBoolean(WorldStateKey.S3_HAS_PICKAXE, Inventory.contains("Bronze pickaxe"));
        worldState.setBoolean(WorldStateKey.S3_HAS_TIN_ORE, Inventory.contains("Tin ore"));
        worldState.setBoolean(WorldStateKey.S3_HAS_COPPER_ORE, Inventory.contains("Copper ore"));
        worldState.setBoolean(WorldStateKey.S3_HAS_BRONZE_BAR, Inventory.contains("Bronze bar"));
        worldState.setBoolean(WorldStateKey.S3_HAS_HAMMER, Inventory.contains("Hammer"));
        worldState.setBoolean(WorldStateKey.S3_HAS_DAGGER, Inventory.contains("Bronze dagger"));
        worldState.setBoolean(WorldStateKey.S4_HAS_BOW, Inventory.contains("Shortbow"));
        worldState.setBoolean(WorldStateKey.S4_HAS_ARROWS, Inventory.contains("Bronze arrow"));
        worldState.setBoolean(WorldStateKey.S7_HAS_AIR_RUNE, Inventory.contains("Air rune"));
        worldState.setBoolean(WorldStateKey.S7_HAS_MIND_RUNE, Inventory.contains("Mind rune"));

        // Equipment
        worldState.setBoolean(WorldStateKey.S4_DAGGER_EQUIPPED, isEquipped(EquipmentSlot.WEAPON, "Bronze dagger"));
        Item weapon = Equipment.getItemInSlot(EquipmentSlot.WEAPON.getSlot());
        worldState.setString(WorldStateKey.COMBAT_WEAPON_EQUIPPED, weapon != null ? weapon.getName() : null);

        // Skills
        worldState.setInteger(WorldStateKey.SKILL_WOODCUTTING_LEVEL, Skills.getRealLevel(Skill.WOODCUTTING));
        worldState.setInteger(WorldStateKey.SKILL_FIREMAKING_LEVEL, Skills.getRealLevel(Skill.FIREMAKING));
        worldState.setInteger(WorldStateKey.SKILL_FISHING_LEVEL, Skills.getRealLevel(Skill.FISHING));
        worldState.setInteger(WorldStateKey.SKILL_COOKING_LEVEL, Skills.getRealLevel(Skill.COOKING));
        worldState.setInteger(WorldStateKey.SKILL_MINING_LEVEL, Skills.getRealLevel(Skill.MINING));
        worldState.setInteger(WorldStateKey.SKILL_SMITHING_LEVEL, Skills.getRealLevel(Skill.SMITHING));
        worldState.setInteger(WorldStateKey.SKILL_ATTACK_LEVEL, Skills.getRealLevel(Skill.ATTACK));
        worldState.setInteger(WorldStateKey.SKILL_STRENGTH_LEVEL, Skills.getRealLevel(Skill.STRENGTH));
        worldState.setInteger(WorldStateKey.SKILL_DEFENCE_LEVEL, Skills.getRealLevel(Skill.DEFENCE));
        worldState.setInteger(WorldStateKey.SKILL_RANGED_LEVEL, Skills.getRealLevel(Skill.RANGED));
        worldState.setInteger(WorldStateKey.SKILL_PRAYER_POINTS, Skills.getBoostedLevel(Skill.PRAYER));
        boolean anyPrayerActive = false;
        for (Prayer prayer : Prayer.values()) {
            if (Prayers.isActive(prayer)) {
                anyPrayerActive = true;
                break;
            }
        }
        worldState.setBoolean(WorldStateKey.SKILL_PRAYER_ACTIVE, anyPrayerActive);
        worldState.setInteger(WorldStateKey.SKILL_MAGIC_LEVEL, Skills.getRealLevel(Skill.MAGIC));

        // Combat State
        worldState.setBoolean(WorldStateKey.COMBAT_IS_IN_COMBAT, Players.getLocal().isInCombat());

        // System States (Placeholders)
        // ...
    }

    // --- Object State Helper ---
    private void updateObjectStates(WorldState worldState, String currentStageName, String currentAreaName) {
        // TODO: Replace null Tiles with verified coordinates for each object
        updateSingleObjectState(worldState, WorldStateKey.S0_DOOR_OPEN, STARTING_DOOR_ID, "Door", null, currentAreaName.equals("Guide_Start_Area"), 10);

        boolean fireCheckRelevant = currentAreaName.equals("Survival_Cooking_Area");
        if (fireCheckRelevant) {
            GameObject litFire = GameObjects.closest(fire -> fire != null && fire.getName().equals("Fire") && fire.hasAction("Cook") && TUTORIAL_AREAS.get("Survival_Cooking_Area").contains(fire.getTile()));
            worldState.setBoolean(WorldStateKey.S1_IS_FIRE_LIT, litFire != null);
        } else {
            worldState.setBoolean(WorldStateKey.S1_IS_FIRE_LIT, false);
        }

        updateSingleObjectState(worldState, WorldStateKey.S2_CHEF_DOOR_EXIT_OPEN, CHEF_EXIT_DOOR_ID, "Door", null, currentAreaName.equals("Cooking_Range_Area"), 170);
        updateSingleObjectState(worldState, WorldStateKey.S3_MINE_GATE_OPEN, MINE_EXIT_GATE_ID, "Gate", null, currentAreaName.equals("Mining_Smithing_Area"), 360);
        updateSingleObjectState(worldState, WorldStateKey.S4_RAT_GATE_OPEN, RAT_PEN_GATE_ID, "Gate", null, currentAreaName.equals("Combat_Area"), 440);
        updateSingleObjectState(worldState, WorldStateKey.S5_FINANCIAL_DOOR_IN_OPEN, FIN_DOOR_IN_ID, "Door", null, currentAreaName.equals("Bank_Area"), 460);
        updateSingleObjectState(worldState, WorldStateKey.S5_FINANCIAL_DOOR_OUT_OPEN, FIN_DOOR_OUT_ID, "Door", null, currentAreaName.equals("Financial_Advisor_Area"), 540);
        updateSingleObjectState(worldState, WorldStateKey.S6_CHURCH_DOOR_OUT_OPEN, CHURCH_EXIT_DOOR_ID, "Door", null, currentAreaName.equals("Prayer_Area"), 610);
    }

    // --- Object State Helper ---
    private void updateSingleObjectState(WorldState worldState, WorldStateKey key, int objectId, String objectName, Tile objectTile, boolean checkRelevant, int openStageIdThreshold) {
        if (checkRelevant) {
            GameObject obj = GameObjects.closest(o -> o != null
                    && (o.getID() == objectId) // Prioritize ID if valid (>0)
                    && (objectTile == null || o.getTile().equals(objectTile))); // Use Tile if provided

            if (obj == null && objectId <= 0) { // Fallback to name if ID invalid or not found
                obj = GameObjects.closest(o -> o != null
                        && (o.getName().equals(objectName))
                        && (objectTile == null || o.getTile().equals(objectTile)));
            }
            // State is OPEN if the object exists and does NOT have the "Open" action
            worldState.setBoolean(key, obj != null && !obj.hasAction("Open"));
        } else {
            int currentStageId = worldState.getInteger(WorldStateKey.TUT_STAGE_ID);
            worldState.setBoolean(key, currentStageId >= openStageIdThreshold); // Fallback based on progress
        }
    }

    // --- Equipment Helper ---
    private enum EquipmentSlot { WEAPON(3); private final int slot; EquipmentSlot(int s) {this.slot=s;} public int getSlot(){return slot;} }
    private boolean isEquipped(EquipmentSlot slot, String itemName) {
        Item item = Equipment.getItemInSlot(slot.getSlot());
        return item != null && item.getName().equals(itemName);
    }
}