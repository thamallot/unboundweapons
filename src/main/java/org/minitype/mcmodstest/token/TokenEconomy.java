package org.minitype.mcmodstest.token;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.Monster;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.minitype.mcmodstest.ModItems;

import java.util.UUID;

public final class TokenEconomy {

    private static final long PVP_PAIR_COOLDOWN_MS = 30L * 60L * 1000L;
    private static final long CHARGE_RECOVERY_MS = 30L * 60L * 1000L;
    private static final float MOB_TOKEN_DROP_CHANCE = 0.40f;
    private static final Identifier CHARGE_PENALTY_ID =
            Identifier.of("unbnd_weapons", "token_charge_penalty");

    private static int recoveryTicker;

    private TokenEconomy() {
    }

    public static void initialize() {
        TokenCommands.register();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            TokenEconomyState state = getState(server);
            state.ensurePlayer(handler.player.getUuid(), System.currentTimeMillis());
            applyChargePenalty(handler.player, state);

            handler.player.sendMessage(
                    Text.literal(
                            "§6Token mode: §e" + state.getMode().id().toUpperCase()
                                    + " §7| Charges: §e" + state.getCharges(handler.player.getUuid())
                    ),
                    false
            );
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!(damageSource.getAttacker() instanceof ServerPlayerEntity killer)) {
                return;
            }

            TokenEconomyState state = getState(killer.getEntityWorld().getServer());
            TokenMode mode = state.getMode();

            if (entity instanceof ServerPlayerEntity victim) {
                if (mode == TokenMode.SMP || mode == TokenMode.HYBRID) {
                    transferPlayerCharge(killer, victim, state);
                }
                return;
            }

            if ((mode == TokenMode.SINGLEPLAYER || mode == TokenMode.HYBRID)
                    && entity instanceof Monster
                    && killer.getRandom().nextFloat() <= MOB_TOKEN_DROP_CHANCE) {
                awardSpendableToken(killer);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            recoveryTicker++;

            if (recoveryTicker < 200) {
                return;
            }

            recoveryTicker = 0;
            recoverCharges(server);
        });
    }

    public static TokenEconomyState getState(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TokenEconomyState.TYPE);
    }

    private static void transferPlayerCharge(
            ServerPlayerEntity killer,
            ServerPlayerEntity victim,
            TokenEconomyState state
    ) {
        if (killer.getUuid().equals(victim.getUuid())) {
            return;
        }

        long now = System.currentTimeMillis();
        state.ensurePlayer(killer.getUuid(), now);
        state.ensurePlayer(victim.getUuid(), now);

        String pairKey = pairKey(killer.getUuid(), victim.getUuid());
        long lastTransfer = state.getPairCooldown(pairKey);

        if (now - lastTransfer < PVP_PAIR_COOLDOWN_MS) {
            killer.sendMessage(Text.literal("§7This player cannot transfer another charge yet."), true);
            return;
        }

        int victimCharges = state.getCharges(victim.getUuid());
        int killerCharges = state.getCharges(killer.getUuid());

        if (victimCharges <= 0) {
            killer.sendMessage(Text.literal("§7This player has no Token Charges left."), true);
            return;
        }

        if (killerCharges >= TokenEconomyState.MAX_CHARGES) {
            killer.sendMessage(Text.literal("§7Your Token Charges are already full."), true);
            return;
        }

        state.setCharges(victim.getUuid(), victimCharges - 1);
        state.setCharges(killer.getUuid(), killerCharges + 1);
        state.setPairCooldown(pairKey, now);
        state.setRecoveryTime(victim.getUuid(), now);

        awardSpendableToken(killer);
        applyChargePenalty(killer, state);
        applyChargePenalty(victim, state);

        killer.sendMessage(
                Text.literal("§6You took a Token Charge. §eCharges: " + (killerCharges + 1)),
                false
        );
        victim.sendMessage(
                Text.literal("§cYou lost a Token Charge. §eCharges: " + (victimCharges - 1)),
                false
        );
    }

    private static void awardSpendableToken(ServerPlayerEntity player) {
        ItemStack token = new ItemStack(ModItems.MEGA_TOKEN);

        if (!player.getInventory().insertStack(token)) {
            player.dropItem(token, false);
        }

        player.sendMessage(Text.literal("§b§l+1 UNBOUND TOKEN"), false);
        player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
    }

    private static void recoverCharges(MinecraftServer server) {
        TokenEconomyState state = getState(server);
        long now = System.currentTimeMillis();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID uuid = player.getUuid();
            state.ensurePlayer(uuid, now);
            int charges = state.getCharges(uuid);

            if (charges < TokenEconomyState.STARTING_CHARGES
                    && now - state.getRecoveryTime(uuid) >= CHARGE_RECOVERY_MS) {
                state.setCharges(uuid, charges + 1);
                state.setRecoveryTime(uuid, now);
                player.sendMessage(Text.literal("§aYou recovered 1 Token Charge."), false);
            }

            applyChargePenalty(player, state);
        }
    }

    private static void applyChargePenalty(ServerPlayerEntity player, TokenEconomyState state) {
        EntityAttributeInstance attackDamage = player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);

        if (attackDamage == null) {
            return;
        }

        attackDamage.removeModifier(CHARGE_PENALTY_ID);

        int missingCharges = Math.max(0, TokenEconomyState.STARTING_CHARGES - state.getCharges(player.getUuid()));

        if (missingCharges > 0) {
            attackDamage.addTemporaryModifier(
                    new EntityAttributeModifier(
                            CHARGE_PENALTY_ID,
                            -0.03 * missingCharges,
                            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    )
            );
        }
    }

    private static String pairKey(UUID first, UUID second) {
        String firstId = first.toString();
        String secondId = second.toString();
        return firstId.compareTo(secondId) <= 0
                ? firstId + ":" + secondId
                : secondId + ":" + firstId;
    }
}
