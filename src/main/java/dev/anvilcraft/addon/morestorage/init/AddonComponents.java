package dev.anvilcraft.addon.morestorage.init;

import dev.anvilcraft.addon.morestorage.AnvilCraftMoreStorage;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
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
     */
    public static final DataComponentType<ItemContainerContents> CRAFTING_GRID = register(
        "crafting_grid",
        builder -> builder
            .persistent(ItemContainerContents.CODEC)
            .networkSynchronized(ItemContainerContents.STREAM_CODEC)
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
