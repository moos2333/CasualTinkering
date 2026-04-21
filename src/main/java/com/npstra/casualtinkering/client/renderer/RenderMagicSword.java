package com.npstra.casualtinkering.client.renderer;

import com.npstra.casualtinkering.entity.EntityMagicSword;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.library.tools.ToolCore;
import slimeknights.tconstruct.tools.melee.TinkerMeleeWeapons;

import java.util.Arrays;

@SideOnly(Side.CLIENT)
public class RenderMagicSword extends Render<EntityMagicSword> {
    private final RenderItem itemRenderer;

    public RenderMagicSword(RenderManager renderManager) {
        super(renderManager);
        this.itemRenderer = Minecraft.getMinecraft().getRenderItem();
    }

    @Override
    public void doRender(EntityMagicSword entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (entity == null) return;
        String materialId = entity.getBladeMaterialId();
        Material material = TinkerRegistry.getMaterial(materialId);
        if (material == null) material = Material.UNKNOWN;
        ToolCore broadsword = TinkerMeleeWeapons.broadSword;
        ItemStack broadswordStack = broadsword.buildItem(Arrays.asList(material, material, material));
        if (broadswordStack.isEmpty()) broadswordStack = new ItemStack(net.minecraft.init.Items.DIAMOND_SWORD);
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);
        GlStateManager.scale(0.8F, 0.8F, 0.8F);
        GlStateManager.disableLighting();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        float motionX = (float) entity.motionX;
        float motionY = (float) entity.motionY;
        float motionZ = (float) entity.motionZ;
        float yaw = (float) (Math.atan2(motionZ, motionX) * 180.0 / Math.PI);
        float pitch = (float) (Math.atan2(motionY, MathHelper.sqrt(motionX * motionX + motionZ * motionZ)) * 180.0 / Math.PI);
        GlStateManager.rotate(yaw + 90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-pitch + 90.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 0.0F, 1.0F);
        this.itemRenderer.renderItem(broadswordStack, ItemCameraTransforms.TransformType.NONE);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityMagicSword entity) {
        return null;
    }
}