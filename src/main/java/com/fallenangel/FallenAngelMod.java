package com.fallenangel;

import io.github.apace100.origins.api.OriginsAPI;
import net.fabricmc.api.ModInitializer;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FallenAngelMod implements ModInitializer {

	public static final String MOD_ID = "fallenangel";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Item FALLEN_ANGEL_ICON = Registry.register(
			Registries.ITEM,
			Identifier.of(MOD_ID, "fallen_angel_icon"),
			new Item(new Item.Settings().maxCount(1))
	);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Fallen Angel Origin addon");
		LOGGER.info("Registering Fallen Angel origin...");
		
		// Register the origin layer
		OriginsAPI.registerOriginLayer(Identifier.of(MOD_ID, "origin"));
		
		LOGGER.info("Fallen Angel origin registered successfully!");
	}
}
