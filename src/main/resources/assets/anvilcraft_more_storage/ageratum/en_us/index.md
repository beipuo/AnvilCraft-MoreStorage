---
navigation:
  title: "§6More Storage"
  icon: "anvilcraft_more_storage:netherite_crate"
items:
  - anvilcraft_more_storage:copper_crate
  - anvilcraft_more_storage:iron_crate
  - anvilcraft_more_storage:zinc_crate
  - anvilcraft_more_storage:lead_crate
  - anvilcraft_more_storage:silver_crate
  - anvilcraft_more_storage:tin_crate
  - anvilcraft_more_storage:gold_crate
  - anvilcraft_more_storage:cursed_gold_crate
  - anvilcraft_more_storage:enchanted_gold_crate
  - anvilcraft_more_storage:diamond_crate
  - anvilcraft_more_storage:emerald_crate
  - anvilcraft_more_storage:obsidian_crate
  - anvilcraft_more_storage:tungsten_crate
  - anvilcraft_more_storage:titanium_crate
  - anvilcraft_more_storage:netherite_crate
  - anvilcraft_more_storage:ember_metal_crate
  - anvilcraft_more_storage:royal_steel_crate
  - anvilcraft_more_storage:frost_metal_crate
  - anvilcraft_more_storage:transcendium_crate
  - anvilcraft_more_storage:copper_large_crate
  - anvilcraft_more_storage:iron_large_crate
  - anvilcraft_more_storage:zinc_large_crate
  - anvilcraft_more_storage:lead_large_crate
  - anvilcraft_more_storage:silver_large_crate
  - anvilcraft_more_storage:tin_large_crate
  - anvilcraft_more_storage:gold_large_crate
  - anvilcraft_more_storage:cursed_gold_large_crate
  - anvilcraft_more_storage:enchanted_gold_large_crate
  - anvilcraft_more_storage:diamond_large_crate
  - anvilcraft_more_storage:emerald_large_crate
  - anvilcraft_more_storage:obsidian_large_crate
  - anvilcraft_more_storage:tungsten_large_crate
  - anvilcraft_more_storage:titanium_large_crate
  - anvilcraft_more_storage:netherite_large_crate
  - anvilcraft_more_storage:ember_metal_large_crate
  - anvilcraft_more_storage:royal_steel_large_crate
  - anvilcraft_more_storage:frost_metal_large_crate
  - anvilcraft_more_storage:transcendium_large_crate
---

# More Storage

> Higher tiers of the <ref item="anvilcraft:crate"/> and the <ref item="anvilcraft:large_crate"/>, a few of which also carry their material's own properties.

<info>
Tiered crates are axe-mineable just like the original, and a large crate item stacks to 16.
</info>

# Capacity

The original crate holds 2048 items and the large crate 65536. A tier's capacity is that number times its multiplier.

| Tier | Multiplier | Crate | Large Crate |
|:---|:---:|---:|---:|
| Copper | 1.5 | 3072 | 98304 |
| Iron | 2 | 4096 | 131072 |
| Zinc | 3 | 6144 | 196608 |
| Lead | 3 | 6144 | 196608 |
| Silver | 3 | 6144 | 196608 |
| Tin | 3 | 6144 | 196608 |
| Gold | 4 | 8192 | 262144 |
| Cursed Gold | 4 | 8192 | 262144 |
| Enchanted Gold | 4.5 | 9216 | 294912 |
| Diamond | 6 | 12288 | 393216 |
| Emerald | 8 | 16384 | 524288 |
| Obsidian | 10 | 20480 | 655360 |
| Tungsten | 11 | 22528 | 720896 |
| Titanium | 11 | 22528 | 720896 |
| Netherite | 12 | 24576 | 786432 |
| Ember Metal | 12 | 24576 | 786432 |
| Royal Steel | 14 | 28672 | 917504 |
| Frost Metal | 16 | 32768 | 1048576 |
| Transcendium | 18 | 36864 | 1179648 |

<info>
Every multiplier can be set on its own in the mod config, anywhere from 1.0 to 1024.0.
</info>

<warning>
A tiered large crate cannot be upgraded into a <ref item="anvilcraft:shulker_container"/> with an anvil.
</warning>

# Material properties

Zinc, tin, silver and titanium are AnvilCraft's "common metals" (see <ref item="anvilcraft:zinc_ingot"/>) and have no properties of their own, so their crates differ in capacity only. The remaining five materials carry their nature into the crate:

## Cursed Gold Crate

<row halign="center">
<item id="anvilcraft_more_storage:cursed_gold_crate"/>
<item id="anvilcraft_more_storage:cursed_gold_large_crate"/>
</row>

- Just like <ref item="anvilcraft:cursed_gold_ingot"/>, carrying one curses you: Weakness, plus Slowness above 8 and Hunger above 64
- Piglins take it as currency
- Carrying any enchanted gold item, the enchanted gold crate included, cancels the curse entirely

## Enchanted Gold Crate

<row halign="center">
<item id="anvilcraft_more_storage:enchanted_gold_crate"/>
<item id="anvilcraft_more_storage:enchanted_gold_large_crate"/>
</row>

- Counts as enchanted gold alongside <ref item="anvilcraft:enchanted_gold_ingot"/>: cancels every debuff the cursed gold family inflicts
- Carrying 64 of them grants Luck

## Ember Metal Crate

- Like the tools made from <ref item="anvilcraft:ember_metal_ingot"/>, it survives fire and lava

## Transcendium Crate

- Carries the **Eternal** property of <ref item="anvilcraft:transcendium_ingot"/>: the item is never destroyed by fire or lava, and never despawns on the ground
- The block itself shrugs off explosions

## Lead Crate

- Shields radiation the way <ref item="anvilcraft:lead_block"/> does: next to <ref item="anvilcraft:uranium_block"/> or <ref item="anvilcraft:plutonium_block"/>, every lead crate raises the number of adjacent radioactive blocks that neighbour needs before it decays by one

<warning>
The lead crate shares the lead block's block tag, so dropping an anvil onto it resets a vault as well.
</warning>
