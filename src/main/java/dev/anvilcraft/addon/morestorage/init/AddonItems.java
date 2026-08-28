package dev.anvilcraft.addon.morestorage.init;

import dev.anvilcraft.addon.morestorage.terminal.HyperdimensionCraftingTerminalItem;
import dev.anvilcraft.addon.morestorage.terminal.LocalCraftingTerminalItem;
import dev.anvilcraft.addon.morestorage.terminal.ShulkerCraftingTerminalItem;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.registrum.util.entry.ItemEntry;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.Eternal;
import dev.dubhe.anvilcraft.item.property.component.TerminalBinding;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;

import static dev.anvilcraft.addon.morestorage.AnvilCraftMoreStorage.REGISTRUM;

/**
 * Items this addon adds on top of AnvilCraft's own.
 */
public class AddonItems {
    static {
        REGISTRUM.defaultCreativeTab(AddonItemGroups.MORE_STORAGE.getKey());
    }

    /**
     * A hyperdimension terminal that can also craft.
     *
     * <p>The properties are AnvilCraft's terminal verbatim — same rarity, same fire resistance, the
     * same {@code ETERNAL} and {@code TERMINAL_BINDING} components, because the station's binding
     * code and the storage RPC both read them — plus the nine crafting slots this addon adds.
     */
    public static final ItemEntry<HyperdimensionCraftingTerminalItem> HYPERDIMENSION_CRAFTING_TERMINAL = REGISTRUM
        .item("hyperdimension_crafting_terminal", HyperdimensionCraftingTerminalItem::new)
        .properties(properties -> properties
            .stacksTo(1)
            .fireResistant()
            .rarity(Rarity.EPIC)
            .component(ModComponents.ETERNAL, Eternal.INSTANCE)
            .component(ModComponents.TERMINAL_BINDING, TerminalBinding.EMPTY)
            .component(AddonComponents.CRAFTING_GRID, ItemContainerContents.EMPTY))
        .lang("Hyperdimension Crafting Terminal")
        .model((ctx, provider) -> provider.generated(ctx.lazy()))
        // A crafting table bolted onto a finished terminal, which is exactly what the item is.
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("A")
            .pattern("B")
            .define('A', Blocks.CRAFTING_TABLE)
            .define('B', ModItems.HYPERDIMENSION_TERMINAL)
            .unlockedBy("has_hyperdimension_terminal", RegistrumRecipeProvider.has(ModItems.HYPERDIMENSION_TERMINAL))
            .save(provider))
        .register();

    /**
     * A local terminal that can also craft.
     *
     * <p>The properties are AnvilCraft's own local terminal minus its {@code TERMINAL_BALANCE_MODE}:
     * item balancing looks the connected storage up from a fixed list of terminal items, so the
     * component would sit there inert on a subclass. The nine crafting slots are what this addon adds.
     */
    public static final ItemEntry<LocalCraftingTerminalItem> LOCAL_CRAFTING_TERMINAL = REGISTRUM
        .item("local_crafting_terminal", LocalCraftingTerminalItem::new)
        .properties(properties -> properties
            .stacksTo(1)
            .component(AddonComponents.CRAFTING_GRID, ItemContainerContents.EMPTY))
        .lang("Local Crafting Terminal")
        .model((ctx, provider) -> provider.generated(ctx.lazy()))
        // A crafting table bolted onto a finished terminal, which is exactly what the item is.
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("A")
            .pattern("B")
            .define('A', Blocks.CRAFTING_TABLE)
            .define('B', ModItems.LOCAL_TERMINAL)
            .unlockedBy("has_local_terminal", RegistrumRecipeProvider.has(ModItems.LOCAL_TERMINAL))
            .save(provider))
        .register();

    /** A shulker terminal that can also craft; same reasoning as {@link #LOCAL_CRAFTING_TERMINAL}. */
    public static final ItemEntry<ShulkerCraftingTerminalItem> SHULKER_CRAFTING_TERMINAL = REGISTRUM
        .item("shulker_crafting_terminal", ShulkerCraftingTerminalItem::new)
        .properties(properties -> properties
            .stacksTo(1)
            .component(AddonComponents.CRAFTING_GRID, ItemContainerContents.EMPTY))
        .lang("Shulker Crafting Terminal")
        .model((ctx, provider) -> provider.generated(ctx.lazy()))
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
            .pattern("A")
            .pattern("B")
            .define('A', Blocks.CRAFTING_TABLE)
            .define('B', ModItems.SHULKER_TERMINAL)
            .unlockedBy("has_shulker_terminal", RegistrumRecipeProvider.has(ModItems.SHULKER_TERMINAL))
            .save(provider))
        .register();

    private AddonItems() {
    }

    /** Loads this class, and with it every entry above. */
    public static void register() {
    }
}
