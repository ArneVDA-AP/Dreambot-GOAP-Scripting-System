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
    TUT_CURRENT_SECTION,// Enum/String: Current section (e.g., "S0_Start", "S1_Survival")

    // Section 0: Start
    S0_SETTINGS_DONE,       // Boolean
    S0_DOOR_OPEN,           // Boolean
    TUT_S0_READY_FOR_DOOR,  // Boolean
    TUT_ISLAND_COMPLETED,   // Boolean

    // Section 1: Survival Expert
    S1_HAS_AXE,             // Boolean
    S1_HAS_LOGS,            // Boolean
    S1_HAS_TINDERBOX,       // Boolean
    S1_IS_FIRE_LIT,         // Boolean
    S1_HAS_FISHING_NET,     // Boolean
    S1_HAS_RAW_SHRIMP,      // Boolean
    S1_HAS_COOKED_SHRIMP,   // Boolean
    S1_SURVIVAL_TASKS_DONE, // Boolean (Aggregate state for section completion)

    // Section 2: Master Chef
    S2_HAS_POT,             // Boolean
    S2_HAS_FLOUR,           // Boolean (Pot of Flour)
    S2_HAS_BUCKET,          // Boolean
    S2_HAS_BUCKET_OF_WATER, // Boolean
    S2_HAS_DOUGH,           // Boolean
    S2_HAS_BREAD,           // Boolean
    S2_COOKING_TASKS_DONE,  // Boolean

    // Section 3: Quest Guide & Mining/Smithing
    S3_QUEST_TAB_OPENED,    // Boolean
    S3_CLIMBED_LADDER,      // Boolean
    S3_HAS_PICKAXE,         // Boolean
    S3_PROSPECTED_TIN,      // Boolean
    S3_PROSPECTED_COPPER,   // Boolean
    S3_HAS_TIN_ORE,         // Boolean
    S3_HAS_COPPER_ORE,      // Boolean
    S3_HAS_BRONZE_BAR,      // Boolean
    S3_HAS_HAMMER,          // Boolean
    S3_HAS_DAGGER,          // Boolean (Bronze Dagger)
    S3_MINING_SMITHING_DONE,// Boolean

    // Section 4: Combat Instructor
    S4_EQUIPMENT_TAB_OPENED,// Boolean
    S4_DAGGER_EQUIPPED,     // Boolean
    S4_KILLED_RAT_MELEE,    // Boolean
    S4_HAS_BOW,             // Boolean
    S4_HAS_ARROWS,          // Boolean
    S4_KILLED_RAT_RANGED,   // Boolean
    S4_COMBAT_TASKS_DONE,   // Boolean

    // Section 5: Financial Advisor
    S5_BANK_VISITED,        // Boolean
    S5_POLL_BOOTH_CHECKED,  // Boolean
    S5_DOOR_OPEN,           // Boolean
    S5_FINANCIAL_TASKS_DONE,// Boolean

    // Section 6: Brother Brace
    S6_PRAYER_TAB_OPENED,   // Boolean
    S6_PRAYER_POINTS,       // Integer (Covered by SKILL_PRAYER_POINTS)
    S6_FRIENDS_TAB_OPENED,  // Boolean
    S6_DOOR_OPEN,           // Boolean
    S6_PRAYER_TASKS_DONE,   // Boolean

    // Section 7: Magic Instructor
    S7_MAGIC_TAB_OPENED,    // Boolean
    S7_HAS_AIR_RUNE,        // Boolean
    S7_HAS_MIND_RUNE,       // Boolean
    S7_CAST_WIND_STRIKE,    // Boolean
    S7_KILLED_CHICKEN,      // Boolean
    S7_MAGIC_TASKS_DONE,    // Boolean

    // Section 8: Mainland Arrival
    S8_ARRIVED_MAINLAND,    // Boolean (Final state)

    // --- Combat ---
    COMBAT_LEVEL, // Integer
    COMBAT_IS_IN_COMBAT, // Boolean
    COMBAT_WEAPON_EQUIPPED, // String/Enum/Integer(ID)

    // --- Skills ---
    SKILL_WOODCUTTING_LEVEL, // Integer (Added for S1)
    SKILL_FIREMAKING_LEVEL, // Integer (Added for S1)
    SKILL_FISHING_LEVEL,    // Integer (Added for S1)
    SKILL_COOKING_LEVEL,    // Integer
    SKILL_MINING_LEVEL,     // Integer
    SKILL_SMITHING_LEVEL,   // Integer
    SKILL_ATTACK_LEVEL,     // Integer (Added for S4)
    SKILL_STRENGTH_LEVEL,   // Integer (Added for S4)
    SKILL_DEFENCE_LEVEL,    // Integer (Added for S4)
    SKILL_RANGED_LEVEL,     // Integer (Added for S4)
    SKILL_PRAYER_POINTS,    // Integer
    SKILL_PRAYER_ACTIVE,    // Boolean
    SKILL_MAGIC_LEVEL,      // Integer

    // --- Inventory ---
    INV_SPACE, // Integer
    INV_COINS, // Integer

    // --- Location ---
    LOC_CURRENT_AREA, // Enum/String (e.g., "TUT_S0_START_ROOM", "TUT_S1_SURVIVAL_AREA")
    LOC_IS_WALKING, // Boolean
    LOC_NEAREST_BANK, // Enum/String/Area
    LOC_TARGET_TILE, // Tile

    // --- Interaction ---
    INTERACT_IS_DIALOGUE_OPEN, // Boolean
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
