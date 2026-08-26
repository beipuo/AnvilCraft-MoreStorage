package dev.anvilcraft.addon.morestorage.crate;

import dev.anvilcraft.addon.morestorage.init.AddonBlockEntities;
import dev.dubhe.anvilcraft.block.container.storage.LargeCrateBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A 3x3x3 large crate of a single {@link CrateTier}.
 *
 * <p>The multipart behaviour — placement of all 27 parts, shapes, loot from the main part only —
 * is inherited from AnvilCraft's {@code LargeCrateBlock} unchanged.
 */
public class TieredLargeCrateBlock extends LargeCrateBlock {
    private final CrateTier tier;

    public TieredLargeCrateBlock(Properties properties, CrateTier tier) {
        super(properties);
        this.tier = tier;
    }

    public CrateTier getTier() {
        return this.tier;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return AddonBlockEntities.largeCrateType(this.tier).create(pos, state);
    }
}
