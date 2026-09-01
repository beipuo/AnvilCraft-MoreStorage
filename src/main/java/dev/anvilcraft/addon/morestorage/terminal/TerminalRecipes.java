package dev.anvilcraft.addon.morestorage.terminal;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * What each terminal mode makes out of its inputs.
 *
 * <p>One place for all four, because both sides need the same answer: the screen draws it into the
 * result slot every time the inputs change, and the server recomputes it when the player clicks that
 * slot. Nothing here mutates the inputs — taking the result is the server's business.
 *
 * <p>Three of the modes are a recipe lookup. The anvil is not: its result comes out of a real
 * {@link AnvilMenu}, built on the spot and thrown away. That is deliberate — repairing, combining
 * enchantments, the level cost and the "too expensive" ceiling are a hundred lines of rules that would
 * drift out of step the moment vanilla or another mod changed one of them, and an anvil menu already
 * has them right. It is given {@link ContainerLevelAccess#NULL} so that taking the result cannot damage
 * an anvil that is not there.
 *
 * <p>The screen builds one of those menus too, to preview the result before anything is clicked, which
 * is the one place this differs from a real anvil: vanilla's client is told the result by the server
 * rather than working it out. A mod that adds anvil recipes from a server-only handler would therefore
 * preview as making nothing and then make something anyway — wrong in the harmless direction, since the
 * server is still the only side that decides.
 */
public final class TerminalRecipes {
    private TerminalRecipes() {
    }

    /**
     * What the terminal currently makes.
     *
     * @param result   the item the result slot shows, empty when the inputs make nothing
     * @param cost     experience levels taking it costs; only the anvil ever charges any
     * @param takeable whether the player may actually take it — false for an anvil result they cannot
     *                 afford, which is still drawn, with its cost in red
     */
    public record Outcome(ItemStack result, int cost, boolean takeable) {
        public static final Outcome EMPTY = new Outcome(ItemStack.EMPTY, 0, false);
    }

    /**
     * The outcome for {@code inputs} in {@code mode}.
     *
     * <p>{@code inputs} is the terminal's whole nine-slot grid; each mode reads only the slots it uses.
     */
    public static Outcome outcome(
        Player player,
        TerminalMode mode,
        List<ItemStack> inputs,
        String anvilName,
        int stonecutterChoice
    ) {
        Level level = player.level();
        return switch (mode) {
            case CRAFTING -> TerminalRecipes.plain(CraftingTerminalGrid.result(level, inputs));
            case STONECUTTING -> {
                List<RecipeHolder<StonecutterRecipe>> recipes = TerminalRecipes.stonecutting(level, inputs.get(0));
                yield stonecutterChoice < 0 || stonecutterChoice >= recipes.size()
                    ? Outcome.EMPTY
                    : TerminalRecipes.plain(TerminalRecipes.assembleStonecutting(
                        level,
                        recipes.get(stonecutterChoice),
                        inputs.get(0)
                    ));
            }
            case SMITHING -> TerminalRecipes.plain(TerminalRecipes.smithing(level, inputs)
                .map(holder -> holder.value().assemble(TerminalRecipes.smithingInput(inputs), level.registryAccess()))
                .orElse(ItemStack.EMPTY));
            case ANVIL -> {
                if (inputs.get(0).isEmpty()) {
                    yield Outcome.EMPTY;
                }
                AnvilMenu menu = TerminalRecipes.anvilMenu(player, inputs.get(0), inputs.get(1), anvilName);
                ItemStack result = menu.getSlot(AnvilMenu.RESULT_SLOT).getItem();
                yield result.isEmpty()
                    ? Outcome.EMPTY
                    : new Outcome(result, menu.getCost(), menu.getSlot(AnvilMenu.RESULT_SLOT).mayPickup(player));
            }
        };
    }

    /** An outcome that costs nothing, which is every mode but the anvil. */
    private static Outcome plain(ItemStack result) {
        return result.isEmpty() ? Outcome.EMPTY : new Outcome(result, 0, true);
    }

    /**
     * The stonecutting recipes {@code input} matches, in the order the terminal lists them.
     *
     * <p>The order is the recipe manager's own — sorted by the result's description id — so the index
     * the client sends means the same recipe the server resolves without either having to send a list.
     */
    public static List<RecipeHolder<StonecutterRecipe>> stonecutting(Level level, ItemStack input) {
        if (input.isEmpty()) {
            return List.of();
        }
        return level.getRecipeManager().getRecipesFor(RecipeType.STONECUTTING, new SingleRecipeInput(input), level);
    }

    public static ItemStack assembleStonecutting(
        Level level,
        RecipeHolder<StonecutterRecipe> recipe,
        ItemStack input
    ) {
        return recipe.value().assemble(new SingleRecipeInput(input), level.registryAccess());
    }

    /** The smithing recipe the first three slots match, if any. */
    public static Optional<RecipeHolder<SmithingRecipe>> smithing(Level level, List<ItemStack> inputs) {
        SmithingRecipeInput input = TerminalRecipes.smithingInput(inputs);
        if (input.base().isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, level);
    }

    /** Template, base, addition — the smithing table's own slot order, left to right. */
    public static SmithingRecipeInput smithingInput(List<ItemStack> inputs) {
        return new SmithingRecipeInput(inputs.get(0), inputs.get(1), inputs.get(2));
    }

    /**
     * A throwaway anvil holding {@code base} and {@code addition}, already renamed and evaluated.
     *
     * <p>Setting either input slot is what makes the menu recompute, so the name has to be in place
     * first; the explicit {@code createResult} is there so the outcome does not depend on that.
     *
     * <p>The caller may go on to take the result with
     * {@code menu.getSlot(AnvilMenu.RESULT_SLOT).onTake(player, result)}, which charges the levels and
     * consumes the inputs exactly as an anvil would, then read the leftovers back out of slots 0 and 1.
     */
    public static AnvilMenu anvilMenu(Player player, ItemStack base, ItemStack addition, String anvilName) {
        AnvilMenu menu = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.NULL);
        menu.setItemName(anvilName);
        menu.getSlot(AnvilMenu.INPUT_SLOT).set(base.copy());
        menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(addition.copy());
        menu.createResult();
        return menu;
    }
}
