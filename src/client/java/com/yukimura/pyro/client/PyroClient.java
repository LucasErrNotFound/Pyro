package com.yukimura.pyro.client;

import com.yukimura.pyro.entity.PyroEntities;
import com.yukimura.pyro.item.DynamiteItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PyroClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		EntityRenderers.register(PyroEntities.DYNAMITE_ENTITY, ThrownItemRenderer::new);
		ClientTickEvents.END_CLIENT_TICK.register(PyroClient::onClientTick);
	}

	private static void onClientTick(Minecraft client) {
		if (client.player == null || client.level == null) return;
		Player player = client.player;
		Level level = client.level;
		spawnFuseParticlesForHand(player.getMainHandItem(), InteractionHand.MAIN_HAND, player, level);
		spawnFuseParticlesForHand(player.getOffhandItem(), InteractionHand.OFF_HAND, player, level);
	}

	private static void spawnFuseParticlesForHand(ItemStack stack, InteractionHand hand, Player player, Level level) {
		if (!DynamiteItem.isIgnited(stack)) return;

		float yawRadians  = (float) Math.toRadians(player.getYRot());
		double rightX     = -Math.cos(yawRadians);
		double rightZ     = -Math.sin(yawRadians);
		double forwardX   = -Math.sin(yawRadians);
		double forwardZ   =  Math.cos(yawRadians);
		double sideOffset = (hand == InteractionHand.MAIN_HAND) ? 0.2 : -0.2;

		double x = player.getX() + rightX * sideOffset + forwardX * 0.35 + (level.getRandom().nextDouble() - 0.5) * 0.08;
		double y = player.getY() + 1.0;
		double z = player.getZ() + rightZ * sideOffset + forwardZ * 0.35 + (level.getRandom().nextDouble() - 0.5) * 0.08;

		level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.04, 0.0);
		if (level.getRandom().nextInt(3) == 0) {
			level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0.0, 0.02, 0.0);
		}
	}
}