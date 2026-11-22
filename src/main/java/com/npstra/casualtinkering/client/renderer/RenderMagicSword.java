package com.npstra.casualtinkering.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.npstra.casualtinkering.entity.EntityMagicSword;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

import java.util.HashMap;
import java.util.Map;

public class RenderMagicSword extends EntityRenderer<EntityMagicSword> {
    private static final Item SWORD = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryBuild("tconstruct", "sword"));
    private static final Item HANDLE = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryBuild("tconstruct", "tool_handle"));
    private static final String FALLBACK_HANDLE = "tconstruct:wood";
    private static final ItemStack FALLBACK = new ItemStack(Items.DIAMOND_SWORD);

    private final ItemRenderer itemRenderer;
    private final Map<String, ItemStack> blades = new HashMap<>();

    public RenderMagicSword(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    private ItemStack getBlade(String material) {
        return this.blades.computeIfAbsent(material, key -> {
            MaterialVariantId variant = MaterialVariantId.tryParse(key);
            if (variant == null || SWORD == null) return FALLBACK;
            String blade = variant.getId().toString();
            String handle = HANDLE instanceof IMaterialItem handlePart && handlePart.canUseMaterial(variant.getId()) ? blade : FALLBACK_HANDLE;
            ListTag materials = new ListTag();
            materials.add(StringTag.valueOf(blade));
            materials.add(StringTag.valueOf(handle));
            materials.add(StringTag.valueOf(handle));
            ItemStack stack = new ItemStack(SWORD);
            stack.getOrCreateTag().put("tic_materials", materials);
            return stack;
        });
    }

    @Override
    public void render(EntityMagicSword entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
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

        this.itemRenderer.renderStatic(getBlade(entity.getMaterial()), ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), 0);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMagicSword entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
