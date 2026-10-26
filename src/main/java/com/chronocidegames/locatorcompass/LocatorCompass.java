package com.chronocidegames.locatorcompass;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

public class LocatorCompass implements ModInitializer {
	public static final String MOD_ID = "locator_compass";

	@Override
	public void onInitialize() {
		ModComponents.init();
		ModItems.init();

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
				.register(output -> output.insertAfter(Items.RECOVERY_COMPASS, ModItems.LOCATOR_COMPASS));
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
