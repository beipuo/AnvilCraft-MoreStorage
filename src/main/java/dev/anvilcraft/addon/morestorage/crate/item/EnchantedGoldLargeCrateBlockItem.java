package dev.anvilcraft.addon.morestorage.crate.item;

import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.item.abnormal.IEnchantedGold;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The large enchanted gold crate, which cancels the cursed gold effects and grants Luck in bulk.
 *
 * <p>Mirrors AnvilCraft's {@code EnchantedGoldBlockItem}, foil included; the small crate uses that
 * class directly.
 */
public class EnchantedGoldLargeCrateBlockItem extends TieredLargeCrateBlockItem implements IEnchantedGold {
    public EnchantedGoldLargeCrateBlockItem(SimpleMultiPartBlock<Cube3x3PartHalf> block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        IEnchantedGold.super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
