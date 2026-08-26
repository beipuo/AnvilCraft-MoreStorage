package dev.anvilcraft.addon.morestorage.init;

import dev.anvilcraft.addon.morestorage.crate.CrateTier;
import dev.anvilcraft.addon.morestorage.crate.TieredCrateBlock;
import dev.anvilcraft.addon.morestorage.crate.TieredLargeCrateBlock;
import dev.anvilcraft.lib.v2.registrum.providers.DataGenContext;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumBlockstateProvider;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;
import dev.dubhe.anvilcraft.block.container.storage.LargeCrateBlock;
import dev.dubhe.anvilcraft.block.item.SimpleMultiPartBlockItem;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StorageRef;
import net.minecraft.core.BlockPos;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.ModelFile;

import java.util.EnumMap;
import java.util.Map;

import static dev.anvilcraft.addon.morestorage.AnvilCraftMoreStorage.REGISTRUM;

/**
 * Blocks and block items for every {@link CrateTier}.
 *
 * <p>Models, blockstates, loot tables, recipes and the {@code en_us} names are all attached to the
 * builders here and written out by {@code runData} into {@code src/generated/resources}; the only
 * hand-written assets left are the translations Registrum cannot generate.
 */
public class AddonBlocks {
    static {
        REGISTRUM.defaultCreativeTab(AddonItemGroups.MORE_STORAGE.getKey());
    }

    private static final Map<CrateTier, BlockEntry<TieredCrateBlock>> CRATES = new EnumMap<>(CrateTier.class);
    private static final Map<CrateTier, BlockEntry<TieredLargeCrateBlock>> LARGE_CRATES =
        new EnumMap<>(CrateTier.class);

    static {
        for (CrateTier tier : CrateTier.values()) {
            CRATES.put(tier, registerCrate(tier));
            LARGE_CRATES.put(tier, registerLargeCrate(tier));
        }
    }

    private AddonBlocks() {
    }

    public static BlockEntry<TieredCrateBlock> crate(CrateTier tier) {
        return CRATES.get(tier);
    }

    public static BlockEntry<TieredLargeCrateBlock> largeCrate(CrateTier tier) {
        return LARGE_CRATES.get(tier);
    }

    /** Loads this class, and with it every entry above. */
    public static void register() {
    }

    private static BlockEntry<TieredCrateBlock> registerCrate(CrateTier tier) {
        return REGISTRUM
            .block(tier.crateName(), properties -> new TieredCrateBlock(properties, tier))
            // Mirrors AnvilCraft's own crate: plank-like, non-occluding, never a valid spawn spot.
            .initialProperties(() -> Blocks.OAK_PLANKS)
            .properties(properties -> properties.noOcclusion().isValidSpawn(AddonBlocks::never))
            .lang(tier.crateLang())
            .tag(BlockTags.MINEABLE_WITH_AXE)
            .blockstate((ctx, provider) -> provider.simpleBlock(
                ctx.getEntry(),
                crateModel(provider, ctx.getName(), "anvilcraft:block/crate", tier)
            ))
            // The STORAGE component is what makes AnvilCraft assign a storage id on placement, so it
            // has to be present with the matching storage type or the crate never gets a storage.
            .item()
            .properties(properties -> properties
                .rarity(tier.rarity())
                .component(ModComponents.STORAGE, StorageRef.crate()))
            .build()
            .recipe((ctx, provider) -> upgradeRecipe(provider, ctx.get(), ModBlocks.CRATE, tier))
            .register();
    }

    private static BlockEntry<TieredLargeCrateBlock> registerLargeCrate(CrateTier tier) {
        return REGISTRUM
            .block(tier.largeCrateName(), properties -> new TieredLargeCrateBlock(properties, tier))
            .initialProperties(() -> Blocks.OAK_PLANKS)
            .properties(properties -> properties.noOcclusion().isValidSpawn(AddonBlocks::never))
            .lang(tier.largeCrateLang())
            .tag(BlockTags.MINEABLE_WITH_AXE)
            // Only the main part drops, exactly as upstream's large crate does.
            .loot(SimpleMultiPartBlock::loot)
            .blockstate((ctx, provider) -> largeCrateBlockstate(ctx, provider, tier))
            .item((block, properties) -> new SimpleMultiPartBlockItem<Cube3x3PartHalf>(block, properties) {
                @Override
                public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
                    // Required for the merge interaction: without it, sneak-clicking a crate with
                    // this item in hand places the large crate instead of reaching the crate's
                    // useItemOn.
                    return true;
                }
            })
            .properties(properties -> properties
                .stacksTo(16)
                .rarity(tier.rarity())
                .component(ModComponents.STORAGE, StorageRef.largeCrate()))
            .build()
            .recipe((ctx, provider) -> upgradeRecipe(provider, ctx.get(), ModBlocks.LARGE_CRATE, tier))
            .register();
    }

    /**
     * The 3x3x3 large crate is drawn by a single oversized model on its middle-centre part, with
     * every other part contributing nothing but a particle texture — the same split AnvilCraft uses.
     */
    private static void largeCrateBlockstate(
        DataGenContext<Block, TieredLargeCrateBlock> ctx,
        RegistrumBlockstateProvider provider,
        CrateTier tier
    ) {
        ModelFile whole = crateModel(provider, ctx.getName(), "anvilcraft:block/large_crate", tier);
        ModelFile part = provider.models()
            .withExistingParent(ctx.getName() + "_part", ResourceLocation.parse("anvilcraft:block/large_crate_part"))
            .texture("particle", tier.texture());
        var builder = provider.getVariantBuilder(ctx.getEntry());
        for (Cube3x3PartHalf half : Cube3x3PartHalf.values()) {
            builder.partialState()
                .with(LargeCrateBlock.HALF, half)
                .modelForState()
                .modelFile(half == Cube3x3PartHalf.MID_CENTER ? whole : part)
                .addModel();
        }
    }

    /** Reskins one of AnvilCraft's crate models with this tier's material texture. */
    private static ModelFile crateModel(
        RegistrumBlockstateProvider provider,
        String name,
        String parent,
        CrateTier tier
    ) {
        return provider.models()
            .withExistingParent(name, ResourceLocation.parse(parent))
            .texture("top", tier.texture())
            .texture("side", tier.texture())
            .texture("bottom", tier.texture())
            .texture("particle", tier.texture());
    }

    /** A ring of the tier's material around AnvilCraft's own crate of the matching size. */
    private static void upgradeRecipe(
        RegistrumRecipeProvider provider,
        ItemLike result,
        ItemLike base,
        CrateTier tier
    ) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, result)
            .pattern("AAA")
            .pattern("ABA")
            .pattern("AAA")
            .define('A', tier.ingredient())
            .define('B', base)
            .unlockedBy("has_crate", RegistrumRecipeProvider.has(base))
            .unlockedBy("has_" + tier.id(), RegistrumRecipeProvider.has(tier.ingredient()))
            .save(provider);
    }

    private static boolean never(BlockState state, BlockGetter level, BlockPos pos, EntityType<?> entityType) {
        return false;
    }
}
