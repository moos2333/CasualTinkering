package com.npstra.casualtinkering.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import com.npstra.casualtinkering.entity.ThrownBoomerang;

public class ThrownBoomerangRenderer extends EntityRenderer<ThrownBoomerang> {
    private final ItemRenderer itemRenderer;
    public ThrownBoomerangRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }
    @Override
    public void render(ThrownBoomerang entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        if (entity.tickCount >= 2 || !(this.entityRenderDispatcher.camera.getEntity().distanceToSqr(entity) < 12.25)) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(entityYaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            poseStack.mulPose(Axis.ZP.rotationDegrees((entity.tickCount + partialTicks) * 30));
            poseStack.translate(-0.03125, -0.09375, 0);
            this.itemRenderer.renderStatic(entity.getDisplayTool(), ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
            poseStack.popPose();
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, light);
    }
    @Override
    public ResourceLocation getTextureLocation(ThrownBoomerang entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}