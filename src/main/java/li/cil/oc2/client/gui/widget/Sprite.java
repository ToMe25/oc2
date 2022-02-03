/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.util.Mth;

public final class Sprite {
    public final Texture texture;
    public final int width, height;
    public final int u0, v0;

    ///////////////////////////////////////////////////////////////////

    public Sprite(final Texture texture) {
        this.texture = texture;
        this.width = texture.width;
        this.height = texture.height;
        this.u0 = 0;
        this.v0 = 0;
    }

    public Sprite(final Texture texture, final int width, final int height, final int u0, final int v0) {
        this.texture = texture;
        this.width = width;
        this.height = height;
        this.u0 = u0;
        this.v0 = v0;
    }

    ///////////////////////////////////////////////////////////////////

    public void draw(final PoseStack stack, final int x, final int y) {
        draw(stack, x, y, 0, 0);
    }

    public void draw(final PoseStack stack, final int x, final int y, final int uOffset, final int vOffset) {
        blit(stack, x, y, u0 + uOffset, v0 + vOffset, width, height);
    }

    public void drawFillY(final PoseStack stack, final int x, final int y, final float value) {
        final int h = (int) (this.height * Mth.clamp(value, 0, 1));
        blit(stack, x, y + (height - h), u0, v0 + (height - h), width, h);
    }

    private void blit(final PoseStack stack, final int x, final int y, final int u0, final int v0, final int width, final int height) {
        texture.bind();
        GuiComponent.blit(stack, x, y, u0, v0, width, height, texture.width, texture.height);
    }
}
