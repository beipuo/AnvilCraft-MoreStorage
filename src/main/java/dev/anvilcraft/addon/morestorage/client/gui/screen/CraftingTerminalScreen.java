package dev.anvilcraft.addon.morestorage.client.gui.screen;

import dev.anvilcraft.addon.morestorage.AnvilCraftMoreStorage;
import dev.anvilcraft.addon.morestorage.client.gui.ICategoryListRows;
import dev.anvilcraft.addon.morestorage.client.gui.IStorageScreenLayout;
import dev.anvilcraft.addon.morestorage.client.rpc.CraftingTerminalClientStub;
import dev.anvilcraft.addon.morestorage.mixin.StorageScreenInvoker;
import dev.anvilcraft.addon.morestorage.rpc.CraftingTerminalServerStub;
import dev.anvilcraft.addon.morestorage.terminal.CraftingTerminalGrid;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.client.gui.component.category.CategoryList;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * AnvilCraft's storage screen, 81 pixels taller, with a crafting grid in the gap.
 *
 * <p>The parent class is reused wholesale — search, categories, the storage list, the slider and
 * both inventory transfer buttons all keep working — and everything this screen adds lives in the
 * strip the taller background opened up between the storage grid and the player inventory. The
 * numbers the parent hard-codes are supplied through {@link IStorageScreenLayout}, which its mixin
 * reads; the two render hooks are where the crafting slots are actually drawn.
 *
 * <p>Nothing here owns the grid. It is read out of the bound terminal stack every frame, so the
 * server's edits show up through ordinary inventory sync and a click only has to say what was
 * clicked.
 */
public class CraftingTerminalScreen extends StorageScreen implements IStorageScreenLayout {
    private static final ResourceLocation BACKGROUND =
        AnvilCraftMoreStorage.of("textures/gui/misc/background/hyperdimension_crafting_terminal.png");
    private static final ResourceLocation CLEAR =
        AnvilCraftMoreStorage.of("textures/gui/misc/hyperdimension_crafting_terminal/clear.png");

    private static final int BG_HEIGHT = 303;
    private static final int BG_TEXTURE_HEIGHT = 512;
    private static final int INVENTORY_Y = 221;
    private static final int DEPOSIT_BUTTON_Y = 220;
    private static final int WITHDRAW_BUTTON_Y = 242;
    private static final int CATEGORY_ROWS = 12;
    private static final int CATEGORY_HEIGHT = 240;

    /** Top-left of the item in the first crafting slot; the rest step by {@link #SLOT_SIZE}. */
    private static final int GRID_X = 136;
    private static final int GRID_Y = 154;
    private static final int SLOT_SIZE = 18;

    private static final int RESULT_X = 230;
    private static final int RESULT_Y = 172;
    /** The result slot's frame is 24×24, wider than the item it holds, so it gets its own box. */
    private static final int RESULT_AREA_X = 226;
    private static final int RESULT_AREA_Y = 168;
    private static final int RESULT_AREA_SIZE = 24;

    private static final int CLEAR_X = 194;
    private static final int CLEAR_Y = 152;
    private static final int CLEAR_WIDTH = 9;
    private static final int CLEAR_HEIGHT = 11;

    /**
     * The two label bars the taller background added, both drawn in the parent's title bar style. Each
     * one is the lavender fill's own box, so the text sits exactly inside it.
     */
    private static final int LABEL_X = 108;
    private static final int GRID_LABEL_Y = 139;
    private static final int GRID_LABEL_WIDTH = 102;
    private static final int INVENTORY_LABEL_Y = 209;
    private static final int INVENTORY_LABEL_WIDTH = 75;
    private static final int LABEL_COLOR = 0xFF404040;

    private final UUID storageId;

    /** The grid the cached result was computed from, so the recipe lookup is not run every frame. */
    private List<ItemStack> resultGrid = List.of();
    private ItemStack resultStack = ItemStack.EMPTY;

    public CraftingTerminalScreen(BlockPos sourcePos, UUID storageId, Component title) {
        super(sourcePos, title);
        this.storageId = storageId;
    }

    public static void openScreen(BlockPos sourcePos, UUID storageId, Component title) {
        Minecraft.getInstance().setScreen(new CraftingTerminalScreen(sourcePos, storageId, title));
    }

    @Override
    protected void init() {
        super.init();
        for (GuiEventListener child : this.children()) {
            if (child instanceof CategoryList categories) {
                categories.setHeight(CraftingTerminalScreen.CATEGORY_HEIGHT);
                ((ICategoryListRows) categories).moreStorage$setRows(CraftingTerminalScreen.CATEGORY_ROWS);
            }
        }
        this.addRenderableWidget(new TexturedButton(
            this.getLeftPos() + CraftingTerminalScreen.CLEAR_X,
            this.getTopPos() + CraftingTerminalScreen.CLEAR_Y,
            CraftingTerminalScreen.CLEAR_WIDTH,
            CraftingTerminalScreen.CLEAR_HEIGHT,
            CraftingTerminalScreen.CLEAR,
            CraftingTerminalScreen.CLEAR_HEIGHT,
            CraftingTerminalScreen.CLEAR_WIDTH,
            CraftingTerminalScreen.CLEAR_HEIGHT * 2,
            button -> this.request(CraftingTerminalClientStub.clearGrid(this.storageId))
        ));
    }

    @Override
    public int getImageHeight() {
        return CraftingTerminalScreen.BG_HEIGHT;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 || button == 1) {
            int slot = this.gridSlotAt(mouseX, mouseY);
            if (slot != -1) {
                this.request(Screen.hasShiftDown()
                    ? CraftingTerminalClientStub.gridQuickMove(this.storageId, slot)
                    : CraftingTerminalClientStub.gridClick(this.storageId, slot, button));
                return true;
            }
            if (this.isOverResult(mouseX, mouseY)) {
                this.request(CraftingTerminalClientStub.craft(this.storageId, Screen.hasShiftDown()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public ResourceLocation moreStorage$background() {
        return CraftingTerminalScreen.BACKGROUND;
    }

    @Override
    public int moreStorage$backgroundHeight() {
        return CraftingTerminalScreen.BG_HEIGHT;
    }

    @Override
    public int moreStorage$backgroundTextureHeight() {
        return CraftingTerminalScreen.BG_TEXTURE_HEIGHT;
    }

    @Override
    public int moreStorage$inventoryY() {
        return CraftingTerminalScreen.INVENTORY_Y;
    }

    @Override
    public int moreStorage$depositButtonY() {
        return CraftingTerminalScreen.DEPOSIT_BUTTON_Y;
    }

    @Override
    public int moreStorage$withdrawButtonY() {
        return CraftingTerminalScreen.WITHDRAW_BUTTON_Y;
    }

    @Override
    public int moreStorage$categoryRows() {
        return CraftingTerminalScreen.CATEGORY_ROWS;
    }

    @Override
    public void moreStorage$renderContents(GuiGraphics graphics, int mouseX, int mouseY) {
        this.renderSectionLabels(graphics);
        List<ItemStack> grid = this.grid();
        for (int slot = 0; slot < CraftingTerminalGrid.SIZE; slot++) {
            int x = this.slotX(slot);
            int y = this.slotY(slot);
            this.renderSlot(graphics, grid.get(slot), x, y, this.isOver(mouseX, mouseY, x, y));
        }
        this.renderSlot(
            graphics,
            this.result(grid),
            this.getLeftPos() + CraftingTerminalScreen.RESULT_X,
            this.getTopPos() + CraftingTerminalScreen.RESULT_Y,
            this.isOverResult(mouseX, mouseY)
        );
    }

    @Override
    public void moreStorage$renderOverlay(GuiGraphics graphics, int mouseX, int mouseY) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !player.containerMenu.getCarried().isEmpty()) {
            return;
        }
        ItemStack hovered = this.hoveredStack(mouseX, mouseY);
        if (!hovered.isEmpty()) {
            graphics.renderTooltip(this.font, hovered, mouseX, mouseY);
        }
    }

    /**
     * The two bars the taller background added, drawn the way the parent draws its own title: centred
     * in the bar, dark grey, no shadow. Both strings are vanilla's own, so the crafting grid and the
     * player inventory read correctly in every language without this addon shipping a translation.
     */
    private void renderSectionLabels(GuiGraphics graphics) {
        this.renderLabel(
            graphics,
            Component.translatable("container.crafting"),
            CraftingTerminalScreen.GRID_LABEL_Y,
            CraftingTerminalScreen.GRID_LABEL_WIDTH
        );
        this.renderLabel(
            graphics,
            Component.translatable("container.inventory"),
            CraftingTerminalScreen.INVENTORY_LABEL_Y,
            CraftingTerminalScreen.INVENTORY_LABEL_WIDTH
        );
    }

    private void renderLabel(GuiGraphics graphics, Component label, int y, int width) {
        graphics.drawString(
            this.font,
            label,
            this.getLeftPos() + CraftingTerminalScreen.LABEL_X + (width - this.font.width(label)) / 2,
            this.getTopPos() + y,
            CraftingTerminalScreen.LABEL_COLOR,
            false
        );
    }

    private void renderSlot(GuiGraphics graphics, ItemStack stack, int x, int y, boolean hovered) {
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x, y);
            graphics.renderItemDecorations(this.font, stack, x, y);
        }
        if (hovered) {
            AbstractContainerScreen.renderSlotHighlight(graphics, x, y, 0);
        }
    }

    private ItemStack hoveredStack(double mouseX, double mouseY) {
        List<ItemStack> grid = this.grid();
        int slot = this.gridSlotAt(mouseX, mouseY);
        if (slot != -1) {
            return grid.get(slot);
        }
        return this.isOverResult(mouseX, mouseY) ? this.result(grid) : ItemStack.EMPTY;
    }

    private int slotX(int slot) {
        return this.getLeftPos() + CraftingTerminalScreen.GRID_X
               + CraftingTerminalScreen.SLOT_SIZE * (slot % CraftingTerminalGrid.WIDTH);
    }

    private int slotY(int slot) {
        return this.getTopPos() + CraftingTerminalScreen.GRID_Y
               + CraftingTerminalScreen.SLOT_SIZE * (slot / CraftingTerminalGrid.WIDTH);
    }

    private int gridSlotAt(double mouseX, double mouseY) {
        for (int slot = 0; slot < CraftingTerminalGrid.SIZE; slot++) {
            if (this.isOver(mouseX, mouseY, this.slotX(slot), this.slotY(slot))) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * Whether the mouse is inside the 18×18 cell around the 16×16 item at {@code x, y} — one pixel
     * of the border on each side, so neighbouring crafting slots never both claim a pixel.
     */
    private boolean isOver(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x - 1 && mouseX <= x + 16 && mouseY >= y - 1 && mouseY <= y + 16;
    }

    private boolean isOverResult(double mouseX, double mouseY) {
        int x = this.getLeftPos() + CraftingTerminalScreen.RESULT_AREA_X;
        int y = this.getTopPos() + CraftingTerminalScreen.RESULT_AREA_Y;
        int size = CraftingTerminalScreen.RESULT_AREA_SIZE - 1;
        return mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size;
    }

    /** The live crafting grid, straight off the terminal this screen was opened from. */
    private List<ItemStack> grid() {
        Player player = Minecraft.getInstance().player;
        ItemStack terminal = player == null
            ? ItemStack.EMPTY
            : CraftingTerminalGrid.findTerminal(player.getInventory(), this.storageId);
        return terminal.isEmpty()
            ? NonNullList.withSize(CraftingTerminalGrid.SIZE, ItemStack.EMPTY)
            : CraftingTerminalGrid.read(terminal);
    }

    /**
     * What {@code grid} makes, cached against the grid it was computed from — the recipe lookup is a
     * scan over every crafting recipe and this runs once per frame.
     */
    private ItemStack result(List<ItemStack> grid) {
        if (this.matchesCachedGrid(grid)) {
            return this.resultStack;
        }
        Level level = Minecraft.getInstance().level;
        this.resultGrid = grid.stream().map(ItemStack::copy).toList();
        this.resultStack = level == null ? ItemStack.EMPTY : CraftingTerminalGrid.result(level, grid);
        return this.resultStack;
    }

    private boolean matchesCachedGrid(List<ItemStack> grid) {
        if (this.resultGrid.size() != grid.size()) {
            return false;
        }
        for (int slot = 0; slot < grid.size(); slot++) {
            if (!ItemStack.matches(this.resultGrid.get(slot), grid.get(slot))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Refreshes the storage list once the server answers, but only if something moved — crafting
     * pulls ingredients out of the storage and the clear button pushes them back, so the list on the
     * right is stale until it is reordered.
     */
    private void request(CompletableFuture<CraftingTerminalServerStub.GridState> pending) {
        pending.thenAcceptAsync(
            state -> {
                if (state.changed()) {
                    ((StorageScreenInvoker) this).moreStorage$reorder(false);
                }
            },
            this.screenExecutor
        );
    }
}
