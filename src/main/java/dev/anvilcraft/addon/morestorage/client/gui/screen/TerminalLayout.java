package dev.anvilcraft.addon.morestorage.client.gui.screen;

import dev.anvilcraft.addon.morestorage.AnvilCraftMoreStorage;
import dev.anvilcraft.addon.morestorage.terminal.TerminalMode;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

/**
 * Where each terminal mode's background puts its slots.
 *
 * <p>All four backgrounds are the same 300×303 panel and differ only in the middle strip, so the
 * screen swaps this table rather than its geometry: the storage grid, the category list, the player
 * inventory and both transfer buttons stay exactly where they were in every mode.
 *
 * <p>Every coordinate is relative to the top-left of that panel, and slot coordinates are the
 * <em>item's</em> top-left — the 16×16 the item is drawn into, one pixel inside the frame around it.
 */
public final class TerminalLayout {
    /** Distance between neighbouring slots in a row or column of the crafting grid. */
    private static final int SLOT_STEP = 18;

    /** The switch buttons, hanging off the right edge of the 300-wide panel. */
    public static final int MODE_BUTTON_X = 301;
    public static final int MODE_BUTTON_Y = 138;
    public static final int MODE_BUTTON_SIZE = 18;
    /** One pixel of gap between buttons, matching the strip they are cut from. */
    public static final int MODE_BUTTON_STRIDE = 19;

    /** The anvil's name field: the lavender box's own bounds. */
    public static final int ANVIL_NAME_X = 172;
    public static final int ANVIL_NAME_Y = 155;
    public static final int ANVIL_NAME_WIDTH = 112;
    public static final int ANVIL_NAME_HEIGHT = 18;

    /**
     * Where the anvil's level cost is written, centred on this x.
     *
     * <p>Chosen to land exactly on the yellow-green line already drawn into {@code anvil_terminal.png}
     * under the arrow: one text line tall, in vanilla's own colour for a cost the player can pay. If
     * that line is a mock-up rather than a frame for the number, it wants deleting from the texture —
     * nothing here can paint over it.
     */
    public static final int ANVIL_COST_CENTRE_X = 227;
    public static final int ANVIL_COST_Y = 197;

    /** The stonecutter's recipe picker: four columns of the recipes its input matches. */
    public static final int RECIPE_X = 157;
    public static final int RECIPE_Y = 153;
    public static final int RECIPE_COLUMNS = 4;
    public static final int RECIPE_ROWS = 3;
    public static final int RECIPE_CELL_WIDTH = 16;
    public static final int RECIPE_CELL_HEIGHT = 17;

    private static final Map<TerminalMode, TerminalLayout> BY_MODE = TerminalLayout.byMode();

    private final ResourceLocation background;
    private final int[] slotX;
    private final int[] slotY;
    private final int resultX;
    private final int resultY;
    private final int resultAreaX;
    private final int resultAreaY;
    private final int resultAreaSize;

    private TerminalLayout(
        String background,
        int[] slotX,
        int[] slotY,
        int resultX,
        int resultY,
        int resultAreaX,
        int resultAreaY,
        int resultAreaSize
    ) {
        this.background = AnvilCraftMoreStorage.of("textures/gui/misc/background/" + background + ".png");
        this.slotX = slotX;
        this.slotY = slotY;
        this.resultX = resultX;
        this.resultY = resultY;
        this.resultAreaX = resultAreaX;
        this.resultAreaY = resultAreaY;
        this.resultAreaSize = resultAreaSize;
    }

    public static TerminalLayout of(TerminalMode mode) {
        return TerminalLayout.BY_MODE.get(mode);
    }

    private static Map<TerminalMode, TerminalLayout> byMode() {
        Map<TerminalMode, TerminalLayout> layouts = new EnumMap<>(TerminalMode.class);
        // The 3×3 grid, and a result frame 24 wide rather than 18 — the one mode whose result sits in a
        // bigger box than the item it holds.
        layouts.put(TerminalMode.CRAFTING, new TerminalLayout(
            "crafting_terminal",
            TerminalLayout.row(136, TerminalLayout.SLOT_STEP, 3, 3),
            TerminalLayout.column(154, TerminalLayout.SLOT_STEP, 3, 3),
            230, 172,
            226, 168, 24
        ));
        layouts.put(TerminalMode.STONECUTTING, new TerminalLayout(
            "stonecutter_terminal",
            new int[]{125},
            new int[]{170},
            248, 170,
            244, 166, 24
        ));
        layouts.put(TerminalMode.SMITHING, new TerminalLayout(
            "smithing_terminal",
            new int[]{129, 147, 165},
            new int[]{172, 172, 172},
            219, 172,
            218, 171, 18
        ));
        layouts.put(TerminalMode.ANVIL, new TerminalLayout(
            "anvil_terminal",
            new int[]{141, 190},
            new int[]{183, 183},
            248, 183,
            247, 182, 18
        ));
        return layouts;
    }

    /** The x of every slot in a {@code columns} × {@code rows} grid, row-major. */
    private static int[] row(int first, int step, int columns, int rows) {
        int[] xs = new int[columns * rows];
        for (int slot = 0; slot < xs.length; slot++) {
            xs[slot] = first + step * (slot % columns);
        }
        return xs;
    }

    /** The y of every slot in the same grid. */
    private static int[] column(int first, int step, int columns, int rows) {
        int[] ys = new int[columns * rows];
        for (int slot = 0; slot < ys.length; slot++) {
            ys[slot] = first + step * (slot / columns);
        }
        return ys;
    }

    /** The background to blit instead of the storage station's. */
    public ResourceLocation background() {
        return this.background;
    }

    public int slotX(int slot) {
        return this.slotX[slot];
    }

    public int slotY(int slot) {
        return this.slotY[slot];
    }

    /** Top-left of the item in the result slot. */
    public int resultX() {
        return this.resultX;
    }

    public int resultY() {
        return this.resultY;
    }

    /** Top-left of the box a click on the result has to land in, which is the frame, not the item. */
    public int resultAreaX() {
        return this.resultAreaX;
    }

    public int resultAreaY() {
        return this.resultAreaY;
    }

    public int resultAreaSize() {
        return this.resultAreaSize;
    }
}
