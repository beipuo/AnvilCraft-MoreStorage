---
navigation:
  title: "§6更多存储"
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
  - anvilcraft_more_storage:hyperdimension_crafting_terminal
---

# 更多存储

> <ref item="anvilcraft:crate"/>和<ref item="anvilcraft:large_crate"/>高级版本，并添加以及少数材料自带的性质。另外还有一个能合成的<ref item="anvilcraft:hyperdimension_terminal"/>。


<info>
等级板条箱同样属于"可用斧挖掘"，大型板条箱的物品最多堆叠 16 个。
</info>

# 容量

原版板条箱可容纳 2048 个物品，大型板条箱 65536 个。等级容量 = 原版容量 × 倍率。

| 等级 | 倍率 | 板条箱 | 大型板条箱 |
|:---|:---:|---:|---:|
| 铜 | 1.5 | 3072 | 98304 |
| 铁 | 2 | 4096 | 131072 |
| 锌 | 3 | 6144 | 196608 |
| 铅 | 3 | 6144 | 196608 |
| 银 | 3 | 6144 | 196608 |
| 锡 | 3 | 6144 | 196608 |
| 金 | 4 | 8192 | 262144 |
| 诅咒金 | 4 | 8192 | 262144 |
| 附魔金 | 4.5 | 9216 | 294912 |
| 钻石 | 6 | 12288 | 393216 |
| 绿宝石 | 8 | 16384 | 524288 |
| 黑曜石 | 10 | 20480 | 655360 |
| 钨 | 11 | 22528 | 720896 |
| 钛 | 11 | 22528 | 720896 |
| 下界合金 | 12 | 24576 | 786432 |
| 余烬金属 | 12 | 24576 | 786432 |
| 皇家钢 | 14 | 28672 | 917504 |
| 浮霜金属 | 16 | 32768 | 1048576 |
| 超限合金 | 18 | 36864 | 1179648 |

<info>
每个等级的倍率都可以在模组配置里单独调整，范围 1.0 - 1024.0。
</info>

<warning>
等级大型板条箱无法用铁砧升级为<ref item="anvilcraft:shulker_container"/>。
</warning>

# 材料特性

锌、锡、银、钛属于铁砧工艺的"一般金属"（见<ref item="anvilcraft:zinc_ingot"/>），它们本身没有特性，做成的板条箱也只有容量差别。剩下五种材料把自己的性质带进了板条箱：

## 诅咒金板条箱

<row halign="center">
<item id="anvilcraft_more_storage:cursed_gold_crate"/>
<item id="anvilcraft_more_storage:cursed_gold_large_crate"/>
</row>

- 和<ref item="anvilcraft:cursed_gold_ingot"/>一样，背包里带着它就会被诅咒：虚弱，超过 8 个再加缓慢，超过 64 个再加饥饿
- 猪灵把它当作通货
- 身上带有任意附魔金物品（包括附魔金板条箱）时，诅咒完全失效

## 附魔金板条箱

<row halign="center">
<item id="anvilcraft_more_storage:enchanted_gold_crate"/>
<item id="anvilcraft_more_storage:enchanted_gold_large_crate"/>
</row>

- 与<ref item="anvilcraft:enchanted_gold_ingot"/>同列：抵消诅咒金系列物品带来的全部负面效果
- 携带满 64 个时获得幸运

## 余烬金属板条箱

- 与<ref item="anvilcraft:ember_metal_ingot"/>制作的工具一样，掉进火焰或熔岩里不会被摧毁

## 超限合金板条箱

- 拥有<ref item="anvilcraft:transcendium_ingot"/>的**永恒**：物品不会被火焰熔岩摧毁，掉在地上也不会随时间消失
- 方块本身免疫爆炸

## 铅板条箱

- 和<ref item="anvilcraft:lead_block"/>一样能屏蔽辐射：与<ref item="anvilcraft:uranium_block"/>、<ref item="anvilcraft:plutonium_block"/>相邻时，每一个铅板条箱都会让对方衰变所需的相邻放射性方块数 +1

<warning>
铅板条箱共用了铅块的方块标签，所以铁砧砸在它上面同样会触发重置宝库。
</warning>

# 超维合成终端

<row halign="center">
<item id="anvilcraft_more_storage:hyperdimension_crafting_terminal"/>
</row>

> 界面里多了一个 3×3 合成栏的<ref item="anvilcraft:hyperdimension_terminal"/>。

绑定和打开方式与普通终端完全一致：右键<ref item="anvilcraft:hyperdimension_storage_station"/>完成绑定，之后在任何地方手持右键即可打开该存储。

## 合成

- 合成栏里放的是真实物品，且保存在终端自身上，摆到一半也能随身带走
- 点击产物栏把成品拿到鼠标上；按住 Shift 点击则连续合成并直接进背包，最多 64 次或直到材料耗尽
- 每次合成后，被清空的格子会自动从绑定存储里补货，所以只要存储里还有材料就能一直重复同一个配方
- 容器类副产物（空桶、玻璃瓶）会留在原来的格子里，和工作台一样
- 合成栏上方的按钮把所有格子里的物品退回存储

<info>
只有合成时才会从存储取料。手动放进合成栏的物品仍然来自你自己的背包。
</info>

<warning>
和普通终端一样，合成终端无法放进超维存储里。
</warning>
