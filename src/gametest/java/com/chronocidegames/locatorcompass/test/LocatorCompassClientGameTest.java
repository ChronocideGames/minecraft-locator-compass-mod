package com.chronocidegames.locatorcompass.test;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.chronocidegames.locatorcompass.LocatorCompassItem;
import com.chronocidegames.locatorcompass.ModItems;
import com.chronocidegames.locatorcompass.client.BlockLocator;
import com.chronocidegames.locatorcompass.client.LocatorNeedle;

import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/// End-to-end checks run in a real client: what the compass finds, where the needle points,
/// and how the one-slot bag behaves in the inventory menu.
@SuppressWarnings("UnstableApiUsage")
public class LocatorCompassClientGameTest implements FabricClientGameTest {
	private static final int LEFT = 0;
	private static final int RIGHT = 1;
	// Inventory menu slot ids: 9-35 main inventory, 36-44 hotbar.
	private static final int HOTBAR_0 = 36;

	private ClientGameTestContext context;
	private TestSingleplayerContext singleplayer;
	private BlockPos feet;

	@Override
	public void runTest(ClientGameTestContext context) {
		this.context = context;

		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
				.create()) {
			this.singleplayer = singleplayer;

			// Stand in the middle of chunk (0, 0), facing east, on the flat test world.
			singleplayer.getServer().runCommand("tp @a 8.5 -60 8.5 -90 0");
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();
			this.feet = context.computeOnClient(client -> client.player.blockPosition());
			check(feet.getX() == 8 && feet.getZ() == 8, "player should be at x=8 z=8, was " + feet);

			testFinding();
			testRange();
			testSourceMappings();
			testBagClicks();
			takeScreenshots();
		}
	}

	private void testFinding() {
		checkFinds(null, null, "an empty compass finds nothing");
		checkFinds(Items.RAW_IRON, null, "raw iron with no ore around finds nothing");

		BlockPos ahead = feet.offset(3, 0, 0);       // 3 blocks east, same level
		BlockPos highUp = feet.offset(0, 20, 1);     // 1 block south (closer horizontally), 20 up
		setBlock(ahead, "minecraft:iron_ore", Blocks.IRON_ORE);
		setBlock(highUp, "minecraft:deepslate_iron_ore", Blocks.DEEPSLATE_IRON_ORE);

		checkFinds(Items.RAW_IRON, ahead, "raw iron prefers the 3D-closer ore over the horizontally-closer one");
		checkFinds(Items.IRON_INGOT, ahead, "iron ingots find iron ore");
		checkAngle(Items.RAW_IRON, 0.0F, "needle points straight ahead at an ore directly east while facing east");

		setBlock(ahead, "minecraft:air", Blocks.AIR);
		checkFinds(Items.RAW_IRON, highUp, "with the near ore gone, the deepslate ore 20 blocks up is found");
		checkAngle(Items.RAW_IRON, 0.25F, "needle points right at an ore to the south while facing east");
		checkFinds(Items.IRON_ORE, null, "an iron ore block finds only iron ore, not deepslate iron ore");
		checkFinds(Items.DEEPSLATE_IRON_ORE, highUp, "a deepslate iron ore block finds itself");
		setBlock(highUp, "minecraft:air", Blocks.AIR);
	}

	private void testRange() {
		// Chunk (0,0) covers x 0..15; the 3x3 area covers x -16..31.
		BlockPos eastInside = new BlockPos(31, feet.getY(), 8);
		BlockPos eastOutside = new BlockPos(32, feet.getY(), 8);
		BlockPos westInside = new BlockPos(-16, feet.getY(), 8);
		BlockPos westOutside = new BlockPos(-17, feet.getY(), 8);

		setBlock(eastOutside, "minecraft:iron_ore", Blocks.IRON_ORE);
		setBlock(westOutside, "minecraft:iron_ore", Blocks.IRON_ORE);
		checkFinds(Items.RAW_IRON, null, "ores just outside the 3x3 chunks are ignored");

		setBlock(eastInside, "minecraft:iron_ore", Blocks.IRON_ORE);
		checkFinds(Items.RAW_IRON, eastInside, "ore at the far east edge of the 3x3 chunks is found");
		setBlock(eastInside, "minecraft:air", Blocks.AIR);

		setBlock(westInside, "minecraft:iron_ore", Blocks.IRON_ORE);
		checkFinds(Items.RAW_IRON, westInside, "ore at the far west edge of the 3x3 chunks is found");

		for (BlockPos pos : new BlockPos[] {westInside, eastOutside, westOutside}) {
			setBlock(pos, "minecraft:air", Blocks.AIR);
		}
	}

	private void testSourceMappings() {
		BlockPos leaves = feet.offset(2, 0, 4);
		BlockPos cobweb = feet.offset(-2, 0, -3);
		BlockPos wallTorch = feet.offset(0, 1, -4);
		BlockPos water = feet.offset(-4, 0, 2);
		setBlock(leaves, "minecraft:oak_leaves[persistent=true]", Blocks.OAK_LEAVES);
		setBlock(cobweb, "minecraft:cobweb", Blocks.COBWEB);
		setBlock(wallTorch, "minecraft:wall_torch[facing=north]", Blocks.WALL_TORCH);
		setBlock(water, "minecraft:water", Blocks.WATER);

		checkFinds(Items.STICK, leaves, "sticks find leaves");
		checkFinds(Items.APPLE, leaves, "apples find oak leaves");
		checkFinds(Items.STRING, cobweb, "string finds cobwebs");
		checkFinds(Items.TORCH, wallTorch, "a torch finds wall torches too");
		checkFinds(Items.WATER_BUCKET, water, "a water bucket finds water");
		checkFinds(Items.DIRT, feet.offset(0, -2, 0), "a dirt block finds the dirt under the grass");
		checkFinds(Items.DIAMOND_PICKAXE, null, "a tool finds nothing");

		for (BlockPos pos : new BlockPos[] {leaves, cobweb, wallTorch, water}) {
			setBlock(pos, "minecraft:air", Blocks.AIR);
		}
	}

	private void testBagClicks() {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = singleplayer.getConnection().getServerPlayer();
			AbstractContainerMenu menu = player.inventoryMenu;
			player.getInventory().clearContent();
			menu.setCarried(ItemStack.EMPTY);

			ItemStack compass = new ItemStack(ModItems.LOCATOR_COMPASS);
			menu.getSlot(HOTBAR_0).set(compass);
			menu.getSlot(9).set(new ItemStack(Items.RAW_IRON, 16));

			// Cursor item right-clicked onto the compass goes in.
			menu.clicked(9, LEFT, ContainerInput.PICKUP, player);
			menu.clicked(HOTBAR_0, RIGHT, ContainerInput.PICKUP, player);
			checkStack(stored(menu.getSlot(HOTBAR_0).getItem()), Items.RAW_IRON, 16, "raw iron put into compass");
			check(menu.getCarried().isEmpty(), "cursor empty after inserting, has " + menu.getCarried());

			// Empty cursor right-clicked onto the compass takes it out.
			menu.clicked(HOTBAR_0, RIGHT, ContainerInput.PICKUP, player);
			checkStack(menu.getCarried(), Items.RAW_IRON, 16, "raw iron taken out onto cursor");
			check(stored(menu.getSlot(HOTBAR_0).getItem()).isEmpty(), "compass empty after taking out");

			// A different item on the cursor swaps with the contents.
			menu.clicked(HOTBAR_0, RIGHT, ContainerInput.PICKUP, player);
			menu.setCarried(new ItemStack(Items.STICK, 3));
			menu.clicked(HOTBAR_0, RIGHT, ContainerInput.PICKUP, player);
			checkStack(stored(menu.getSlot(HOTBAR_0).getItem()), Items.STICK, 3, "sticks swapped into compass");
			checkStack(menu.getCarried(), Items.RAW_IRON, 16, "raw iron swapped onto cursor");
			menu.setCarried(ItemStack.EMPTY);

			// Only one stack fits: 60 + 16 sticks leaves 12 on the cursor.
			LocatorCompassItem.setStored(menu.getSlot(HOTBAR_0).getItem(), new ItemStack(Items.STICK, 60));
			menu.setCarried(new ItemStack(Items.STICK, 16));
			menu.clicked(HOTBAR_0, RIGHT, ContainerInput.PICKUP, player);
			checkStack(stored(menu.getSlot(HOTBAR_0).getItem()), Items.STICK, 64, "compass filled to one stack");
			checkStack(menu.getCarried(), Items.STICK, 12, "leftover sticks stay on cursor");
			menu.setCarried(ItemStack.EMPTY);

			// Another Locator Compass can't go inside.
			menu.setCarried(new ItemStack(ModItems.LOCATOR_COMPASS));
			menu.clicked(HOTBAR_0, RIGHT, ContainerInput.PICKUP, player);
			check(menu.getCarried().is(ModItems.LOCATOR_COMPASS), "a locator compass can't be stored in another");
			checkStack(stored(menu.getSlot(HOTBAR_0).getItem()), Items.STICK, 64, "contents unchanged after refused insert");
			menu.setCarried(ItemStack.EMPTY);

			// Now carry the compass and right-click it onto slots.
			menu.clicked(HOTBAR_0, LEFT, ContainerInput.PICKUP, player);
			check(menu.getCarried().is(ModItems.LOCATOR_COMPASS), "picked up the compass");
			menu.clicked(10, RIGHT, ContainerInput.PICKUP, player);
			checkStack(menu.getSlot(10).getItem(), Items.STICK, 64, "contents unloaded into empty slot");
			check(stored(menu.getCarried()).isEmpty(), "carried compass empty after unloading");

			menu.getSlot(11).set(new ItemStack(Items.STRING, 5));
			menu.clicked(11, RIGHT, ContainerInput.PICKUP, player);
			checkStack(stored(menu.getCarried()), Items.STRING, 5, "carried compass scooped up string");
			check(menu.getSlot(11).getItem().isEmpty(), "string slot emptied");

			menu.clicked(10, RIGHT, ContainerInput.PICKUP, player);
			checkStack(stored(menu.getCarried()), Items.STICK, 64, "carried compass swapped string for sticks");
			checkStack(menu.getSlot(10).getItem(), Items.STRING, 5, "string left in the slot");

			player.getInventory().clearContent();
			menu.setCarried(ItemStack.EMPTY);
		});
	}

	private void takeScreenshots() {
		BlockPos ore = feet.offset(5, 0, -5); // ahead-left while facing east
		setBlock(ore, "minecraft:iron_ore", Blocks.IRON_ORE);

		giveHeld(Items.RAW_IRON);
		context.waitTicks(60);
		context.takeScreenshot("locator_compass_pointing_at_ore");

		giveHeld(null);
		context.waitTicks(20);
		context.takeScreenshot("locator_compass_empty_spinning");

		context.getInput().pressKey(options -> options.keyInventory);
		context.waitTicks(5);
		context.takeScreenshot("inventory");
		context.setScreen(() -> null);
	}

	/// Locator compass (holding `stored`, if any) in the main hand, vanilla compass in the off hand.
	private void giveHeld(@Nullable Item stored) {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = singleplayer.getConnection().getServerPlayer();
			ItemStack compass = new ItemStack(ModItems.LOCATOR_COMPASS);

			if (stored != null) {
				LocatorCompassItem.setStored(compass, new ItemStack(stored));
			}

			player.getInventory().setItem(0, compass);
			player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.COMPASS));
		});
		singleplayer.getConnection().waitForClientboundPackets();
	}

	private void setBlock(BlockPos pos, String state, Block expected) {
		singleplayer.getServer().runCommand("setblock %d %d %d %s".formatted(pos.getX(), pos.getY(), pos.getZ(), state));
		context.waitFor(client -> client.level.getBlockState(pos).is(expected));
	}

	private ItemStack compassHolding(@Nullable Item stored) {
		ItemStack compass = new ItemStack(ModItems.LOCATOR_COMPASS);

		if (stored != null) {
			LocatorCompassItem.setStored(compass, new ItemStack(stored));
		}

		return compass;
	}

	private void checkFinds(@Nullable Item stored, @Nullable BlockPos expected, String what) {
		context.waitTicks(6); // let any cached search expire
		BlockPos found = context.computeOnClient(client -> BlockLocator.find(compassHolding(stored), client.level, client.player));
		check(Objects.equals(found, expected), what + ": expected " + expected + ", found " + found);
	}

	private void checkAngle(Item stored, float expected, String what) {
		float angle = context.computeOnClient(client -> new LocatorNeedle(false).get(compassHolding(stored), client.level, client.player, 0));
		float error = Math.abs(((angle - expected) % 1.0F + 1.5F) % 1.0F - 0.5F);
		check(error < 0.03F, what + ": expected " + expected + ", got " + angle);
	}

	private static ItemStack stored(ItemStack compass) {
		return LocatorCompassItem.getStored(compass);
	}

	private static void checkStack(ItemStack stack, Item item, int count, String what) {
		check(stack.is(item) && stack.getCount() == count, what + ": expected " + count + "x " + item + ", got " + stack);
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
