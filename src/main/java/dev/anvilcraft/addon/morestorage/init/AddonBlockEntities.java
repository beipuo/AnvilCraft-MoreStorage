package dev.anvilcraft.addon.morestorage.init;

import dev.anvilcraft.addon.morestorage.crate.CrateTier;
import dev.anvilcraft.addon.morestorage.crate.TieredCrateBlockEntity;
import dev.anvilcraft.addon.morestorage.crate.TieredLargeCrateBlockEntity;
import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntityEntry;

import java.util.EnumMap;
import java.util.Map;

import static dev.anvilcraft.addon.morestorage.AnvilCraftMoreStorage.REGISTRUM;

/**
 * One block entity type per tier and per crate size.
 *
 * <p>A tiered crate cannot reuse AnvilCraft's {@code ModBlockEntities.CRATE}: that type declares
 * AnvilCraft's crate as its only valid block.
 */
public class AddonBlockEntities {
    private static final Map<CrateTier, BlockEntityEntry<TieredCrateBlockEntity>> CRATES =
        new EnumMap<>(CrateTier.class);
    private static final Map<CrateTier, BlockEntityEntry<TieredLargeCrateBlockEntity>> LARGE_CRATES =
        new EnumMap<>(CrateTier.class);

    static {
        for (CrateTier tier : CrateTier.values()) {
            CRATES.put(tier, REGISTRUM
                .<TieredCrateBlockEntity>blockEntity(
                    tier.crateName(),
                    (type, pos, state) -> new TieredCrateBlockEntity(type, pos, state, tier)
                )
                .validBlock(AddonBlocks.crate(tier))
                .register());
            LARGE_CRATES.put(tier, REGISTRUM
                .<TieredLargeCrateBlockEntity>blockEntity(
                    tier.largeCrateName(),
                    (type, pos, state) -> new TieredLargeCrateBlockEntity(type, pos, state, tier)
                )
                .validBlock(AddonBlocks.largeCrate(tier))
                .register());
        }
    }

    private AddonBlockEntities() {
    }

    public static BlockEntityEntry<TieredCrateBlockEntity> crateType(CrateTier tier) {
        return CRATES.get(tier);
    }

    public static BlockEntityEntry<TieredLargeCrateBlockEntity> largeCrateType(CrateTier tier) {
        return LARGE_CRATES.get(tier);
    }

    /** Loads this class, and with it every entry above. */
    public static void register() {
    }
}
