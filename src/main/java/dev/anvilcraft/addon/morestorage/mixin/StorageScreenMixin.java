package dev.anvilcraft.addon.morestorage.mixin;

import dev.anvilcraft.addon.morestorage.client.gui.IStorageScreenLayout;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * Lets a taller storage screen exist.
 *
 * <p>{@code StorageScreen} spells its layout out in private constants and a {@code private final}
 * background, so a subclass cannot move anything. Every handler here asks whether the screen
 * implements {@link IStorageScreenLayout} and only then substitutes that screen's number — the
 * storage station itself never sees a change.
 *
 * <p>The two callbacks are the other half of the deal: the parent's {@code render} draws the carried
 * stack and the tooltip last, so a subclass has no override that lands between the inventory and the
 * cursor. Contents go in right after the inventory, the overlay at the very end.
 */
@Mixin(StorageScreen.class)
public abstract class StorageScreenMixin {
    @Unique
    private @Nullable IStorageScreenLayout moreStorage$layout() {
        return this instanceof IStorageScreenLayout layout ? layout : null;
    }

    @ModifyArg(
        method = "renderBackground(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
        at = @At(
            value = "INVOKE",
            // The float-UV overload: (texture, x, y, uOffset, vOffset, width, height, texWidth, texHeight).
            target = "Lnet/minecraft/client/gui/GuiGraphics;"
                     + "blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"
        ),
        index = 0
    )
    private ResourceLocation moreStorage$background(ResourceLocation original) {
        IStorageScreenLayout layout = this.moreStorage$layout();
        return layout == null ? original : layout.moreStorage$background();
    }

    @ModifyConstant(
        method = {
            "renderBackground(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            "init()V"
        },
        constant = @Constant(intValue = 222)
    )
    private int moreStorage$backgroundHeight(int original) {
        IStorageScreenLayout layout = this.moreStorage$layout();
        return layout == null ? original : layout.moreStorage$backgroundHeight();
    }

    @ModifyConstant(
        method = "renderBackground(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
        constant = @Constant(intValue = 256)
    )
    private int moreStorage$backgroundTextureHeight(int original) {
        IStorageScreenLayout layout = this.moreStorage$layout();
        return layout == null ? original : layout.moreStorage$backgroundTextureHeight();
    }

    @ModifyConstant(
        method = {
            "renderPlayerInventory(Lnet/minecraft/client/gui/GuiGraphics;II)V",
            "getInventorySlot(DD)I",
            "getItemAreaData(DD)Ldev/dubhe/anvilcraft/client/gui/screen/StorageScreen$ItemArea;"
        },
        constant = @Constant(intValue = 140)
    )
    private int moreStorage$inventoryY(int original) {
        IStorageScreenLayout layout = this.moreStorage$layout();
        return layout == null ? original : layout.moreStorage$inventoryY();
    }

    @ModifyConstant(method = "init()V", constant = @Constant(intValue = 139))
    private int moreStorage$depositButtonY(int original) {
        IStorageScreenLayout layout = this.moreStorage$layout();
        return layout == null ? original : layout.moreStorage$depositButtonY();
    }

    @ModifyConstant(method = "init()V", constant = @Constant(intValue = 161))
    private int moreStorage$withdrawButtonY(int original) {
        IStorageScreenLayout layout = this.moreStorage$layout();
        return layout == null ? original : layout.moreStorage$withdrawButtonY();
    }

    @Inject(
        method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
        at = @At(
            value = "INVOKE",
            target = "Ldev/dubhe/anvilcraft/client/gui/screen/StorageScreen;"
                     + "renderPlayerInventory(Lnet/minecraft/client/gui/GuiGraphics;II)V",
            shift = At.Shift.AFTER
        )
    )
    private void moreStorage$renderContents(
        GuiGraphics graphics,
        int mouseX,
        int mouseY,
        float partialTick,
        CallbackInfo ci
    ) {
        IStorageScreenLayout layout = this.moreStorage$layout();
        if (layout != null) {
            layout.moreStorage$renderContents(graphics, mouseX, mouseY);
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("TAIL"))
    private void moreStorage$renderOverlay(
        GuiGraphics graphics,
        int mouseX,
        int mouseY,
        float partialTick,
        CallbackInfo ci
    ) {
        IStorageScreenLayout layout = this.moreStorage$layout();
        if (layout != null) {
            layout.moreStorage$renderOverlay(graphics, mouseX, mouseY);
        }
    }
}
