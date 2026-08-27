package com.npstra.casualtinkering.client.renderer;

import com.npstra.casualtinkering.entity.EntityMagicLance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderMagicLance extends Render<EntityMagicLance> {
    private final RenderItem itemRenderer;

    public RenderMagicLance(RenderManager renderManager) {
        super(renderManager);
        this.itemRenderer = Minecraft.getMinecraft().getRenderItem();
    }

    @Override
    public void doRender(EntityMagicLance entity, double x, double y, double z, float entityYaw, float partialTicks) {
        ItemStack stack = entity.getShovelStack();
        if (stack.isEmpty()) return;

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);
        GlStateManager.scale(0.6F, 0.6F, 0.6F);
        GlStateManager.disableLighting();
        GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 0.0F, 1.0F);
        this.itemRenderer.renderItem(stack, ItemCameraTransforms.TransformType.NONE);
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityMagicLance entity) {
        return null;
    }
}