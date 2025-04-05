package Core.GameIntegration; // New package for DreamBot specific integration

import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey; // Import the enum
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.equipment.Equipment;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area; // If needed for LOC_CURRENT_AREA
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.prayer.Prayer;
import org.dreambot.api.methods.prayer.Prayers; // Import Prayers
import org.dreambot.api.methods.settings.PlayerSettings; // If using VarPlayers for TUT_CURRENT_SECTION
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.widget.Widgets;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.wrappers.items.Item;

/**
 * Responsible for observing the live OSRS game state via the DreamBot API
 * and updating the GOAP agent's WorldState accordingly.
 */
public class DreamBotWorldObserver {

    private final AbstractScript script; // Reference to the main script for API access

    // Define Tutorial Island Areas
    private static final Area TUT_AREA_S0_START = new Area(3093, 3108, 3095, 3106);
    private static final Area TUT_AREA_S1_SURVIVAL = new Area(3100, 3099, 3110, 3091);
    private static final Area TUT_AREA_S2_COOKING = new Area(3075, 3088, 3079, 3082);
    private static final Area TUT_AREA_S3_MINING = new Area(3044, 9503, 3050, 9497);
    private static final Area TUT_AREA_S3_SMITHING = new Area(3036, 9500, 3042, 9494);
    private static final Area TUT_AREA_S4_COMBAT = new Area(3104, 9513, 3116, 9503);
    private static final Area TUT_AREA_S4_RANGED = new Area(3122, 9511, 3132, 9501);
    private static final Area TUT_AREA_S5_FINANCIAL = new Area(3120, 3089, 3127, 3095);
    private static final Area TUT_AREA_S6_PRAYER = new Area(3095, 3090, 3103, 3096);
    private static final Area TUT_AREA_S7_MAGIC = new Area(3138, 3084, 3145, 3090);
    private static final Area TUT_AREA_S8_MAINLAND = new Area(3208, 3223, 3211, 3219);

    /**
     * Constructor for the World Observer.
     * @param script A reference to the running DreamBot script instance.
     */
    public DreamBotWorldObserver(AbstractScript script) {
        this.script = script;
        if (this.script == null) {
            // Maybe throw an exception or log a critical error
            System.err.println("CRITICAL: DreamBotWorldObserver initialized without a valid script reference!");
        }
    }

    /**
     * Updates the provided WorldState object with the current game state
     * by querying the DreamBot API. This should be called frequently,
     * typically once per main loop cycle.
     *
     * @param worldState The WorldState object to update.
     */
    public void updateWorldState(WorldState worldState) {
        if (script == null) {
            System.err.println("OBSERVER: Cannot update WorldState, script reference is null.");
            return;
        }
        if (worldState == null) {
            System.err.println("OBSERVER: Cannot update null WorldState.");
            return;
        }

// --- Interaction ---
        worldState.setBoolean(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN, Dialogues.inDialogue());
        // INTERACT_NPC_NAME might be harder to get directly, often inferred from context/last interaction
        // For now, we might only set it when an action *starts* dialogue. Observer clears it when dialogue closes.
        if (!Dialogues.inDialogue()) {
            worldState.setString(WorldStateKey.INTERACT_NPC_NAME, null);
        }
        worldState.setBoolean(WorldStateKey.INTERACT_IS_ANIMATING, Players.getLocal().isAnimating());

        // --- Inventory ---
        worldState.setInteger(WorldStateKey.INV_SPACE, Inventory.getEmptySlots());
        worldState.setInteger(WorldStateKey.INV_COINS, Inventory.count("Coins"));
        // Tutorial Items
        worldState.setBoolean(WorldStateKey.S1_HAS_AXE, Inventory.contains("Bronze axe"));
        worldState.setBoolean(WorldStateKey.S1_HAS_TINDERBOX, Inventory.contains("Tinderbox"));
        worldState.setBoolean(WorldStateKey.S1_HAS_LOGS, Inventory.contains("Logs"));
        worldState.setBoolean(WorldStateKey.S1_HAS_FISHING_NET, Inventory.contains("Small fishing net"));
        worldState.setBoolean(WorldStateKey.S1_HAS_RAW_SHRIMP, Inventory.contains("Raw shrimps"));
        worldState.setBoolean(WorldStateKey.S1_HAS_COOKED_SHRIMP, Inventory.contains("Shrimps")); // Corrected name
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
        //... Add checks for all other required tutorial items...

        // --- Equipment ---
        // Example: Check if dagger is equipped
        worldState.setBoolean(WorldStateKey.S4_DAGGER_EQUIPPED, Equipment.contains("Bronze dagger"));
        // Example: General weapon check (might store name or ID)
        Item weapon = Equipment.getItemInSlot(EquipmentSlot.WEAPON.getSlot());
        worldState.setString(WorldStateKey.COMBAT_WEAPON_EQUIPPED, weapon!= null? weapon.getName() : null);


        // --- Skills ---
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
        worldState.setInteger(WorldStateKey.SKILL_PRAYER_POINTS, Skills.getBoostedLevel(Skill.PRAYER)); // Current points
        worldState.setInteger(WorldStateKey.SKILL_MAGIC_LEVEL, Skills.getRealLevel(Skill.MAGIC));
        // worldState.setBoolean(WorldStateKey.SKILL_PRAYER_ACTIVE, Prayers.isAnyPrayerActive()); // Check if *any* prayer is active


        // --- Location & Movement ---
        worldState.setBoolean(WorldStateKey.LOC_IS_WALKING, Players.getLocal().isMoving());
        // Determine current area
        Tile playerTile = Players.getLocal().getTile();
        String currentArea = "Unknown"; // Default
        if (TUT_AREA_S0_START.contains(playerTile)) {
            currentArea = "TUT_S0_START_ROOM";
        } else if (TUT_AREA_S1_SURVIVAL.contains(playerTile)) {
            currentArea = "TUT_S1_SURVIVAL_AREA";
        } else if (TUT_AREA_S2_COOKING.contains(playerTile)) {
            currentArea = "TUT_S2_COOKING_AREA";
        } else if (TUT_AREA_S3_MINING.contains(playerTile)) {
            currentArea = "TUT_S3_MINING_AREA";
        } else if (TUT_AREA_S3_SMITHING.contains(playerTile)) {
            currentArea = "TUT_S3_SMITHING_AREA";
        } else if (TUT_AREA_S4_COMBAT.contains(playerTile)) {
            currentArea = "TUT_S4_COMBAT_AREA";
        } else if (TUT_AREA_S4_RANGED.contains(playerTile)) {
            currentArea = "TUT_S4_RANGED_AREA";
        } else if (TUT_AREA_S5_FINANCIAL.contains(playerTile)) {
            currentArea = "TUT_S5_FINANCIAL_AREA";
        } else if (TUT_AREA_S6_PRAYER.contains(playerTile)) {
            currentArea = "TUT_S6_PRAYER_AREA";
        } else if (TUT_AREA_S7_MAGIC.contains(playerTile)) {
            currentArea = "TUT_S7_MAGIC_AREA";
        } else if (TUT_AREA_S8_MAINLAND.contains(playerTile)) {
            currentArea = "TUT_S8_MAINLAND_AREA";
        }
        worldState.setString(WorldStateKey.LOC_CURRENT_AREA, currentArea);


        // --- Tutorial Progress ---
        // This is often tracked by a VarPlayer setting in OSRS
        int tutorialProgressVar = PlayerSettings.getConfig(281); // Corrected VarPlayer ID
        // Map the VarPlayer value to our section enum/string
        String section = mapTutorialVarToSection(tutorialProgressVar);
        worldState.setString(WorldStateKey.TUT_CURRENT_SECTION, section);
        worldState.setBoolean(WorldStateKey.TUT_ISLAND_COMPLETED, section.equals("S8_Mainland")); // Example completion check


        // --- Other States ---
        worldState.setBoolean(WorldStateKey.COMBAT_IS_IN_COMBAT, Players.getLocal().isInCombat());
        // S0_DOOR_OPEN might need checking GameObjects.closest("Door").hasAction("Open") or state based on section progress
        worldState.setBoolean(WorldStateKey.S0_DOOR_OPEN, GameObjects.closest("Door")!= null &&!GameObjects.closest("Door").hasAction("Open"));
        // S1_IS_FIRE_LIT might need checking GameObjects.closest("Fire") nearby
        worldState.setBoolean(WorldStateKey.S1_IS_FIRE_LIT, GameObjects.closest("Fire")!= null && TUT_AREA_S1_SURVIVAL.contains(Players.getLocal()));

        // --- System ---
        // SYS_ACCOUNT_AGE_MINUTES - Harder to get, maybe track time since script start?
        // SYS_MOUSE_IDLE_MS - Could be tracked by monitoring mouse events (more complex)
        // SYS_LAST_ACTION_RESULT - This should probably be set by the ExecutionEngine, not the observer

        // Add state change logging here later (Step 3.5)
    }

    /**
     * Helper method to map the OSRS Tutorial Island VarPlayer value
     * to our internal section representation (String or Enum).
     * NOTE: The VarPlayer values need to be researched and mapped correctly.
     * These are placeholders.
     *
     * @param varpValue The value of the Tutorial Island progress VarPlayer (e.g., 281).
     * @return A String representing the current tutorial section.
     */
    private String mapTutorialVarToSection(int varpValue) {
        if (varpValue >= 0 && varpValue <= 2) return "S0_Start";
        if (varpValue >= 3 && varpValue <= 31) return "S1_Survival";
        if (varpValue >= 32 && varpValue <= 34) return "S2_Cooking";
        if (varpValue == 35 || varpValue == 36) return "S3_QuestGuide";
        if (varpValue >= 37 && varpValue <= 39) return "S3_MiningSmithing";
        if (varpValue >= 40 && varpValue <= 41) return "S3_MiningSmithing"; // Smithing is still part of this section
        if (varpValue >= 42 && varpValue <= 44) return "S4_Combat";
        if (varpValue >= 45 && varpValue <= 46) return "S4_Combat"; // Ranged is part of combat section
        if (varpValue >= 47 && varpValue <= 49) return "S5_Financial";
        if (varpValue >= 50 && varpValue <= 51) return "S6_Prayer";
        if (varpValue >= 52 && varpValue <= 61) return "S7_Magic";
        if (varpValue == 62) return "S8_AccountGuide";
        if (varpValue == 1000) return "S8_Mainland"; // Tutorial Complete VarPlayer value
        return "UnknownSection_" + varpValue;
    }

    // Helper method to check equipment - Requires EquipmentSlot enum
    private boolean isEquipped(EquipmentSlot slot, String itemName) {
        Item item = Equipment.getItemInSlot(slot.getSlot());
        return item!= null && item.getName().equals(itemName);
    }
    // Define EquipmentSlot enum or import if available elsewhere
    private enum EquipmentSlot { WEAPON(3); private final int slot; EquipmentSlot(int s) {this.slot=s;} public int getSlot(){return slot;} }

}