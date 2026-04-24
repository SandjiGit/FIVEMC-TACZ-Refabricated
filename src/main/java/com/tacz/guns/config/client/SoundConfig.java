package com.tacz.guns.config.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public class SoundConfig {
    public static ModConfigSpec.BooleanValue ENABLE_LAZY_SOUND_LOAD;

    public static void init(ModConfigSpec.Builder builder) {
        builder.push("sound");

        builder.comment("Load TACZ sound buffers on demand instead of decoding all gun-pack sounds during resource reload.");
        ENABLE_LAZY_SOUND_LOAD = builder.define("EnableLazySoundLoad", true);

        builder.pop();
    }
}
