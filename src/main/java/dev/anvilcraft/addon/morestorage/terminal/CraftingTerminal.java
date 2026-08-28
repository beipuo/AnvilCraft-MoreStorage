package dev.anvilcraft.addon.morestorage.terminal;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import javax.annotation.Nullable;

/**
 * A terminal that carries a crafting grid.
 *
 * <p>AnvilCraft's three terminals reach their storage in different ways — the hyperdimension one
 * remembers a bound storage in a component, the local and shulker ones resolve a target from the
 * player every time they are used — but everything this addon builds on top only ever needs the
 * resulting target id: it names the storage the grid restocks from, and it is what the screen, the
 * RPC and its validator use to find the terminal a call belongs to.
 *
 * @see CraftingTerminalGrid#findTerminal(net.minecraft.world.entity.player.Inventory, UUID)
 */
public interface CraftingTerminal {
    /**
     * The storage target this terminal's grid works against, or {@code null} when it has none — an
     * unbound hyperdimension terminal.
     *
     * <p>Must agree on both sides: the client names the target when it opens the screen and the
     * server resolves the same id back to the storages behind it, so this may only be derived from
     * data both sides have (the stack, the player's id).
     */
    @Nullable UUID craftingTarget(Player player, ItemStack stack);
}
