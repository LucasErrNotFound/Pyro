package com.yukimura.pyro;

import com.yukimura.pyro.entity.PyroEntities;
import com.yukimura.pyro.item.PyroItems;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pyro implements ModInitializer {

	public static final String MOD_ID = "pyro";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");

		PyroItems.initialize();
		PyroEntities.registerPyroEntityTypes();
	}
}