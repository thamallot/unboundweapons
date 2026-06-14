package org.minitype.mcmodstest;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.TridentEntity;

import net.minecraft.enchantment.Enchantments;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import net.minecraft.particle.ParticleTypes;

import net.minecraft.registry.RegistryKeys;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import net.minecraft.text.Text;

import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MegaWeapons implements ModInitializer {

    private static final float MAX_TRIDENT_CHARGE = 3.0f;
    private static final float MAX_TRIDENT_DAMAGE = 100.0f;
    private static final Map<UUID, Long> tridentChargeStart = new HashMap<>();
    private static final Set<UUID> blackFlashArmed = new HashSet<>();
    private static final Map<UUID, Long> guardBreakArmed = new HashMap<>();
    private static final Map<UUID, Integer> guardBreakCritChain = new HashMap<>();
    private static final Map<UUID, Long> lastGuardBreakCritTime = new HashMap<>();

    // =========================================================
    // COMBO TRACKING
    // =========================================================

    private static final Map<UUID, Integer> comboCounter = new HashMap<>();
    private static final Map<UUID, Long> lastHitTime = new HashMap<>();

    // Combo resets after 3 seconds
    private static final long COMBO_RESET_TIME = 3000;
    private static final int BLACK_FLASH_CHARGE_HITS = 5;
    private static final float BLACK_FLASH_BONUS_DAMAGE = 5.0f;
    private static final int GUARD_BREAK_REQUIRED_CRITS = 3;
    private static final int GUARD_BREAK_LEVEL_REQUIREMENT = 10;
    private static final long GUARD_BREAK_CHAIN_RESET_TIME = 5000;
    private static final long GUARD_BREAK_ARM_TIME = 5000;

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
        // NETWORK PAYLOADS
        // =====================================================

        PayloadTypeRegistry.playC2S().register(
                BlackFlashPayload.ID,
                BlackFlashPayload.CODEC
        );

        // =====================================================
        // BLACK FLASH KEYBIND RECEIVER
        // =====================================================

        ServerPlayNetworking.registerGlobalReceiver(
                BlackFlashPayload.ID,
                (payload, context) -> {

                    ServerPlayerEntity player = context.player();
                    UUID uuid = player.getUuid();
                    int hits = comboCounter.getOrDefault(uuid, 0);

                    if (hits >= BLACK_FLASH_CHARGE_HITS) {

                        blackFlashArmed.add(uuid);

                        player.sendMessage(
                                Text.literal("\u00A70\u00A7l> \u00A7c\u00A7lBLACK FLASH ARMED \u00A70\u00A7l<"),
                                true
                        );

                        player.playSound(
                                SoundEvents.BLOCK_BEACON_POWER_SELECT,
                                1.0f,
                                1.4f
                        );

                    } else {

                        player.sendMessage(
                                Text.literal("\u00A77Black Flash Charge: \u00A7e" + hits + "\u00A77/" + BLACK_FLASH_CHARGE_HITS),
                                true
                        );
                    }
                }
        );

        // =====================================================
        // TOKEN ECONOMY
        // =====================================================

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {

            // Must be killed by a player
            if (!(damageSource.getAttacker() instanceof ServerPlayerEntity player)) {
                return;
            }

            // Must be a hostile mob
            if (!(entity instanceof net.minecraft.entity.mob.Monster)) {
                return;
            }

            // 20% token drop chance
            if (player.getRandom().nextFloat() > 0.40f) {
                return;
            }

            ItemStack token = new ItemStack(ModItems.MEGA_TOKEN);

            if (!player.getInventory().insertStack(token)) {
                player.dropItem(token, false);
            }

            player.sendMessage(
                    Text.literal("§b§l+1 UNBOUND TOKEN"),
                    false
            );

            player.playSound(
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    1.0f,
                    1.2f
            );
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
            // AXE GUARD BREAK ARM
            // Right click with a level 10+ axe after three critical hits
            // =================================================

            if (isAxe(stack) && !player.isSneaking()) {

                int level = stack.getOrDefault(ModComponents.MEGA_LEVEL, 0);

                if (level < GUARD_BREAK_LEVEL_REQUIREMENT) {
                    return ActionResult.PASS;
                }

                UUID uuid = player.getUuid();
                int crits = guardBreakCritChain.getOrDefault(uuid, 0);

                if (crits < GUARD_BREAK_REQUIRED_CRITS) {

                    player.sendMessage(
                            Text.literal("\u00A77Guard Break Crits: \u00A7e" + crits + "\u00A77/" + GUARD_BREAK_REQUIRED_CRITS),
                            true
                    );

                    return ActionResult.SUCCESS;
                }

                guardBreakArmed.put(uuid, System.currentTimeMillis());
                guardBreakCritChain.put(uuid, 0);
                lastGuardBreakCritTime.remove(uuid);

                player.sendMessage(
                        Text.literal("\u00A74\u00A7lGUARD BREAK ARMED"),
                        true
                );

                world.playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        SoundEvents.BLOCK_ANVIL_LAND,
                        SoundCategory.PLAYERS,
                        0.7f,
                        1.6f
                );

                ((ServerWorld) world).spawnParticles(
                        ParticleTypes.CRIT,
                        player.getX(),
                        player.getY() + 1,
                        player.getZ(),
                        12,
                        0.3,
                        0.4,
                        0.3,
                        0.15
                );

                return ActionResult.SUCCESS;
            }

            // =================================================
            // AXE LOGIC
            // =================================================

            if (isAxe(stack) && !player.isSneaking()) {

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
                            stack.isOf(Items.TRIDENT) ||
                            isAxe(stack)
            ) {

                // =============================================
                // UPGRADE SYSTEM
                // Sneak + Right Click
                // =============================================

                if (player.isSneaking()) {

                    int currentLevel = stack.getOrDefault(ModComponents.MEGA_LEVEL, 0);

                    // Cost increases every 5 levels
                    int upgradeCost = 1 + (currentLevel / 5);

                    int tokensFound = 0;

                    // Count tokens first
                    for (int i = 0; i < player.getInventory().size(); i++) {

                        ItemStack invStack = player.getInventory().getStack(i);

                        if (invStack.isOf(ModItems.MEGA_TOKEN)) {
                            tokensFound += invStack.getCount();
                        }
                    }

                    boolean upgraded = false;

                    if (tokensFound >= upgradeCost) {

                        int tokensToRemove = upgradeCost;

                        for (int i = 0; i < player.getInventory().size(); i++) {

                            ItemStack invStack = player.getInventory().getStack(i);

                            if (invStack.isOf(ModItems.MEGA_TOKEN)) {

                                int removeAmount = Math.min(tokensToRemove, invStack.getCount());

                                invStack.decrement(removeAmount);

                                tokensToRemove -= removeAmount;

                                if (tokensToRemove <= 0) {
                                    break;
                                }
                            }
                        }

                        upgraded = true;
                    }

                    if (upgraded) {

                        int newLevel = currentLevel + 1;
                        stack.set(ModComponents.MEGA_LEVEL, newLevel);

                        if (player instanceof ServerPlayerEntity serverPlayer) {
                            checkMilestoneUnlock(serverPlayer, stack, newLevel);
                        }

                        player.sendMessage(
                                Text.literal(
                                        "§6§l" +
                                                stack.getName().getString().toUpperCase() +
                                                " UPGRADED TO LEVEL " +
                                                newLevel
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
                                Text.literal("§cYou need " + upgradeCost + " Unbound Token(s) to upgrade!"),
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

                    int level = stack.getOrDefault(ModComponents.MEGA_LEVEL, 0);

                    if (level < 10) {
                        return ActionResult.PASS;
                    }

                    UUID uuid = player.getUuid();

                    long now = System.currentTimeMillis();

                    long lastHit = lastHitTime.getOrDefault(uuid, 0L);

                    int hits;

                    // =================================================
                    // BLACK FLASH RELEASE
                    // =================================================

                    if (
                            blackFlashArmed.contains(uuid) &&
                                    comboCounter.getOrDefault(uuid, 0) >= BLACK_FLASH_CHARGE_HITS
                    ) {
                        triggerImpact(
                                player,
                                target,
                                sWorld,
                                ImpactType.BLACK_FLASH
                        );

                        blackFlashArmed.remove(uuid);
                        comboCounter.put(uuid, 0);
                        lastHitTime.put(uuid, now);

                        return ActionResult.PASS;
                    }

                    // =================================================
                    // BUILD BLACK FLASH CHARGE
                    // =================================================

                    if (now - lastHit > COMBO_RESET_TIME) {
                        hits = 1;
                        blackFlashArmed.remove(uuid);
                    } else {
                        hits = comboCounter.getOrDefault(uuid, 0) + 1;
                    }

                    lastHitTime.put(uuid, now);

                    if (hits >= BLACK_FLASH_CHARGE_HITS) {

                        comboCounter.put(uuid, BLACK_FLASH_CHARGE_HITS);

                        player.sendMessage(
                                Text.literal("\u00A7c\u00A7lBLACK FLASH READY \u00A77Press your Black Flash key to arm it."),
                                true
                        );

                    } else {

                        comboCounter.put(uuid, hits);

                        player.sendMessage(
                                Text.literal("\u00A77Black Flash Charge: \u00A7e" + hits + "\u00A77/" + BLACK_FLASH_CHARGE_HITS),
                                true
                        );
                    }
                }

                // =============================================
                // AXE GUARD BREAK RELEASE
                // =============================================

                if (isAxe(stack)) {

                    UUID uuid = player.getUuid();
                    int level = stack.getOrDefault(ModComponents.MEGA_LEVEL, 0);

                    if (level >= GUARD_BREAK_LEVEL_REQUIREMENT) {

                        long now = System.currentTimeMillis();

                        // =============================================
                        // GUARD BREAK RELEASE
                        // =============================================

                        if (guardBreakArmed.containsKey(uuid)) {

                            long armedTime = guardBreakArmed.get(uuid);

                            if (now - armedTime > GUARD_BREAK_ARM_TIME) {

                                guardBreakArmed.remove(uuid);

                                player.sendMessage(
                                        Text.literal("\u00A77Guard Break faded."),
                                        true
                                );

                            } else {

                                guardBreakArmed.remove(uuid);
                                guardBreakCritChain.put(uuid, 0);
                                lastGuardBreakCritTime.remove(uuid);

                                float bonusDamage = 4.0f + (level * 0.5f);

                                target.damage(
                                        sWorld,
                                        player.getDamageSources().playerAttack(player),
                                        bonusDamage
                                );

                                target.addStatusEffect(
                                        new StatusEffectInstance(
                                                StatusEffects.WEAKNESS,
                                                80,
                                                1
                                        )
                                );

                                target.addStatusEffect(
                                        new StatusEffectInstance(
                                                StatusEffects.SLOWNESS,
                                                60,
                                                1
                                        )
                                );

                                sWorld.spawnParticles(
                                        ParticleTypes.EXPLOSION,
                                        target.getX(),
                                        target.getY() + 1,
                                        target.getZ(),
                                        1,
                                        0,
                                        0,
                                        0,
                                        0
                                );

                                sWorld.spawnParticles(
                                        ParticleTypes.CRIT,
                                        target.getX(),
                                        target.getY() + 1,
                                        target.getZ(),
                                        20,
                                        0.4,
                                        0.4,
                                        0.4,
                                        0.2
                                );

                                world.playSound(
                                        null,
                                        target.getX(),
                                        target.getY(),
                                        target.getZ(),
                                        SoundEvents.BLOCK_ANVIL_LAND,
                                        SoundCategory.PLAYERS,
                                        1.0f,
                                        0.7f
                                );

                                player.sendMessage(
                                        Text.literal("\u00A74\u00A7lGUARD BREAK! \u00A77+" + (int) bonusDamage + " damage"),
                                        true
                                );

                                return ActionResult.PASS;
                            }
                        }

                        // =============================================
                        // GUARD BREAK CRIT CHAIN BUILDUP
                        // =============================================

                        long lastCrit = lastGuardBreakCritTime.getOrDefault(uuid, 0L);

                        if (now - lastCrit > GUARD_BREAK_CHAIN_RESET_TIME) {
                            guardBreakCritChain.put(uuid, 0);
                        }

                        if (player instanceof ServerPlayerEntity serverPlayer && isCriticalHit(serverPlayer)) {

                            int crits = guardBreakCritChain.getOrDefault(uuid, 0) + 1;
                            crits = Math.min(crits, GUARD_BREAK_REQUIRED_CRITS);

                            guardBreakCritChain.put(uuid, crits);
                            lastGuardBreakCritTime.put(uuid, now);

                            if (crits >= GUARD_BREAK_REQUIRED_CRITS) {

                                player.sendMessage(
                                        Text.literal("\u00A74\u00A7lGUARD BREAK READY \u00A77Right click to arm."),
                                        true
                                );

                                player.playSound(
                                        SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(),
                                        1.0f,
                                        0.7f
                                );

                            } else {

                                player.sendMessage(
                                        Text.literal("\u00A77Guard Break Crits: \u00A7e" + crits + "\u00A77/" + GUARD_BREAK_REQUIRED_CRITS),
                                        true
                                );
                            }

                        } else {

                            if (guardBreakCritChain.getOrDefault(uuid, 0) > 0) {
                                player.sendMessage(
                                        Text.literal("\u00A78Guard Break crit chain broken."),
                                        true
                                );
                            }

                            guardBreakCritChain.put(uuid, 0);
                            lastGuardBreakCritTime.remove(uuid);
                        }
                    }
                }

                // =============================================
                // AXE PARALYZE
                // Sneak + Attack
                // =============================================

                if (
                        isAxe(stack) &&
                                player.isSneaking() &&
                                stack.getOrDefault(ModComponents.MEGA_LEVEL, 0) >= 5
                ) {

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
            double multiplier = 1.0 + (charge * 2.0);

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
            tridentChargeStart.remove(uuid);
            blackFlashArmed.remove(uuid);
            guardBreakArmed.remove(uuid);
            guardBreakCritChain.remove(uuid);
            lastGuardBreakCritTime.remove(uuid);
        });
    }

    private boolean isAxe(ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.item.AxeItem;
    }

    private boolean isCriticalHit(ServerPlayerEntity player) {
        return player.fallDistance > 0.0F
                && !player.isOnGround()
                && !player.isClimbing()
                && !player.isTouchingWater()
                && !player.hasVehicle()
                && !player.isSprinting()
                && player.getAttackCooldownProgress(0.5f) > 0.9f;
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

            target.damage(
                    world,
                    player.getDamageSources().playerAttack(player),
                    BLACK_FLASH_BONUS_DAMAGE
            );

            target.takeKnockback(
                    3.5,
                    player.getX() - target.getX(),
                    player.getZ() - target.getZ()
            );

            player.sendMessage(
                    Text.literal("§0§l> §c§lBLACK FLASH §0§l<"),
                    true
            );
        }

    }
    private void checkMilestoneUnlock(ServerPlayerEntity player, ItemStack stack, int newLevel) {

        // =====================================================
        // SWORD MILESTONES
        // =====================================================

        if (
                stack.isOf(Items.WOODEN_SWORD) ||
                        stack.isOf(Items.STONE_SWORD) ||
                        stack.isOf(Items.IRON_SWORD) ||
                        stack.isOf(Items.GOLDEN_SWORD) ||
                        stack.isOf(Items.DIAMOND_SWORD) ||
                        stack.isOf(Items.NETHERITE_SWORD)
        ) {

            if (newLevel == 5) {
                player.sendMessage(
                        Text.literal("§b§lSWORD DASH UNLOCKED"),
                        false
                );

                player.sendMessage(
                        Text.literal("§7Sprint with a sword and press Dash to surge forward."),
                        false
                );

                player.playSound(
                        SoundEvents.ENTITY_PLAYER_LEVELUP,
                        1.0f,
                        1.2f
                );
            }

            if (newLevel == 10) {
                player.sendMessage(
                        Text.literal("§0§l> §c§lBLACK FLASH UNLOCKED §0§l<"),
                        false
                );

                player.sendMessage(
                        Text.literal("§7Every 4th sword hit triggers a powerful impact."),
                        false
                );

                player.playSound(
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                        1.0f,
                        1.0f
                );
            }
        }

        // =====================================================
        // TRIDENT MILESTONES
        // =====================================================

        if (stack.isOf(Items.TRIDENT)) {

            if (newLevel == 10) {
                player.sendMessage(
                        Text.literal("§3§lVORTEX THROW UNLOCKED"),
                        false
                );

                player.sendMessage(
                        Text.literal("§7Hold right click to charge a high-speed trident throw."),
                        false
                );

                player.playSound(
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                        1.0f,
                        1.0f
                );
            }

            if (newLevel == 15) {
                stack.addEnchantment(
                        player.getEntityWorld()
                                .getRegistryManager()
                                .getOrThrow(RegistryKeys.ENCHANTMENT)
                                .getOrThrow(Enchantments.LOYALTY),
                        3
                );

                player.sendMessage(
                        Text.literal("§3§lLOYALTY III UNLOCKED"),
                        false
                );

                player.sendMessage(
                        Text.literal("§7Your trident now returns after being thrown."),
                        false
                );

                player.playSound(
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                        1.0f,
                        1.2f
                );
            }
        }

        // =====================================================
        // AXE MILESTONES
        // =====================================================

        if (isAxe(stack)) {

            if (newLevel == 5) {
                player.sendMessage(
                        Text.literal("§4§lARMOR PARALYSIS UNLOCKED"),
                        false
                );

                player.sendMessage(
                        Text.literal("§7Sneak-attack enemies with an axe to paralyze them."),
                        false
                );

                player.playSound(
                        SoundEvents.ENTITY_PLAYER_LEVELUP,
                        1.0f,
                        0.8f
                );
            }

            if (newLevel == 10) {
                player.sendMessage(
                        Text.literal("\u00A74\u00A7lGUARD BREAK UNLOCKED"),
                        false
                );

                player.sendMessage(
                        Text.literal("\u00A77Right click with an axe to arm a devastating heavy strike."),
                        false
                );

                player.playSound(
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                        1.0f,
                        0.8f
                );
            }
        }
    }

}
