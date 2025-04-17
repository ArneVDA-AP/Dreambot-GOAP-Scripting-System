package Core.GOAP;

/**
 * Defines the standardized keys used to represent the game state within the WorldState map.
 * Prefixes indicate context:
 * S0_ - S8_ = Tutorial Island Sections 0-8
 * COMBAT_ = Combat Related
 * SKILL_ = Skill Related
 * INV_ = Inventory Related
 * LOC_ = Location/Movement Related
 * INTERACT_ = Interaction (NPCs, Objects, Dialogue) Related
 * SYS_ = System/Meta Related
 */
public enum WorldStateKey {
    // --- Tutorial Island ---
    // TUT_CURRENT_SECTION, // Keep this for the mapped stage name/ID string
    TUT_STAGE_ID,           // Integer: Store the raw VarPlayer 281 value directly
    TUT_STAGE_NAME,         // String: Store the mapped stage name from JSON
    TUT_ISLAND_COMPLETED,
    // Section 0: Start
    S0_SETTINGS_DONE,       // Boolean
    S0_DOOR_OPEN,           // Boolean

    // Section 1: Survival Expert
    S1_HAS_AXE,             // Boolean
    S1_HAS_LOGS,            // Boolean
    S1_HAS_TINDERBOX,       // Boolean
    S1_IS_FIRE_LIT,         // Boolean
    S1_HAS_FISHING_NET,     // Boolean
    S1_HAS_RAW_SHRIMP,      // Boolean
    S1_HAS_COOKED_SHRIMP,   // Boolean
    S1_SURVIVAL_TASKS_DONE, // Boolean (May be represented by reaching a specific TUT_STAGE_ID)

    // Section 2: Master Chef
    S2_HAS_POT,             // Boolean
    S2_HAS_FLOUR,           // Boolean (Pot of Flour)
    S2_HAS_BUCKET,          // Boolean
    S2_HAS_BUCKET_OF_WATER, // Boolean
    S2_HAS_DOUGH,           // Boolean
    S2_HAS_BREAD,           // Boolean
    S2_COOKING_TASKS_DONE,  // Boolean
    S2_CHEF_DOOR_EXIT_OPEN,

    // Section 3: Quest Guide & Mining/Smithing
    S3_QUEST_TAB_OPENED,    // Boolean (Covered by UI_QUEST_TAB_OPEN)
    S3_CLIMBED_LADDER,      // Boolean (May need specific check or rely on area/stage)
    S3_HAS_PICKAXE,         // Boolean
    S3_PROSPECTED_TIN,      // Boolean (Hard to observe directly, rely on stage ID)
    S3_PROSPECTED_COPPER,   // Boolean (Hard to observe directly, rely on stage ID)
    S3_HAS_TIN_ORE,         // Boolean
    S3_HAS_COPPER_ORE,      // Boolean
    S3_HAS_BRONZE_BAR,      // Boolean
    S3_HAS_HAMMER,          // Boolean
    S3_HAS_DAGGER,          // Boolean (Bronze Dagger)
    S3_MINING_SMITHING_DONE,// Boolean
    S3_MINE_GATE_OPEN,

    // Section 4: Combat Instructor
    S4_EQUIPMENT_TAB_OPENED,// Boolean (Covered by UI_EQUIPMENT_TAB_OPEN)
    S4_EQUIP_STATS_VIEWED,  // Boolean (Covered by UI_EQUIPMENT_STATS_OPEN)
    S4_DAGGER_EQUIPPED,     // Boolean
    S4_KILLED_RAT_MELEE,    // Boolean (Hard to observe directly, rely on stage ID)
    S4_HAS_BOW,             // Boolean
    S4_HAS_ARROWS,          // Boolean
    S4_BOW_EQUIPPED,
    S4_ARROWS_EQUIPPED,
    S4_KILLED_RAT_RANGED,   // Boolean (Hard to observe directly, rely on stage ID)
    S4_COMBAT_TASKS_DONE,   // Boolean
    S4_RAT_GATE_OPEN,

    // Section 5: Financial Advisor
    S5_BANK_VISITED,        // Boolean (Check if bank interface was opened in area?)
    S5_POLL_BOOTH_CHECKED,  // Boolean (Check if poll interface was opened?)
    S5_DOOR_OPEN,           // Boolean (Need specific check for this door)
    S5_FINANCIAL_TASKS_DONE,// Boolean
    S5_FINANCIAL_DOOR_IN_OPEN,
    S5_FINANCIAL_DOOR_OUT_OPEN,

    // Section 6: Brother Brace
    S6_PRAYER_TAB_OPENED,   // Boolean (Covered by UI_PRAYER_TAB_OPEN)
    S6_FRIENDS_TAB_OPENED,  // Boolean (Covered by UI_FRIENDS_TAB_OPEN)
    S6_IGNORE_TAB_OPENED,   // Boolean (Covered by UI_IGNORE_TAB_OPEN)
    S6_DOOR_OPEN,           // Boolean (Need specific check for this door)
    S6_PRAYER_TASKS_DONE,   // Boolean,
    S6_CHURCH_DOOR_OUT_OPEN,

    // Section 7: Magic Instructor
    S7_MAGIC_TAB_OPENED,    // Boolean (Covered by UI_MAGIC_SPELLBOOK_OPEN)
    S7_HAS_AIR_RUNE,        // Boolean
    S7_HAS_MIND_RUNE,       // Boolean
    S7_CAST_WIND_STRIKE,    // Boolean (Hard to observe directly, rely on stage ID)
    S7_KILLED_CHICKEN,      // Boolean (Hard to observe directly, rely on stage ID)
    S7_MAGIC_TASKS_DONE,    // Boolean

    // Section 8: Mainland Arrival
    S8_ARRIVED_MAINLAND,    // Boolean (Check for varp == 1000)

    // --- UI Elements ---
    UI_INVENTORY_OPEN,
    UI_SKILLS_TAB_OPEN,
    UI_MUSIC_TAB_OPEN,
    UI_EQUIPMENT_TAB_OPEN,
    UI_EQUIPMENT_STATS_OPEN, // Specific interface within equipment tab
    UI_COMBAT_OPTIONS_OPEN,
    UI_QUEST_TAB_OPEN,
    UI_PRAYER_TAB_OPEN,
    UI_FRIENDS_TAB_OPEN,
    UI_IGNORE_TAB_OPEN,
    UI_MAGIC_SPELLBOOK_OPEN,
    UI_BANK_OPEN,
    UI_POLL_BOOTH_OPEN,

    // --- Combat ---
    COMBAT_LEVEL, // Integer
    COMBAT_IS_IN_COMBAT, // Boolean
    COMBAT_WEAPON_EQUIPPED, // String/Enum/Integer(ID)

    // --- Skills ---
    SKILL_WOODCUTTING_LEVEL, // Integer
    SKILL_FIREMAKING_LEVEL, // Integer
    SKILL_FISHING_LEVEL,    // Integer
    SKILL_COOKING_LEVEL,    // Integer
    SKILL_MINING_LEVEL,     // Integer
    SKILL_SMITHING_LEVEL,   // Integer
    SKILL_ATTACK_LEVEL,     // Integer
    SKILL_STRENGTH_LEVEL,   // Integer
    SKILL_DEFENCE_LEVEL,    // Integer
    SKILL_RANGED_LEVEL,     // Integer
    SKILL_PRAYER_POINTS,    // Integer
    SKILL_PRAYER_ACTIVE,    // Boolean
    SKILL_MAGIC_LEVEL,      // Integer

    // --- Inventory ---
    INV_SPACE, // Integer
    INV_COINS, // Integer

    // --- Location ---
    LOC_CURRENT_AREA_NAME, // String (e.g., "TUT_AREA_S1_SURVIVAL")
    LOC_IS_WALKING, // Boolean
    LOC_NEAREST_BANK, // Enum/String/Area
    LOC_TARGET_TILE, // Tile

    // --- Interaction ---
    INTERACT_IS_DIALOGUE_OPEN, // Boolean (Specific NPC chat)
    INTERACT_NPC_NAME, // String: Name of NPC currently in dialogue with
    INTERACT_TARGET_NPC, // String: Name of NPC targeted by an action
    INTERACT_TARGET_OBJECT, // String: Name of Object targeted by an action
    INTERACT_TARGET_ITEM, // String: Name of Item targeted by an action
    INTERACT_IS_ANIMATING, // Boolean

    // --- System ---
    SYS_ACCOUNT_AGE_MINUTES, // Integer
    SYS_MOUSE_IDLE_MS, // Long
    SYS_LAST_ACTION_RESULT // ActionResult
}
