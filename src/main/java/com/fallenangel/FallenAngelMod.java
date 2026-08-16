package com.fallenangel;

import com.fallenangel.action.DivineEnchantAction;
import net.fabricmc.api.ModInitializer;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the Fallen Angel Origin addon.
 */
public class FallenAngelMod implements ModInitializer {

    public static final String MOD_ID = "fallenangel";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Purely cosmetic item, used only so the Origin has a real,
     *  game-native icon rendered from the supplied artwork. */
    public static final Item FALLEN_ANGEL_ICON = Registry.register(
            Registries.ITEM,
            Identifier.of(MOD_ID, "fallen_angel_icon"),
            new Item(new Item.Settings().maxCount(1))
    );

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Fallen Angel Origin addon");

        // Registers the "fallenangel:divine_enchant" entity_action type
        // so it can be referenced from data/origins/powers/divine_enchant.json
        DivineEnchantAction.register();
    }
}
