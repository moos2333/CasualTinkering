package com.npstra.casualtinkering.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.npstra.casualtinkering.entity.EntityMagicSword;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class RenderMagicSword extends EntityRenderer<EntityMagicSword> {
    public RenderMagicSword(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EntityMagicSword entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        ItemStack stack = entity.getRenderStack();
        if (stack.isEmpty()) return;

        poseStack.pushPose();
        poseStack.translate(0, 0.5, 0);

        float motionX = (float) entity.getDeltaMovement().x;
        float motionY = (float) entity.getDeltaMovement().y;
        float motionZ = (float) entity.getDeltaMovement().z;
        float yawAngle = (float) (Math.atan2(motionZ, motionX) * 180.0 / Math.PI);
        float pitch = (float) (Math.atan2(motionY, Math.sqrt(motionX * motionX + motionZ * motionZ)) * 180.0 / Math.PI);

        poseStack.mulPose(Axis.YP.rotationDegrees(yawAngle + 90));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch + 90));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45));

        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), 0);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMagicSword entity) {
        return null;
    }
}