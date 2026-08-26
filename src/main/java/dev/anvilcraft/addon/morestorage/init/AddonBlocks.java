package dev.anvilcraft.addon.morestorage.init;

import dev.anvilcraft.addon.morestorage.crate.CrateTier;
import dev.anvilcraft.addon.morestorage.crate.CrateTrait;
import dev.anvilcraft.addon.morestorage.crate.TieredCrateBlock;
import dev.anvilcraft.addon.morestorage.crate.TieredLargeCrateBlock;
import dev.anvilcraft.addon.morestorage.crate.item.CursedLargeCrateBlockItem;
import dev.anvilcraft.addon.morestorage.crate.item.EnchantedGoldLargeCrateBlockItem;
import dev.anvilcraft.addon.morestorage.crate.item.TieredLargeCrateBlockItem;
import dev.anvilcraft.lib.v2.registrum.Registrum;
import dev.anvilcraft.lib.v2.registrum.builders.BlockBuilder;
import dev.anvilcraft.lib.v2.registrum.builders.ItemBuilder;
import dev.anvilcraft.lib.v2.registrum.providers.DataGenContext;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumBlockstateProvider;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;
import dev.anvilcraft.lib.v2.util.nullness.NonNullBiFunction;
import dev.dubhe.anvilcraft.block.container.storage.LargeCrateBlock;
import dev.dubhe.anvilcraft.block.item.CursedBlockItem;
import dev.dubhe.anvilcraft.block.item.EnchantedGoldBlockItem;
import dev.dubhe.anvilcraft.block.item.SimpleMultiPartBlockItem;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.item.property.component.Eternal;
import dev.dubhe.anvilcraft.item.property.component.StorageRef;
import net.minecraft.core.BlockPos;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
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
        BlockBuilder<TieredCrateBlock, Registrum> block = REGISTRUM
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
            .recipe((ctx, provider) -> upgradeRecipe(provider, ctx.get(), ModBlocks.CRATE, tier));
        applyBlockTrait(block, tier.trait());
        // The STORAGE component is what makes AnvilCraft assign a storage id on placement, so it has
        // to be present with the matching storage type or the crate never gets a storage.
        ItemBuilder<BlockItem, BlockBuilder<TieredCrateBlock, Registrum>> item = block
            .item(crateItemFactory(tier.trait()))
            .properties(properties -> properties
                .rarity(tier.rarity())
                .component(ModComponents.STORAGE, StorageRef.crate()));
        applyItemTrait(item, tier.trait());
        return item.build().register();
    }

    private static BlockEntry<TieredLargeCrateBlock> registerLargeCrate(CrateTier tier) {
        BlockBuilder<TieredLargeCrateBlock, Registrum> block = REGISTRUM
            .block(tier.largeCrateName(), properties -> new TieredLargeCrateBlock(properties, tier))
            .initialProperties(() -> Blocks.OAK_PLANKS)
            .properties(properties -> properties.noOcclusion().isValidSpawn(AddonBlocks::never))
            .lang(tier.largeCrateLang())
            .tag(BlockTags.MINEABLE_WITH_AXE)
            // Only the main part drops, exactly as upstream's large crate does.
            .loot(SimpleMultiPartBlock::loot)
            .blockstate((ctx, provider) -> largeCrateBlockstate(ctx, provider, tier))
            .recipe((ctx, provider) -> upgradeRecipe(provider, ctx.get(), ModBlocks.LARGE_CRATE, tier));
        applyBlockTrait(block, tier.trait());
        ItemBuilder<SimpleMultiPartBlockItem<Cube3x3PartHalf>, BlockBuilder<TieredLargeCrateBlock, Registrum>> item =
            block
                .item(largeCrateItemFactory(tier.trait()))
                .properties(properties -> properties
                    .stacksTo(16)
                    .rarity(tier.rarity())
                    .component(ModComponents.STORAGE, StorageRef.largeCrate()));
        applyItemTrait(item, tier.trait());
        return item.build().register();
    }

    /** The block half of a tier's material behaviour. */
    private static void applyBlockTrait(BlockBuilder<?, ?> block, CrateTrait trait) {
        if (trait == CrateTrait.ETERNAL) {
            // Eternal transcendium gear shrugs off explosions; so does a crate built from it.
            block.properties(properties -> properties.explosionResistance(Float.MAX_VALUE));
        }
        if (trait == CrateTrait.LEAD_SHIELDING) {
            // The only thing RadioactiveBlock looks at when counting shielding neighbours. Left off
            // the matching item tag on purpose: that one feeds recipes which would consume the crate.
            block.tag(ModBlockTags.STORAGE_BLOCKS_LEAD);
        }
    }

    /** The item half of a tier's material behaviour. */
    private static void applyItemTrait(ItemBuilder<?, ?> item, CrateTrait trait) {
        if (trait.fireResistant()) {
            item.properties(Item.Properties::fireResistant);
        }
        if (trait == CrateTrait.ETERNAL) {
            // Carries the no-despawn behaviour and the "Eternal" tooltip line with it.
            item.properties(properties -> properties.component(ModComponents.ETERNAL, Eternal.INSTANCE));
        }
        if (trait == CrateTrait.ENCHANTED_GOLD) {
            // What ICursed checks to decide the carrier is protected from the curse.
            item.tag(ModItemTags.ENCHANTED_GOLD);
        }
    }

    private static NonNullBiFunction<TieredCrateBlock, Item.Properties, BlockItem> crateItemFactory(CrateTrait trait) {
        return switch (trait) {
            case CURSED -> CursedBlockItem::new;
            case ENCHANTED_GOLD -> EnchantedGoldBlockItem::new;
            default -> BlockItem::new;
        };
    }

    private static NonNullBiFunction<
        TieredLargeCrateBlock,
        Item.Properties,
        SimpleMultiPartBlockItem<Cube3x3PartHalf>
        > largeCrateItemFactory(CrateTrait trait) {
        return switch (trait) {
            case CURSED -> CursedLargeCrateBlockItem::new;
            case ENCHANTED_GOLD -> EnchantedGoldLargeCrateBlockItem::new;
            default -> TieredLargeCrateBlockItem::new;
        };
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
