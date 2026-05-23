package org.minitype.mcmodstest;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public class ModItems {

    public static final Identifier MEGA_TOKEN_ID =
            Identifier.of("unbnd_weapons", "mega_token");

    public static final RegistryKey<Item> MEGA_TOKEN_KEY =
            RegistryKey.of(RegistryKeys.ITEM, MEGA_TOKEN_ID);

    public static final Item MEGA_TOKEN = Registry.register(
            Registries.ITEM,
            MEGA_TOKEN_KEY,
            new Item(new Item.Settings()
                    .registryKey(MEGA_TOKEN_KEY)
                    .maxCount(64)
                    .rarity(Rarity.UNCOMMON)
            ) {
                @Override
                public boolean hasGlint(ItemStack stack) {
                    return true;
                }
            }
    );

    public static void initialize() {
        System.out.println("Mod Items Initialized!");
    }
}
