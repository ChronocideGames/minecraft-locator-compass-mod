package com.chronocidegames.locatorcompass.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.NeedleDirectionHelper;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/// Needle angle for the Locator Compass, as a fraction of a full turn (0..1, 0 = straight ahead).
///
/// Mirrors the vanilla compass: with a target it points at it horizontally (with the same swing
/// as the player turns); without one it spins randomly, exactly like a compass in the Nether or End.
public final class LocatorNeedle extends NeedleDirectionHelper implements RangeSelectItemModelProperty {
	public static final MapCodec<LocatorNeedle> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.BOOL.optionalFieldOf("wobble", true).forGetter(LocatorNeedle::wobble)
	).apply(instance, LocatorNeedle::new));

	private final Wobbler wobbler;
	private final Wobbler noTargetWobbler;
	private final RandomSource random = RandomSource.create();

	public LocatorNeedle(boolean wobble) {
		super(wobble);
		this.wobbler = this.newWobbler(0.8F);
		this.noTargetWobbler = this.newWobbler(0.8F);
	}

	@Override
	protected float calculate(ItemStack stack, ClientLevel level, int seed, ItemOwner owner) {
		long gameTime = level.getGameTime();
		BlockPos target = BlockLocator.find(stack, level, owner);
		return target == null ? this.randomlySpinning(seed, gameTime) : this.towards(owner, gameTime, target);
	}

	@Override
	public MapCodec<LocatorNeedle> type() {
		return MAP_CODEC;
	}

	private float randomlySpinning(int seed, long gameTime) {
		if (this.noTargetWobbler.shouldUpdate(gameTime)) {
			this.noTargetWobbler.update(gameTime, this.random.nextFloat());
		}

		float rotation = this.noTargetWobbler.rotation() + (float) hash(seed) / (float) Integer.MAX_VALUE;
		return Mth.positiveModulo(rotation, 1.0F);
	}

	private float towards(ItemOwner owner, long gameTime, BlockPos target) {
		Vec3 from = owner.position();
		Vec3 to = Vec3.atCenterOf(target);
		float angleToTarget = (float) (Mth.atan2(to.z() - from.z(), to.x() - from.x()) / (Math.PI * 2));
		float facing = Mth.positiveModulo(owner.getVisualRotationYInDegrees() / 360.0F, 1.0F);
		float rotation;

		if (owner.asLivingEntity() instanceof Player player && player.isLocalPlayer() && player.level().tickRateManager().runsNormally()) {
			if (this.wobbler.shouldUpdate(gameTime)) {
				this.wobbler.update(gameTime, 0.5F - (facing - 0.25F));
			}

			rotation = angleToTarget + this.wobbler.rotation();
		} else {
			rotation = 0.5F - (facing - 0.25F - angleToTarget);
		}

		return Mth.positiveModulo(rotation, 1.0F);
	}

	private static int hash(int seed) {
		return seed * 1327217883;
	}
}
