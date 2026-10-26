package com.chronocidegames.locatorcompass.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import com.chronocidegames.locatorcompass.LocatorTargets;
import com.chronocidegames.locatorcompass.ModComponents;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

/// Finds the nearest block matching a Locator Compass's stored item, searching the 3x3 chunks
/// centred on the holder's chunk over the full world height.
///
/// Distance is measured in 3D from the middle of the holder to the middle of the block. Results are
/// cached briefly so the search runs a few times a second rather than every frame.
public final class BlockLocator {
	/// How long a search result is reused, in game ticks.
	private static final int RESCAN_TICKS = 4;
	/// Chunks searched on each side of the holder's chunk (1 -> a 3x3 area).
	private static final int CHUNK_RADIUS = 1;
	private static final int MAX_CACHE_ENTRIES = 256;

	private record CacheKey(Item item, long holderBlockPos) {
	}

	private record CachedResult(long gameTime, @Nullable BlockPos target) {
	}

	private record Candidate(LevelChunkSection section, int minX, int minY, int minZ, double minDistanceSq) {
	}

	private static final Map<CacheKey, CachedResult> CACHE = new HashMap<>();
	private static @Nullable ClientLevel cachedLevel;

	private BlockLocator() {
	}

	public static @Nullable BlockPos find(ItemStack compass, ClientLevel level, ItemOwner owner) {
		ItemStackTemplate stored = compass.get(ModComponents.STORED_ITEM);

		if (stored == null) {
			return null;
		}

		if (level != cachedLevel) {
			CACHE.clear();
			cachedLevel = level;
		}

		Vec3 origin = searchOrigin(owner);
		long gameTime = level.getGameTime();
		CacheKey key = new CacheKey(stored.item().value(), BlockPos.containing(origin).asLong());
		CachedResult cached = CACHE.get(key);

		if (cached != null && gameTime >= cached.gameTime() && gameTime - cached.gameTime() < RESCAN_TICKS) {
			return cached.target();
		}

		if (CACHE.size() >= MAX_CACHE_ENTRIES) {
			CACHE.clear();
		}

		BlockPos target = findNearest(level, origin, LocatorTargets.resolve(stored.item()));
		CACHE.put(key, new CachedResult(gameTime, target));
		return target;
	}

	/// Middle of the holder's body for living entities (so blocks underfoot and at head height are
	/// treated evenly), or the holder's position for things like item frames.
	private static Vec3 searchOrigin(ItemOwner owner) {
		LivingEntity living = owner.asLivingEntity();
		return living != null ? living.position().add(0.0, living.getBbHeight() / 2.0, 0.0) : owner.position();
	}

	static @Nullable BlockPos findNearest(ClientLevel level, Vec3 origin, Set<Block> targets) {
		if (targets.isEmpty()) {
			return null;
		}

		Predicate<BlockState> matches = state -> targets.contains(state.getBlock());
		int centerChunkX = Mth.floor(origin.x()) >> 4;
		int centerChunkZ = Mth.floor(origin.z()) >> 4;
		List<Candidate> candidates = new ArrayList<>();

		for (int chunkX = centerChunkX - CHUNK_RADIUS; chunkX <= centerChunkX + CHUNK_RADIUS; chunkX++) {
			for (int chunkZ = centerChunkZ - CHUNK_RADIUS; chunkZ <= centerChunkZ + CHUNK_RADIUS; chunkZ++) {
				LevelChunk chunk = level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);

				if (chunk == null) {
					continue;
				}

				LevelChunkSection[] sections = chunk.getSections();

				for (int index = 0; index < sections.length; index++) {
					LevelChunkSection section = sections[index];

					// The palette check skips sections that can't contain a match without reading any blocks.
					if (section == null || section.hasOnlyAir() || !section.maybeHas(matches)) {
						continue;
					}

					int minX = chunkX << 4;
					int minY = chunk.getSectionYFromSectionIndex(index) << 4;
					int minZ = chunkZ << 4;
					candidates.add(new Candidate(section, minX, minY, minZ, distanceSqToSection(origin, minX, minY, minZ)));
				}
			}
		}

		// Search the closest sections first; stop once no remaining section could hold anything closer.
		candidates.sort(Comparator.comparingDouble(Candidate::minDistanceSq));
		double bestDistanceSq = Double.MAX_VALUE;
		BlockPos best = null;

		for (Candidate candidate : candidates) {
			if (candidate.minDistanceSq() >= bestDistanceSq) {
				break;
			}

			LevelChunkSection section = candidate.section();

			for (int y = 0; y < 16; y++) {
				double dy = candidate.minY() + y + 0.5 - origin.y();

				for (int z = 0; z < 16; z++) {
					double dz = candidate.minZ() + z + 0.5 - origin.z();

					for (int x = 0; x < 16; x++) {
						if (!matches.test(section.getBlockState(x, y, z))) {
							continue;
						}

						double dx = candidate.minX() + x + 0.5 - origin.x();
						double distanceSq = dx * dx + dy * dy + dz * dz;

						if (distanceSq < bestDistanceSq) {
							bestDistanceSq = distanceSq;
							best = new BlockPos(candidate.minX() + x, candidate.minY() + y, candidate.minZ() + z);
						}
					}
				}
			}
		}

		return best;
	}

	/// Squared distance from `origin` to the nearest block centre a 16x16x16 section could contain.
	private static double distanceSqToSection(Vec3 origin, int minX, int minY, int minZ) {
		double dx = axisGap(origin.x(), minX);
		double dy = axisGap(origin.y(), minY);
		double dz = axisGap(origin.z(), minZ);
		return dx * dx + dy * dy + dz * dz;
	}

	private static double axisGap(double coordinate, int sectionMin) {
		double low = sectionMin + 0.5;
		double high = sectionMin + 15.5;
		return coordinate < low ? low - coordinate : coordinate > high ? coordinate - high : 0.0;
	}
}
