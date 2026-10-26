package com.chronocidegames.locatorcompass;

import java.util.function.Consumer;
import java.util.stream.Stream;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/// A compass with a single bag slot. Right-click an item onto it (or it onto an item) in an inventory
/// to put that item in; right-click it onto an empty slot, or with an empty cursor, to take the item out.
/// The needle (see the client-side `LocatorNeedle`) points at the nearest matching block.
public class LocatorCompassItem extends Item {
	public LocatorCompassItem(Properties properties) {
		super(properties);
	}

	public static ItemStack getStored(ItemStack compass) {
		ItemStackTemplate stored = compass.get(ModComponents.STORED_ITEM);
		return stored == null ? ItemStack.EMPTY : stored.create();
	}

	public static void setStored(ItemStack compass, ItemStack stored) {
		if (stored.isEmpty()) {
			compass.remove(ModComponents.STORED_ITEM);
		} else {
			compass.set(ModComponents.STORED_ITEM, ItemStackTemplate.fromNonEmptyStack(stored));
		}
	}

	public static boolean canStore(ItemStack stack) {
		return !stack.isEmpty()
				&& !(stack.getItem() instanceof LocatorCompassItem)
				&& stack.getItem().canFitInsideContainerItems();
	}

	/// Space left in the compass's slot for `incoming`, or 0 if it holds a different item.
	private static int spaceFor(ItemStack stored, ItemStack incoming) {
		if (stored.isEmpty()) {
			return incoming.getMaxStackSize();
		}

		return ItemStack.isSameItemSameComponents(stored, incoming) ? stored.getMaxStackSize() - stored.getCount() : 0;
	}

	/// The compass is on the cursor and was clicked onto `slot`.
	@Override
	public boolean overrideStackedOnOther(ItemStack compass, Slot slot, ClickAction action, Player player) {
		if (action != ClickAction.SECONDARY) {
			return false;
		}

		ItemStack inSlot = slot.getItem();
		ItemStack stored = getStored(compass);

		if (inSlot.isEmpty()) {
			if (stored.isEmpty()) {
				return false; // Nothing to unload: let vanilla place the compass in the slot.
			}

			// safeInsert splits from and returns the same stack, so compare against the count from before.
			int before = stored.getCount();
			ItemStack rest = slot.safeInsert(stored);

			if (rest.getCount() == before) {
				playSound(player, SoundEvents.BUNDLE_INSERT_FAIL);
			} else {
				setStored(compass, rest);
				playSound(player, SoundEvents.BUNDLE_REMOVE_ONE);
			}

			return true;
		}

		if (!canStore(inSlot)) {
			playSound(player, SoundEvents.BUNDLE_INSERT_FAIL);
			return true;
		}

		if (stored.isEmpty() || ItemStack.isSameItemSameComponents(stored, inSlot)) {
			int space = spaceFor(stored, inSlot);
			ItemStack taken = space > 0 ? slot.safeTake(inSlot.getCount(), space, player) : ItemStack.EMPTY;

			if (taken.isEmpty()) {
				playSound(player, SoundEvents.BUNDLE_INSERT_FAIL);
				return true;
			}

			if (stored.isEmpty()) {
				stored = taken;
			} else {
				stored.grow(taken.getCount());
			}

			setStored(compass, stored);
			playSound(player, SoundEvents.BUNDLE_INSERT);
			return true;
		}

		// A different item: swap it with the one in the compass, if the slot can take the old one.
		int count = inSlot.getCount();

		if (count > inSlot.getMaxStackSize() || !slot.mayPlace(stored) || stored.getCount() > slot.getMaxStackSize(stored)) {
			playSound(player, SoundEvents.BUNDLE_INSERT_FAIL);
			return true;
		}

		ItemStack taken = slot.safeTake(count, count, player);

		if (taken.isEmpty()) {
			playSound(player, SoundEvents.BUNDLE_INSERT_FAIL);
			return true;
		}

		if (!slot.getItem().isEmpty()) {
			// Only part of the stack came out; put it back and give up rather than mix items.
			ItemStack rest = slot.safeInsert(taken);
			returnToPlayer(player, rest);
			playSound(player, SoundEvents.BUNDLE_INSERT_FAIL);
			return true;
		}

		returnToPlayer(player, slot.safeInsert(stored));
		setStored(compass, taken);
		playSound(player, SoundEvents.BUNDLE_INSERT);
		return true;
	}

	/// The compass is in `slot` and `carried` (possibly empty) was clicked onto it.
	@Override
	public boolean overrideOtherStackedOnMe(ItemStack compass, ItemStack carried, Slot slot, ClickAction action, Player player, SlotAccess carriedAccess) {
		if (action != ClickAction.SECONDARY || !slot.allowModification(player)) {
			return false;
		}

		ItemStack stored = getStored(compass);

		if (carried.isEmpty()) {
			if (stored.isEmpty()) {
				return false; // Nothing inside: let vanilla pick the compass up.
			}

			if (carriedAccess.set(stored)) {
				setStored(compass, ItemStack.EMPTY);
				slot.setChanged();
				playSound(player, SoundEvents.BUNDLE_REMOVE_ONE);
			}

			return true;
		}

		if (!canStore(carried)) {
			playSound(player, SoundEvents.BUNDLE_INSERT_FAIL);
			return true;
		}

		if (stored.isEmpty() || ItemStack.isSameItemSameComponents(stored, carried)) {
			int moved = Math.min(spaceFor(stored, carried), carried.getCount());

			if (moved <= 0) {
				playSound(player, SoundEvents.BUNDLE_INSERT_FAIL);
				return true;
			}

			ItemStack movedStack = carried.split(moved);

			if (stored.isEmpty()) {
				stored = movedStack;
			} else {
				stored.grow(movedStack.getCount());
			}

			setStored(compass, stored);
			slot.setChanged();
			playSound(player, SoundEvents.BUNDLE_INSERT);
			return true;
		}

		// A different item: swap the cursor stack with the one in the compass.
		ItemStack incoming = carried.copy();

		if (carriedAccess.set(stored)) {
			setStored(compass, incoming);
			slot.setChanged();
			playSound(player, SoundEvents.BUNDLE_INSERT);
		}

		return true;
	}

	@Override
	public void onDestroyed(ItemEntity entity) {
		ItemStackTemplate stored = entity.getItem().get(ModComponents.STORED_ITEM);

		if (stored != null) {
			entity.getItem().remove(ModComponents.STORED_ITEM);
			ItemUtils.onContainerDestroyed(entity, Stream.of(stored.create()));
		}
	}

	@Override
	public void appendHoverText(ItemStack compass, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		ItemStackTemplate stored = compass.get(ModComponents.STORED_ITEM);

		if (stored == null) {
			tooltip.accept(Component.translatable("tooltip.locator_compass.empty").withStyle(ChatFormatting.GRAY));
			tooltip.accept(Component.translatable("tooltip.locator_compass.how_to").withStyle(ChatFormatting.DARK_GRAY));
			return;
		}

		ItemStack stack = stored.create();
		Component contents = stack.getCount() > 1
				? Component.translatable("tooltip.locator_compass.contents_count", stack.getHoverName(), stack.getCount())
				: Component.translatable("tooltip.locator_compass.contents", stack.getHoverName());
		tooltip.accept(contents.copy().withStyle(ChatFormatting.GRAY));

		if (LocatorTargets.resolve(stored.item()).isEmpty()) {
			tooltip.accept(Component.translatable("tooltip.locator_compass.nothing_to_find").withStyle(ChatFormatting.DARK_GRAY));
		}
	}

	private static void returnToPlayer(Player player, ItemStack stack) {
		if (!stack.isEmpty()) {
			player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
		}
	}

	private static void playSound(Player player, SoundEvent sound) {
		player.playSound(sound, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
	}
}
