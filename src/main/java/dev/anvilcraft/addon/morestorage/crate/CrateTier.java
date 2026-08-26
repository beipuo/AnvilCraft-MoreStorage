package dev.anvilcraft.addon.morestorage.crate;

import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

import java.util.function.Supplier;

/**
 * The crate tiers this mod adds on top of AnvilCraft's crate and large crate.
 *
 * <p>Declared in ascending capacity order, which is the order the creative tab, the config file and
 * the generated lang files all follow.
 *
 * <p>{@link #defaultMultiplier()} is only the default: the effective multiplier is read from the
 * config at runtime (see {@code AddonConfig}). A multiplier scales the capacity of AnvilCraft's own
 * crate <em>and</em> large crate, so a tier's large crate is always the same multiple of the
 * vanilla large crate as its crate is of the vanilla crate.
 */
public enum CrateTier {
    COPPER("copper", 1.5D, Rarity.COMMON, "minecraft:block/copper_block", () -> Items.COPPER_INGOT, "Copper"),
    IRON("iron", 2.0D, Rarity.COMMON, "minecraft:block/iron_block", () -> Items.IRON_INGOT, "Iron"),
    ZINC("zinc", 3.0D, Rarity.COMMON, "anvilcraft:block/zinc_block", () -> ModItems.ZINC_INGOT.get(), "Zinc"),
    LEAD("lead", 3.0D, Rarity.COMMON, "anvilcraft:block/lead_block", () -> ModItems.LEAD_INGOT.get(), "Lead"),
    SILVER("silver", 3.0D, Rarity.COMMON, "anvilcraft:block/silver_block", () -> ModItems.SILVER_INGOT.get(), "Silver"),
    TIN("tin", 3.0D, Rarity.COMMON, "anvilcraft:block/tin_block", () -> ModItems.TIN_INGOT.get(), "Tin"),
    GOLD("gold", 4.0D, Rarity.UNCOMMON, "minecraft:block/gold_block", () -> Items.GOLD_INGOT, "Golden"),
    CURSED_GOLD("cursed_gold", 4.0D, Rarity.UNCOMMON, "anvilcraft:block/cursed_gold_block", () -> ModItems.CURSED_GOLD_INGOT.get(), "Cursed Gold"),
    ENCHANTED_GOLD("enchanted_gold", 4.5D, Rarity.UNCOMMON, "anvilcraft:block/enchanted_gold_block", () -> ModItems.ENCHANTED_GOLD_INGOT.get(), "Enchanted Gold"),
    DIAMOND("diamond", 6.0D, Rarity.UNCOMMON, "minecraft:block/diamond_block", () -> Items.DIAMOND, "Diamond"),
    EMERALD("emerald", 8.0D, Rarity.RARE, "minecraft:block/emerald_block", () -> Items.EMERALD, "Emerald"),
    OBSIDIAN("obsidian", 10.0D, Rarity.RARE, "minecraft:block/obsidian", () -> Items.OBSIDIAN, "Obsidian"),
    TUNGSTEN("tungsten", 11.0D, Rarity.RARE, "anvilcraft:block/tungsten_block", () -> ModItems.TUNGSTEN_INGOT.get(), "Tungsten"),
    TITANIUM("titanium", 11.0D, Rarity.RARE, "anvilcraft:block/titanium_block", () -> ModItems.TITANIUM_INGOT.get(), "Titanium"),
    NETHERITE("netherite", 12.0D, Rarity.EPIC, "minecraft:block/netherite_block", () -> Items.NETHERITE_INGOT, "Netherite"),
    EMBER_METAL("ember_metal", 12.0D, Rarity.EPIC, "anvilcraft:block/ember_metal_block", () -> ModItems.EMBER_METAL_INGOT.get(), "Ember Metal"),
    ROYAL_STEEL("royal_steel", 14.0D, Rarity.EPIC, "anvilcraft:block/royal_steel_block", () -> ModItems.ROYAL_STEEL_INGOT.get(), "Royal Steel"),
    FROST_METAL("frost_metal", 16.0D, Rarity.EPIC, "anvilcraft:block/frost_metal_block", () -> ModItems.FROST_METAL_INGOT.get(), "Frost Metal"),
    TRANSCENDIUM("transcendium", 18.0D, Rarity.EPIC, "anvilcraft:block/transcendium_block", () -> ModItems.TRANSCENDIUM_INGOT.get(), "Transcendium");

    /** Hard ceiling for a configured multiplier. AnvilCraft stores capacity in an int. */
    public static final double MAX_MULTIPLIER = 1024.0D;

    /**
     * Floor for a configured multiplier. AnvilCraft's
     * {@code SpaceSizeItemStacksResourceHandler#addSpaceSize} refuses to shrink a storage, so a
     * multiplier below 1.0 could never be applied to a crate that already exists.
     */
    public static final double MIN_MULTIPLIER = 1.0D;

    private final String id;
    private final double defaultMultiplier;
    private final Rarity rarity;
    private final String texture;
    private final Supplier<Item> ingredient;
    private final String englishName;

    CrateTier(
        String id,
        double defaultMultiplier,
        Rarity rarity,
        String texture,
        Supplier<Item> ingredient,
        String englishName
    ) {
        this.id = id;
        this.defaultMultiplier = defaultMultiplier;
        this.rarity = rarity;
        this.texture = texture;
        this.ingredient = ingredient;
        this.englishName = englishName;
    }

    public String id() {
        return this.id;
    }

    public double defaultMultiplier() {
        return this.defaultMultiplier;
    }

    public Rarity rarity() {
        return this.rarity;
    }

    /** Block texture used to skin this tier's crate model. */
    public String texture() {
        return this.texture;
    }

    /** The material that upgrades a plain crate into this tier. */
    public Item ingredient() {
        return this.ingredient.get();
    }

    public String crateName() {
        return this.id + "_crate";
    }

    public String largeCrateName() {
        return this.id + "_large_crate";
    }

    /** Display name of this tier's crate, used as the generated {@code en_us} entry. */
    public String crateLang() {
        return this.englishName + " Crate";
    }

    /** Display name of this tier's large crate, used as the generated {@code en_us} entry. */
    public String largeCrateLang() {
        return "Large " + this.englishName + " Crate";
    }
}
