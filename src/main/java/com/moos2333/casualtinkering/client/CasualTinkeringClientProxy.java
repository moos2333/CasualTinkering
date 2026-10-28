package com.moos2333.casualtinkering.client;

import com.moos2333.casualtinkering.client.renderer.RenderMagicLance;
import com.moos2333.casualtinkering.client.renderer.RenderMagicSword;
import com.moos2333.casualtinkering.common.CasualTinkeringCommonProxy;
import com.moos2333.casualtinkering.entity.EntityMagicLance;
import com.moos2333.casualtinkering.entity.EntityMagicSword;
import net.minecraft.client.Minecraft;
import slimeknights.mantle.client.book.repository.FileRepository;
import slimeknights.tconstruct.library.TinkerRegistryClient;
import slimeknights.tconstruct.library.book.TinkerBook;
import slimeknights.tconstruct.library.client.ToolBuildGuiInfo;
import com.moos2333.casualtinkering.tools.CasualTinkeringRegister;

public class CasualTinkeringClientProxy extends CasualTinkeringCommonProxy {
    @Override
    public void initToolGuis() {
        if (CasualTinkeringRegister.circularSaw != null) {
            ToolBuildGuiInfo info = new ToolBuildGuiInfo(CasualTinkeringRegister.circularSaw);
            info.addSlotPosition(33 + 22, 42 + 22);
            info.addSlotPosition(33 + 2, 42 + 2);
            info.addSlotPosition(33 + 2, 42 - 18);
            info.addSlotPosition(33 - 18, 42 - 18);
            TinkerRegistryClient.addToolBuilding(info);
        }
        if (CasualTinkeringRegister.magicdevice != null) {
            ToolBuildGuiInfo info = new ToolBuildGuiInfo(CasualTinkeringRegister.magicdevice);
            info.addSlotPosition(33 - 20 - 1, 42 + 20);
            info.addSlotPosition(33 + 20 - 5, 42 - 20 + 4);
            info.addSlotPosition(33 - 2 - 1, 42 + 2);
            TinkerRegistryClient.addToolBuilding(info);
        }
    }
    @Override
    public void registerRenderers() {
        Minecraft.getMinecraft().getRenderManager().entityRenderMap.put(EntityMagicSword.class, new RenderMagicSword(Minecraft.getMinecraft().getRenderManager()));
        Minecraft.getMinecraft().getRenderManager().entityRenderMap.put(EntityMagicLance.class, new RenderMagicLance(Minecraft.getMinecraft().getRenderManager()));
    }
    @Override
    public void registerBookPages() {
        TinkerBook.INSTANCE.addRepository(new FileRepository("casualtinkering:book"));
    }
}