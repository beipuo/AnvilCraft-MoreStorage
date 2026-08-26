package dev.anvilcraft.addon.morestorage.crate;

import dev.anvilcraft.addon.morestorage.AnvilCraftMoreStorage;

/**
 * The tooltip lines AnvilCraft's own crate and large crate show, mirrored for the tiered crates.
 *
 * <p>Upstream drives these from {@code ItemTooltipManager}'s own item to text maps, which are
 * private and offer addons no way to register into, so the addon repeats the convention instead: a
 * {@code tooltip.<namespace>.item.<path>} key shown normally and a {@code .shift} key shown while
 * Shift is held, each a single string whose lines are split on {@code \n}.
 */
public final class CrateTooltip {
    /** {@code tooltip.anvilcraft.item.crate} upstream. */
    public static final String CRATE = "Stores items";

    /** {@code tooltip.anvilcraft.item.large_crate} upstream. */
    public static final String LARGE_CRATE = "A large crate, stores more items";

    /**
     * Both sizes share this text.
     *
     * <p>The capacity is a placeholder rather than a baked-in number because the config can change
     * it, unlike upstream's fixed 2048 / 65536. The 1000 threshold is upstream's own, from
     * {@code BreakBlockEventListener#preventCrateBreak}, which tests for {@code CrateBlock} and
     * {@code LargeCrateBlock} and therefore covers these crates too.
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

    public static String key(String itemName) {
        return "tooltip.%s.item.%s".formatted(AnvilCraftMoreStorage.MOD_ID, itemName);
    }

    public static String shiftKey(String itemName) {
        return key(itemName) + ".shift";
    }
}
