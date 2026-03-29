package com.npstra.casualtinkering.client;

import com.npstra.casualtinkering.common.CasualTinkeringCommonProxy;
import slimeknights.tconstruct.library.TinkerRegistryClient;
import slimeknights.tconstruct.library.client.ToolBuildGuiInfo;
import com.npstra.casualtinkering.tools.CasualTinkeringRegister;

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
}