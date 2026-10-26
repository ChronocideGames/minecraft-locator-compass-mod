package com.chronocidegames.locatorcompass;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/// Decides which blocks a Locator Compass looks for, given the item stored in it.
///
/// 1. **Source tags.** If the item is in any item tag named `locator_compass:sources/<name>`,
///    the compass looks for every block in the block tag with the same name. This is how crafting
///    materials map to where they come from (raw iron -> iron ores, sticks -> leaves, ...). Data packs
///    can add or override these tags.
/// 2. **Same block.** Otherwise, if the item places a block, the compass looks for that block
///    (including variants placed by the same item, such as wall torches for torches).
/// 3. Otherwise there is nothing to look for, and the needle spins.
public final class LocatorTargets {
	public static final String SOURCES_PREFIX = "sources/";

	private static Map<Item, Set<Block>> placedBlocks;

	private LocatorTargets() {
	}

	public static Set<Block> resolve(Holder<Item> item) {
		Set<Block> result = new ReferenceOpenHashSet<>();
		boolean hasSourceTag = false;

		for (TagKey<Item> itemTag : (Iterable<TagKey<Item>>) item.tags()::iterator) {
			Identifier id = itemTag.location();

			if (!id.getNamespace().equals(LocatorCompass.MOD_ID) || !id.getPath().startsWith(SOURCES_PREFIX)) {
				continue;
			}

			hasSourceTag = true;

			for (Holder<Block> block : BuiltInRegistries.BLOCK.getTagOrEmpty(TagKey.create(Registries.BLOCK, id))) {
				result.add(block.value());
			}
		}

		if (!hasSourceTag) {
			result.addAll(placedBlocks().getOrDefault(item.value(), Set.of()));
		}

		return result;
	}

	/// Every item mapped to the blocks it places. Built once: the block registry doesn't change after startup.
	private static synchronized Map<Item, Set<Block>> placedBlocks() {
		if (placedBlocks == null) {
			Map<Item, Set<Block>> map = new HashMap<>();

			for (Block block : BuiltInRegistries.BLOCK) {
				Item item = block.asItem();

				if (item != Items.AIR) {
					map.computeIfAbsent(item, i -> new ReferenceOpenHashSet<>()).add(block);
				}
			}

			placedBlocks = map;
		}

		return placedBlocks;
	}
}
