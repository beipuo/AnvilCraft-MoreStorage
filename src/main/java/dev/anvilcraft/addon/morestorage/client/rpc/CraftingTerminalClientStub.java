package dev.anvilcraft.addon.morestorage.client.rpc;

import dev.anvilcraft.addon.morestorage.rpc.CraftingTerminalServerStub;
import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Client half of the hyperdimension crafting terminal.
 *
 * <p>Thin wrappers over {@link CraftingTerminalServerStub}: the screen reports which slot was clicked
 * and the server decides what that means. Every call carries the local player's id (the validator
 * checks it against the packet's sender) and the bound storage id, plus the cursor for the creative
 * case where the client — not the server — owns what the mouse is holding.
 */
public final class CraftingTerminalClientStub {
    private CraftingTerminalClientStub() {
    }

    /** A click on grid slot {@code slot}; {@code button} is 0 for left, 1 for right. */
    public static CompletableFuture<CraftingTerminalServerStub.GridState> gridClick(
        UUID storageId,
        int slot,
        int button
    ) {
        return RPC.invoke(
            RpcTarget.server(),
            CraftingTerminalServerStub::gridClick,
            CraftingTerminalClientStub.playerId(),
            storageId,
            slot,
            button,
            CraftingTerminalClientStub.carried()
        );
    }

    /** Shift-click on grid slot {@code slot}: send it back to the storage. */
    public static CompletableFuture<CraftingTerminalServerStub.GridState> gridQuickMove(UUID storageId, int slot) {
        return RPC.invoke(
            RpcTarget.server(),
            CraftingTerminalServerStub::gridQuickMove,
            CraftingTerminalClientStub.playerId(),
            storageId,
            slot
        );
    }

    /** A click on the result slot; {@code batch} is the shift-click form. */
    public static CompletableFuture<CraftingTerminalServerStub.GridState> craft(UUID storageId, boolean batch) {
        return RPC.invoke(
            RpcTarget.server(),
            CraftingTerminalServerStub::craft,
            CraftingTerminalClientStub.playerId(),
            storageId,
            batch,
            CraftingTerminalClientStub.carried()
        );
    }

    /** The clear button. */
    public static CompletableFuture<CraftingTerminalServerStub.GridState> clearGrid(UUID storageId) {
        return RPC.invoke(
            RpcTarget.server(),
            CraftingTerminalServerStub::clearGrid,
            CraftingTerminalClientStub.playerId(),
            storageId
        );
    }

    private static ItemStack carried() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? ItemStack.EMPTY : player.containerMenu.getCarried();
    }

    private static UUID playerId() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            throw new IllegalStateException("Cannot use a crafting terminal without a client player");
        }
        return player.getGameProfile().getId();
    }
}
