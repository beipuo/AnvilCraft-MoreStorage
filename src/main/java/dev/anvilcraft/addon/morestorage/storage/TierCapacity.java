package dev.anvilcraft.addon.morestorage.storage;

import dev.anvilcraft.addon.morestorage.AnvilCraftMoreStorage;
import dev.anvilcraft.addon.morestorage.crate.CrateTier;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.SpaceSizeItemStacksResourceHandler;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.CrateStorage;
import dev.dubhe.anvilcraft.saved.storage.LargeCrateStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Translates a {@link CrateTier} into an AnvilCraft storage capacity, and applies it.
 *
 * <p>Tiered crates deliberately reuse AnvilCraft's {@code StorageType.CRATE} /
 * {@code StorageType.LARGE_CRATE} rather than introducing new storage types — that enum switches
 * exhaustively over its own constants, so it cannot be extended from outside. Reusing it means the
 * tiered crates keep working with AnvilCraft's storage screen, item handler capability, neighbour
 * aggregation and tooltips; the only difference is the capacity of the backing storage, which is
 * raised through {@code SpaceSizeItemStacksResourceHandler#addSpaceSize}.
 */
public final class TierCapacity {
    /** Only used to read the constructor-supplied capacity off a throwaway storage. */
    private static final UUID PROBE_ID = new UUID(0L, 0L);

    private static int crateBase = -1;
    private static int largeCrateBase = -1;

    private TierCapacity() {
    }

    /**
     * AnvilCraft's own crate capacity, read from a throwaway {@link CrateStorage} rather than
     * hardcoded, so the tiers stay proportional if upstream changes it.
     */
    public static int crateBase() {
        if (crateBase < 0) {
            crateBase = new CrateStorage(PROBE_ID).getItems().getSpaceSize();
        }
        return crateBase;
    }

    /** AnvilCraft's own large crate capacity. See {@link #crateBase()}. */
    public static int largeCrateBase() {
        if (largeCrateBase < 0) {
            largeCrateBase = new LargeCrateStorage(PROBE_ID).getItems().getSpaceSize();
        }
        return largeCrateBase;
    }

    public static int crateCapacity(CrateTier tier) {
        return scale(crateBase(), tier);
    }

    public static int largeCrateCapacity(CrateTier tier) {
        return scale(largeCrateBase(), tier);
    }

    private static int scale(int base, CrateTier tier) {
        long scaled = Math.round(base * AnvilCraftMoreStorage.CONFIG.multiplier(tier));
        return (int) Math.min(Integer.MAX_VALUE, Math.max(base, scaled));
    }

    /**
     * Raise the storage behind {@code id} to {@code capacity}, creating it if AnvilCraft has not
     * yet done so.
     *
     * <p>Does nothing on the client, for an unassigned id, or when the id already belongs to a
     * storage of another kind — the last case would mean somebody else owns that storage, and
     * {@code Storages#getOrCreate} would throw rather than let us touch it.
     */
    public static <S extends BaseStorage<SpaceSizeItemStacksResourceHandler>> void raise(
        @Nullable Level level,
        @Nullable UUID id,
        Class<S> type,
        int capacity
    ) {
        if (level == null || level.isClientSide() || id == null) {
            return;
        }
        Storages storages = Storages.get();
        Optional<BaseStorage<?>> existing = storages.get(id);
        if (existing.isPresent() && !type.isInstance(existing.get())) {
            return;
        }
        SpaceSizeItemStacksResourceHandler items = storages.getOrCreate(id, type).getItems();
        if (items.getSpaceSize() < capacity) {
            // addSpaceSize only ever grows, which is why the config floors multipliers at 1.0.
            items.addSpaceSize(size -> capacity);
            storages.setDirty();
        }
    }
}
