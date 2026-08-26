package dev.anvilcraft.addon.morestorage;

import dev.anvilcraft.addon.morestorage.crate.CrateTier;
import dev.anvilcraft.lib.v2.config.BoundedDiscrete;
import dev.anvilcraft.lib.v2.config.CollapsibleObject;
import dev.anvilcraft.lib.v2.config.Comment;
import dev.anvilcraft.lib.v2.config.Config;

/**
 * Per-tier capacity multipliers, relative to AnvilCraft's own crate capacities.
 *
 * <p>Fields are filled in from the config file by AnvilLib's {@code ConfigManager} on every config
 * load; read them through {@link #multiplier(CrateTier)}, which returns the tier default until the
 * config is available (datagen, early registration).
 *
 * <p>Only instance fields may be declared here: {@code ConfigManager} and {@code ConfigData} walk
 * every declared field of this class and of every {@link CollapsibleObject} type, so a static
 * constant would be turned into a config option.
 */
@Config(name = AnvilCraftMoreStorage.MOD_ID)
public class AddonConfig {
    @Comment("Capacity of each crate tier as a multiple of AnvilCraft's own crate capacity")
    @CollapsibleObject
    public CapacityMultipliers capacityMultipliers = new CapacityMultipliers();

    /** The configured multiplier for {@code tier}. */
    public double multiplier(CrateTier tier) {
        return switch (tier) {
            case COPPER -> this.capacityMultipliers.copper;
            case IRON -> this.capacityMultipliers.iron;
            case GOLD -> this.capacityMultipliers.gold;
            case DIAMOND -> this.capacityMultipliers.diamond;
            case EMERALD -> this.capacityMultipliers.emerald;
            case OBSIDIAN -> this.capacityMultipliers.obsidian;
            case NETHERITE -> this.capacityMultipliers.netherite;
        };
    }

    /**
     * The same multiplier applies to a tier's crate and its large crate.
     *
     * <p>The minimum is {@link CrateTier#MIN_MULTIPLIER} because AnvilCraft's storage capacity can
     * only ever be raised, never lowered: a smaller value could not be applied to crates that
     * already exist. Raising a multiplier takes effect for an existing crate the next time its
     * chunk loads.
     */
    public static class CapacityMultipliers {
        @Comment("Capacity of the copper crate relative to the base crate")
        @BoundedDiscrete(min = CrateTier.MIN_MULTIPLIER, max = CrateTier.MAX_MULTIPLIER)
        public double copper = CrateTier.COPPER.defaultMultiplier();

        @Comment("Capacity of the iron crate relative to the base crate")
        @BoundedDiscrete(min = CrateTier.MIN_MULTIPLIER, max = CrateTier.MAX_MULTIPLIER)
        public double iron = CrateTier.IRON.defaultMultiplier();

        @Comment("Capacity of the gold crate relative to the base crate")
        @BoundedDiscrete(min = CrateTier.MIN_MULTIPLIER, max = CrateTier.MAX_MULTIPLIER)
        public double gold = CrateTier.GOLD.defaultMultiplier();

        @Comment("Capacity of the diamond crate relative to the base crate")
        @BoundedDiscrete(min = CrateTier.MIN_MULTIPLIER, max = CrateTier.MAX_MULTIPLIER)
        public double diamond = CrateTier.DIAMOND.defaultMultiplier();

        @Comment("Capacity of the emerald crate relative to the base crate")
        @BoundedDiscrete(min = CrateTier.MIN_MULTIPLIER, max = CrateTier.MAX_MULTIPLIER)
        public double emerald = CrateTier.EMERALD.defaultMultiplier();

        @Comment("Capacity of the obsidian crate relative to the base crate")
        @BoundedDiscrete(min = CrateTier.MIN_MULTIPLIER, max = CrateTier.MAX_MULTIPLIER)
        public double obsidian = CrateTier.OBSIDIAN.defaultMultiplier();

        @Comment("Capacity of the netherite crate relative to the base crate")
        @BoundedDiscrete(min = CrateTier.MIN_MULTIPLIER, max = CrateTier.MAX_MULTIPLIER)
        public double netherite = CrateTier.NETHERITE.defaultMultiplier();
    }
}
