package com.yukimura.pyro.client;

import com.yukimura.pyro.entity.PyroEntities;
import com.yukimura.pyro.item.DynamiteItem;
import com.yukimura.pyro.item.MolotovItem;
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
		EntityRenderers.register(PyroEntities.MOLOTOV_ENTITY, ThrownItemRenderer::new);
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

		if (!DynamiteItem.isIgnited(stack) && !MolotovItem.isIgnited(stack)) return;

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
		Vector3fc leftVector = camera.leftVector();

		double side = (hand == InteractionHand.MAIN_HAND) ? 0.15 : -0.15;
		double forwardOffsetX = forwardVector.x() * 0.15;
		double forwardOffsetY = forwardVector.y() * 0.15;
		double forwardOffsetZ = forwardVector.z() * 0.15;
		double lateralOffsetX = leftVector.x() * (-side);
		double lateralOffsetY = leftVector.y() * (-side);
		double lateralOffsetZ = leftVector.z() * (-side);
		return new Vec3(
			cameraPosition.x + forwardOffsetX + lateralOffsetX,
			cameraPosition.y + forwardOffsetY + lateralOffsetY,
			cameraPosition.z + forwardOffsetZ + lateralOffsetZ
		);
	}

	private static Vec3 thirdPersonEstimate(Minecraft minecraft, Player player, InteractionHand hand) {
		Camera camera = minecraft.gameRenderer.getMainCamera();

		Vector3fc forwardVector = camera.forwardVector();
		Vector3fc leftVector = camera.leftVector();

		double forwardX = forwardVector.x();
		double forwardZ = forwardVector.z();
		double horizontalLength = Math.sqrt(forwardX * forwardX + forwardZ * forwardZ);

		double horizontalForwardX, horizontalForwardZ;
		if (horizontalLength < 0.001) {
			float yawRadians = (float) Math.toRadians(player.yBodyRot);
			horizontalForwardX = -Mth.sin(yawRadians);
			horizontalForwardZ = Mth.cos(yawRadians);
		} else {
			double inverseHorizontalLength = 1.0 / horizontalLength;
			horizontalForwardX = forwardX * inverseHorizontalLength;
			horizontalForwardZ = forwardZ * inverseHorizontalLength;
		}

		double leftX = leftVector.x();
		double leftY = leftVector.y();
		double leftZ = leftVector.z();

		if (minecraft.options.getCameraType() == CameraType.THIRD_PERSON_FRONT) {
			horizontalForwardX = -horizontalForwardX;
			horizontalForwardZ = -horizontalForwardZ;
			leftX = -leftX;
			leftY = -leftY;
			leftZ = -leftZ;
		}

		double armSide = (hand == InteractionHand.MAIN_HAND) ? 0.3 : -0.3;
		Vec3 position = player.position();
		return new Vec3(
			position.x + horizontalForwardX * 0.6 + leftX * (-armSide),
			position.y + player.getEyeHeight() * 0.55 + leftY * (-armSide),
			position.z + horizontalForwardZ * 0.6 + leftZ * (-armSide)
		);
	}
}
