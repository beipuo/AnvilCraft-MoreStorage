package dev.anvilcraft.addon.morestorage.mixin;

import dev.anvilcraft.addon.morestorage.client.gui.ICategoryListRows;
import dev.dubhe.anvilcraft.client.gui.component.category.CategoryList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Makes the category list's visible row count settable.
 *
 * <p>{@code COLUMN * ROW} is a constant expression, so javac folds it to the literal {@code 8} in
 * each of the six methods that page, scroll or draw. Redirecting that literal is enough to grow the
 * list — the widget already lays its buttons out from {@code getY()} and sizes its scrollbar from
 * {@code getHeight()}, both of which a caller can change.
 */
@Mixin(CategoryList.class)
public abstract class CategoryListMixin implements ICategoryListRows {
    @Unique
    private int moreStorage$rows = 8;

    @Override
    public int moreStorage$rows() {
        return this.moreStorage$rows;
    }

    @Override
    public void moreStorage$setRows(int rows) {
        this.moreStorage$rows = rows;
    }

    @ModifyConstant(
        method = {
            "mouseClicked(DDI)Z",
            "mouseDragged(DDIDD)Z",
            "mouseScrolled(DDDD)Z",
            "renderWidget(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            "renderScrollbar(Lnet/minecraft/client/gui/GuiGraphics;)V",
            "canScroll()Z"
        },
        constant = @Constant(intValue = 8)
    )
    private int moreStorage$visibleRows(int original) {
        return this.moreStorage$rows;
    }
}
