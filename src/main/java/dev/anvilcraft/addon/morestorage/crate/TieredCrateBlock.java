package dev.anvilcraft.addon.morestorage.crate;

import dev.anvilcraft.addon.morestorage.init.AddonBlockEntities;
import dev.anvilcraft.addon.morestorage.init.AddonBlocks;
import dev.dubhe.anvilcraft.block.container.storage.CrateBlock;
import dev.dubhe.anvilcraft.block.container.storage.LargeCrateBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A crate of a single {@link CrateTier}.
 *
 * <p>Everything except the block entity and the merge interaction is inherited from AnvilCraft's
 * {@code CrateBlock}: the storage screen, hammer removal, content dropping and middle-click storage
 * id copying all work unchanged.
 */
public class TieredCrateBlock extends CrateBlock {
    private final CrateTier tier;

    public TieredCrateBlock(Properties properties, CrateTier tier) {
        super(properties);
        this.tier = tier;
    }

    public CrateTier getTier() {
        return this.tier;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return AddonBlockEntities.crateType(this.tier).create(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack itemStack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        if (!player.isSpectator()
            && player.isShiftKeyDown()
            && itemStack.getItem() instanceof BlockItem blockItem
            && blockItem.getBlock() instanceof LargeCrateBlock) {
            if (!AddonBlocks.largeCrate(this.tier).isIn(itemStack)) {
                // Only the large crate of the same tier may absorb these crates. Falling through to
                // AnvilCraft's handler here would move the contents into a large crate of a
                // different capacity, quietly losing this tier's extra space.
                return ItemInteractionResult.FAIL;
            }
            if (level.isClientSide()) {
                return ItemInteractionResult.sidedSuccess(true);
            }
            return TieredCrateMerge.merge(level, pos, itemStack, player, this.tier);
        }
        return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
    }
}
