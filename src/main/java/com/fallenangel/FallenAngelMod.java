package com.fallenangel;

import com.fallenangel.action.DivineEnchantAction;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FallenAngelMod implements ModInitializer {

    public static final String MOD_ID = "fallenangel";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Fallen Angel Origin addon");

        DivineEnchantAction.register();
    }
}
