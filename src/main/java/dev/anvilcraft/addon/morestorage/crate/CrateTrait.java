package dev.anvilcraft.addon.morestorage.crate;

import javax.annotation.Nullable;

/**
 * The behaviour a tier's crate inherits from the material it is made of.
 *
 * <p>Each one mirrors something the metal itself does in AnvilCraft, as documented in its Ageratum
 * page; the plain metals (zinc, tin, silver, titanium, ...) are documented as having no properties at
 * all, so their crates differ from each other only in capacity. At most one trait applies to a tier.
 */
public enum CrateTrait {
    /** Capacity is the only difference. */
    NONE,

    /**
     * Carrying the crate curses the player, exactly as carrying cursed gold does: Weakness, plus
     * Slowness above 8 and Hunger above 64, cancelled by carrying enchanted gold. Piglins take it as
     * currency. Implemented by AnvilCraft's own {@code ICursed}.
     */
    CURSED,

    /**
     * Cancels the cursed gold effects and grants Luck once 64 are carried, via AnvilCraft's
     * {@code IEnchantedGold} and the {@code anvilcraft:enchanted_gold} item tag that its curse check
     * looks at.
     */
    ENCHANTED_GOLD,

    /** The crate item survives fire and lava, like ember metal tools do. */
    FIREPROOF,

    /**
     * The transcendium property: fireproof, never despawns on the ground, and the block shrugs off
     * explosions. Carried by AnvilCraft's {@code ETERNAL} data component, which also supplies the
     * "Eternal" tooltip line.
     */
    ETERNAL,

    /**
     * Shields adjacent uranium and plutonium blocks from radioactive decay. {@code RadioactiveBlock}
     * counts neighbours in the {@code c:storage_blocks/lead} block tag and raises the decay threshold
     * by one for each, so the crate only has to carry that tag.
     */
    LEAD_SHIELDING;

    /** Whether the crate item should survive fire and lava. */
    public boolean fireResistant() {
        return this == FIREPROOF || this == ETERNAL;
    }

    /**
     * Lang key suffix for the extra tooltip line describing this trait, or null when there is
     * nothing to add — {@link #NONE} has nothing to say and {@link #ETERNAL} already gets a line
     * from AnvilCraft's own property tooltip.
     */
    public @Nullable String tooltipId() {
        return switch (this) {
            case CURSED -> "cursed";
            case ENCHANTED_GOLD -> "enchanted_gold";
            case FIREPROOF -> "fireproof";
            case LEAD_SHIELDING -> "lead_shielding";
            case NONE, ETERNAL -> null;
        };
    }

    /**
     * English text of that line, written into the generated {@code en_us}. The cursed wording is
     * upstream's own, from the tooltip of AnvilCraft's block of cursed gold.
     */
    public @Nullable String tooltipEnglish() {
        return switch (this) {
            case CURSED -> "Carriers will be cursed";
            case ENCHANTED_GOLD -> "Cancels the cursed gold curse; carrying 64 grants Luck";
            case FIREPROOF -> "Survives fire and lava";
            case LEAD_SHIELDING -> "Shields the radioactive blocks it touches from decaying";
            case NONE, ETERNAL -> null;
        };
    }
}
