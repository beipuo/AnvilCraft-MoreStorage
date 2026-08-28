package dev.anvilcraft.addon.morestorage.rpc;

import dev.anvilcraft.addon.morestorage.terminal.CraftingTerminalGrid;
import dev.anvilcraft.lib.v2.rpc.CallableParam;
import dev.anvilcraft.lib.v2.rpc.IRemoteCallableValidator;
import dev.anvilcraft.lib.v2.rpc.RemoteCallable;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.block.container.storage.HyperdimensionStorageStationBlock;
import dev.dubhe.anvilcraft.block.item.ShulkerContainerBlockItem;
import dev.dubhe.anvilcraft.item.HyperdimensionTerminalItem;
import dev.dubhe.anvilcraft.saved.storage.HyperdimensionStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Server half of the hyperdimension crafting terminal.
 *
 * <p>The grid lives on the terminal stack, so every call here finds the terminal in the caller's
 * inventory, mutates its {@code CRAFTING_GRID} component and lets the ordinary inventory sync carry
 * the change back. The returned {@link GridState} is only there so the screen can react without
 * waiting a tick for that sync.
 *
 * <p>All of it is server-authoritative: the client never decides what a click does, it only reports
 * which slot was hit. {@link CraftingTerminalValidator} makes sure the caller really carries a
 * terminal bound to the storage it names, which is what keeps a spoofed packet from reaching
 * somebody else's storage.
 */
public final class CraftingTerminalServerStub {
    /** How many crafts a single shift-click may perform. */
    private static final int MAX_BATCH_CRAFTS = 64;

    public static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> GRID_STREAM_CODEC =
        ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list());

    private CraftingTerminalServerStub() {
    }

    /**
     * What the grid and the cursor look like after a call.
     *
     * @param grid    the nine slots, so the screen can redraw before the inventory sync lands
     * @param carried what the cursor holds now
     * @param changed whether anything actually moved; the screen plays its click sound only then
     */
    public record GridState(List<ItemStack> grid, ItemStack carried, boolean changed) {
        public static final StreamCodec<RegistryFriendlyByteBuf, GridState> STREAM_CODEC = StreamCodec.composite(
            CraftingTerminalServerStub.GRID_STREAM_CODEC, GridState::grid,
            ItemStack.OPTIONAL_STREAM_CODEC, GridState::carried,
            ByteBufCodecs.BOOL, GridState::changed,
            GridState::new
        );
    }

    /**
     * A click on one of the nine grid slots, with the ordinary inventory-click semantics:
     * left picks up or drops everything, right picks up half or drops one, and a click with a
     * different item on the cursor swaps the two.
     *
     * @param slot         index into the 3×3 grid, row-major
     * @param button       0 for left, anything else for right
     * @param clientCarried what the client thinks the cursor holds; only trusted in creative mode,
     *                      where the client owns its own cursor
     */
    @RemoteCallable(validator = CraftingTerminalValidator.class)
    public static GridState gridClick(
        UUID playerId,
        UUID storageId,
        int slot,
        int button,
        @CallableParam(clazz = ItemStack.class, field = "OPTIONAL_STREAM_CODEC") ItemStack clientCarried
    ) {
        ServerPlayer player = CraftingTerminalServerStub.getServerPlayer(playerId);
        ItemStack terminal = CraftingTerminalGrid.findTerminal(player.getInventory(), storageId);
        if (terminal.isEmpty() || slot < 0 || slot >= CraftingTerminalGrid.SIZE) {
            return CraftingTerminalServerStub.unchanged(
                terminal,
                CraftingTerminalServerStub.carried(player, clientCarried)
            );
        }
        NonNullList<ItemStack> grid = CraftingTerminalGrid.read(terminal);
        ItemStack cursor = CraftingTerminalServerStub.carried(player, clientCarried).copy();
        ItemStack inSlot = grid.get(slot);
        boolean changed = true;
        if (cursor.isEmpty()) {
            if (inSlot.isEmpty()) {
                changed = false;
            } else {
                int taken = button == 0 ? inSlot.getCount() : (inSlot.getCount() + 1) / 2;
                cursor = inSlot.copyWithCount(taken);
                inSlot.shrink(taken);
                grid.set(slot, inSlot.isEmpty() ? ItemStack.EMPTY : inSlot);
            }
        } else if (inSlot.isEmpty()) {
            int placed = Math.min(button == 0 ? cursor.getCount() : 1, cursor.getMaxStackSize());
            grid.set(slot, cursor.copyWithCount(placed));
            cursor.shrink(placed);
        } else if (ItemStack.isSameItemSameComponents(cursor, inSlot)) {
            int placed = Math.min(button == 0 ? cursor.getCount() : 1, inSlot.getMaxStackSize() - inSlot.getCount());
            changed = placed > 0;
            inSlot.grow(placed);
            cursor.shrink(placed);
        } else if (cursor.getCount() <= inSlot.getMaxStackSize()) {
            grid.set(slot, cursor);
            cursor = inSlot;
        } else {
            changed = false;
        }
        if (!changed) {
            return CraftingTerminalServerStub.unchanged(
                terminal,
                CraftingTerminalServerStub.carried(player, clientCarried)
            );
        }
        return CraftingTerminalServerStub.commit(player, terminal, grid, cursor);
    }

    /** Shift-click on a grid slot: the whole stack goes back to the storage. */
    @RemoteCallable(validator = CraftingTerminalValidator.class)
    public static GridState gridQuickMove(UUID playerId, UUID storageId, int slot) {
        ServerPlayer player = CraftingTerminalServerStub.getServerPlayer(playerId);
        ItemStack terminal = CraftingTerminalGrid.findTerminal(player.getInventory(), storageId);
        if (terminal.isEmpty() || slot < 0 || slot >= CraftingTerminalGrid.SIZE) {
            return CraftingTerminalServerStub.unchanged(terminal, player.containerMenu.getCarried());
        }
        NonNullList<ItemStack> grid = CraftingTerminalGrid.read(terminal);
        if (grid.get(slot).isEmpty()) {
            return CraftingTerminalServerStub.unchanged(terminal, player.containerMenu.getCarried());
        }
        HyperdimensionStorage storage = CraftingTerminalServerStub.getStorage(storageId);
        CraftingTerminalServerStub.evacuate(player, storage, grid, slot);
        return CraftingTerminalServerStub.commit(player, terminal, grid, null);
    }

    /** The clear button: every grid slot goes back to the storage. */
    @RemoteCallable(validator = CraftingTerminalValidator.class)
    public static GridState clearGrid(UUID playerId, UUID storageId) {
        ServerPlayer player = CraftingTerminalServerStub.getServerPlayer(playerId);
        ItemStack terminal = CraftingTerminalGrid.findTerminal(player.getInventory(), storageId);
        if (terminal.isEmpty()) {
            return CraftingTerminalServerStub.unchanged(terminal, player.containerMenu.getCarried());
        }
        NonNullList<ItemStack> grid = CraftingTerminalGrid.read(terminal);
        if (CraftingTerminalGrid.isEmpty(grid)) {
            return CraftingTerminalServerStub.unchanged(terminal, player.containerMenu.getCarried());
        }
        HyperdimensionStorage storage = CraftingTerminalServerStub.getStorage(storageId);
        for (int slot = 0; slot < CraftingTerminalGrid.SIZE; slot++) {
            CraftingTerminalServerStub.evacuate(player, storage, grid, slot);
        }
        return CraftingTerminalServerStub.commit(player, terminal, grid, null);
    }

    /**
     * Empties one grid slot: into the storage first, then whatever the storage refused into the
     * inventory, and anything still left onto the floor.
     */
    private static void evacuate(
        ServerPlayer player,
        HyperdimensionStorage storage,
        NonNullList<ItemStack> grid,
        int slot
    ) {
        ItemStack stack = grid.get(slot);
        if (stack.isEmpty()) {
            return;
        }
        grid.set(slot, ItemStack.EMPTY);
        CraftingTerminalServerStub.giveOrStore(player, storage, stack);
    }

    /**
     * A click on the result slot.
     *
     * @param batch shift-click: craft repeatedly into the inventory until the ingredients, the
     *              inventory space or {@link #MAX_BATCH_CRAFTS} run out. Otherwise a single craft
     *              lands on the cursor.
     */
    @RemoteCallable(validator = CraftingTerminalValidator.class)
    public static GridState craft(
        UUID playerId,
        UUID storageId,
        boolean batch,
        @CallableParam(clazz = ItemStack.class, field = "OPTIONAL_STREAM_CODEC") ItemStack clientCarried
    ) {
        ServerPlayer player = CraftingTerminalServerStub.getServerPlayer(playerId);
        ItemStack terminal = CraftingTerminalGrid.findTerminal(player.getInventory(), storageId);
        if (terminal.isEmpty()) {
            return CraftingTerminalServerStub.unchanged(
                terminal,
                CraftingTerminalServerStub.carried(player, clientCarried)
            );
        }
        Level level = player.level();
        NonNullList<ItemStack> grid = CraftingTerminalGrid.read(terminal);
        HyperdimensionStorage storage = CraftingTerminalServerStub.getStorage(storageId);
        ItemStack cursor = CraftingTerminalServerStub.carried(player, clientCarried).copy();
        int crafted = 0;
        for (int attempt = 0; attempt < (batch ? CraftingTerminalServerStub.MAX_BATCH_CRAFTS : 1); attempt++) {
            CraftingInput.Positioned positioned = CraftingTerminalGrid.positioned(grid);
            Optional<RecipeHolder<CraftingRecipe>> recipe =
                CraftingTerminalGrid.recipe(level, positioned.input());
            if (recipe.isEmpty()) {
                break;
            }
            ItemStack result = recipe.get().value().assemble(positioned.input(), level.registryAccess());
            if (result.isEmpty() || !CraftingTerminalServerStub.hasRoom(player, cursor, result, batch)) {
                break;
            }
            CraftingTerminalServerStub.consume(player, storage, grid, recipe.get(), positioned);
            result.onCraftedBy(level, player, result.getCount());
            player.awardRecipes(List.<RecipeHolder<?>>of(recipe.get()));
            if (batch) {
                CraftingTerminalServerStub.giveOrDrop(player, result);
            } else if (cursor.isEmpty()) {
                cursor = result;
            } else {
                cursor.grow(result.getCount());
            }
            crafted++;
        }
        if (crafted == 0) {
            return CraftingTerminalServerStub.unchanged(
                terminal,
                CraftingTerminalServerStub.carried(player, clientCarried)
            );
        }
        return CraftingTerminalServerStub.commit(player, terminal, grid, batch ? null : cursor);
    }

    /** Whether one more craft of {@code result} fits where it is headed. */
    private static boolean hasRoom(ServerPlayer player, ItemStack cursor, ItemStack result, boolean batch) {
        if (batch) {
            return CraftingTerminalServerStub.inventorySpace(player.getInventory(), result) >= result.getCount();
        }
        if (cursor.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(cursor, result)
               && cursor.getCount() + result.getCount() <= cursor.getMaxStackSize();
    }

    /**
     * Takes one of every ingredient, then refills whatever ran dry from the storage — the network
     * restock that makes the terminal worth using over a crafting table.
     *
     * <p>{@link CraftingInput} is cropped to the smallest box containing the ingredients, so
     * {@code positioned.left()/top()} is what maps a recipe index back to a grid slot. Container
     * remainders (buckets, bottles) take the emptied slot when there is one, exactly like vanilla,
     * and otherwise go to the storage.
     */
    private static void consume(
        ServerPlayer player,
        HyperdimensionStorage storage,
        NonNullList<ItemStack> grid,
        RecipeHolder<CraftingRecipe> recipe,
        CraftingInput.Positioned positioned
    ) {
        CraftingInput input = positioned.input();
        NonNullList<ItemStack> remaining = recipe.value().getRemainingItems(input);
        for (int row = 0; row < input.height(); row++) {
            for (int column = 0; column < input.width(); column++) {
                int index = row * input.width() + column;
                int slot = (row + positioned.top()) * CraftingTerminalGrid.WIDTH + column + positioned.left();
                ItemStack stack = grid.get(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                ItemStack resource = stack.copyWithCount(1);
                ItemStack leftover = index < remaining.size() ? remaining.get(index).copy() : ItemStack.EMPTY;
                stack.shrink(1);
                if (!stack.isEmpty()) {
                    CraftingTerminalServerStub.giveOrStore(player, storage, leftover);
                    continue;
                }
                if (!leftover.isEmpty()) {
                    grid.set(slot, leftover);
                    continue;
                }
                int restocked = CraftingTerminalServerStub.extractFrom(storage, resource, 1);
                grid.set(slot, restocked > 0 ? resource.copyWithCount(restocked) : ItemStack.EMPTY);
            }
        }
    }

    /** Inserts as much of {@code stack} as the storage accepts, returning how much went in. */
    private static int insertInto(HyperdimensionStorage storage, ItemStack stack) {
        if (stack.isEmpty() || !CraftingTerminalServerStub.canStore(stack)) {
            return 0;
        }
        ItemStack leftover = storage.getItems().insertItem(stack.copy(), false);
        return stack.getCount() - leftover.getCount();
    }

    /** Pulls up to {@code amount} of {@code resource} out of the storage. */
    private static int extractFrom(HyperdimensionStorage storage, ItemStack resource, int amount) {
        UnlimitedItemStacksResourceHandler items = storage.getItems();
        int extracted = 0;
        for (int slot = 0; slot < items.size() && extracted < amount; slot++) {
            if (items.getAmountAsLong(slot) <= 0
                || !items.getUnlimitedStackInSlot(slot).isSameItemSameComponents(resource)) {
                continue;
            }
            extracted += items.extractItem(slot, amount - extracted, false).getCount();
        }
        return extracted;
    }

    /**
     * The same rule AnvilCraft applies to a hyperdimension storage: it refuses the containers and
     * terminals that would let it hold itself. Widened to every {@link HyperdimensionTerminalItem}
     * so this addon's crafting terminal is refused too.
     */
    private static boolean canStore(ItemStack stack) {
        return !(stack.getItem() instanceof ShulkerContainerBlockItem)
               && !(stack.getItem() instanceof BlockItem item
                    && item.getBlock() instanceof HyperdimensionStorageStationBlock)
               && !(stack.getItem() instanceof HyperdimensionTerminalItem);
    }

    /** Storage first, then the inventory, then the floor. */
    private static void giveOrStore(ServerPlayer player, HyperdimensionStorage storage, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        stack.shrink(CraftingTerminalServerStub.insertInto(storage, stack));
        CraftingTerminalServerStub.giveOrDrop(player, stack);
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        player.getInventory().add(stack);
        if (!stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    /** How many more of {@code stack} the main inventory could hold. */
    private static int inventorySpace(Inventory inventory, ItemStack stack) {
        long space = 0;
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack existing = inventory.getItem(slot);
            if (existing.isEmpty()) {
                space += stack.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(existing, stack) && existing.isStackable()) {
                space += Math.max(0, inventory.getMaxStackSize(existing) - existing.getCount());
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, space);
    }

    /**
     * The cursor to work against. Creative mode owns its own cursor client-side — the server's
     * {@code setCarried} broadcast never reaches it — so there the client's report is the truth.
     */
    private static ItemStack carried(ServerPlayer player, ItemStack clientCarried) {
        return player.hasInfiniteMaterials() ? clientCarried : player.containerMenu.getCarried();
    }

    /** A no-op answer: whatever the grid and cursor already were. */
    private static GridState unchanged(ItemStack terminal, ItemStack cursor) {
        List<ItemStack> grid = terminal.isEmpty()
            ? NonNullList.withSize(CraftingTerminalGrid.SIZE, ItemStack.EMPTY)
            : CraftingTerminalGrid.read(terminal);
        return new GridState(grid, cursor, false);
    }

    /**
     * Writes the grid back onto the terminal and, if {@code cursor} is given, onto the cursor, then
     * pushes both to the client.
     *
     * <p>The terminal is an ordinary inventory item, so {@code broadcastChanges} notices the changed
     * component and re-sends the slot; no bespoke grid packet is needed.
     */
    private static GridState commit(
        ServerPlayer player,
        ItemStack terminal,
        List<ItemStack> grid,
        ItemStack cursor
    ) {
        CraftingTerminalGrid.write(terminal, grid);
        if (cursor != null) {
            player.containerMenu.setCarried(cursor);
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return new GridState(grid, player.containerMenu.getCarried(), true);
    }

    private static HyperdimensionStorage getStorage(UUID storageId) {
        return Storages.get().getOrCreate(storageId, HyperdimensionStorage.class);
    }

    private static ServerPlayer getServerPlayer(UUID playerId) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            throw new IllegalStateException("Cannot craft without a running server");
        }
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            throw new IllegalStateException("Cannot craft without a server player");
        }
        return player;
    }

    /**
     * Lets a call through only when the sender is the player it claims to be and really carries a
     * crafting terminal bound to the storage it names.
     */
    public static final class CraftingTerminalValidator implements IRemoteCallableValidator {
        @Override
        public boolean validate(IPayloadContext context, Method method, Object[] args) {
            if (!(context.player() instanceof ServerPlayer player)
                || args.length < 2
                || !(args[0] instanceof UUID playerId)
                || !(args[1] instanceof UUID storageId)) {
                return false;
            }
            return player.getGameProfile().getId().equals(playerId)
                   && !CraftingTerminalGrid.findTerminal(player.getInventory(), storageId).isEmpty();
        }
    }
}
