package com.solr98.beyondcmdextension.jei;

import com.solr98.beyondcmdextension.Beyond_cmd_extension;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class BDJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Beyond_cmd_extension.MODID, "jei_plugin");
    }
}
