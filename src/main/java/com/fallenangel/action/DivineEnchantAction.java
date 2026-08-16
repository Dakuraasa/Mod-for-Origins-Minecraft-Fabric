package com.fallenangel.action;

import com.fallenangel.FallenAngelMod;

import io.github.apace100.apoli.power.factory.action.ActionFactory;
import io.github.apace100.apoli.registry.ApoliRegistries;
import io.github.apace100.calio.data.SerializableData;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * ==========================================================================
 *  READ ME BEFORE COMPILING
 * ==========================================================================
 * This class registers a custom Apoli "entity_action" type
 * ("fallenangel:divine_enchant") that data/origins/powers/divine_enchant.json
 * plugs into an "origins:active_self" power.
 *
 * The registration call in {@link #register()} (the ActionFactory /
 * ApoliRegistries part) is the single piece of this whole project that is
 * most likely to need a small tweak, because Apoli's internal Java API
 * (class/package names for factories and registries) has changed slightly
 * between versions - e.g. their own changelog for 1.21.1 alpha builds
 * mentions renaming "Active$Key" to "KeyBindingReference" that a
 * decompiled/mapped alpha 12 (matching gradle.properties) with your IDE and, if
 * "ActionFactory" or "ApoliRegistries" don't resolve exactly as written here,
 * look at one of Apoli's own built-in action classes (e.g. the source for
 * "clear_effect" or "spawn_particles" in the apoli-fabric GitHub repo) for
 * the exact factory/registration signature used by the version pinned in
 * gradle.properties, and mirror it here. The enchantment-picking logic
 * below (everything in applyDivineEnchant/pickRandomValidEnchantment) uses
 * plain vanilla/Fabric APIs and should not need changes.
 * ==========================================================================
 */
public class DivineEnchantAction {

    public static final Identifier ID = Identifier.of(FallenAngelMod.MOD_ID, "divine_enchant");

    public static void register() {
        ActionFactory<Entity> factory = new ActionFactory<>(
                ID,
                new SerializableData(),
                DivineEnchantAction::apply
        );
        Registry.register(ApoliRegistries.ENTITY_ACTION, ID, factory);
    }

    private static void apply(SerializableData.Instance data, Entity entity) {
        if (!(entity instanceof PlayerEntity player)) {
            return;
        }
        World world = player.getWorld();
        if (world.isClient || !(world instanceof ServerWorld serverWorld)) {
            return;
        }

        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty()) {
            return;
        }

        // --- Special case: plain Apple -> Enchanted Golden Apple ---
        if (stack.isOf(Items.APPLE)) {
            transformAppleToEnchantedGoldenApple(player, stack, serverWorld);
            return;
        }

        // Only applies to weapons/tools (and anything else the player could
        // legitimately enchant). We simply let "is there a valid
        // enchantment for this item" decide - if the item is not a
        // weapon/tool/armor etc. no enchantment will match, and nothing
        // happens (ability is "wasted" rather than crashing).
        RegistryEntry<Enchantment> chosen = pickRandomValidEnchantment(serverWorld, stack, player.getRandom());
        if (chosen == null) {
            return;
        }

        int maxLevel = chosen.value().getMaxLevel();
        int level = 1 + player.getRandom().nextInt(Math.max(1, maxLevel));

        ItemEnchantmentsComponent existing =
                stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(existing);
        builder.set(chosen, level);
        stack.set(DataComponentTypes.ENCHANTMENTS, builder.build());

        spawnDivineSparkles(serverWorld, player);
    }

    /**
     * Picks a random enchantment that:
     *  - Is a "primary" (normally obtainable) enchantment for this item
     *    according to vanilla's own rules, so we never crash or produce an
     *    invalid combination.
     *  - Is not already present at its maximum level on the item.
     */
    private static RegistryEntry<Enchantment> pickRandomValidEnchantment(ServerWorld world, ItemStack stack, Random random) {
        Registry<Enchantment> enchantmentRegistry = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        ItemEnchantmentsComponent current =
                stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);

        List<RegistryEntry<Enchantment>> valid = new ArrayList<>();
        for (RegistryEntry<Enchantment> entry : enchantmentRegistry.streamEntries().toList()) {
            Enchantment enchantment = entry.value();

            // Vanilla rule: an enchantment can be applied to an item if the
            // item is in the enchantment's "primary items" item set.
            if (!isPrimaryItemFor(entry, stack)) {
                continue;
            }

            int existingLevel = current.getLevel(entry);
            if (existingLevel >= enchantment.getMaxLevel()) {
                continue; // already maxed out, pick something else
            }

            // Skip if it would conflict with an enchantment already on the item.
            if (conflictsWithExisting(entry, current, enchantmentRegistry)) {
                continue;
            }

            valid.add(entry);
        }

        if (valid.isEmpty()) {
            return null;
        }
        return valid.get(random.nextInt(valid.size()));
    }

    private static boolean isPrimaryItemFor(RegistryEntry<Enchantment> entry, ItemStack stack) {
        var primaryItems = entry.value().definition().primaryItems();
        if (primaryItems.isEmpty()) {
            // No restriction defined -> fall back to the supported-items set.
            RegistryEntryList<net.minecraft.item.Item> supported = entry.value().definition().supportedItems();
            return supported.contains(stack.getRegistryEntry());
        }
        RegistryEntryList<net.minecraft.item.Item> primary = primaryItems.get();
        return primary.contains(stack.getRegistryEntry());
    }

    private static boolean conflictsWithExisting(RegistryEntry<Enchantment> candidate,
                                                   ItemEnchantmentsComponent current,
                                                   Registry<Enchantment> registry) {
        for (RegistryEntry<Enchantment> existingEnchant : current.getEnchantments()) {
            if (existingEnchant.equals(candidate)) {
                continue;
            }
            TagKey<Enchantment> exclusiveSet = candidate.value().definition().exclusiveSet().orElse(null);
            if (exclusiveSet != null && existingEnchant.isIn(exclusiveSet)) {
                return true;
            }
            TagKey<Enchantment> otherExclusiveSet = existingEnchant.value().definition().exclusiveSet().orElse(null);
            if (otherExclusiveSet != null && candidate.isIn(otherExclusiveSet)) {
                return true;
            }
        }
        return false;
    }

    private static void transformAppleToEnchantedGoldenApple(PlayerEntity player, ItemStack apple, ServerWorld world) {
        apple.decrement(1);
        ItemStack goldenApple = new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1);
        if (!player.getInventory().insertStack(goldenApple)) {
            player.dropItem(goldenApple, false);
        }
        spawnDivineSparkles(world, player);
    }

    /** Pastel purple + white particle burst, used for both the apple
     *  transformation and successful enchant, so the ability always
     *  reads clearly as "something angelic just happened". */
    private static void spawnDivineSparkles(ServerWorld world, PlayerEntity player) {
        double x = player.getX();
        double y = player.getBodyY(0.5D);
        double z = player.getZ();

        world.spawnParticles(ParticleTypes.WITCH, x, y, z, 12, 0.4, 0.6, 0.4, 0.01);
        world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 10, 0.4, 0.6, 0.4, 0.01);
    }
}
