package dev.anvilcraft.addon.morestorage.init;

import com.mojang.serialization.Codec;
import dev.anvilcraft.addon.morestorage.AnvilCraftMoreStorage;
import dev.anvilcraft.addon.morestorage.terminal.TerminalMode;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.UnaryOperator;

/**
 * Data components this addon attaches to its own items.
 */
public class AddonComponents {
    private static final DeferredRegister<DataComponentType<?>> DEFERRED_REGISTER = DeferredRegister.create(
        Registries.DATA_COMPONENT_TYPE,
        AnvilCraftMoreStorage.MOD_ID
    );

    /**
     * The nine crafting slots of a hyperdimension crafting terminal.
     *
     * <p>The grid holds real items, so it lives on the terminal stack itself: that makes it
     * persistent, survives the screen being closed, and — because the terminal is an inventory item
     * — reaches the client through the ordinary container sync instead of a bespoke packet.
     *
     * <p>Every mode's inputs share it, taking the first {@link TerminalMode#inputSlots()} slots.
     */
    public static final DataComponentType<ItemContainerContents> CRAFTING_GRID = register(
        "crafting_grid",
        builder -> builder
            .persistent(ItemContainerContents.CODEC)
            .networkSynchronized(ItemContainerContents.STREAM_CODEC)
    );

    /** Which of the four workbenches the terminal currently is. */
    public static final DataComponentType<TerminalMode> TERMINAL_MODE = register(
        "terminal_mode",
        builder -> builder
            .persistent(TerminalMode.CODEC)
            .networkSynchronized(TerminalMode.STREAM_CODEC)
    );

    /**
     * What the anvil mode renames its result to.
     *
     * <p>Kept on the stack rather than in the screen because the server is the one that applies it:
     * the client only reports what was typed, exactly as vanilla's rename packet does.
     */
    public static final DataComponentType<String> ANVIL_NAME = register(
        "anvil_name",
        builder -> builder
            .persistent(Codec.STRING)
            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
    );

    /** Which of the recipes the stonecutter mode's input matches is selected. */
    public static final DataComponentType<Integer> STONECUTTER_CHOICE = register(
        "stonecutter_choice",
        builder -> builder
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
    );

    private AddonComponents() {
    }

    private static <T> DataComponentType<T> register(
        String name,
        UnaryOperator<DataComponentType.Builder<T>> customizer
    ) {
        DataComponentType<T> type = customizer.apply(DataComponentType.builder()).build();
        DEFERRED_REGISTER.register(name, () -> type);
        return type;
    }

    public static void register(IEventBus modEventBus) {
        DEFERRED_REGISTER.register(modEventBus);
    }
}
