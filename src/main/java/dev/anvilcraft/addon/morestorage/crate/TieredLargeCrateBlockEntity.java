package dev.anvilcraft.addon.morestorage.crate;

import dev.anvilcraft.addon.morestorage.storage.TierCapacity;
import dev.dubhe.anvilcraft.block.entity.storage.LargeCrateBlockEntity;
import dev.dubhe.anvilcraft.saved.storage.LargeCrateStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/** The large crate counterpart of {@link TieredCrateBlockEntity}. */
public class TieredLargeCrateBlockEntity extends LargeCrateBlockEntity implements TieredCrate {
    private final CrateTier tier;

    public TieredLargeCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, CrateTier tier) {
        super(type, pos, state);
        this.tier = tier;
    }

    @Override
    public CrateTier getTier() {
        return this.tier;
    }

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
        TierCapacity.raise(
            this.level,
            this.getId(),
            LargeCrateStorage.class,
            TierCapacity.largeCrateCapacity(this.tier)
        );
    }
}
