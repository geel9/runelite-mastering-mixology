package com.geel.masteringmixology;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class MasteringMixologyPluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(MasteringMixologyPlugin.class);
        RuneLite.main(args);
    }
}