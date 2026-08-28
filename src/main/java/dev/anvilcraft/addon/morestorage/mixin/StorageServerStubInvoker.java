package dev.anvilcraft.addon.morestorage.mixin;

import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.UUID;

/**
 * The parts of AnvilCraft's terminal plumbing this addon needs but upstream keeps to itself.
 *
 * <p>A terminal target id is not a storage id: the local terminal's resolves to the nearest large
 * crate, the shulker terminal's to a shulker container or to the aggregate of the shulker boxes the
 * player carries, and only the hyperdimension one names a storage directly. Upstream resolves all of
 * that in {@code terminalStorages}, and derives the two per-player ids in
 * {@code localTerminalId}/{@code shulkerTerminalId} — all three private. Reimplementing them here
 * would mean copying the search radii and the shulker priority order and keeping the copies in step,
 * so the crafting terminal borrows the originals instead.
 *
 * <p>The public terminal API is used wherever it reaches: {@code insertIntoTerminal} puts items back.
 * Only extraction has no usable public form, because {@code extractFromTerminal} takes whatever the
 * first non-empty slot holds while restocking a crafting grid has to ask for one specific item.
 */
@Mixin(StorageServerStub.class)
public interface StorageServerStubInvoker {
    /** The storages a terminal target id currently resolves to; empty when it is out of range. */
    @Invoker("terminalStorages")
    static List<BaseStorage<?>> moreStorage$terminalStorages(ServerPlayer player, UUID targetId) {
        throw new AssertionError();
    }

    /** The local terminal's per-player target id. */
    @Invoker("localTerminalId")
    static UUID moreStorage$localTerminalId(UUID playerId) {
        throw new AssertionError();
    }

    /** The shulker terminal's per-player target id. */
    @Invoker("shulkerTerminalId")
    static UUID moreStorage$shulkerTerminalId(UUID playerId) {
        throw new AssertionError();
    }
}
