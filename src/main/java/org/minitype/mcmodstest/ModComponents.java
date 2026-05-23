package org.minitype.mcmodstest;

import com.mojang.serialization.Codec;

import net.minecraft.component.ComponentType;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import net.minecraft.util.Identifier;

public class ModComponents {

    public static final ComponentType<Integer> MEGA_LEVEL = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of("unbnd_weapons", "level"),
            ComponentType.<Integer>builder()
                    .codec(Codec.INT)
                    .build()
    );

    public static void initialize() {
        System.out.println("Mod Components Initialized!");
    }
}
