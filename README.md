# Locator Compass

A Fabric mod for Minecraft Java **26.3** that adds the **Locator Compass**: a compass with a one-slot bag.
Put an item in it and the needle points to the nearest matching block.

## How it works

- **Empty:** the needle spins randomly, like a normal compass in the Nether or the End.
- **Holding an item:** the needle points toward the nearest matching block in the 3×3 chunks centred on you,
  over the full height of the world. It only points horizontally, but "nearest" is measured in 3D, so an ore
  3 blocks ahead on your level beats one 1 block over and 20 blocks down.
- **Nothing in range:** the needle spins, as if empty.

It looks like a normal compass with a copper tint, in your hand, in item frames and in the inventory.

### Putting items in and taking them out

The slot holds one stack of one item type, and works like a bundle: use **right-click** in any inventory.

| You do this | Result |
| --- | --- |
| Right-click an item onto the compass | The item goes in (up to one stack). A different item swaps with what's inside. |
| Right-click the compass with an empty cursor | The contents come out onto your cursor. |
| Pick up the compass, right-click it onto an item | That item goes in (or swaps with what's inside). |
| Pick up the compass, right-click it onto an empty slot | The contents are put into that slot. |

Left-click moves the compass around as normal. If a compass burns or despawns, its contents drop.
The tooltip shows what it's finding, or says when the item has no block to find.

### Crafting

Shapeless: **Compass + Leather + String**. It's in the Tools & Utilities creative tab, after the recovery compass.

## What it looks for

1. **Crafting materials find their sources** (see the table below).
2. **Placeable items find the same block.** A stone block finds stone, wheat seeds find wheat crops, a torch
   finds torches and wall torches. An iron ore block finds iron ore only, not deepslate iron ore.
3. **Anything else** (tools, armour, mob drops, ...) finds nothing, and the needle spins.

| Item(s) | Points to |
| --- | --- |
| Raw iron, iron ingot, iron nugget | Iron ore, deepslate iron ore |
| Raw gold, gold ingot, gold nugget | Gold ores (incl. nether gold ore) |
| Raw copper, copper ingot, copper nugget | Copper ores |
| Coal / Diamond / Emerald / Lapis lazuli / Redstone | Their ores (both stone and deepslate) |
| Nether quartz | Nether quartz ore |
| Netherite scrap, netherite ingot | Ancient debris |
| Amethyst shard | Amethyst buds, clusters, budding amethyst |
| Glowstone dust | Glowstone |
| Prismarine shard / crystals | Prismarine blocks, sea lanterns |
| Stick | Leaves (any) |
| Charcoal | Logs (any) |
| String | Cobwebs |
| Clay ball, brick | Clay |
| Flint | Gravel |
| Snowball | Snow, snow blocks, powder snow |
| Nether brick | Netherrack |
| Bone, bone meal | Bone blocks (fossils) |
| Honeycomb, honey bottle | Bee nests, beehives |
| Resin brick | Resin clumps |
| Chorus fruit, popped chorus fruit | Chorus plants and flowers |
| Water bucket / Lava bucket | Water / Lava source blocks (not flowing) |
| Wheat, bread | Wheat crops |
| Sugar, paper | Sugar cane |
| Melon slice | Melons |
| Beetroot | Beetroot crops |
| Baked / poisonous potato | Potato crops |
| Apple | Oak and dark oak leaves |
| Dried kelp | Kelp |
| Dyes | The flowers/plants they come from (e.g. red dye → poppy, red tulip, rose bush, beetroots; green dye → cactus; brown dye → cocoa) |

### Changing the list

The list is data-driven with tags, so a data pack can add to or change it. For a group called `<name>`:

- `data/locator_compass/tags/item/sources/<name>.json` — items that, when stored, look for this group
- `data/locator_compass/tags/block/sources/<name>.json` — blocks (or block tags) they point to

An item in any `sources/` item tag uses those blocks *instead of* the block it places. For example, the mod
treats **Iron Bars** (a placeable block) as finding iron bars; to make them point at iron ore instead, add
`minecraft:iron_bars` to `data/locator_compass/tags/item/sources/iron.json`.

## Installing (Prism Launcher)

1. In your 26.3 instance, open **Edit → Version**, choose **Install Loader → Fabric**, and pick Fabric Loader
   **0.20.1** or newer.
2. Open **Mods → Download mods**, search **Fabric API**, and install the 26.3 version (0.165.0+26.3 or newer).
3. Download `locator-compass-<version>.jar` from this repository's
   [Releases](https://github.com/ChronocideGames/minecraft-locator-compass-mod/releases) page and drop it into
   the instance's **Mods** folder (**Mods → Add file**).

The mod is needed on both client and server in multiplayer.

## Building

Requires Java 25.

```sh
./gradlew build              # jar ends up in build/libs/
./gradlew runClient          # dev client
./gradlew runClientGameTest  # automated end-to-end client tests (src/gametest)
```
