package dev.anvilcraft.addon.morestorage.crate.item;

import dev.dubhe.anvilcraft.block.item.SimpleMultiPartBlockItem;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;

/** The block item of a tiered large crate. */
public class TieredLargeCrateBlockItem extends SimpleMultiPartBlockItem<Cube3x3PartHalf> {
    public TieredLargeCrateBlockItem(SimpleMultiPartBlock<Cube3x3PartHalf> block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        // Required for the merge interaction: without it, sneak-clicking a crate with this item in
        // hand places the large crate instead of reaching the crate's useItemOn.
        return true;
    }
}
