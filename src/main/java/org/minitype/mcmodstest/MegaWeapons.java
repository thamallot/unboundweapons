package org.minitype.mcmodstest;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.TridentEntity;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import net.minecraft.particle.ParticleTypes;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import net.minecraft.text.Text;

import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MegaWeapons implements ModInitializer {

    private static final float MAX_TRIDENT_CHARGE = 3.0f;
    private static final float MAX_TRIDENT_DAMAGE = 100.0f;
    private static final Map<UUID, Long> tridentChargeStart = new HashMap<>();

    // =========================================================
    // COMBO TRACKING
    // =========================================================

    private static final Map<UUID, Integer> comboCounter = new HashMap<>();
    private static final Map<UUID, Long> lastHitTime = new HashMap<>();

    // Combo resets after 3 seconds
    private static final long COMBO_RESET_TIME = 3000;

    // =========================================================
    // IMPACT TYPES
    // =========================================================

    public enum ImpactType {
        BLACK_FLASH,
        PARALYZE
    }

    @Override
    public void onInitialize() {
        ModItems.initialize();
        System.out.println("ModItems Initialized!");

        ModComponents.initialize();
        System.out.println("ModComponents Initialized!");

        System.out.println("Mega Weapons Initialized!");

        // =====================================================
        // TOKEN ECONOMY
        // =====================================================

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {

            if (damageSource.getAttacker() instanceof ServerPlayerEntity player) {

                ItemStack nugget = new ItemStack(ModItems.MEGA_TOKEN);

                // Try inventory first
                if (!player.getInventory().insertStack(nugget)) {
                    player.dropItem(nugget, false);
                }

                player.sendMessage(
                        Text.literal("§e§l+1 MEGA TOKEN"),
                        false
                );

                player.playSound(
                        SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                        1.0f,
                        1.0f
                );
            }
        });

        // =====================================================
        // USE ITEM CALLBACK
        // =====================================================

        UseItemCallback.EVENT.register((player, world, hand) -> {

            ItemStack stack = player.getStackInHand(hand);

            if (world.isClient()) {
                return ActionResult.PASS;
            }

            // =================================================
            // AXE LOGIC
            // =================================================

            if (stack.getItem() instanceof net.minecraft.item.AxeItem) {

                ((ServerWorld) world).spawnParticles(
                        ParticleTypes.CLOUD,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        3,
                        0.2,
                        0.1,
                        0.2,
                        0.02
                );

                return ActionResult.CONSUME;
            }

            // =================================================
            // SWORD LOGIC
            // =================================================

            if (
                    stack.isOf(Items.WOODEN_SWORD) ||
                            stack.isOf(Items.STONE_SWORD) ||
                            stack.isOf(Items.IRON_SWORD) ||
                            stack.isOf(Items.GOLDEN_SWORD) ||
                            stack.isOf(Items.DIAMOND_SWORD) ||
                            stack.isOf(Items.NETHERITE_SWORD) ||
                            stack.isOf(Items.TRIDENT)
            ) {

                // =============================================
                // UPGRADE SYSTEM
                // Sneak + Right Click
                // =============================================

                if (player.isSneaking()) {

                    boolean upgraded = false;

                    for (int i = 0; i < player.getInventory().size(); i++) {

                        ItemStack invStack = player.getInventory().getStack(i);

                        if (invStack.isOf(ModItems.MEGA_TOKEN)) {

                            invStack.decrement(1);
                            upgraded = true;
                            break;
                        }
                    }

                    if (upgraded) {

                        int currentLevel = stack.getOrDefault(ModComponents.MEGA_LEVEL, 0);

                        stack.set(ModComponents.MEGA_LEVEL, currentLevel + 1);

                        player.sendMessage(
                                Text.literal(
                                        "§6§l" +
                                                stack.getName().getString().toUpperCase() +
                                                " UPGRADED TO LEVEL " +
                                                (currentLevel + 1)
                                ),
                                true
                        );

                        world.playSound(
                                null,
                                player.getX(),
                                player.getY(),
                                player.getZ(),
                                SoundEvents.BLOCK_ANVIL_USE,
                                SoundCategory.PLAYERS,
                                1.0f,
                                1.2f
                        );

                    } else {

                        player.sendMessage(
                                Text.literal("§cYou need a Mega Token (Gold Nugget)!"),
                                true
                        );
                    }

                    return ActionResult.SUCCESS;
                }

                return ActionResult.PASS;
            }

            return ActionResult.PASS;
        });

        // =====================================================
        // ATTACK CALLBACK
        // =====================================================

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {

            if (!world.isClient() && entity instanceof LivingEntity target) {

                ItemStack stack = player.getStackInHand(hand);
                ServerWorld sWorld = (ServerWorld) world;

                // =============================================
                // SWORD COMBO SYSTEM
                // =============================================

                if (
                        stack.isOf(Items.WOODEN_SWORD) ||
                                stack.isOf(Items.STONE_SWORD) ||
                                stack.isOf(Items.IRON_SWORD) ||
                                stack.isOf(Items.GOLDEN_SWORD) ||
                                stack.isOf(Items.DIAMOND_SWORD) ||
                                stack.isOf(Items.NETHERITE_SWORD)
                ) {

                    UUID uuid = player.getUuid();

                    long now = System.currentTimeMillis();

                    long lastHit = lastHitTime.getOrDefault(uuid, 0L);

                    int hits;

                    // Reset combo if too slow
                    if (now - lastHit > COMBO_RESET_TIME) {
                        hits = 1;
                    } else {
                        hits = comboCounter.getOrDefault(uuid, 0) + 1;
                    }

                    lastHitTime.put(uuid, now);

                    // BLACK FLASH
                    if (hits >= 4) {

                        triggerImpact(
                                player,
                                target,
                                sWorld,
                                ImpactType.BLACK_FLASH
                        );

                        comboCounter.put(uuid, 0);

                    } else {

                        comboCounter.put(uuid, hits);

                        player.sendMessage(
                                Text.literal("§7Combo: §e" + hits),
                                true
                        );
                    }
                }

                // =============================================
                // AXE PARALYZE
                // Sneak + Attack
                // =============================================

                if (stack.getItem() instanceof net.minecraft.item.AxeItem
                        && player.isSneaking()) {

                    triggerImpact(
                            player,
                            target,
                            sWorld,
                            ImpactType.PARALYZE
                    );
                }
            }

            return ActionResult.PASS;
        });

        // =====================================================
        // TRACK TRIDENT CHARGE START
        // =====================================================

        UseItemCallback.EVENT.register((player, world, hand) -> {

            ItemStack stack = player.getStackInHand(hand);

            if (stack.isOf(Items.TRIDENT)) {

                int level = stack.getOrDefault(ModComponents.MEGA_LEVEL, 0);

                // Only level 10+ tridents
                if (level >= 10) {

                    tridentChargeStart.put(
                            player.getUuid(),
                            System.currentTimeMillis()
                    );

                    player.sendMessage(
                            Text.literal("§bCharging Trident..."),
                            true
                    );
                }
            }

            return ActionResult.PASS;
        });

        // =====================================================
        // BOOST THROWN TRIDENTS
        // =====================================================

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {

            // Must be trident
            if (!(entity instanceof TridentEntity trident)) {
                return;
            }

            // Must have owner
            if (!(trident.getOwner() instanceof ServerPlayerEntity player)) {
                return;
            }

            UUID uuid = player.getUuid();

            // Player never charged
            if (!tridentChargeStart.containsKey(uuid)) {
                return;
            }

            long startTime = tridentChargeStart.get(uuid);

            long heldTime = System.currentTimeMillis() - startTime;

            // Remove stored charge
            tridentChargeStart.remove(uuid);

            // Convert to seconds-ish
            float charge = Math.min(
                    heldTime / 1000.0f,
                    3.0f
            );

            // SPEED MULTIPLIER
            double multiplier = 1.0 + (charge * 3.0);

            Vec3d velocity = trident.getVelocity();

            trident.setVelocity(
                    velocity.x * multiplier,
                    velocity.y * multiplier,
                    velocity.z * multiplier
            );

            // PARTICLES
            world.spawnParticles(
                    ParticleTypes.SONIC_BOOM,
                    trident.getX(),
                    trident.getY(),
                    trident.getZ(),
                    3,
                    0.2,
                    0.2,
                    0.2,
                    0.05
            );

            // SOUND
            world.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.ENTITY_WARDEN_SONIC_BOOM,
                    SoundCategory.PLAYERS,
                    1.0f,
                    1.2f
            );

            // DEBUG
            player.sendMessage(
                    Text.literal(
                            "§bVORTEX LAUNCH x " + (int)multiplier + "/10"
                    ),
                    true
            );
        });

        // =====================================================
        // TRIDENT CHARGE HUD
        // =====================================================

        ServerTickEvents.END_SERVER_TICK.register(server -> {

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

                // Must be actively using item
                if (!player.isUsingItem()) {
                    continue;
                }

                ItemStack stack = player.getActiveItem();

                // Must be trident
                if (!stack.isOf(Items.TRIDENT)) {
                    continue;
                }

                int level = stack.getOrDefault(ModComponents.MEGA_LEVEL, 0);

                // Must be level 10+
                if (level < 10) {
                    continue;
                }

                UUID uuid = player.getUuid();

                // Missing charge start
                if (!tridentChargeStart.containsKey(uuid)) {
                    continue;
                }

                long startTime = tridentChargeStart.get(uuid);

                float heldSeconds =
                        (System.currentTimeMillis() - startTime) / 1000.0f;

                // Clamp charge
                float charge = Math.min(
                        heldSeconds,
                        MAX_TRIDENT_CHARGE
                );

                // Percent
                int percent =
                        (int)((charge / MAX_TRIDENT_CHARGE) * 100);

                // DAMAGE FORMULA
                float predictedDamage =
                        10.0f + (charge * 30.0f);

                // BAR SIZE
                int totalBars = 20;

                int filledBars =
                        (int)((charge / MAX_TRIDENT_CHARGE) * totalBars);

                StringBuilder bar = new StringBuilder();

                for (int i = 0; i < totalBars; i++) {

                    if (i < filledBars) {
                        bar.append("§b█");
                    } else {
                        bar.append("§7█");
                    }
                }

                // ACTION BAR
                player.sendMessage(
                        Text.literal(
                                "§3TRIDENT CHARGE\n" +
                                        bar +
                                        " §f" + percent + "%\n" +
                                        "§cDamage: §f" +
                                        (int)predictedDamage
                        ),
                        true
                );
            }
        });

        // =====================================================
        // MEMORY CLEANUP
        // =====================================================

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {

            UUID uuid = handler.player.getUuid();

            comboCounter.remove(uuid);
            lastHitTime.remove(uuid);
        });
    }

    // =========================================================
    // IMPACT HANDLER
    // =========================================================

    private void triggerImpact(
            net.minecraft.entity.player.PlayerEntity player,
            LivingEntity target,
            ServerWorld world,
            ImpactType type
    ) {

        // Shared effects
        world.spawnParticles(
                ParticleTypes.SONIC_BOOM,
                target.getX(),
                target.getY() + 1,
                target.getZ(),
                1,
                0,
                0,
                0,
                0
        );

        player.playSound(
                SoundEvents.ENTITY_WARDEN_SONIC_BOOM,
                1.0f,
                1.0f
        );

        // =====================================================
        // PARALYZE
        // =====================================================

        if (type == ImpactType.PARALYZE) {

            target.addStatusEffect(
                    new StatusEffectInstance(
                            StatusEffects.SLOWNESS,
                            60,
                            255
                    )
            );

            player.sendMessage(
                    Text.literal("§4§lENEMY PARALYZED!"),
                    true
            );
        }

        // =====================================================
        // BLACK FLASH
        // =====================================================

        if (type == ImpactType.BLACK_FLASH) {

            target.takeKnockback(
                    2.0,
                    player.getX() - target.getX(),
                    player.getZ() - target.getZ()
            );

            player.sendMessage(
                    Text.literal("§0§l> §c§lBLACK FLASH §0§l<"),
                    true
            );
        }
    }
}