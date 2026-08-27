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
import java.util.Map;
import java.util.WeakHashMap;

@SideOnly(Side.CLIENT)
public class RenderMagicSword extends Render<EntityMagicSword> {
    private final RenderItem itemRenderer;
    private final Map<String, ItemStack> stackCache = new WeakHashMap<>();
    private final ItemStack fallbackStack = new ItemStack(net.minecraft.init.Items.DIAMOND_SWORD);

    public RenderMagicSword(RenderManager renderManager) {
        super(renderManager);
        this.itemRenderer = Minecraft.getMinecraft().getRenderItem();
    }

    @Override
    public void doRender(EntityMagicSword entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (entity == null) return;
        String materialId = entity.getBladeMaterialId();
        ItemStack stack = stackCache.get(materialId);
        if (stack == null) {
            Material material = TinkerRegistry.getMaterial(materialId);
            if (material == null) material = Material.UNKNOWN;
            ToolCore broadsword = TinkerMeleeWeapons.broadSword;
            stack = broadsword.buildItem(Arrays.asList(material, material, material));
            if (stack.isEmpty()) stack = fallbackStack;
            stackCache.put(materialId, stack);
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);
        GlStateManager.scale(0.8F, 0.8F, 0.8F);
        GlStateManager.disableLighting();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        if (!entity.isDirect()) {
            float motionX = (float) entity.motionX;
            float motionY = (float) entity.motionY;
            float motionZ = (float) entity.motionZ;
            float yaw = (float) (Math.atan2(motionZ, motionX) * 180.0 / Math.PI);
            float pitch = (float) (Math.atan2(motionY, MathHelper.sqrt(motionX * motionX + motionZ * motionZ)) * 180.0 / Math.PI);
            GlStateManager.rotate(yaw + 90.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-pitch + 90.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(45.0F, 0.0F, 0.0F, 1.0F);
        } else {
            float yaw = entity.rotationYaw;
            float pitch = entity.rotationPitch;
            GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-pitch, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(45.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        }

        this.itemRenderer.renderItem(stack, ItemCameraTransforms.TransformType.NONE);

        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityMagicSword entity) {
        return null;
    }
}