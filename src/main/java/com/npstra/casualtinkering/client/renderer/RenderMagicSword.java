package com.npstra.casualtinkering.client.renderer;

import com.npstra.casualtinkering.entity.EntityMagicSword;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderMagicSword extends Render<EntityMagicSword>
{
    private final RenderItem itemRenderer;
    private final ItemStack swordStack;

    public RenderMagicSword(RenderManager renderManager)
    {
        super(renderManager);
        this.itemRenderer = Minecraft.getMinecraft().getRenderItem();
        this.swordStack = new ItemStack(Items.DIAMOND_SWORD);
    }

    @Override
    public void doRender(EntityMagicSword entity, double x, double y, double z, float entityYaw, float partialTicks)
    {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);
        GlStateManager.rotate(180.0F - renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
        GlStateManager.disableLighting();
        GlStateManager.enableRescaleNormal();

        GlStateManager.rotate(entity.rotationYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-entity.rotationPitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);

        this.itemRenderer.renderItem(this.swordStack, ItemCameraTransforms.TransformType.FIXED);

        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityMagicSword entity)
    {
        return null;
    }
}