package dev.anvilcraft.addon.morestorage.mixin;

import dev.anvilcraft.addon.morestorage.client.gui.ICategoryListRows;
import dev.dubhe.anvilcraft.client.gui.component.category.CategoryList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Makes the category list's visible row count settable.
 *
 * <p>The parent widget now keeps all layout values in {@link CategoryList.ButtonInfo}. Replacing that
 * immutable record with a copy that has a different row count lets the parent's own paging, scrolling
 * and rendering logic use the terminal's taller layout without duplicating the widget.
 */
@Mixin(CategoryList.class)
public abstract class CategoryListMixin implements ICategoryListRows {
    @Shadow
    private CategoryList.ButtonInfo info;

    @Override
    public int moreStorage$rows() {
        return this.info.row();
    }

    @Override
    public void moreStorage$setRows(int rows) {
        this.info = new CategoryList.ButtonInfo(
            rows,
            this.info.rowGap(),
            this.info.column(),
            this.info.columnGap(),
            this.info.button(),
            this.info.setting(),
            this.info.width(),
            this.info.height(),
            this.info.extraRenderer()
        );
    }
}
