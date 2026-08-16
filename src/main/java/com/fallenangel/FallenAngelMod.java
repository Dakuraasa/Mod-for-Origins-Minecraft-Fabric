package com.fallenangel;

import com.fallenangel.action.DivineEnchantAction;
import net.fabricmc.api.ModInitializer;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the Fallen Angel Origin addon.
 *
 * This mod does NOT touch Origins/Apoli's Java internals for the
 * "easy" parts of the race (extra hearts, strength, undead damage,
 * disabling everything in the Nether, the Totem-like resurrection,
 * and walking on water). Those are all implemented purely with
 * Origins power JSON files under
 * src/main/resources/data/origins/powers/, because that is the
 * documented, stable part of the Origins API.
 *
 * The ONLY thing that genuinely needs Java code is the "Divine
 * Enchantment" active ability (picking a random, item-compatible,
 * correctly-leveled enchantment - and the Apple -> Enchanted Golden
 * Apple special case). That logic lives in
 * {@link DivineEnchantAction}, and is wired into Apoli's custom
 * action registry below.
 */
public class FallenAngelMod implements ModInitializer {

    public static final String MOD_ID = "fallenangel";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Purely cosmetic item, used only so the Origin has a real,
     *  game-native icon rendered from the supplied artwork
     *  (assets/fallenangel/textures/item/fallen_angel_icon.png).
     *  It has no other function and does not appear in survival
     *  inventories or creative tabs. */
    public static final Item FALLEN_ANGEL_ICON = new Item(new Item.Settings()
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "fallen_angel_icon")))
            .maxCount(1));

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Fallen Angel Origin addon");

        Registry.register(Registries.ITEM,
                Identifier.of(MOD_ID, "fallen_angel_icon"),
                FALLEN_ANGEL_ICON);

        // Registers the "fallenangel:divine_enchant" entity_action type
        // so it can be referenced from data/origins/powers/divine_enchant.json
        DivineEnchantAction.register();
    }
}
