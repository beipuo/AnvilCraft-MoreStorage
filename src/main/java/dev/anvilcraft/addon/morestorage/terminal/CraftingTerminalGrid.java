package dev.anvilcraft.addon.morestorage.terminal;

import dev.anvilcraft.addon.morestorage.init.AddonComponents;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The nine crafting slots carried by a {@link CraftingTerminal}.
 *
 * <p>Everything here works off the terminal {@link ItemStack} rather than a container: the grid is
 * stored in {@link AddonComponents#CRAFTING_GRID}, which both sides can read. The server mutates the
 * stack in the player's inventory and the change reaches the client through the normal inventory
 * sync, so the screen never has to be told the grid separately.
 */
public final class CraftingTerminalGrid {
    public static final int WIDTH = 3;
    public static final int HEIGHT = 3;
    public static final int SIZE = CraftingTerminalGrid.WIDTH * CraftingTerminalGrid.HEIGHT;

    private CraftingTerminalGrid() {
    }

    /** The grid of {@code terminal}, always {@link #SIZE} entries long. */
    public static NonNullList<ItemStack> read(ItemStack terminal) {
        NonNullList<ItemStack> items = NonNullList.withSize(CraftingTerminalGrid.SIZE, ItemStack.EMPTY);
        terminal.getOrDefault(AddonComponents.CRAFTING_GRID, ItemContainerContents.EMPTY).copyInto(items);
        return items;
    }

    public static void write(ItemStack terminal, List<ItemStack> items) {
        terminal.set(AddonComponents.CRAFTING_GRID, ItemContainerContents.fromItems(items));
    }

    public static boolean isEmpty(List<ItemStack> items) {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * The recipe input for {@code items}. {@link CraftingInput#of} crops empty rows and columns, so
     * a shaped recipe matches wherever in the grid it was laid out — exactly like a crafting table.
     */
    public static CraftingInput input(List<ItemStack> items) {
        return CraftingInput.of(CraftingTerminalGrid.WIDTH, CraftingTerminalGrid.HEIGHT, items);
    }

    /**
     * The same input, keeping the offset the cropping introduced.
     *
     * <p>Anything that has to walk the recipe's slots and get back to a grid index needs
     * {@link CraftingInput.Positioned#left()} and {@link CraftingInput.Positioned#top()}.
     */
    public static CraftingInput.Positioned positioned(List<ItemStack> items) {
        return CraftingInput.ofPositioned(CraftingTerminalGrid.WIDTH, CraftingTerminalGrid.HEIGHT, items);
    }

    public static Optional<RecipeHolder<CraftingRecipe>> recipe(Level level, CraftingInput input) {
        if (input.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
    }

    /** What the grid currently makes, or empty if it matches no recipe. */
    public static ItemStack result(Level level, List<ItemStack> items) {
        CraftingInput input = CraftingTerminalGrid.input(items);
        return CraftingTerminalGrid.recipe(level, input)
            .map(holder -> holder.value().assemble(input, level.registryAccess()))
            .orElse(ItemStack.EMPTY);
    }

    /**
     * Finds the crafting terminal working against {@code targetId} in {@code inventory}.
     *
     * <p>Main hand first, then the offhand, then the rest of the inventory — the same order
     * AnvilCraft uses when it looks for a bound terminal, so the one the player is actually holding
     * wins when several are carried.
     */
    public static ItemStack findTerminal(Inventory inventory, UUID targetId) {
        for (ItemStack stack : List.of(inventory.getSelected(), inventory.offhand.get(0))) {
            if (CraftingTerminalGrid.isCrafter(inventory.player, stack, targetId)) {
                return stack;
            }
        }
        for (ItemStack stack : inventory.items) {
            if (CraftingTerminalGrid.isCrafter(inventory.player, stack, targetId)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Whether {@code stack} is a crafting terminal whose target is {@code targetId}.
     *
     * <p>Which is not the same question for every kind: the hyperdimension one compares the storage
     * bound to that one stack, the local and shulker ones a target derived from the player — so both
     * of theirs match as soon as the player carries any terminal of that kind.
     */
    private static boolean isCrafter(Player player, ItemStack stack, UUID targetId) {
        return stack.getItem() instanceof CraftingTerminal crafter
               && targetId.equals(crafter.craftingTarget(player, stack));
    }
}
