package dev.anvilcraft.addon.morestorage.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.anvilcraft.addon.morestorage.AnvilCraftMoreStorage;
import dev.anvilcraft.addon.morestorage.client.gui.ICategoryListRows;
import dev.anvilcraft.addon.morestorage.client.gui.IStorageScreenLayout;
import dev.anvilcraft.addon.morestorage.client.gui.component.TerminalModeButton;
import dev.anvilcraft.addon.morestorage.client.rpc.CraftingTerminalClientStub;
import dev.anvilcraft.addon.morestorage.mixin.StorageScreenInvoker;
import dev.anvilcraft.addon.morestorage.rpc.CraftingTerminalServerStub;
import dev.anvilcraft.addon.morestorage.terminal.CraftingTerminalGrid;
import dev.anvilcraft.addon.morestorage.terminal.TerminalMode;
import dev.anvilcraft.addon.morestorage.terminal.TerminalRecipes;
import dev.anvilcraft.addon.morestorage.terminal.TerminalState;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.client.gui.component.category.CategoryList;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

/**
 * AnvilCraft's storage screen, 81 pixels taller, with a workbench in the gap.
 *
 * <p>The parent class is reused wholesale — search, categories, the storage list, the slider and
 * both inventory transfer buttons all keep working — and everything this screen adds lives in the
 * strip the taller background opened up between the storage grid and the player inventory. The
 * numbers the parent hard-codes are supplied through {@link IStorageScreenLayout}, which its mixin
 * reads; the two render hooks are where the workbench is actually drawn.
 *
 * <p>Which workbench that is comes from the terminal's {@link TerminalMode}: the four buttons down the
 * right-hand side swap the background and, with it, the whole {@link TerminalLayout}. Every mode draws
 * its inputs from the same nine slots, so most of the screen does not care which one is showing — it
 * asks the layout where slot <var>n</var> goes and the mode how many there are. Only the stonecutter's
 * recipe picker and the anvil's name field and level cost are mode-specific enough to be drawn by name.
 *
 * <p>Nothing here owns any of that state. It is read out of the bound terminal stack every frame, so
 * the server's edits show up through ordinary inventory sync and a click only has to say what was
 * clicked.
 */
public class CraftingTerminalScreen extends StorageScreen implements IStorageScreenLayout {
    private static final ResourceLocation CLEAR =
        AnvilCraftMoreStorage.of("textures/gui/misc/crafting_terminal/clear.png");

    private static final int BG_HEIGHT = 303;
    private static final int BG_TEXTURE_HEIGHT = 512;
    private static final int INVENTORY_Y = 221;
    private static final int DEPOSIT_BUTTON_Y = 220;
    private static final int WITHDRAW_BUTTON_Y = 242;
    private static final int CATEGORY_ROWS = 12;
    private static final int CATEGORY_HEIGHT = 240;

    private static final int CLEAR_X = 194;
    private static final int CLEAR_Y = 152;
    private static final int CLEAR_WIDTH = 9;
    private static final int CLEAR_HEIGHT = 11;

    /**
     * The two label bars the taller background added, both drawn in the parent's title bar style. Each
     * one is the lavender fill's own box, so the text sits exactly inside it.
     */
    private static final int LABEL_X = 108;
    private static final int MODE_LABEL_Y = 139;
    private static final int MODE_LABEL_WIDTH = 102;
    private static final int INVENTORY_LABEL_Y = 209;
    private static final int INVENTORY_LABEL_WIDTH = 75;
    private static final int LABEL_COLOR = 0xFF404040;
    private static final int ANVIL_NAME_COLOR = 0xFFFFFFFF;

    /** Vanilla's own two colours for an anvil's level cost: affordable, and not. */
    private static final int COST_COLOR = 0xFF80FF20;
    private static final int COST_DENIED_COLOR = 0xFFFF6060;

    /** The wash behind the stonecutter recipe the player picked. */
    private static final int SELECTED_RECIPE_COLOR = 0x60FFFFFF;

    /** Vanilla's cap on a renamed item, so the field cannot hold more than the server will keep. */
    private static final int MAX_NAME_LENGTH = 50;

    /** The two anvil name-field variants embedded in {@code anvil_terminal.png}. */
    private static final int ANVIL_NAME_TEXTURE_X = 107;
    private static final int ANVIL_NAME_TEXTURE_EMPTY_Y = 319;
    private static final int ANVIL_NAME_TEXTURE_FILLED_Y = 303;
    private static final int ANVIL_NAME_TEXTURE_WIDTH = 110;
    private static final int ANVIL_NAME_TEXTURE_HEIGHT = 16;

    private final UUID targetId;

    /**
     * The clear button, shown only in the crafting mode.
     *
     * <p>Its spot in the background — just above the grid — is the anvil's name field and the middle of
     * the stonecutter's recipe picker in the other layouts, so there is nowhere for it to sit there.
     * Shift-clicking a slot still sends it home in every mode, and switching mode empties the whole grid
     * anyway, so nothing is out of reach.
     */
    private @Nullable TexturedButton clearButton;

    /** The anvil mode's name field, present in every mode but only visible in that one. */
    private @Nullable EditBox nameField;

    /** The mode the widgets were last brought in line with, so a switch is noticed once. */
    private TerminalMode syncedMode = TerminalMode.CRAFTING;

    /** Set while the name field is being filled in from the terminal, so it does not echo back. */
    private boolean syncingName;

    /** The base input used the last time the anvil name field was synchronised. */
    private ItemStack syncedAnvilInput = ItemStack.EMPTY;

    /** First visible row of the stonecutter's recipe picker. */
    private int recipeScroll;

    /** The input the cached recipe list was looked up for. */
    private ItemStack recipeInput = ItemStack.EMPTY;
    private List<RecipeHolder<StonecutterRecipe>> recipeList = List.of();

    /** Everything the cached outcome was computed from, so it is recomputed only when one changes. */
    private @Nullable TerminalMode outcomeMode;
    private List<ItemStack> outcomeInputs = List.of();
    private String outcomeName = "";
    private int outcomeChoice = -1;
    private TerminalRecipes.Outcome outcome = TerminalRecipes.Outcome.EMPTY;

    public CraftingTerminalScreen(BlockPos sourcePos, UUID targetId, Component title) {
        super(sourcePos, title);
        this.targetId = targetId;
    }

    public static void openScreen(BlockPos sourcePos, UUID targetId, Component title) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        CraftingTerminalScreen screen = new CraftingTerminalScreen(sourcePos, targetId, title);
        minecraft.player.containerMenu = screen.getMenu();
        minecraft.setScreen(screen);
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
        this.clearButton = this.addRenderableWidget(new TexturedButton(
            this.getLeftPos() + CraftingTerminalScreen.CLEAR_X,
            this.getTopPos() + CraftingTerminalScreen.CLEAR_Y,
            CraftingTerminalScreen.CLEAR_WIDTH,
            CraftingTerminalScreen.CLEAR_HEIGHT,
            CraftingTerminalScreen.CLEAR,
            CraftingTerminalScreen.CLEAR_HEIGHT,
            CraftingTerminalScreen.CLEAR_WIDTH,
            CraftingTerminalScreen.CLEAR_HEIGHT * 2,
            button -> this.request(CraftingTerminalClientStub.clearGrid(this.targetId))
        ));
        for (TerminalMode mode : TerminalMode.values()) {
            this.addRenderableWidget(new TerminalModeButton(
                this.getLeftPos() + TerminalLayout.MODE_BUTTON_X,
                this.getTopPos() + TerminalLayout.MODE_BUTTON_Y + TerminalLayout.MODE_BUTTON_STRIDE * mode.ordinal(),
                mode,
                this::mode,
                button -> this.request(CraftingTerminalClientStub.setMode(this.targetId, mode))
            ));
        }
        // Unbordered, like the parent's search box: the box it sits in is part of the background.
        this.nameField = this.addRenderableWidget(new EditBox(
            this.font,
            this.getLeftPos() + TerminalLayout.ANVIL_NAME_X + 4,
            this.getTopPos() + TerminalLayout.ANVIL_NAME_Y + 5,
            TerminalLayout.ANVIL_NAME_WIDTH - 8,
            9,
            Component.translatable(TerminalMode.ANVIL.labelKey())
        ));
        this.nameField.setBordered(false);
        this.nameField.setMaxLength(CraftingTerminalScreen.MAX_NAME_LENGTH);
        this.nameField.setTextColor(CraftingTerminalScreen.ANVIL_NAME_COLOR);
        this.syncName();
        this.nameField.setResponder(this::onNameChanged);
        this.syncedMode = this.mode();
    }

    @Override
    public int getImageHeight() {
        return CraftingTerminalScreen.BG_HEIGHT;
    }

    /**
     * Brings the widgets in line with the mode before the parent draws them.
     *
     * <p>The mode can change while the screen is open, and {@code init} has long since run, so the
     * name field is shown or hidden here rather than added and removed.
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        TerminalMode mode = this.mode();
        if (mode == TerminalMode.ANVIL && !ItemStack.matches(this.syncedAnvilInput, this.grid().get(0))) {
            this.syncName();
        }
        if (this.clearButton != null) {
            this.clearButton.visible = mode == TerminalMode.CRAFTING;
        }
        if (this.nameField != null) {
            boolean anvil = mode == TerminalMode.ANVIL;
            boolean hasInput = anvil && !this.grid().get(0).isEmpty();
            this.nameField.visible = hasInput;
            this.nameField.setEditable(hasInput);
            if (!anvil && this.nameField.isFocused()) {
                this.setFocused(null);
            }
        }
        if (mode != this.syncedMode) {
            this.syncedMode = mode;
            this.recipeScroll = 0;
            this.syncName();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 || button == 1) {
            TerminalMode mode = this.mode();
            int slot = this.inputSlotAt(mode, mouseX, mouseY);
            if (slot != -1) {
                this.request(Screen.hasShiftDown()
                    ? CraftingTerminalClientStub.gridQuickMove(this.targetId, slot)
                    : CraftingTerminalClientStub.gridClick(this.targetId, slot, button));
                return true;
            }
            if (this.isOverResult(mode, mouseX, mouseY)) {
                this.request(CraftingTerminalClientStub.craft(this.targetId, Screen.hasShiftDown()));
                return true;
            }
            if (mode == TerminalMode.STONECUTTING) {
                int recipe = this.recipeAt(mouseX, mouseY);
                if (recipe != -1) {
                    this.request(CraftingTerminalClientStub.setStonecutterChoice(this.targetId, recipe));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && this.mode() == TerminalMode.STONECUTTING && this.isOverRecipes(mouseX, mouseY)) {
            this.recipeScroll = Mth.clamp(
                this.recipeScroll + (scrollY > 0 ? -1 : 1),
                0,
                CraftingTerminalScreen.maxRecipeScroll(this.recipes(this.grid()).size())
            );
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /**
     * Keeps typing in the name field from reaching the rest of the screen.
     *
     * <p>Without this the inventory key would close the terminal the moment it was typed into a name,
     * which is the same reason vanilla's anvil screen guards its own field with {@code canConsumeInput}.
     * Escape is deliberately let through, so it still closes the screen.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.nameField != null && this.nameField.isFocused() && keyCode != InputConstants.KEY_ESCAPE) {
            return this.nameField.keyPressed(keyCode, scanCode, modifiers) || this.nameField.canConsumeInput();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public ResourceLocation moreStorage$background() {
        return TerminalLayout.of(this.mode()).background();
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
        TerminalMode mode = this.mode();
        TerminalLayout layout = TerminalLayout.of(mode);
        this.renderSectionLabels(graphics, mode);
        List<ItemStack> inputs = this.grid();
        if (mode == TerminalMode.ANVIL) {
            this.renderAnvilNameBackground(graphics, inputs.get(0));
        }
        for (int slot = 0; slot < mode.inputSlots(); slot++) {
            int x = this.getLeftPos() + layout.slotX(slot);
            int y = this.getTopPos() + layout.slotY(slot);
            this.renderSlot(graphics, inputs.get(slot), x, y, this.isOver(mouseX, mouseY, x, y));
        }
        if (mode == TerminalMode.STONECUTTING) {
            this.renderRecipes(graphics, inputs, mouseX, mouseY);
        }
        TerminalRecipes.Outcome result = this.outcome(mode, inputs);
        this.renderSlot(
            graphics,
            result.result(),
            this.getLeftPos() + layout.resultX(),
            this.getTopPos() + layout.resultY(),
            this.isOverResult(mode, mouseX, mouseY)
        );
        if (mode == TerminalMode.ANVIL) {
            this.renderAnvilCost(graphics, result);
        }
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
     * in the bar, dark grey, no shadow. Both strings are vanilla's own — the mode's container title and
     * the inventory's — so every mode reads correctly in every language without this addon shipping a
     * translation for it.
     */
    private void renderSectionLabels(GuiGraphics graphics, TerminalMode mode) {
        this.renderLabel(
            graphics,
            Component.translatable(mode.labelKey()),
            CraftingTerminalScreen.MODE_LABEL_Y,
            CraftingTerminalScreen.MODE_LABEL_WIDTH
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

    /**
     * The level cost of the anvil's result, in vanilla's colours: green when the player can pay it, red
     * when they cannot. A result that costs nothing writes nothing — that is a plain rename of an
     * unenchanted item, and vanilla shows no number for it either.
     */
    private void renderAnvilCost(GuiGraphics graphics, TerminalRecipes.Outcome result) {
        if (result.cost() <= 0) {
            return;
        }
        Component cost = Component.literal(Integer.toString(result.cost()));
        graphics.drawString(
            this.font,
            cost,
            this.getLeftPos() + TerminalLayout.ANVIL_COST_CENTRE_X - this.font.width(cost) / 2,
            this.getTopPos() + TerminalLayout.ANVIL_COST_Y,
            result.takeable() ? CraftingTerminalScreen.COST_COLOR : CraftingTerminalScreen.COST_DENIED_COLOR,
            true
        );
    }

    /**
     * The recipes the stonecutter's input matches, four to a row, with the picked one washed light.
     *
     * <p>Each cell shows what that recipe makes, which is the only thing that tells two of them apart.
     */
    private void renderRecipes(GuiGraphics graphics, List<ItemStack> inputs, int mouseX, int mouseY) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        List<RecipeHolder<StonecutterRecipe>> recipes = this.recipes(inputs);
        int choice = this.choice();
        int visible = TerminalLayout.RECIPE_COLUMNS * TerminalLayout.RECIPE_ROWS;
        for (int cell = 0; cell < visible; cell++) {
            int index = this.recipeScroll * TerminalLayout.RECIPE_COLUMNS + cell;
            if (index >= recipes.size()) {
                break;
            }
            int x = this.recipeX(cell);
            int y = this.recipeY(cell);
            if (index == choice) {
                graphics.fill(x, y, x + 16, y + 16, CraftingTerminalScreen.SELECTED_RECIPE_COLOR);
            }
            ItemStack result = TerminalRecipes.assembleStonecutting(level, recipes.get(index), inputs.get(0));
            graphics.renderItem(result, x, y);
            graphics.renderItemDecorations(this.font, result, x, y);
            if (this.isOver(mouseX, mouseY, x, y)) {
                AbstractContainerScreen.renderSlotHighlight(graphics, x, y, 0);
            }
        }
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
        TerminalMode mode = this.mode();
        List<ItemStack> inputs = this.grid();
        int slot = this.inputSlotAt(mode, mouseX, mouseY);
        if (slot != -1) {
            return inputs.get(slot);
        }
        if (this.isOverResult(mode, mouseX, mouseY)) {
            return this.outcome(mode, inputs).result();
        }
        if (mode == TerminalMode.STONECUTTING) {
            int recipe = this.recipeAt(mouseX, mouseY);
            Level level = Minecraft.getInstance().level;
            List<RecipeHolder<StonecutterRecipe>> recipes = this.recipes(inputs);
            if (recipe != -1 && level != null && recipe < recipes.size()) {
                return TerminalRecipes.assembleStonecutting(level, recipes.get(recipe), inputs.get(0));
            }
        }
        return ItemStack.EMPTY;
    }

    /** Which of the mode's input slots the mouse is over, or {@code -1}. */
    private int inputSlotAt(TerminalMode mode, double mouseX, double mouseY) {
        TerminalLayout layout = TerminalLayout.of(mode);
        for (int slot = 0; slot < mode.inputSlots(); slot++) {
            int x = this.getLeftPos() + layout.slotX(slot);
            int y = this.getTopPos() + layout.slotY(slot);
            if (this.isOver(mouseX, mouseY, x, y)) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * Whether the mouse is inside the 18×18 cell around the 16×16 item at {@code x, y} — one pixel
     * of the border on each side, so neighbouring slots never both claim a pixel.
     */
    private boolean isOver(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x - 1 && mouseX <= x + 16 && mouseY >= y - 1 && mouseY <= y + 16;
    }

    private boolean isOverResult(TerminalMode mode, double mouseX, double mouseY) {
        TerminalLayout layout = TerminalLayout.of(mode);
        int x = this.getLeftPos() + layout.resultAreaX();
        int y = this.getTopPos() + layout.resultAreaY();
        int size = layout.resultAreaSize() - 1;
        return mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size;
    }

    private int recipeX(int cell) {
        return this.getLeftPos() + TerminalLayout.RECIPE_X
               + TerminalLayout.RECIPE_CELL_WIDTH * (cell % TerminalLayout.RECIPE_COLUMNS);
    }

    private int recipeY(int cell) {
        return this.getTopPos() + TerminalLayout.RECIPE_Y
               + TerminalLayout.RECIPE_CELL_HEIGHT * (cell / TerminalLayout.RECIPE_COLUMNS);
    }

    /** Which recipe the mouse is over, as an index into the whole list, or {@code -1}. */
    private int recipeAt(double mouseX, double mouseY) {
        int visible = TerminalLayout.RECIPE_COLUMNS * TerminalLayout.RECIPE_ROWS;
        for (int cell = 0; cell < visible; cell++) {
            int x = this.recipeX(cell);
            int y = this.recipeY(cell);
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                int index = this.recipeScroll * TerminalLayout.RECIPE_COLUMNS + cell;
                return index < this.recipes(this.grid()).size() ? index : -1;
            }
        }
        return -1;
    }

    private boolean isOverRecipes(double mouseX, double mouseY) {
        int x = this.getLeftPos() + TerminalLayout.RECIPE_X;
        int y = this.getTopPos() + TerminalLayout.RECIPE_Y;
        return mouseX >= x
               && mouseX < x + TerminalLayout.RECIPE_CELL_WIDTH * TerminalLayout.RECIPE_COLUMNS
               && mouseY >= y
               && mouseY < y + TerminalLayout.RECIPE_CELL_HEIGHT * TerminalLayout.RECIPE_ROWS;
    }

    /** How far down the picker can scroll when it holds {@code count} recipes. */
    private static int maxRecipeScroll(int count) {
        int rows = (count + TerminalLayout.RECIPE_COLUMNS - 1) / TerminalLayout.RECIPE_COLUMNS;
        return Math.max(0, rows - TerminalLayout.RECIPE_ROWS);
    }

    /**
     * The stonecutting recipes the input matches, cached against that input — the lookup filters and
     * sorts every stonecutting recipe in the game and this is asked for once a frame.
     */
    private List<RecipeHolder<StonecutterRecipe>> recipes(List<ItemStack> inputs) {
        ItemStack input = inputs.get(0);
        if (!ItemStack.isSameItemSameComponents(this.recipeInput, input)) {
            this.recipeInput = input.copy();
            Level level = Minecraft.getInstance().level;
            this.recipeList = level == null ? List.of() : TerminalRecipes.stonecutting(level, input);
            this.recipeScroll = Mth.clamp(
                this.recipeScroll,
                0,
                CraftingTerminalScreen.maxRecipeScroll(this.recipeList.size())
            );
        }
        return this.recipeList;
    }
    /**
     * What the terminal currently makes, cached against everything that decides it.
     *
     * <p>Worth caching in every mode and unavoidable in one: the anvil's answer comes out of a whole
     * {@link net.minecraft.world.inventory.AnvilMenu} built for the occasion, which is not something to
     * do sixty times a second.
     */
    private TerminalRecipes.Outcome outcome(TerminalMode mode, List<ItemStack> inputs) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return TerminalRecipes.Outcome.EMPTY;
        }
        String name = this.anvilName();
        int choice = this.choice();
        if (mode == this.outcomeMode
            && choice == this.outcomeChoice
            && name.equals(this.outcomeName)
            && this.matchesCachedInputs(inputs)) {
            return this.outcome;
        }
        this.outcomeMode = mode;
        this.outcomeChoice = choice;
        this.outcomeName = name;
        this.outcomeInputs = inputs.stream().map(ItemStack::copy).toList();
        this.outcome = TerminalRecipes.outcome(player, mode, inputs, name, choice);
        return this.outcome;
    }

    private boolean matchesCachedInputs(List<ItemStack> inputs) {
        if (this.outcomeInputs.size() != inputs.size()) {
            return false;
        }
        for (int slot = 0; slot < inputs.size(); slot++) {
            if (!ItemStack.matches(this.outcomeInputs.get(slot), inputs.get(slot))) {
                return false;
            }
        }
        return true;
    }

    /** Fills the name field in from the terminal without that counting as the player typing. */
    private void syncName() {
        if (this.nameField == null) {
            return;
        }
        ItemStack input = this.grid().get(0);
        this.syncingName = true;
        String name = TerminalState.anvilName(this.terminal());
        this.nameField.setValue(name.isEmpty() && !input.isEmpty() ? input.getHoverName().getString() : name);
        this.syncingName = false;
        this.syncedAnvilInput = input.copy();
    }

    /** Draws the empty or filled name-field strip from the matching part of the anvil background. */
    private void renderAnvilNameBackground(GuiGraphics graphics, ItemStack input) {
        int sourceY = input.isEmpty()
            ? CraftingTerminalScreen.ANVIL_NAME_TEXTURE_EMPTY_Y
            : CraftingTerminalScreen.ANVIL_NAME_TEXTURE_FILLED_Y;
        graphics.blit(
            TerminalLayout.of(TerminalMode.ANVIL).background(),
            this.getLeftPos() + TerminalLayout.ANVIL_NAME_X + 1,
            this.getTopPos() + TerminalLayout.ANVIL_NAME_Y + 1,
            CraftingTerminalScreen.ANVIL_NAME_TEXTURE_X,
            sourceY,
            CraftingTerminalScreen.ANVIL_NAME_TEXTURE_WIDTH,
            CraftingTerminalScreen.ANVIL_NAME_TEXTURE_HEIGHT,
            CraftingTerminalScreen.BG_TEXTURE_HEIGHT,
            CraftingTerminalScreen.BG_TEXTURE_HEIGHT
        );
    }

    private void onNameChanged(String name) {
        if (!this.syncingName) {
            this.request(CraftingTerminalClientStub.setAnvilName(this.targetId, name));
        }
    }

    /**
     * The name to preview with: what is in the field rather than what the server has echoed back, so
     * the result and its cost follow the typing instead of trailing a round trip behind it.
     */
    private String anvilName() {
        return this.nameField == null ? TerminalState.anvilName(this.terminal()) : this.nameField.getValue();
    }

    private int choice() {
        return TerminalState.stonecutterChoice(this.terminal());
    }

    private TerminalMode mode() {
        return TerminalState.mode(this.terminal());
    }

    /** The terminal this screen was opened from, or empty if the player no longer carries it. */
    private ItemStack terminal() {
        Player player = Minecraft.getInstance().player;
        return player == null
            ? ItemStack.EMPTY
            : CraftingTerminalGrid.findTerminal(player.getInventory(), this.targetId);
    }

    /** The live input slots, straight off that terminal. */
    private List<ItemStack> grid() {
        ItemStack terminal = this.terminal();
        return terminal.isEmpty()
            ? NonNullList.withSize(CraftingTerminalGrid.SIZE, ItemStack.EMPTY)
            : CraftingTerminalGrid.read(terminal);
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
