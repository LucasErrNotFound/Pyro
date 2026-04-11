package com.yukimura.pyro.client;

import com.yukimura.pyro.entity.PyroEntities;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class PyroClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		EntityRenderers.register(PyroEntities.DYNAMITE_ENTITY, ThrownItemRenderer::new);
	}
}