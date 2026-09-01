package dev.anvilcraft.addon.morestorage.terminal;

import dev.anvilcraft.addon.morestorage.init.AddonComponents;
import net.minecraft.world.item.ItemStack;

/**
 * The three things besides its nine slots that a crafting terminal remembers.
 *
 * <p>All of them live on the terminal stack, for the same reason the grid does: an inventory item is
 * synced to the client by itself, so the screen can read the mode it should draw and the server can
 * read the mode it should apply without either side sending the other a bespoke packet.
 *
 * @see CraftingTerminalGrid
 */
public final class TerminalState {
    private TerminalState() {
    }

    /** Which of the four workbenches {@code terminal} currently is. */
    public static TerminalMode mode(ItemStack terminal) {
        return terminal.getOrDefault(AddonComponents.TERMINAL_MODE, TerminalMode.CRAFTING);
    }

    public static void setMode(ItemStack terminal, TerminalMode mode) {
        terminal.set(AddonComponents.TERMINAL_MODE, mode);
    }

    /** What the anvil mode renames its result to; empty means "leave the name alone". */
    public static String anvilName(ItemStack terminal) {
        return terminal.getOrDefault(AddonComponents.ANVIL_NAME, "");
    }

    public static void setAnvilName(ItemStack terminal, String name) {
        terminal.set(AddonComponents.ANVIL_NAME, name);
    }

    /**
     * Which of the recipes the stonecutter input matches is selected, as an index into the list
     * {@link TerminalRecipes#stonecutting} returns.
     *
     * <p>Out of range counts as nothing selected rather than as an error, which is what makes changing
     * the input safe: the stored index keeps whatever it was and simply stops naming a recipe until the
     * new input has at least that many.
     */
    public static int stonecutterChoice(ItemStack terminal) {
        return terminal.getOrDefault(AddonComponents.STONECUTTER_CHOICE, 0);
    }

    public static void setStonecutterChoice(ItemStack terminal, int choice) {
        terminal.set(AddonComponents.STONECUTTER_CHOICE, choice);
    }
}
