package dev.anvilcraft.addon.morestorage.crate.item;

import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.item.abnormal.ICursed;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The large cursed gold crate, which curses whoever carries it.
 *
 * <p>The delegation to {@code ICursed.super} is AnvilCraft's own pattern, copied from its
 * {@code CursedBlockItem} — which the small crate can use directly, while a large crate has to be a
 * {@link TieredLargeCrateBlockItem} and therefore needs its own class.
 */
public class CursedLargeCrateBlockItem extends TieredLargeCrateBlockItem implements ICursed {
    public CursedLargeCrateBlockItem(SimpleMultiPartBlock<Cube3x3PartHalf> block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        ICursed.super.inventoryTick(stack, level, entity, slotId, isSelected);
    }
}
