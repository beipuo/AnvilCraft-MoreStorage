package dev.anvilcraft.addon.morestorage.init;

import dev.anvilcraft.addon.morestorage.AnvilCraftMoreStorage;
import dev.anvilcraft.addon.morestorage.crate.CrateTier;
import dev.anvilcraft.addon.morestorage.crate.TieredCrate;
import dev.anvilcraft.addon.morestorage.crate.TieredLargeCrateBlock;
import dev.anvilcraft.addon.morestorage.crate.TieredLargeCrateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.UUID;

/**
 * Exposes each tiered crate's storage as an item handler, so hoppers, pipes and other automation see
 * it the same way they see AnvilCraft's own crates. AnvilCraft registers this per block entity type,
 * so its registration does not cover ours.
 */
@EventBusSubscriber(modid = AnvilCraftMoreStorage.MOD_ID)
public final class AddonCapabilities {
    private AddonCapabilities() {
    }

    @SubscribeEvent
    static void register(RegisterCapabilitiesEvent event) {
        for (CrateTier tier : CrateTier.values()) {
            event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                AddonBlockEntities.crateType(tier).get(),
                (blockEntity, side) -> itemHandler(blockEntity)
            );
            // A large crate only carries a block entity on its main part, so the capability has to be
            // registered on the block and resolved back to that part.
            event.registerBlock(
                Capabilities.ItemHandler.BLOCK,
                (level, pos, state, blockEntity, side) -> {
                    if (!(state.getBlock() instanceof TieredLargeCrateBlock largeCrate)) {
                        return null;
                    }
                    if (level.getBlockEntity(largeCrate.getMainPartPos(pos, state))
                        instanceof TieredLargeCrateBlockEntity mainPart) {
                        return itemHandler(mainPart);
                    }
                    return null;
                },
                AddonBlocks.largeCrate(tier).get()
            );
        }
    }

    private static <T extends StorageBlockEntity & TieredCrate> IItemHandler itemHandler(T blockEntity) {
        UUID id = blockEntity.getId();
        if (id == null) {
            id = UUID.randomUUID();
            blockEntity.setId(id);
        }
        // Idempotent, and guarantees the tier capacity is in place before automation writes to it.
        blockEntity.applyTierCapacity();
        return Storages.get().getOrCreate(id, blockEntity.getStorageType().clazz()).getItems();
    }
}
