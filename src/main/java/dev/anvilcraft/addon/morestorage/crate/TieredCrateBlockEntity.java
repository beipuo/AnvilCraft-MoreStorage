package dev.anvilcraft.addon.morestorage.crate;

import dev.anvilcraft.addon.morestorage.init.AddonBlockEntities;
import dev.anvilcraft.addon.morestorage.storage.TierCapacity;
import dev.dubhe.anvilcraft.block.entity.storage.CrateBlockEntity;
import dev.dubhe.anvilcraft.saved.storage.CrateStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * A crate whose backing storage is widened to its tier's capacity.
 *
 * <p>Extending {@link CrateBlockEntity} rather than {@code StorageBlockEntity} directly is what
 * makes tiered crates interchangeable with AnvilCraft's own: both
 * {@code CrateBlock#getNearbyCrates} and AnvilCraft's storage screen test for
 * {@code instanceof CrateBlockEntity}, so a tiered crate joins the 3x3x3 neighbour group and shares
 * its contents with the plain crates around it without any further wiring.
 *
 * @see AddonBlockEntities
 */
public class TieredCrateBlockEntity extends CrateBlockEntity implements TieredCrate {
    private final CrateTier tier;

    public TieredCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, CrateTier tier) {
        super(type, pos, state);
        this.tier = tier;
    }

    @Override
    public CrateTier getTier() {
        return this.tier;
    }

    /**
     * AnvilCraft creates the backing storage lazily, from whichever of these happens first: the
     * block item's STORAGE component being applied on placement, an item handler capability lookup,
     * or a player opening the screen. All three route through {@code setId}, and a chunk reload
     * routes through {@code onLoad}, so hooking both covers every path in and also re-applies a
     * raised config multiplier to crates that already exist.
     */
    @Override
    public void setId(UUID id) {
        super.setId(id);
        this.applyTierCapacity();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        this.applyTierCapacity();
    }

    @Override
    public void applyTierCapacity() {
        TierCapacity.raise(this.level, this.getId(), CrateStorage.class, TierCapacity.crateCapacity(this.tier));
    }
}
