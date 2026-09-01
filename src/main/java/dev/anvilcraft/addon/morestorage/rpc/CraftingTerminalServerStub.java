package dev.anvilcraft.addon.morestorage.rpc;

import dev.anvilcraft.addon.morestorage.mixin.StorageServerStubInvoker;
import dev.anvilcraft.addon.morestorage.terminal.CraftingTerminalGrid;
import dev.anvilcraft.addon.morestorage.terminal.TerminalMode;
import dev.anvilcraft.addon.morestorage.terminal.TerminalRecipes;
import dev.anvilcraft.addon.morestorage.terminal.TerminalState;
import dev.anvilcraft.lib.v2.rpc.CallableParam;
import dev.anvilcraft.lib.v2.rpc.IRemoteCallableValidator;
import dev.anvilcraft.lib.v2.rpc.RemoteCallable;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;
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
 * Server half of the crafting terminals.
 *
 * <p>The grid lives on the terminal stack, so every call here finds the terminal in the caller's
 * inventory, mutates its {@code CRAFTING_GRID} component and lets the ordinary inventory sync carry
 * the change back. The returned {@link GridState} is only there so the screen can react without
 * waiting a tick for that sync.
 *
 * <p>What the grid trades items with is named by a terminal target id, never by a storage: only the
 * hyperdimension terminal's target is a storage id proper, while the local and shulker ones resolve to
 * whatever is currently in range — a crate, a carried shulker container, every shulker box at once.
 * Insertion therefore goes through upstream's {@code insertIntoTerminal}, which resolves and filters
 * for us; extraction resolves the same storages itself because upstream's only public extraction takes
 * the first non-empty slot rather than a specific item.
 *
 * <p>The terminal is four workbenches in one, and its {@link TerminalMode} decides which. The mode is
 * on the stack as well, so it is read here rather than sent: a click only ever says which slot was hit,
 * and what that means is settled server-side.
 *
 * <p>All of it is server-authoritative: the client never decides what a click does, it only reports
 * which slot was hit. {@link CraftingTerminalValidator} makes sure the caller really carries a
 * terminal working against the target it names, which is what keeps a spoofed packet from reaching
 * somebody else's storage.
 */
public final class CraftingTerminalServerStub {
    /** How many crafts a single shift-click may perform. */
    private static final int MAX_BATCH_CRAFTS = 64;

    /** Vanilla's own cap on a renamed item, and on the rename packet that carries it. */
    private static final int MAX_NAME_LENGTH = 50;

    public static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> GRID_STREAM_CODEC =
        ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list());

    private CraftingTerminalServerStub() {
    }

    /**
     * What the grid and the cursor look like after a call.
     *
     * @param grid    the nine slots, so the screen can redraw before the inventory sync lands
     * @param carried what the cursor holds now
     * @param changed whether anything actually moved; the screen refreshes the storage list only then
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
     * A click on one of the mode's input slots, with the ordinary inventory-click semantics:
     * left picks up or drops everything, right picks up half or drops one, and a click with a
     * different item on the cursor swaps the two.
     *
     * @param slot         index into the nine-slot grid, row-major; only the slots the current mode
     *                     uses are accepted
     * @param button       0 for left, anything else for right
     * @param clientCarried what the client thinks the cursor holds; only trusted in creative mode,
     *                      where the client owns its own cursor
     */
    @RemoteCallable(validator = CraftingTerminalValidator.class)
    public static GridState gridClick(
        UUID playerId,
        UUID targetId,
        int slot,
        int button,
        @CallableParam(clazz = ItemStack.class, field = "OPTIONAL_STREAM_CODEC") ItemStack clientCarried
    ) {
        ServerPlayer player = CraftingTerminalServerStub.getServerPlayer(playerId);
        ItemStack terminal = CraftingTerminalGrid.findTerminal(player.getInventory(), targetId);
        if (terminal.isEmpty() || !CraftingTerminalServerStub.holdsSlot(terminal, slot)) {
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
        CraftingTerminalServerStub.resetChoice(terminal);
        return CraftingTerminalServerStub.commit(player, terminal, grid, cursor);
    }

    /** Shift-click on an input slot: the whole stack goes back to the storage. */
    @RemoteCallable(validator = CraftingTerminalValidator.class)
    public static GridState gridQuickMove(UUID playerId, UUID targetId, int slot) {
        ServerPlayer player = CraftingTerminalServerStub.getServerPlayer(playerId);
        ItemStack terminal = CraftingTerminalGrid.findTerminal(player.getInventory(), targetId);
        if (terminal.isEmpty() || !CraftingTerminalServerStub.holdsSlot(terminal, slot)) {
            return CraftingTerminalServerStub.unchanged(terminal, player.containerMenu.getCarried());
        }
        NonNullList<ItemStack> grid = CraftingTerminalGrid.read(terminal);
        if (grid.get(slot).isEmpty()) {
            return CraftingTerminalServerStub.unchanged(terminal, player.containerMenu.getCarried());
        }
        CraftingTerminalServerStub.evacuate(player, targetId, grid, slot);
        CraftingTerminalServerStub.resetChoice(terminal);
        return CraftingTerminalServerStub.commit(player, terminal, grid, null);
    }

    /** The clear button: every grid slot goes back to the storage. */
    @RemoteCallable(validator = CraftingTerminalValidator.class)
    public static GridState clearGrid(UUID playerId, UUID targetId) {
        ServerPlayer player = CraftingTerminalServerStub.getServerPlayer(playerId);
        ItemStack terminal = CraftingTerminalGrid.findTerminal(player.getInventory(), targetId);
        if (terminal.isEmpty()) {
            return CraftingTerminalServerStub.unchanged(terminal, player.containerMenu.getCarried());
        }
        NonNullList<ItemStack> grid = CraftingTerminalGrid.read(terminal);
        if (CraftingTerminalGrid.isEmpty(grid)) {
            return CraftingTerminalServerStub.unchanged(terminal, player.containerMenu.getCarried());
        }
        for (int slot = 0; slot < CraftingTerminalGrid.SIZE; slot++) {
            CraftingTerminalServerStub.evacuate(player, targetId, grid, slot);
        }
        CraftingTerminalServerStub.resetChoice(terminal);
        return CraftingTerminalServerStub.commit(player, terminal, grid, null);
    }

    /**
     * One of the four switch buttons: the terminal becomes a different workbench.
     *
     * <p>The grid is emptied back into the storage first. Every mode shares the same nine slots and
     * uses only the first few, so anything left in a slot the new mode does not draw would be invisible
     * and unreachable; sending it home is the one behaviour with no way to lose an item.
     */
    @RemoteCallable(validator = CraftingTerminalValidator.class)
    public static GridState setMode(UUID playerId, UUID targetId, TerminalMode mode) {
        ServerPlayer player = CraftingTerminalServerStub.getServerPlayer(playerId);
        ItemStack terminal = CraftingTerminalGrid.findTerminal(player.getInventory(), targetId);
        if (terminal.isEmpty() || TerminalState.mode(terminal) == mode) {
            return CraftingTerminalServerStub.unchanged(terminal, player.containerMenu.getCarried());
        }
        NonNullList<ItemStack> grid = CraftingTerminalGrid.read(terminal);
        for (int slot = 0; slot < CraftingTerminalGrid.SIZE; slot++) {
            CraftingTerminalServerStub.evacuate(player, targetId, grid, slot);
        }
        TerminalState.setMode(terminal, mode);
        TerminalState.setAnvilName(terminal, "");
        TerminalState.setStonecutterChoice(terminal, 0);
        return CraftingTerminalServerStub.commit(player, terminal, grid, null);
    }

    /**
     * What the anvil mode should rename its result to, as typed.
     *
     * <p>Sent per keystroke, exactly like vanilla's rename packet, and capped the same way. Nothing
     * moves, so the answer reports no change and the storage list is left alone.
     */
    @RemoteCallable(validator = CraftingTerminalValidator.class)
    public static GridState setAnvilName(UUID playerId, UUID targetId, String name) {
        ServerPlayer player = CraftingTerminalServerStub.getServerPlayer(playerId);
        ItemStack terminal = CraftingTerminalGrid.findTerminal(player.getInventory(), targetId);
        if (terminal.isEmpty()) {
            return CraftingTerminalServerStub.unchanged(terminal, player.containerMenu.getCarried());
        }
        TerminalState.setAnvilName(
            terminal,
            name.length() > CraftingTerminalServerStub.MAX_NAME_LENGTH
                ? name.substring(0, CraftingTerminalServerStub.MAX_NAME_LENGTH)
                : name
        );
        return CraftingTerminalServerStub.store(player, terminal);
    }

    /** Which of the recipes the stonecutter input matches the player picked. */
    @RemoteCallable(validator = CraftingTerminalValidator.class)
    public static GridState setStonecutterChoice(UUID playerId, UUID targetId, int choice) {
        ServerPlayer player = CraftingTerminalServerStub.getServerPlayer(playerId);
        ItemStack terminal = CraftingTerminalGrid.findTerminal(player.getInventory(), targetId);
        if (terminal.isEmpty()) {
            return CraftingTerminalServerStub.unchanged(terminal, player.containerMenu.getCarried());
        }
        TerminalState.setStonecutterChoice(terminal, Math.max(0, choice));
        return CraftingTerminalServerStub.store(player, terminal);
    }

    /**
     * A click on the result slot, meaning whatever the current mode makes of its inputs.
     *
     * @param batch shift-click: the result goes to the inventory instead of the cursor, and the three
     *              recipe modes keep going until the ingredients, the inventory space or
     *              {@link #MAX_BATCH_CRAFTS} run out. The anvil does one at a time whatever happens —
     *              it has no repeatable form.
     */
    @RemoteCallable(validator = CraftingTerminalValidator.class)
    public static GridState craft(
        UUID playerId,
        UUID targetId,
        boolean batch,
        @CallableParam(clazz = ItemStack.class, field = "OPTIONAL_STREAM_CODEC") ItemStack clientCarried
    ) {
        ServerPlayer player = CraftingTerminalServerStub.getServerPlayer(playerId);
        ItemStack terminal = CraftingTerminalGrid.findTerminal(player.getInventory(), targetId);
        if (terminal.isEmpty()) {
            return CraftingTerminalServerStub.unchanged(
                terminal,
                CraftingTerminalServerStub.carried(player, clientCarried)
            );
        }
        NonNullList<ItemStack> grid = CraftingTerminalGrid.read(terminal);
        ItemStack cursor = CraftingTerminalServerStub.carried(player, clientCarried).copy();
        TerminalMode mode = TerminalState.mode(terminal);
        return switch (mode) {
            case CRAFTING -> CraftingTerminalServerStub.craftGrid(player, targetId, terminal, grid, cursor, batch);
            case STONECUTTING, SMITHING ->
                CraftingTerminalServerStub.craftRecipe(player, targetId, terminal, grid, cursor, batch, mode);
            case ANVIL -> CraftingTerminalServerStub.craftAnvil(player, terminal, grid, cursor, batch);
        };
    }

    /** The 3×3 crafting grid, with vanilla's shaped-recipe placement and container remainders. */
    private static GridState craftGrid(
        ServerPlayer player,
        UUID targetId,
        ItemStack terminal,
        NonNullList<ItemStack> grid,
        ItemStack cursor,
        boolean batch
    ) {
        Level level = player.level();
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
            CraftingTerminalServerStub.consume(player, targetId, grid, recipe.get(), positioned);
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
            return CraftingTerminalServerStub.unchanged(terminal, cursor);
        }
        return CraftingTerminalServerStub.commit(player, terminal, grid, batch ? null : cursor);
    }

    /**
     * The stonecutter and the smithing table: one recipe over the mode's own leading slots, one of each
     * consumed per craft and the emptied slots restocked from the storage.
     */
    private static GridState craftRecipe(
        ServerPlayer player,
        UUID targetId,
        ItemStack terminal,
        NonNullList<ItemStack> grid,
        ItemStack cursor,
        boolean batch,
        TerminalMode mode
    ) {
        Level level = player.level();
        int crafted = 0;
        for (int attempt = 0; attempt < (batch ? CraftingTerminalServerStub.MAX_BATCH_CRAFTS : 1); attempt++) {
            TerminalRecipes.Outcome outcome = TerminalRecipes.outcome(
                player,
                mode,
                grid,
                TerminalState.anvilName(terminal),
                TerminalState.stonecutterChoice(terminal)
            );
            ItemStack result = outcome.result().copy();
            if (!outcome.takeable()
                || result.isEmpty()
                || !CraftingTerminalServerStub.hasRoom(player, cursor, result, batch)) {
                break;
            }
            CraftingTerminalServerStub.consumeInputs(player, targetId, grid, mode.inputSlots());
            result.onCraftedBy(level, player, result.getCount());
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
            return CraftingTerminalServerStub.unchanged(terminal, cursor);
        }
        CraftingTerminalServerStub.playModeSound(player, mode);
        return CraftingTerminalServerStub.commit(player, terminal, grid, batch ? null : cursor);
    }

    /**
     * The anvil.
     *
     * <p>A throwaway {@link AnvilMenu} decides both what comes out and what it costs, and taking its
     * result slot is what charges the levels and consumes the inputs — the same call vanilla makes, so
     * repair-material counts and the "too expensive" ceiling need no restating here. The leftovers are
     * read back out of the menu's own input slots.
     *
     * <p>Nothing is restocked from the storage: an anvil consumes what it was given, and quietly
     * pulling in a second copy of an enchanted book or a damaged tool is not something a player asked
     * for.
     */
    private static GridState craftAnvil(
        ServerPlayer player,
        ItemStack terminal,
        NonNullList<ItemStack> grid,
        ItemStack cursor,
        boolean toInventory
    ) {
        AnvilMenu menu = TerminalRecipes.anvilMenu(
            player,
            grid.get(0),
            grid.get(1),
            TerminalState.anvilName(terminal)
        );
        Slot resultSlot = menu.getSlot(AnvilMenu.RESULT_SLOT);
        ItemStack result = resultSlot.getItem().copy();
        if (result.isEmpty()
            || !resultSlot.mayPickup(player)
            || !CraftingTerminalServerStub.hasRoom(player, cursor, result, toInventory)) {
            return CraftingTerminalServerStub.unchanged(terminal, cursor);
        }
        resultSlot.onTake(player, result);
        grid.set(0, menu.getSlot(AnvilMenu.INPUT_SLOT).getItem());
        grid.set(1, menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem());
        CraftingTerminalServerStub.playModeSound(player, TerminalMode.ANVIL);
        if (toInventory) {
            CraftingTerminalServerStub.giveOrDrop(player, result);
            return CraftingTerminalServerStub.commit(player, terminal, grid, null);
        }
        if (cursor.isEmpty()) {
            cursor = result;
        } else {
            cursor.grow(result.getCount());
        }
        return CraftingTerminalServerStub.commit(player, terminal, grid, cursor);
    }

    /**
     * The workbench the mode stands in for, heard where the player is standing.
     *
     * <p>The crafting grid is left silent on purpose: that is the one mode that was already there, and
     * a crafting table makes no sound when you take from it either.
     */
    private static void playModeSound(ServerPlayer player, TerminalMode mode) {
        SoundEvent sound = switch (mode) {
            case CRAFTING -> null;
            case STONECUTTING -> SoundEvents.UI_STONECUTTER_TAKE_RESULT;
            case SMITHING -> SoundEvents.SMITHING_TABLE_USE;
            case ANVIL -> SoundEvents.ANVIL_USE;
        };
        if (sound != null) {
            player.level().playSound(null, player.blockPosition(), sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    /** Whether {@code slot} is one of the slots the terminal's current mode actually draws. */
    private static boolean holdsSlot(ItemStack terminal, int slot) {
        return slot >= 0 && slot < TerminalState.mode(terminal).inputSlots();
    }

    /**
     * Forgets the selected stonecutter recipe, because the input it was chosen for is gone.
     *
     * <p>Zero rather than "nothing selected": with a fresh input the first of its recipes is the one a
     * player almost always wants, and one that resolves to nothing until clicked would make the common
     * case two clicks. Out of range is still harmless — the result simply stays empty.
     */
    private static void resetChoice(ItemStack terminal) {
        if (TerminalState.mode(terminal) == TerminalMode.STONECUTTING) {
            TerminalState.setStonecutterChoice(terminal, 0);
        }
    }

    /**
     * Empties one grid slot: into the storage first, then whatever the storage refused into the
     * inventory, and anything still left onto the floor.
     */
    private static void evacuate(
        ServerPlayer player,
        UUID targetId,
        NonNullList<ItemStack> grid,
        int slot
    ) {
        ItemStack stack = grid.get(slot);
        if (stack.isEmpty()) {
            return;
        }
        grid.set(slot, ItemStack.EMPTY);
        CraftingTerminalServerStub.giveOrStore(player, targetId, stack);
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
        UUID targetId,
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
                    CraftingTerminalServerStub.giveOrStore(player, targetId, leftover);
                    continue;
                }
                if (!leftover.isEmpty()) {
                    grid.set(slot, leftover);
                    continue;
                }
                int restocked = CraftingTerminalServerStub.extractFrom(player, targetId, resource, 1);
                grid.set(slot, restocked > 0 ? resource.copyWithCount(restocked) : ItemStack.EMPTY);
            }
        }
    }

    /**
     * The same one-of-each-and-restock, for the modes whose inputs are simply the first
     * {@code inputSlots} of the grid with no shape to them.
     */
    private static void consumeInputs(
        ServerPlayer player,
        UUID targetId,
        NonNullList<ItemStack> grid,
        int inputSlots
    ) {
        for (int slot = 0; slot < inputSlots; slot++) {
            ItemStack stack = grid.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack resource = stack.copyWithCount(1);
            stack.shrink(1);
            if (!stack.isEmpty()) {
                continue;
            }
            int restocked = CraftingTerminalServerStub.extractFrom(player, targetId, resource, 1);
            grid.set(slot, restocked > 0 ? resource.copyWithCount(restocked) : ItemStack.EMPTY);
        }
    }

    /**
     * Pulls up to {@code amount} of {@code resource} out of the storages the terminal target resolves
     * to right now.
     *
     * <p>Upstream's own {@code extractFromTerminal} takes whatever the first non-empty slot holds,
     * which is right for a bundle-like click and useless for restocking a grid slot, so this walks the
     * resolved storages itself and asks for one specific item.
     */
    private static int extractFrom(ServerPlayer player, UUID targetId, ItemStack resource, int amount) {
        List<BaseStorage<?>> storages = StorageServerStubInvoker.moreStorage$terminalStorages(player, targetId);
        int extracted = 0;
        for (BaseStorage<?> storage : storages) {
            UnlimitedItemStacksResourceHandler items = storage.getItems();
            for (int slot = 0; slot < items.size() && extracted < amount; slot++) {
                if (items.getAmountAsLong(slot) <= 0
                    || !items.getUnlimitedStackInSlot(slot).isSameItemSameComponents(resource)) {
                    continue;
                }
                extracted += items.extractItem(slot, amount - extracted, false).getCount();
            }
        }
        return extracted;
    }

    /**
     * Storage first, then the inventory, then the floor.
     *
     * <p>Upstream's {@code insertIntoTerminal} is what resolves the target and applies each storage's
     * own refusals — the rule that keeps a storage from swallowing the terminal that reaches it — so
     * there is nothing for this addon to re-decide.
     */
    private static void giveOrStore(ServerPlayer player, UUID targetId, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        stack.shrink(StorageServerStub.insertIntoTerminal(player, targetId, stack, stack.getCount()));
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
     * Pushes an edit to the terminal that moved no items — a mode's own setting.
     *
     * <p>Reported as unchanged so the screen leaves the storage list where it is: nothing entered or
     * left a storage, and reordering it on every keystroke of a rename would be a round trip per
     * letter.
     */
    private static GridState store(ServerPlayer player, ItemStack terminal) {
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return new GridState(CraftingTerminalGrid.read(terminal), player.containerMenu.getCarried(), false);
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
     * crafting terminal working against the target it names.
     */
    public static final class CraftingTerminalValidator implements IRemoteCallableValidator {
        @Override
        public boolean validate(IPayloadContext context, Method method, Object[] args) {
            if (!(context.player() instanceof ServerPlayer player)
                || args.length < 2
                || !(args[0] instanceof UUID playerId)
                || !(args[1] instanceof UUID targetId)) {
                return false;
            }
            return player.getGameProfile().getId().equals(playerId)
                   && !CraftingTerminalGrid.findTerminal(player.getInventory(), targetId).isEmpty();
        }
    }
}
