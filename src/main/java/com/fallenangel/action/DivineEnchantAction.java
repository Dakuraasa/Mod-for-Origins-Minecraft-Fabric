package com.fallenangel.action;

import com.fallenangel.FallenAngelMod;

import io.github.apace100.apoli.action.EntityAction;
import io.github.apace100.apoli.action.context.EntityActionContext;
import io.github.apace100.apoli.registry.ApoliRegistries;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.registry.DataObjectFactory;

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

public class DivineEnchantAction extends EntityAction {

    public static final Identifier ID = Identifier.of(FallenAngelMod.MOD_ID, "divine_enchant");

    public DivineEnchantAction(SerializableData.Instance data) {
        super(data);
    }

    public static void register() {
        Registry.register(
                ApoliRegistries.ENTITY_ACTION,
                ID,
                DataObjectFactory.simple(
                        new SerializableData(),
                        DivineEnchantAction::new,
                        (action, data) -> data.new Instance()
                )
        );
    }

    @Override
    public void accept(EntityActionContext context) {
        Entity entity = context.entity();
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

    private static RegistryEntry<Enchantment> pickRandomValidEnchantment(ServerWorld world, ItemStack stack, Random random) {
        Registry<Enchantment> enchantmentRegistry = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        ItemEnchantmentsComponent current =
                stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);

        List<RegistryEntry<Enchantment>> valid = new ArrayList<>();
        for (RegistryEntry<Enchantment> entry : enchantmentRegistry.streamEntries().toList()) {
            Enchantment enchantment = entry.value();

            if (!isPrimaryItemFor(entry, stack)) {
                continue;
            }

            int existingLevel = current.getLevel(entry);
            if (existingLevel >= enchantment.getMaxLevel()) {
                continue;
            }

            if (conflictsWithExisting(entry, current)) {
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
            RegistryEntryList<net.minecraft.item.Item> supported = entry.value().definition().supportedItems();
            return supported.contains(stack.getRegistryEntry());
        }
        RegistryEntryList<net.minecraft.item.Item> primary = primaryItems.get();
        return primary.contains(stack.getRegistryEntry());
    }

    private static boolean conflictsWithExisting(RegistryEntry<Enchantment> candidate,
                                                   ItemEnchantmentsComponent current) {
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

    private static void spawnDivineSparkles(ServerWorld world, PlayerEntity player) {
        double x = player.getX();
        double y = player.getBodyY(0.5D);
        double z = player.getZ();

        world.spawnParticles(ParticleTypes.WITCH, x, y, z, 12, 0.4, 0.6, 0.4, 0.01);
        world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 10, 0.4, 0.6, 0.4, 0.01);
    }
}
