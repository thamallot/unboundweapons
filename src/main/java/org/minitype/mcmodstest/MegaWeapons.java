package org.minitype.mcmodstest;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;

import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.EnchantmentHelper;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ArrowItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.KineticWeaponComponent;
import net.minecraft.registry.tag.ItemTags;

import net.minecraft.particle.ParticleTypes;

import net.minecraft.registry.RegistryKeys;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import net.minecraft.text.Text;

import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.minitype.mcmodstest.token.TokenEconomy;

public class MegaWeapons implements ModInitializer {

    private static final float MAX_TRIDENT_CHARGE = 3.0f;
    private static final float MAX_TRIDENT_DAMAGE = 100.0f;
    private static final Map<UUID, Long> tridentChargeStart = new HashMap<>();
    private static final Set<UUID> blackFlashArmed = new HashSet<>();
    private static final Map<UUID, Long> guardBreakArmed = new HashMap<>();
    private static final Map<UUID, Integer> guardBreakCritChain = new HashMap<>();
    private static final Map<UUID, Long> lastGuardBreakCritTime = new HashMap<>();
    private static final Map<UUID, SkewerDashState> skewerDashes = new HashMap<>();
    private static final Map<UUID, Long> lastSkewerDashTime = new HashMap<>();
    private static final Map<UUID, MomentumState> momentumStates = new HashMap<>();
    private static final Set<UUID> momentumConsumePending = new HashSet<>();
    private static final Map<UUID, MeteorDropState> meteorDrops = new HashMap<>();
    private static final Map<UUID, Long> lastMeteorDropTime = new HashMap<>();
    private static final Map<UUID, GravityWellState> gravityWells = new HashMap<>();
    private static final Map<UUID, Long> lastGravityWellTime = new HashMap<>();
    private static final Map<UUID, Long> lastRecoilShotTime = new HashMap<>();
    private static final Set<UUID> volleyArmed = new HashSet<>();
    private static final Set<UUID> volleyGeneratedArrows = new HashSet<>();

    private static final double SKEWER_DASH_SPEED = 1.374;
    private static final double SKEWER_DASH_MAX_DISTANCE = 12.0;
    private static final int SKEWER_DASH_MAX_TICKS = 20;
    private static final int SKEWER_DASH_LEVEL_REQUIREMENT = 10;
    private static final long SKEWER_DASH_COOLDOWN_MS = 5000;
    private static final float SKEWER_WALL_DAMAGE_MIN = 1.0f;
    private static final float SKEWER_WALL_DAMAGE_MAX = 10.0f;
    private static final int MOMENTUM_LEVEL_REQUIREMENT = 5;
    private static final int MOMENTUM_MAX_LUNGE_LEVEL = 255;
    private static final long MOMENTUM_CHARGE_INTERVAL_MS = 7000;
    private static final int MOMENTUM_CLICKS_BEFORE_RESET = 3;
    private static final int METEOR_DROP_LEVEL_REQUIREMENT = 5;
    private static final int METEOR_LAUNCH_LEVEL_REQUIREMENT = 10;
    private static final long METEOR_DROP_COOLDOWN_MS = 5000;
    private static final double METEOR_LAUNCH_SPEED = 1.8;
    private static final double METEOR_DROP_SPEED = -2.4;
    private static final int METEOR_DROP_MAX_TICKS = 100;
    private static final float METEOR_DROP_BASE_DAMAGE = 4.0f;
    private static final float METEOR_DROP_MAX_DAMAGE = 16.0f;
    private static final double METEOR_DROP_BASE_RADIUS = 2.5;
    private static final double METEOR_DROP_MAX_RADIUS = 5.0;
    private static final double METEOR_DROP_BASE_BOUNCE = 0.85;
    private static final double METEOR_DROP_MAX_BOUNCE = 1.35;
    private static final int GRAVITY_WELL_LEVEL_REQUIREMENT = 10;
    private static final long GRAVITY_WELL_COOLDOWN_MS = 12000;
    private static final double GRAVITY_WELL_RADIUS = 10.0;
    private static final int GRAVITY_WELL_DURATION_TICKS = 30;
    private static final int RECOIL_SHOT_LEVEL_REQUIREMENT = 5;
    private static final long RECOIL_SHOT_COOLDOWN_MS = 5000;
    private static final double RECOIL_SHOT_PLAYER_SPEED = 1.5;
    private static final float RECOIL_SHOT_ARROW_SPEED = 3.0f;
    private static final int VOLLEY_LEVEL_REQUIREMENT = 10;
    private static final int VOLLEY_GRID_SIZE = 5;
    private static final int VOLLEY_ARROW_COST = 24;
    private static final int VOLLEY_INFINITY_ARROW_COST = 7;
    private static final float VOLLEY_UPWARD_SPEED = (float) SKEWER_DASH_SPEED;
    private static final double VOLLEY_STRAIGHT_UP_THRESHOLD = 0.95;

    private static final class SkewerDashState {
        private final Vec3d start;
        private final Vec3d direction;
        private UUID targetUuid;
        private int ticks;

        private SkewerDashState(Vec3d start, Vec3d direction) {
            this.start = start;
            this.direction = direction;
        }
    }

    private static final class MomentumState {
        private final ItemStack spear;
        private final int originalLungeLevel;
        private long nextChargeTime;
        private int temporaryLungeLevel;
        private int spearClicksRemaining = MOMENTUM_CLICKS_BEFORE_RESET;

        private MomentumState(ItemStack spear, int originalLungeLevel, long now) {
            this.spear = spear;
            this.originalLungeLevel = originalLungeLevel;
            this.temporaryLungeLevel = originalLungeLevel;
            this.nextChargeTime = now + MOMENTUM_CHARGE_INTERVAL_MS;
        }
    }

    private static final class MeteorDropState {
        private final ItemStack mace;
        private double startY;
        private boolean awaitingDrop;
        private int ticks;

        private MeteorDropState(ItemStack mace, double startY, boolean awaitingDrop) {
            this.mace = mace;
            this.startY = startY;
            this.awaitingDrop = awaitingDrop;
        }
    }

    private static final class GravityWellState {
        private final ItemStack mace;
        private int ticks;

        private GravityWellState(ItemStack mace) {
            this.mace = mace;
        }
    }

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

        TokenEconomy.initialize();

        System.out.println("Mega Weapons Initialized!");

        // =====================================================
        // NETWORK PAYLOADS
        // =====================================================

        PayloadTypeRegistry.playC2S().register(
                BlackFlashPayload.ID,
                BlackFlashPayload.CODEC
        );

        PayloadTypeRegistry.playC2S().register(
                SkewerDashPayload.ID,
                SkewerDashPayload.CODEC
        );

        PayloadTypeRegistry.playC2S().register(
                MomentumPayload.ID,
                MomentumPayload.CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                MomentumAttackPayload.ID,
                MomentumAttackPayload.CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                MeteorDropPayload.ID,
                MeteorDropPayload.CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                GravityWellPayload.ID,
                GravityWellPayload.CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                RecoilShotPayload.ID,
                RecoilShotPayload.CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                VolleyArmPayload.ID,
                VolleyArmPayload.CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                VolleyCancelPayload.ID,
                VolleyCancelPayload.CODEC
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

        ServerPlayNetworking.registerGlobalReceiver(
                SkewerDashPayload.ID,
                (payload, context) -> startSkewerDash(context.player())
        );

        ServerPlayNetworking.registerGlobalReceiver(
                MomentumPayload.ID,
                (payload, context) -> startMomentum(context.player())
        );
        ServerPlayNetworking.registerGlobalReceiver(
                MomentumAttackPayload.ID,
                (payload, context) -> consumeMomentumOnSpearClick(context.player())
        );
        ServerPlayNetworking.registerGlobalReceiver(
                MeteorDropPayload.ID,
                (payload, context) -> startMeteorDrop(context.player())
        );
        ServerPlayNetworking.registerGlobalReceiver(
                GravityWellPayload.ID,
                (payload, context) -> startGravityWell(context.player())
        );
        ServerPlayNetworking.registerGlobalReceiver(
                RecoilShotPayload.ID,
                (payload, context) -> useRecoilShot(context.player())
        );
        ServerPlayNetworking.registerGlobalReceiver(
                VolleyArmPayload.ID,
                (payload, context) -> armVolley(context.player())
        );
        ServerPlayNetworking.registerGlobalReceiver(
                VolleyCancelPayload.ID,
                (payload, context) -> volleyArmed.remove(context.player().getUuid())
        );

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
                            stack.isOf(Items.COPPER_SWORD) ||
                            stack.isOf(Items.TRIDENT) ||
                            stack.isOf(Items.MACE) ||
                            stack.isOf(Items.BOW) ||
                            isAxe(stack) ||
                            stack.isIn(ItemTags.SPEARS) ||
                            stack.isOf(Items.COPPER_SWORD)
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
                                stack.isOf(Items.NETHERITE_SWORD) ||
                                stack.isOf(Items.COPPER_SWORD)
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

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof PersistentProjectileEntity
                    && volleyGeneratedArrows.remove(entity.getUuid())) {
                return;
            }

            if (entity instanceof PersistentProjectileEntity arrow
                    && arrow.getOwner() instanceof ServerPlayerEntity player
                    && arrow.getWeaponStack().isOf(Items.BOW)
                    && volleyArmed.contains(player.getUuid())) {
                triggerVolleyOrPowerShot(player, arrow, world);
            }
        });

        // =====================================================
        // TRIDENT CHARGE HUD
        // =====================================================

        ServerTickEvents.END_SERVER_TICK.register(server -> {

            tickSkewerDashes(server);
            tickMomentum(server);
            tickMeteorDrops(server);
            tickGravityWells(server);

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
            skewerDashes.remove(uuid);
            lastSkewerDashTime.remove(uuid);
            meteorDrops.remove(uuid);
            lastMeteorDropTime.remove(uuid);
            gravityWells.remove(uuid);
            lastGravityWellTime.remove(uuid);
            lastRecoilShotTime.remove(uuid);
            volleyArmed.remove(uuid);
            resetMomentum(handler.player, false);
        });
    }

    private static void startMomentum(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();

        if (momentumStates.containsKey(uuid)) {
            player.sendMessage(Text.literal("§7Momentum is already charging."), true);
            return;
        }

        if (!player.isSprinting()) {
            player.sendMessage(Text.literal("§7Start sprinting before activating Momentum."), true);
            return;
        }

        int spearSlot = -1;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack candidate = player.getInventory().getStack(slot);

            if (candidate.isIn(ItemTags.SPEARS)
                    && candidate.getOrDefault(ModComponents.MEGA_LEVEL, 0)
                    >= MOMENTUM_LEVEL_REQUIREMENT) {
                spearSlot = slot;
                break;
            }
        }

        if (spearSlot < 0) {
            player.sendMessage(
                    Text.literal("§7Momentum unlocks when a spear reaches Mega Level 5."),
                    true
            );
            return;
        }

        player.getInventory().setSelectedSlot(spearSlot);
        player.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(spearSlot));

        ItemStack spear = player.getInventory().getSelectedStack();
        var lunge = player.getEntityWorld()
                .getRegistryManager()
                .getOrThrow(RegistryKeys.ENCHANTMENT)
                .getOrThrow(Enchantments.LUNGE);
        int originalLevel = EnchantmentHelper.getLevel(lunge, spear);

        momentumStates.put(uuid, new MomentumState(spear, originalLevel, System.currentTimeMillis()));
        player.sendMessage(Text.literal("§6§lMOMENTUM §7Keep sprinting to charge Lunge."), true);
    }

    private static void tickMomentum(net.minecraft.server.MinecraftServer server) {
        long now = System.currentTimeMillis();
        var iterator = momentumStates.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, MomentumState> entry = iterator.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            MomentumState state = entry.getValue();

            if (player == null) {
                momentumConsumePending.remove(entry.getKey());
                iterator.remove();
                continue;
            }

            if (momentumConsumePending.remove(entry.getKey())) {
                state.spearClicksRemaining--;

                if (state.spearClicksRemaining <= 0) {
                    restoreMomentum(player, state);
                    player.sendMessage(Text.literal("§6Momentum reset after 3 spear left clicks."), true);
                    iterator.remove();
                } else {
                    player.sendMessage(
                            Text.literal(
                                    "§6Momentum: §e"
                                            + state.spearClicksRemaining
                                            + " spear left clicks remaining"
                            ),
                            true
                    );
                }
                continue;
            }

            if (!player.isAlive()
                    || !player.isSprinting()
                    || player.getMainHandStack() != state.spear) {
                restoreMomentum(player, state);
                player.sendMessage(Text.literal("§7Momentum reset."), true);
                iterator.remove();
                continue;
            }

            while (now >= state.nextChargeTime
                    && state.temporaryLungeLevel < MOMENTUM_MAX_LUNGE_LEVEL) {
                state.temporaryLungeLevel++;
                setLungeLevel(player, state.spear, state.temporaryLungeLevel);
                state.nextChargeTime += MOMENTUM_CHARGE_INTERVAL_MS;

                player.sendMessage(
                        Text.literal("§6Momentum: §eLunge level " + state.temporaryLungeLevel),
                        true
                );
                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.4f);
            }
        }
    }

    private static void consumeMomentumOnSpearClick(ServerPlayerEntity player) {
        MomentumState state = momentumStates.get(player.getUuid());

        if (state == null
                || player.getMainHandStack() != state.spear
                || state.temporaryLungeLevel <= state.originalLungeLevel) {
            return;
        }

        momentumConsumePending.add(player.getUuid());
    }

    private static void resetMomentum(ServerPlayerEntity player, boolean notify) {
        MomentumState state = momentumStates.remove(player.getUuid());
        momentumConsumePending.remove(player.getUuid());

        if (state == null) {
            return;
        }

        restoreMomentum(player, state);

        if (notify) {
            player.sendMessage(Text.literal("§7Momentum reset."), true);
        }
    }

    private static void restoreMomentum(ServerPlayerEntity player, MomentumState state) {
        setLungeLevel(player, state.spear, state.originalLungeLevel);
    }

    private static void setLungeLevel(ServerPlayerEntity player, ItemStack spear, int level) {
        var lunge = player.getEntityWorld()
                .getRegistryManager()
                .getOrThrow(RegistryKeys.ENCHANTMENT)
                .getOrThrow(Enchantments.LUNGE);

        EnchantmentHelper.apply(spear, enchantments -> enchantments.set(lunge, level));
    }

    private static void armVolley(ServerPlayerEntity player) {
        ItemStack bow = player.getMainHandStack();

        if (!bow.isOf(Items.BOW)) {
            return;
        }

        if (bow.getOrDefault(ModComponents.MEGA_LEVEL, 0) < VOLLEY_LEVEL_REQUIREMENT) {
            player.sendMessage(Text.literal("§7Volley unlocks at Mega Level 10."), true);
            return;
        }

        volleyArmed.add(player.getUuid());
        player.sendMessage(Text.literal("§6Volley armed: §7keep holding X and fire."), true);
    }

    private static void triggerVolleyOrPowerShot(
            ServerPlayerEntity player,
            PersistentProjectileEntity primaryArrow,
            ServerWorld world
    ) {
        Vec3d primaryVelocity = primaryArrow.getVelocity();
        boolean shotStraightUp = primaryVelocity.lengthSquared() > 0.001
                && primaryVelocity.normalize().y >= VOLLEY_STRAIGHT_UP_THRESHOLD;
        Vec3d aimPoint = shotStraightUp
                ? player.getEntityPos()
                : player.raycast(64.0, 1.0f, false).getPos();

        if (hasVolleyObstruction(world, aimPoint)) {
            primaryArrow.setVelocity(primaryArrow.getVelocity().multiply(4.0));
            primaryArrow.velocityDirty = true;
            world.spawnParticles(
                    ParticleTypes.CRIT,
                    primaryArrow.getX(),
                    primaryArrow.getY(),
                    primaryArrow.getZ(),
                    20,
                    0.25,
                    0.25,
                    0.25,
                    0.2
            );
            player.sendMessage(Text.literal("§c§lPOWER SHOT §7Overhead obstruction detected: 4x speed."), true);
            return;
        }

        ItemStack bow = primaryArrow.getWeaponStack().copy();
        ItemStack ammunition = primaryArrow.getItemStack().copy();
        ammunition.setCount(1);

        if (!(ammunition.getItem() instanceof ArrowItem arrowItem)) {
            return;
        }

        var infinity = world.getRegistryManager()
                .getOrThrow(RegistryKeys.ENCHANTMENT)
                .getOrThrow(Enchantments.INFINITY);
        boolean hasInfinity = EnchantmentHelper.getLevel(infinity, bow) > 0;
        int totalArrowCost = hasInfinity
                ? VOLLEY_INFINITY_ARROW_COST
                : VOLLEY_ARROW_COST;

        // A normal non-Infinity bow has already consumed the primary arrow
        // by the time this event runs, so only remove the remaining cost.
        int inventoryArrowCost = totalArrowCost - (hasInfinity ? 0 : 1);

        if (!player.isInCreativeMode()
                && !consumeVolleyArrows(player, inventoryArrowCost)) {
            player.sendMessage(
                    Text.literal(
                            "§cVolley needs "
                                    + totalArrowCost
                                    + " arrows"
                                    + (hasInfinity ? " with Infinity." : ".")
                    ),
                    true
            );
            return;
        }

        int gridRadius = VOLLEY_GRID_SIZE / 2;

        for (int gridX = -gridRadius; gridX <= gridRadius; gridX++) {
            for (int gridZ = -gridRadius; gridZ <= gridRadius; gridZ++) {
                // The player's original aimed arrow occupies the center of
                // the pattern, so the summoned 5x5 grid omits that cell.
                if (gridX == 0 && gridZ == 0) {
                    continue;
                }

                double x = aimPoint.x + gridX;
                double z = aimPoint.z + gridZ;
                int height = 7 + world.getRandom().nextInt(9);
                double y = aimPoint.y + height;
                ItemStack enchantedBow = bow.copy();
                ItemStack volleyAmmunition = ammunition.copy();

                PersistentProjectileEntity volleyArrow = arrowItem.createArrow(
                        world,
                        volleyAmmunition,
                        player,
                        enchantedBow
                );
                volleyArrow.setPosition(x, y, z);
                volleyArrow.setOwner(player);
                volleyArrow.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
                volleyGeneratedArrows.add(volleyArrow.getUuid());

                ProjectileEntity.spawnWithVelocity(
                        volleyArrow,
                        world,
                        enchantedBow,
                        0.0,
                        1.0,
                        0.0,
                        VOLLEY_UPWARD_SPEED,
                        0.0f
                );
            }
        }

        world.spawnParticles(
                ParticleTypes.CLOUD,
                aimPoint.x,
                aimPoint.y + 10.0,
                aimPoint.z,
                30,
                3.0,
                3.0,
                3.0,
                0.05
        );

        if (shotStraightUp) {
            primaryArrow.discard();
        }

        player.sendMessage(Text.literal("§6§lARROW VOLLEY"), true);
        player.playSound(SoundEvents.ENTITY_ARROW_SHOOT, 1.0f, 0.6f);
    }

    private static boolean consumeVolleyArrows(ServerPlayerEntity player, int amount) {
        int arrowsFound = 0;

        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);

            if (stack.isIn(ItemTags.ARROWS)) {
                arrowsFound += stack.getCount();
            }
        }

        if (arrowsFound < amount) {
            return false;
        }

        int arrowsRemaining = amount;

        for (int slot = 0; slot < player.getInventory().size() && arrowsRemaining > 0; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);

            if (!stack.isIn(ItemTags.ARROWS)) {
                continue;
            }

            int removeAmount = Math.min(arrowsRemaining, stack.getCount());
            stack.decrement(removeAmount);
            arrowsRemaining -= removeAmount;
        }

        return true;
    }

    private static boolean hasVolleyObstruction(ServerWorld world, Vec3d aimPoint) {
        int gridRadius = VOLLEY_GRID_SIZE / 2;

        for (int gridX = -gridRadius; gridX <= gridRadius; gridX++) {
            for (int gridZ = -gridRadius; gridZ <= gridRadius; gridZ++) {
                for (int height = 1; height <= 15; height++) {
                    BlockPos blockPos = BlockPos.ofFloored(
                            aimPoint.add(gridX, height, gridZ)
                    );

                    if (!world.getBlockState(blockPos).isAir()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static void useRecoilShot(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        long now = System.currentTimeMillis();
        ItemStack bow = player.getMainHandStack();

        if (!bow.isOf(Items.BOW)) {
            return;
        }

        if (bow.getOrDefault(ModComponents.MEGA_LEVEL, 0) < RECOIL_SHOT_LEVEL_REQUIREMENT) {
            player.sendMessage(Text.literal("§7Recoil Shot unlocks at Mega Level 5."), true);
            return;
        }

        if (now - lastRecoilShotTime.getOrDefault(uuid, 0L) < RECOIL_SHOT_COOLDOWN_MS) {
            return;
        }

        ServerWorld world = player.getEntityWorld();
        Vec3d direction = player.getRotationVector().normalize();
        ItemStack arrowStack = new ItemStack(Items.ARROW);
        ArrowEntity arrow = new ArrowEntity(world, player, arrowStack, bow.copy());

        arrow.setVelocity(
                direction.x,
                direction.y,
                direction.z,
                RECOIL_SHOT_ARROW_SPEED,
                0.0f
        );
        arrow.setCritical(true);
        arrow.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
        world.spawnEntity(arrow);

        Vec3d recoilVelocity = direction.multiply(-RECOIL_SHOT_PLAYER_SPEED);
        player.setVelocity(recoilVelocity);
        player.velocityDirty = true;
        player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
        lastRecoilShotTime.put(uuid, now);

        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_ARROW_SHOOT,
                SoundCategory.PLAYERS,
                1.0f,
                0.8f
        );
        player.sendMessage(Text.literal("§6§lRECOIL SHOT"), true);
    }

    private static void startGravityWell(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        long now = System.currentTimeMillis();
        ItemStack mace = player.getMainHandStack();

        if (gravityWells.containsKey(uuid)
                || now - lastGravityWellTime.getOrDefault(uuid, 0L) < GRAVITY_WELL_COOLDOWN_MS) {
            return;
        }

        if (!player.isSneaking() || !mace.isOf(Items.MACE)) {
            return;
        }

        if (mace.getOrDefault(ModComponents.MEGA_LEVEL, 0) < GRAVITY_WELL_LEVEL_REQUIREMENT) {
            player.sendMessage(Text.literal("§7Gravity Well unlocks at Mega Level 10."), true);
            return;
        }

        lastGravityWellTime.put(uuid, now);
        gravityWells.put(uuid, new GravityWellState(mace));
        player.sendMessage(Text.literal("§5§lGRAVITY WELL"), true);
        player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, 1.0f, 0.6f);
    }

    private static void tickGravityWells(net.minecraft.server.MinecraftServer server) {
        var iterator = gravityWells.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, GravityWellState> entry = iterator.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            GravityWellState state = entry.getValue();

            if (player == null
                    || !player.isAlive()
                    || player.getMainHandStack() != state.mace
                    || state.ticks >= GRAVITY_WELL_DURATION_TICKS) {
                iterator.remove();
                continue;
            }

            ServerWorld world = player.getEntityWorld();
            Vec3d center = player.getEntityPos().add(0.0, 2.5, 0.0);

            for (Entity entity : world.getOtherEntities(
                    player,
                    player.getBoundingBox().expand(GRAVITY_WELL_RADIUS),
                    entity -> entity instanceof LivingEntity
                            && entity.isAlive()
                            && entity.isAttackable()
                            && !entity.isSpectator()
                            && !player.isTeammate(entity)
            )) {
                if (!(entity instanceof LivingEntity target)) {
                    continue;
                }

                Vec3d offset = center.subtract(target.getEntityPos());
                double distance = offset.length();

                if (distance > GRAVITY_WELL_RADIUS || distance < 0.1) {
                    continue;
                }

                double pullStrength = 0.22 + (distance / GRAVITY_WELL_RADIUS) * 0.28;
                Vec3d pull = offset.normalize().multiply(pullStrength);
                Vec3d slowedVelocity = target.getVelocity().multiply(0.55);
                target.setVelocity(slowedVelocity.add(pull));
                target.velocityDirty = true;
            }

            if (state.ticks % 2 == 0) {
                world.spawnParticles(
                        ParticleTypes.REVERSE_PORTAL,
                        center.x,
                        center.y,
                        center.z,
                        18,
                        GRAVITY_WELL_RADIUS * 0.45,
                        1.8,
                        GRAVITY_WELL_RADIUS * 0.45,
                        0.08
                );
            }

            state.ticks++;
        }
    }

    private static void startMeteorDrop(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        long now = System.currentTimeMillis();
        ItemStack mace = player.getMainHandStack();
        MeteorDropState activeDrop = meteorDrops.get(uuid);

        if (activeDrop != null) {
            if (activeDrop.awaitingDrop && mace == activeDrop.mace) {
                beginMeteorPlunge(player, activeDrop);
            }
            return;
        }

        if (now - lastMeteorDropTime.getOrDefault(uuid, 0L) < METEOR_DROP_COOLDOWN_MS) {
            return;
        }

        if (!mace.isOf(Items.MACE)) {
            return;
        }

        int level = mace.getOrDefault(ModComponents.MEGA_LEVEL, 0);

        if (level < METEOR_DROP_LEVEL_REQUIREMENT) {
            player.sendMessage(Text.literal("§7Meteor Drop unlocks at Mega Level 5."), true);
            return;
        }

        if (player.isTouchingWater() || player.hasVehicle()) {
            player.sendMessage(Text.literal("§7Meteor Drop cannot be used right now."), true);
            return;
        }

        if (level >= METEOR_LAUNCH_LEVEL_REQUIREMENT) {
            Vec3d direction = player.getRotationVector().normalize();
            Vec3d launchVelocity = direction.multiply(METEOR_LAUNCH_SPEED);

            lastMeteorDropTime.put(uuid, now);
            meteorDrops.put(uuid, new MeteorDropState(mace, player.getY(), true));
            player.setVelocity(launchVelocity);
            player.velocityDirty = true;
            player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
            player.sendMessage(Text.literal("§6§lMETEOR LAUNCH §7Press Dash again to drop."), true);
            player.playSound(SoundEvents.ENTITY_WIND_CHARGE_WIND_BURST.value(), 0.9f, 1.1f);
            return;
        }

        if (player.isOnGround()) {
            player.sendMessage(Text.literal("§7Meteor Drop can only be used while airborne."), true);
            return;
        }

        lastMeteorDropTime.put(uuid, now);
        MeteorDropState state = new MeteorDropState(mace, player.getY(), false);
        meteorDrops.put(uuid, state);
        beginMeteorPlunge(player, state);
    }

    private static void beginMeteorPlunge(ServerPlayerEntity player, MeteorDropState state) {
        state.awaitingDrop = false;
        state.startY = player.getY();
        state.ticks = 0;
        player.setVelocity(0.0, METEOR_DROP_SPEED, 0.0);
        player.velocityDirty = true;
        player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
        player.sendMessage(Text.literal("§6§lMETEOR DROP"), true);
        player.playSound(SoundEvents.ENTITY_WIND_CHARGE_WIND_BURST.value(), 0.9f, 0.7f);
    }

    private static void tickMeteorDrops(net.minecraft.server.MinecraftServer server) {
        var iterator = meteorDrops.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, MeteorDropState> entry = iterator.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            MeteorDropState state = entry.getValue();

            if (player == null || !player.isAlive()) {
                iterator.remove();
                continue;
            }

            if (player.getMainHandStack() != state.mace
                    || player.isTouchingWater()
                    || player.hasVehicle()
                    || state.ticks >= METEOR_DROP_MAX_TICKS) {
                if (!state.awaitingDrop) {
                    player.fallDistance = 0.0f;
                }
                iterator.remove();
                continue;
            }

            if (state.awaitingDrop) {
                // The directional launch can be aimed downward. Preserve a
                // real fall distance so vanilla mace smash attacks still
                // work during this first stage and may hit or miss normally.
                double directionalDropDistance = state.startY - player.getY();

                if (directionalDropDistance > 0.0) {
                    player.fallDistance = Math.max(
                            player.fallDistance,
                            (float) directionalDropDistance
                    );
                }
                state.ticks++;

                if (state.ticks > 2 && player.isOnGround()) {
                    iterator.remove();
                }
                continue;
            }

            if (state.ticks > 0 && player.isOnGround()) {
                triggerMeteorImpact(player, state);
                iterator.remove();
                continue;
            }

            player.fallDistance = 0.0f;
            player.setVelocity(0.0, Math.min(player.getVelocity().y, METEOR_DROP_SPEED), 0.0);
            player.velocityDirty = true;
            state.ticks++;
        }
    }

    private static void triggerMeteorImpact(ServerPlayerEntity player, MeteorDropState state) {
        ServerWorld world = player.getEntityWorld();
        double dropDistance = Math.max(0.0, state.startY - player.getY());
        float damage = (float) Math.min(
                METEOR_DROP_MAX_DAMAGE,
                METEOR_DROP_BASE_DAMAGE + dropDistance * 0.75
        );
        double radius = Math.min(
                METEOR_DROP_MAX_RADIUS,
                METEOR_DROP_BASE_RADIUS + dropDistance * 0.2
        );
        double bounceVelocity = Math.min(
                METEOR_DROP_MAX_BOUNCE,
                METEOR_DROP_BASE_BOUNCE + dropDistance * 0.04
        );

        player.fallDistance = 0.0f;

        for (Entity entity : world.getOtherEntities(
                player,
                player.getBoundingBox().expand(radius, 1.5, radius),
                entity -> entity instanceof LivingEntity
                        && entity.isAlive()
                        && entity.isAttackable()
                        && !entity.isSpectator()
                        && !player.isTeammate(entity)
        )) {
            if (!(entity instanceof LivingEntity target)) {
                continue;
            }

            double horizontalDistance = Math.sqrt(
                    target.squaredDistanceTo(player.getX(), target.getY(), player.getZ())
            );

            if (horizontalDistance > radius) {
                continue;
            }

            double strength = 1.0 - horizontalDistance / radius;
            target.damage(world, player.getDamageSources().playerAttack(player), damage);

            Vec3d knockback = target.getEntityPos().subtract(player.getEntityPos());
            Vec3d horizontal = new Vec3d(knockback.x, 0.0, knockback.z);

            if (horizontal.lengthSquared() > 0.001) {
                horizontal = horizontal.normalize().multiply(0.7 + strength * 0.9);
                target.addVelocity(horizontal.x, 0.35 + strength * 0.35, horizontal.z);
                target.velocityDirty = true;
            }
        }

        world.spawnParticles(
                ParticleTypes.EXPLOSION,
                player.getX(),
                player.getY() + 0.1,
                player.getZ(),
                12,
                radius * 0.45,
                0.15,
                radius * 0.45,
                0.05
        );
        world.playSound(
                null,
                player.getBlockPos(),
                SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                SoundCategory.PLAYERS,
                1.2f,
                0.7f
        );
        player.setVelocity(0.0, bounceVelocity, 0.0);
        player.velocityDirty = true;
        player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
        player.sendMessage(
                Text.literal(
                        "§6Meteor Drop: §c"
                                + String.format(Locale.ROOT, "%.1f", damage)
                                + " damage §7(§e"
                                + String.format(Locale.ROOT, "%.1f", radius)
                                + " block radius§7)"
                ),
                true
        );
    }

    private static void startSkewerDash(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        long now = System.currentTimeMillis();

        if (skewerDashes.containsKey(uuid)
                || now - lastSkewerDashTime.getOrDefault(uuid, 0L) < SKEWER_DASH_COOLDOWN_MS) {
            return;
        }

        int spearSlot = -1;
        int firstSpearSlot = -1;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack candidate = player.getInventory().getStack(slot);

            if (candidate.isIn(ItemTags.SPEARS)) {
                if (firstSpearSlot < 0) {
                    firstSpearSlot = slot;
                }

                if (candidate.getOrDefault(ModComponents.MEGA_LEVEL, 0)
                        >= SKEWER_DASH_LEVEL_REQUIREMENT) {
                    spearSlot = slot;
                    break;
                }
            }
        }

        if (firstSpearSlot < 0) {
            player.sendMessage(Text.literal("§7Place a spear in your hotbar to use Skewer Dash."), true);
            return;
        }

        if (spearSlot < 0) {
            player.sendMessage(
                    Text.literal("§7Skewer Dash unlocks when a spear reaches Mega Level 10."),
                    true
            );
            return;
        }

        player.getInventory().setSelectedSlot(spearSlot);
        player.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(spearSlot));

        Vec3d look = player.getRotationVector();
        Vec3d horizontal = new Vec3d(look.x, 0.0, look.z);

        if (horizontal.lengthSquared() < 0.0001) {
            return;
        }

        Vec3d direction = horizontal.normalize();
        ItemStack spear = player.getInventory().getSelectedStack();

        lastSkewerDashTime.put(uuid, now);
        skewerDashes.put(uuid, new SkewerDashState(player.getEntityPos(), direction));

        player.setVelocity(
                direction.x * SKEWER_DASH_SPEED,
                Math.max(player.getVelocity().y, 0.1),
                direction.z * SKEWER_DASH_SPEED
        );
        player.velocityDirty = true;
        player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
        spear.use(player.getEntityWorld(), player, Hand.MAIN_HAND);
        player.getItemCooldownManager().set(spear, (int) (SKEWER_DASH_COOLDOWN_MS / 50));
        player.sendMessage(Text.literal("§6§lSKEWER DASH"), true);

        player.getEntityWorld().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ITEM_SPEAR_LUNGE_3,
                SoundCategory.PLAYERS,
                1.0f,
                1.0f
        );
    }

    private static void tickSkewerDashes(net.minecraft.server.MinecraftServer server) {
        var iterator = skewerDashes.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, SkewerDashState> entry = iterator.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            SkewerDashState state = entry.getValue();

            if (player == null || !player.isAlive()) {
                iterator.remove();
                continue;
            }

            ServerWorld world = player.getEntityWorld();
            Vec3d velocity = player.getVelocity();
            double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            boolean hitWall = player.horizontalCollision && state.ticks > 0;

            ItemStack spear = player.getMainHandStack();

            if (!spear.isIn(ItemTags.SPEARS)) {
                player.clearActiveItem();
                iterator.remove();
                continue;
            }

            if (!hitWall) {
                Vec3d dashVelocity = new Vec3d(
                        state.direction.x * SKEWER_DASH_SPEED,
                        Math.max(velocity.y, 0.0),
                        state.direction.z * SKEWER_DASH_SPEED
                );

                player.setVelocity(dashVelocity);
                player.velocityDirty = true;
                player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));

                KineticWeaponComponent kineticWeapon = spear.get(DataComponentTypes.KINETIC_WEAPON);

                if (kineticWeapon != null) {
                    int fullyChargedUseTime = Math.max(
                            0,
                            spear.getMaxUseTime(player) - kineticWeapon.getUseTicks()
                    );

                    kineticWeapon.usageTick(
                            spear,
                            fullyChargedUseTime,
                            player,
                            EquipmentSlot.MAINHAND
                    );
                }
            }

            if (state.targetUuid == null) {
                Entity target = world.getOtherEntities(
                                player,
                                player.getBoundingBox().stretch(state.direction.multiply(1.5)).expand(0.6),
                                entity -> entity instanceof LivingEntity
                                        && entity.isAlive()
                                        && entity.isAttackable()
                                        && !entity.isSpectator()
                                        && !player.isTeammate(entity)
                        ).stream()
                        .findFirst()
                        .orElse(null);

                if (target instanceof LivingEntity livingTarget) {
                    state.targetUuid = livingTarget.getUuid();
                }
            }

            LivingEntity carried = findLivingEntity(world, state.targetUuid);

            if (!hitWall && carried != null && carried.isAlive()) {
                Vec3d carryPosition = player.getEntityPos()
                        .add(state.direction.multiply(1.15))
                        .add(0.0, 0.1, 0.0);

                carried.requestTeleport(carryPosition.x, carryPosition.y, carryPosition.z);
                carried.setVelocity(player.getVelocity());
                carried.velocityDirty = true;
            }

            if (hitWall) {
                if (carried != null && carried.isAlive()) {
                    double distanceTravelled = Math.sqrt(
                            player.getEntityPos().squaredDistanceTo(state.start)
                    );
                    double dashProgress = Math.min(
                            1.0,
                            distanceTravelled / SKEWER_DASH_MAX_DISTANCE
                    );
                    float bonusDamage = (float) (SKEWER_WALL_DAMAGE_MAX
                            - (SKEWER_WALL_DAMAGE_MAX - SKEWER_WALL_DAMAGE_MIN) * dashProgress);

                    carried.damage(
                            world,
                            player.getDamageSources().playerAttack(player),
                            bonusDamage
                    );

                    player.sendMessage(
                            Text.literal(
                                    "§6Skewer Dash wall bonus: §c+"
                                            + String.format(Locale.ROOT, "%.1f", bonusDamage)
                                            + " damage"
                            ),
                            false
                    );

                    world.playSound(
                            null,
                            carried.getX(),
                            carried.getY(),
                            carried.getZ(),
                            SoundEvents.BLOCK_ANVIL_LAND,
                            SoundCategory.PLAYERS,
                            0.8f,
                            1.4f
                    );
                    world.spawnParticles(
                            ParticleTypes.CRIT,
                            carried.getX(),
                            carried.getBodyY(0.5),
                            carried.getZ(),
                            20,
                            0.4,
                            0.4,
                            0.4,
                            0.2
                    );
                }

                player.setVelocity(0.0, player.getVelocity().y, 0.0);
                player.velocityDirty = true;
                player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
                player.clearActiveItem();
                iterator.remove();
                continue;
            }

            state.ticks++;

            boolean reachedMaxDistance = player.getEntityPos().squaredDistanceTo(state.start)
                    >= SKEWER_DASH_MAX_DISTANCE * SKEWER_DASH_MAX_DISTANCE;
            boolean dashExpired = state.ticks >= SKEWER_DASH_MAX_TICKS;
            boolean lostMomentum = state.ticks > 2 && horizontalSpeed < 0.15;

            if (reachedMaxDistance || dashExpired || lostMomentum) {
                player.clearActiveItem();
                iterator.remove();
            }
        }
    }

    private static LivingEntity findLivingEntity(ServerWorld world, UUID uuid) {
        if (uuid == null) {
            return null;
        }

        Entity entity = world.getEntity(uuid);
        return entity instanceof LivingEntity living ? living : null;
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
                    3.3,
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
                        stack.isOf(Items.NETHERITE_SWORD) ||
                        stack.isOf(Items.COPPER_SWORD)
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
        // SPEAR MILESTONES
        // =====================================================

        if (stack.isIn(ItemTags.SPEARS)) {
            if (newLevel == MOMENTUM_LEVEL_REQUIREMENT) {
                player.sendMessage(
                        Text.literal("§6§lMOMENTUM UNLOCKED"),
                        false
                );

                player.sendMessage(
                        Text.literal("§7Press Momentum while sprinting; every 7 seconds adds a temporary Lunge level."),
                        false
                );

                player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }

            if (newLevel == SKEWER_DASH_LEVEL_REQUIREMENT) {
                player.sendMessage(
                        Text.literal("§6§lSKEWER DASH UNLOCKED"),
                        false
                );

                player.sendMessage(
                        Text.literal("§7Press Dash with a spear in your hotbar to skewer an enemy."),
                        false
                );

                player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
        }

        // =====================================================
        // BOW MILESTONES
        // =====================================================

        if (stack.isOf(Items.BOW) && newLevel == RECOIL_SHOT_LEVEL_REQUIREMENT) {
            player.sendMessage(
                    Text.literal("§6§lRECOIL SHOT UNLOCKED"),
                    false
            );
            player.sendMessage(
                    Text.literal("§7Press Dash with a bow to fire forward and launch in the opposite direction."),
                    false
            );
            player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        }

        if (stack.isOf(Items.BOW) && newLevel == VOLLEY_LEVEL_REQUIREMENT) {
            player.sendMessage(
                    Text.literal("§6§lVOLLEY UNLOCKED"),
                    false
            );
            player.sendMessage(
                    Text.literal("§7Hold the remappable Volley key while firing your next arrow."),
                    false
            );
            player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.1f);
        }

        // =====================================================
        // MACE MILESTONES
        // =====================================================

        if (stack.isOf(Items.MACE)) {
            if (newLevel == METEOR_DROP_LEVEL_REQUIREMENT) {
                player.sendMessage(
                        Text.literal("§6§lMETEOR DROP UNLOCKED"),
                        false
                );
                player.sendMessage(
                        Text.literal("§7While airborne, press Dash with the mace held to create a landing shockwave."),
                        false
                );
                player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 0.8f);
            }

            if (newLevel == METEOR_LAUNCH_LEVEL_REQUIREMENT) {
                player.sendMessage(
                        Text.literal("§6§lMETEOR LAUNCH UNLOCKED"),
                        false
                );
                player.sendMessage(
                        Text.literal("§7Press Dash to launch in your look direction, then press it again to plunge."),
                        false
                );
                player.sendMessage(
                        Text.literal("§5Sneak + Dash now creates a Gravity Well around you."),
                        false
                );
                player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.9f);
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
