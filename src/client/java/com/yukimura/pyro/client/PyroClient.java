package com.yukimura.pyro.client;

import com.yukimura.pyro.entity.PyroEntities;
import com.yukimura.pyro.item.DynamiteItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

public class PyroClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		EntityRenderers.register(PyroEntities.DYNAMITE_ENTITY, ThrownItemRenderer::new);
		EntityRenderers.register(PyroEntities.CONTACT_DYNAMITE_ENTITY, ThrownItemRenderer::new);
		ClientTickEvents.END_CLIENT_TICK.register(PyroClient::onClientTick);
	}

	private static void onClientTick(Minecraft minecraft) {
		if (minecraft.player == null || minecraft.level == null) return;
		Player player = minecraft.player;
		Level level = minecraft.level;
		boolean firstPerson = minecraft.options.getCameraType() == CameraType.FIRST_PERSON;

		spawnHandParticles(player.getMainHandItem(), InteractionHand.MAIN_HAND, firstPerson, player, minecraft, level);
		spawnHandParticles(player.getOffhandItem(), InteractionHand.OFF_HAND, firstPerson, player, minecraft, level);
	}

	private static void spawnHandParticles(
			ItemStack stack, InteractionHand hand,
			boolean firstPerson,
			Player player, Minecraft minecraft, Level level) {

		if (!DynamiteItem.isIgnited(stack)) return;

		Vec3 fusePosition = firstPerson
				? firstPersonFallback(minecraft, hand)
				: thirdPersonEstimate(minecraft, player, hand);

		if (fusePosition == null) return;

		double spread = 0.04;
		double x = fusePosition.x + (level.getRandom().nextDouble() - 0.5) * spread;
		double y = fusePosition.y + (level.getRandom().nextDouble() - 0.5) * spread;
		double z = fusePosition.z + (level.getRandom().nextDouble() - 0.5) * spread;
		level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.04, 0.0);
		if (level.getRandom().nextInt(3) == 0) {
			level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0.0, 0.02, 0.0);
		}
	}

	private static Vec3 firstPersonFallback(Minecraft minecraft, InteractionHand hand) {
		Camera camera = minecraft.gameRenderer.getMainCamera();
		if (camera == null) return null;

		Vec3 cameraPosition = camera.position();
		Vector3fc forwardVector = camera.forwardVector();
		Vector3fc upVector = camera.upVector();
		Vector3fc leftVector = camera.leftVector();
		Vec3 forward = new Vec3(forwardVector.x(), forwardVector.y(), forwardVector.z());
		Vec3 up = new Vec3(upVector.x(), upVector.y(), upVector.z());
		Vec3 left = new Vec3(leftVector.x(), leftVector.y(), leftVector.z());

		double side = (hand == InteractionHand.MAIN_HAND) ? 0.15 : -0.15;
		return cameraPosition.add(forward.scale(0.15))
		                     .add(left.scale(-side))
		                     .add(up.scale(0.0));
	}

	private static Vec3 thirdPersonEstimate(Minecraft minecraft, Player player, InteractionHand hand) {
		Camera camera = minecraft.gameRenderer.getMainCamera();

		Vector3fc forwardVector = camera.forwardVector();
		Vector3fc leftVector = camera.leftVector();
		Vec3 cameraForward = new Vec3(forwardVector.x(), forwardVector.y(), forwardVector.z());
		Vec3 cameraLeft = new Vec3(leftVector.x(), leftVector.y(), leftVector.z());

		Vec3 horizontalForward = new Vec3(cameraForward.x, 0.0, cameraForward.z);
		double horizontalLength = horizontalForward.length();
		if (horizontalLength < 0.001) {
			float yawRadians = (float) Math.toRadians(player.yBodyRot);
			horizontalForward = new Vec3(-Mth.sin(yawRadians), 0.0, Mth.cos(yawRadians));
		} else {
			horizontalForward = horizontalForward.scale(1.0 / horizontalLength);
		}

		boolean isFrontThirdPerson = minecraft.options.getCameraType() == CameraType.THIRD_PERSON_FRONT;
		if (isFrontThirdPerson) {
			horizontalForward = horizontalForward.scale(-1.0);
			cameraLeft = cameraLeft.scale(-1.0);
		}

		double armSide = (hand == InteractionHand.MAIN_HAND) ? 0.3 : -0.3;
		return player.position()
		             .add(0, player.getEyeHeight() * 0.55, 0)
		             .add(horizontalForward.scale(0.6))
		             .add(cameraLeft.scale(-armSide));
	}
}
