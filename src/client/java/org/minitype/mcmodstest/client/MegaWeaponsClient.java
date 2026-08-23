package org.minitype.mcmodstest.client;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;

import net.minecraft.text.Text;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import org.lwjgl.glfw.GLFW;
import org.minitype.mcmodstest.BlackFlashPayload;
import org.minitype.mcmodstest.GravityWellPayload;
import org.minitype.mcmodstest.MegaWeapons;
import org.minitype.mcmodstest.ModComponents;
import org.minitype.mcmodstest.ModItems;
import org.minitype.mcmodstest.MeteorDropPayload;
import org.minitype.mcmodstest.MomentumAttackPayload;
import org.minitype.mcmodstest.MomentumPayload;
import org.minitype.mcmodstest.RecoilShotPayload;
import org.minitype.mcmodstest.SkewerDashPayload;
import org.minitype.mcmodstest.VolleyArmPayload;
import org.minitype.mcmodstest.VolleyCancelPayload;

public class MegaWeaponsClient implements ClientModInitializer {

    private static KeyBinding dashKey;
    private static KeyBinding blackFlashKey;
    private static KeyBinding momentumKey;
    private static KeyBinding volleyKey;
    private static long lastDashTime = 0;
    private static boolean attackWasPressed;
    private static boolean volleyWasHeld;

    @Override
    public void onInitializeClient() {
        KeyBinding.Category unboundWeaponsCategory = KeyBinding.Category.create(
                Identifier.of("unbnd_weapons", "unbnd_weapons")
        );

        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            // =====================================================
            // UNBOUND TOKEN LORE
            // =====================================================

            if (stack.isOf(ModItems.MEGA_TOKEN)) {
                lines.add(Text.literal("\u00A78A fragment of unbound combat energy."));
                lines.add(Text.literal("\u00A77Used to strengthen weapons."));
            }

            // =====================================================
            // WEAPON LEVEL TOOLTIP
            // =====================================================

            int level = stack.getOrDefault(ModComponents.MEGA_LEVEL, 0);

            if (level > 0) {
                lines.add(Text.literal("\u00A76Mega Level: " + level));
            }
        });
        dashKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.unbnd_weapons.dash",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_R,
                        unboundWeaponsCategory
                )
        );

        blackFlashKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.unbnd_weapons.black_flash",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_V,
                        unboundWeaponsCategory
                )
        );

        momentumKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.unbnd_weapons.momentum",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_Z,
                        unboundWeaponsCategory
                )
        );

        volleyKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.unbnd_weapons.volley",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_X,
                        unboundWeaponsCategory
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            boolean attackPressed = client.options.attackKey.isPressed();

            if (attackPressed
                    && !attackWasPressed
                    && client.player != null
                    && client.currentScreen == null
                    && client.player.getMainHandStack().isIn(ItemTags.SPEARS)) {
                ClientPlayNetworking.send(new MomentumAttackPayload());
            }

            attackWasPressed = attackPressed;

            boolean volleyHeld = volleyKey.isPressed();

            if (volleyHeld && !volleyWasHeld) {
                ClientPlayNetworking.send(new VolleyArmPayload());
            } else if (!volleyHeld && volleyWasHeld) {
                ClientPlayNetworking.send(new VolleyCancelPayload());
            }

            volleyWasHeld = volleyHeld;

            while (blackFlashKey.wasPressed()) {
                ClientPlayNetworking.send(new BlackFlashPayload());
            }

            while (momentumKey.wasPressed()) {
                ClientPlayNetworking.send(new MomentumPayload());
            }

            while (dashKey.wasPressed()) {

                MinecraftClient mc = MinecraftClient.getInstance();

                if (mc.player == null) return;

                // A held mace takes priority over hotbar spear abilities.
                if (mc.player.getMainHandStack().isOf(Items.MACE)) {
                    if (mc.player.isSneaking()) {
                        ClientPlayNetworking.send(new GravityWellPayload());
                    } else {
                        ClientPlayNetworking.send(new MeteorDropPayload());
                    }
                    continue;
                }

                if (mc.player.getMainHandStack().isOf(Items.BOW)) {
                    ClientPlayNetworking.send(new RecoilShotPayload());
                    continue;
                }

                // Spears use the same remappable ability key as sword dash.
                // The server selects the first spear in the hotbar and owns
                // all movement, collision, and damage for Skewer Dash.
                boolean hasSpearInHotbar = false;

                for (int slot = 0; slot < 9; slot++) {
                    if (mc.player.getInventory().getStack(slot).isIn(ItemTags.SPEARS)) {
                        hasSpearInHotbar = true;
                        break;
                    }
                }

                if (hasSpearInHotbar) {
                    ClientPlayNetworking.send(new SkewerDashPayload());
                    continue;
                }

                // Must sprint
                if (!mc.player.isSprinting()) return;

                // Must hold sword
                if (
                        !mc.player.getMainHandStack().isOf(Items.WOODEN_SWORD) &&
                                !mc.player.getMainHandStack().isOf(Items.STONE_SWORD) &&
                                !mc.player.getMainHandStack().isOf(Items.IRON_SWORD) &&
                                !mc.player.getMainHandStack().isOf(Items.GOLDEN_SWORD) &&
                                !mc.player.getMainHandStack().isOf(Items.DIAMOND_SWORD) &&
                                !mc.player.getMainHandStack().isOf(Items.NETHERITE_SWORD)
                ) {
                    return;
                }

                int level = mc.player.getMainHandStack().getOrDefault(ModComponents.MEGA_LEVEL, 0);

                if (level < 5) {
                    return;
                }

                // DASH COOLDOWN
                long now = System.currentTimeMillis();

                if (now - lastDashTime < 5000) {
                    return;
                }

                lastDashTime = now;

                // DASH LOGIC
                Vec3d look = mc.player.getRotationVector();

                Vec3d dash = new Vec3d(
                        look.x,
                        0,
                        look.z
                ).normalize().multiply(1.5);

                mc.player.addVelocity(
                        dash.x,
                        0.15,
                        dash.z
                );


                // COOLDOWN ANIMATION
                mc.player.getItemCooldownManager().set(
                        mc.player.getMainHandStack(),
                        100
                );
            }
        });
    }
}
