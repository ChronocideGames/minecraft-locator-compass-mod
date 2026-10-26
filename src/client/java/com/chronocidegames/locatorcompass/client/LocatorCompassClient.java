package com.chronocidegames.locatorcompass.client;

import com.chronocidegames.locatorcompass.LocatorCompass;

import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;

import net.fabricmc.api.ClientModInitializer;

public class LocatorCompassClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Used by assets/locator_compass/items/locator_compass.json as "property": "locator_compass:locator".
		RangeSelectItemModelProperties.ID_MAPPER.put(LocatorCompass.id("locator"), LocatorNeedle.MAP_CODEC);
	}
}
