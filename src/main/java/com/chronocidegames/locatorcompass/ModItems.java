package com.chronocidegames.locatorcompass;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class ModItems {
	public static final Item LOCATOR_COMPASS = register("locator_compass", LocatorCompassItem::new, new Item.Properties().stacksTo(1));

	private ModItems() {
	}

	static void init() {
		// Loads the class, which registers the items above.
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, LocatorCompass.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}
}
