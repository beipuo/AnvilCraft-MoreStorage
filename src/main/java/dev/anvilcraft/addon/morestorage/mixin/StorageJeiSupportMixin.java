package dev.anvilcraft.addon.morestorage.mixin;

import dev.anvilcraft.addon.morestorage.client.gui.screen.CraftingTerminalScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.util.Optional;

/** Keeps AnvilCraft's two-mode storage JEI handler away from the addon's four-mode terminal. */
@Pseudo
@Mixin(targets = "dev.dubhe.anvilcraft.integration.jei.StorageJeiSupport", remap = false)
public abstract class StorageJeiSupportMixin {
    @Inject(method = "isStorageScreen", at = @At("HEAD"), cancellable = true, remap = false)
    private static void moreStorage$isStorageScreen(CallbackInfoReturnable<Boolean> cir) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof CraftingTerminalScreen || StorageJeiSupportMixin.hasCraftingTerminalParent(screen)) {
            cir.setReturnValue(false);
        }
    }

    private static boolean hasCraftingTerminalParent(Screen screen) {
        if (screen == null) {
            return false;
        }
        try {
            Method method = screen.getClass().getMethod("getParentScreen");
            Object parent = method.invoke(screen);
            return parent instanceof Optional<?> optional
                   && optional.orElse(null) instanceof CraftingTerminalScreen;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
