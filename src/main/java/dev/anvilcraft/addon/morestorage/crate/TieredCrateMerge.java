package dev.anvilcraft.addon.morestorage.crate;

import dev.anvilcraft.addon.morestorage.init.AddonBlocks;
import dev.anvilcraft.addon.morestorage.storage.TierCapacity;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.block.container.storage.LargeCrateBlock;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.storage.ModStorageTypes;
import dev.dubhe.anvilcraft.item.property.component.StorageRef;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.LargeCrateStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Tier-aware version of AnvilCraft's crate-to-large-crate merge.
 *
 * <p>AnvilCraft keeps its own merge private and hardcodes its own two blocks, so this mirrors it for
 * a single tier: all 27 crates must be of {@code tier}, and the resulting large crate is that tier's.
 * As upstream does, the 27 crates are returned to the player and the large crate item is consumed —
 * the point of the interaction is moving the pooled contents into one storage.
 */
public final class TieredCrateMerge {
    private TieredCrateMerge() {
    }

    static ItemInteractionResult merge(
        Level level,
        BlockPos clicked,
        ItemStack largeCrateStack,
        Player player,
        CrateTier tier
    ) {
        BlockPos origin = findOrigin(level, clicked, tier);
        if (origin == null) {
            return ItemInteractionResult.FAIL;
        }

        List<TieredCrateBlockEntity> crates = new ArrayList<>();
        for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
            if (!(level.getBlockEntity(origin.offset(part.getOffset())) instanceof TieredCrateBlockEntity crate)) {
                return ItemInteractionResult.FAIL;
            }
            crates.add(crate);
        }

        StorageRef ref = largeCrateStack.get(ModComponents.STORAGE);
        UUID targetId = ref != null && ref.type().is(ModStorageTypes.LARGE_CRATE.getKey())
            ? ref.id().orElseGet(UUID::randomUUID)
            : UUID.randomUUID();
        LargeCrateStorage target = Storages.get().get(targetId, LargeCrateStorage.class)
            .orElseGet(() -> new LargeCrateStorage(targetId));
        // Widen before transferring, or the fit check below measures the base capacity.
        int capacity = TierCapacity.largeCrateCapacity(tier);
        target.getItems().addSpaceSize(size -> capacity);

        UnlimitedItemStacksResourceHandler targetItems = target.getItems();
        Set<UUID> sourceIds = new HashSet<>();
        List<UnlimitedItemStack> toTransfer = new ArrayList<>();
        for (TieredCrateBlockEntity crate : crates) {
            UUID sourceId = crate.getId();
            if (sourceId == null || !sourceIds.add(sourceId)) {
                continue;
            }
            Optional<BaseStorage<?>> source = Storages.get().get(sourceId);
            if (source.isEmpty()) {
                continue;
            }
            UnlimitedItemStacksResourceHandler items = source.get().getItems();
            for (int slot = 0; slot < items.size(); slot++) {
                UnlimitedItemStack stack = items.getUnlimitedStackInSlot(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                if (!targetItems.insertItem(stack.toStack(), true).isEmpty()) {
                    return ItemInteractionResult.FAIL;
                }
                toTransfer.add(stack);
            }
        }
        for (UnlimitedItemStack stack : toTransfer) {
            targetItems.insertItem(stack.toStack(), false);
        }
        Storages.get().put(target);
        for (UUID sourceId : sourceIds) {
            Storages.get().remove(sourceId);
        }

        replaceWithLargeCrate(level, origin, tier, target.getId());
        if (!player.hasInfiniteMaterials()) {
            largeCrateStack.shrink(1);
        }
        return ItemInteractionResult.sidedSuccess(false);
    }

    /**
     * Clearing all 27 positions before placing anything is deliberate, and mirrors upstream: placing
     * the parts one at a time makes the multipart {@code updateShape} see not-yet-replaced crates as
     * a broken structure and destroy the block again.
     */
    private static void replaceWithLargeCrate(Level level, BlockPos origin, CrateTier tier, UUID storageId) {
        for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
            BlockPos pos = origin.offset(part.getOffset());
            Block.popResource(level, pos, AddonBlocks.crate(tier).asStack());
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
        }
        BlockState mainPart = AddonBlocks.largeCrate(tier).getDefaultState()
            .setValue(LargeCrateBlock.HALF, Cube3x3PartHalf.BOTTOM_CENTER);
        level.setBlock(origin, mainPart, Block.UPDATE_CLIENTS);
        BlockState placed = level.getBlockState(origin);
        placed.getBlock().setPlacedBy(level, origin, placed, null, ItemStack.EMPTY);
        if (level.getBlockEntity(origin) instanceof StorageBlockEntity storage) {
            storage.setId(storageId);
        }
    }

    /**
     * Scans for a 3x3x3 block of same-tier crates containing the clicked position, returning the
     * bottom-centre corner (the main part position) or null when there is no complete cube.
     */
    private static @Nullable BlockPos findOrigin(Level level, BlockPos clicked, CrateTier tier) {
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos candidate = clicked.offset(x, y - 2, z);
                    if (isCompleteCube(level, candidate, tier)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isCompleteCube(Level level, BlockPos origin, CrateTier tier) {
        for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
            if (!(level.getBlockEntity(origin.offset(part.getOffset())) instanceof TieredCrateBlockEntity crate)
                || crate.getTier() != tier) {
                return false;
            }
        }
        return true;
    }
}
