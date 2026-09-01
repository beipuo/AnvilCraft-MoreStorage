package dev.anvilcraft.addon.morestorage.terminal;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * The four workbenches a crafting terminal can be.
 *
 * <p>There is one terminal item per connection kind, not one per workbench: the same stack is a
 * crafting table, a stonecutter, a smithing table or an anvil depending on which of the four buttons
 * down the right-hand side was pressed last. The mode is remembered on the stack, so it survives the
 * screen being closed and the server can read it back on the next click.
 *
 * <p>Declaration order is the order the buttons are drawn in, top to bottom, and it is also the
 * button's row in {@code select_button.png} — {@link #ordinal()} is what picks the icon out of the
 * strip, so a new mode belongs at the end unless the texture grows a row in the middle too.
 *
 * <p>Every mode's inputs live in the same nine-slot container the crafting grid uses, taking the
 * first {@link #inputSlots()} of them; switching mode empties it, so no item is ever left in a slot
 * the current mode does not draw.
 */
public enum TerminalMode implements StringRepresentable {
    /** A crafting table: the full 3×3 grid. */
    CRAFTING("crafting", CraftingTerminalGrid.SIZE, "container.crafting"),
    /** A stonecutter: one input, and a recipe picked from the ones that input matches. */
    STONECUTTING("stonecutting", 1, "container.stonecutter"),
    /** A smithing table: template, base, addition. */
    SMITHING("smithing", 3, "container.upgrade"),
    /** An anvil: two inputs, a name field and a level cost. */
    ANVIL("anvil", 2, "container.repair");

    public static final Codec<TerminalMode> CODEC = StringRepresentable.fromEnum(TerminalMode::values);

    /**
     * Unknown names decode as {@link #CRAFTING} rather than throwing: the only way one can turn up is
     * a stack written by a later version of this addon, and a terminal that opens on the crafting grid
     * is a better answer to that than a disconnect.
     */
    public static final StreamCodec<ByteBuf, TerminalMode> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
        .map(TerminalMode::byName, TerminalMode::getSerializedName);

    private final String name;
    private final int inputSlots;
    private final String labelKey;

    TerminalMode(String name, int inputSlots, String labelKey) {
        this.name = name;
        this.inputSlots = inputSlots;
        this.labelKey = labelKey;
    }

    public static TerminalMode byName(String name) {
        for (TerminalMode mode : TerminalMode.values()) {
            if (mode.name.equals(name)) {
                return mode;
            }
        }
        return TerminalMode.CRAFTING;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /** How many of the nine slots this mode uses, counting from the first. */
    public int inputSlots() {
        return this.inputSlots;
    }

    /**
     * The bar above the inputs, and the switch button's tooltip.
     *
     * <p>All four are vanilla's own container titles, so every mode reads correctly in every language
     * without this addon shipping a translation for it.
     */
    public String labelKey() {
        return this.labelKey;
    }
}
