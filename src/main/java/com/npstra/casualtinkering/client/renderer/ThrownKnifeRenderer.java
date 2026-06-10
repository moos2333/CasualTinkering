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
import com.npstra.casualtinkering.entity.ThrownKnife;

public class ThrownKnifeRenderer extends EntityRenderer<ThrownKnife> {
    private final ItemRenderer itemRenderer;

    public ThrownKnifeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ThrownKnife entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (entity.tickCount >= 2 || !(this.entityRenderDispatcher.camera.getEntity().distanceToSqr(entity) < 12.25D)) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(entityYaw + 90));
            poseStack.mulPose(Axis.ZP.rotationDegrees((entity.tickCount + partialTicks) * 30 % 360));
            poseStack.translate(-0.03125, -0.09375, 0);
            this.itemRenderer.renderStatic(entity.getDisplayTool(), ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
            poseStack.popPose();
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownKnife entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}