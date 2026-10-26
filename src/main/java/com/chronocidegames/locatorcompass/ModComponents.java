package com.chronocidegames.locatorcompass;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStackTemplate;

public final class ModComponents {
	/// The single stack held in a Locator Compass's slot. Absent when the compass is empty.
	public static final DataComponentType<ItemStackTemplate> STORED_ITEM = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			LocatorCompass.id("stored_item"),
			DataComponentType.<ItemStackTemplate>builder()
					.persistent(ItemStackTemplate.CODEC)
					.networkSynchronized(ItemStackTemplate.STREAM_CODEC)
					.cacheEncoding()
					.build()
	);

	private ModComponents() {
	}

	static void init() {
		// Loads the class, which registers the component types above.
	}
}
