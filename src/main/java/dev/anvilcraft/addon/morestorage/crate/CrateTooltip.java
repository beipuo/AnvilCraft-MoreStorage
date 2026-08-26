package dev.anvilcraft.addon.morestorage.crate;

import dev.anvilcraft.addon.morestorage.AnvilCraftMoreStorage;

/**
 * The tooltip lines AnvilCraft's own crate and large crate show, mirrored for the tiered crates.
 *
 * <p>Upstream drives these from {@code ItemTooltipManager}'s own item to text maps, which are
 * private and offer addons no way to register into, so the addon repeats the convention instead: a
 * {@code tooltip.<namespace>.item.<path>} key shown normally and a {@code .shift} key shown while
 * Shift is held, each a single string whose lines are split on {@code \n}.
 *
 * <p>Where it departs from upstream is that all tiers share one key per crate size instead of one
 * per item: every tier says the same thing, and the only number in the text is passed in as an
 * argument, so per-tier keys would be dozens of identical translations to maintain.
 */
public final class CrateTooltip {
    public static final String CRATE_KEY = key("crate");
    public static final String CRATE_SHIFT_KEY = CRATE_KEY + ".shift";
    public static final String LARGE_CRATE_KEY = key("large_crate");
    public static final String LARGE_CRATE_SHIFT_KEY = LARGE_CRATE_KEY + ".shift";

    /** {@code tooltip.anvilcraft.item.crate} upstream. */
    public static final String CRATE = "Stores items";

    /** {@code tooltip.anvilcraft.item.large_crate} upstream. */
    public static final String LARGE_CRATE = "A large crate, stores more items";

    /**
     * Both sizes share this text.
     *
     * <p>The capacity is a placeholder rather than a baked-in number because it differs per tier and
     * the config can change it, unlike upstream's fixed 2048 / 65536. The 1000 threshold is
     * upstream's own, from {@code BreakBlockEventListener#preventCrateBreak}, which tests for
     * {@code CrateBlock} and {@code LargeCrateBlock} and therefore covers these crates too.
     *
     * <p>Upstream's large crate has one line more — that it can be upgraded to a Shulker Container.
     * That is left out here: {@code Upgrade2ShulkerContainerBehavior} only accepts
     * {@code ModBlocks.LARGE_CRATE}, so a tiered large crate cannot be upgraded.
     */
    public static final String SHIFT = """
        Can contain %s items
        Breaking it drops the contents
        When it holds more than 1000 items, hold Shift to break it""";

    private CrateTooltip() {
    }

    /** Key of the extra line describing a tier's material behaviour. */
    public static String traitKey(String traitId) {
        return "tooltip.%s.trait.%s".formatted(AnvilCraftMoreStorage.MOD_ID, traitId);
    }

    private static String key(String name) {
        return "tooltip.%s.item.%s".formatted(AnvilCraftMoreStorage.MOD_ID, name);
    }
}
