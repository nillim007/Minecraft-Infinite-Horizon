---
title: Hydroponics and Compost
---
On the Moon and Mars nothing grows in the soil. Regolith and Mars sand cannot be tilled. For food you have two ways: compost soil for classic fields or the hydroponics basin. The research node "Hydroponics" unlocks both.

Outside Earth, all plants need an active [[kuppel|dome]] and enough light.

## Compost soil

<item:infinite_horizon:compost_soil> is made in the assembly table from regolith or Mars sand, bone meal and gypsum or <item:infinite_horizon:compost>. A hoe turns it into <item:infinite_horizon:compost_farmland>. It behaves like farmland on Earth: it needs water nearby and otherwise dries back into compost soil.

@recipes_for infinite_horizon:compost_soil infinite_horizon:compost

## The hydroponics basin

The <item:infinite_horizon:hydroponics_basin> needs power, water and light (at least {{value:agriculture:infinite_horizon:hydroponics_basin.light_min}}). Put seeds or a sapling into the planting slot. The plant grows one stage every {{value:agriculture:infinite_horizon:hydroponics_basin.ticks_per_growth_stage|t}}, about twice as fast as on good farmland. Once ripe, the harvest lands in the output slot and the seed stays in the planting slot. A sapling needs {{value:agriculture:saplings.growth_stages}} stages and gives {{value:agriculture:saplings.yield.0.count}} logs of its wood type.

@machine infinite_horizon:hydroponics_basin

@recipes_for infinite_horizon:hydroponics_basin

@warning If the dome fails or it gets too dark, the plant stops growing. The status lamp then shows "No growth".

@tip An electric lamp above the basin gives enough light, even in the lunar night.

@if infinite_horizon:ammonia_fertilizer
## Ammonia fertilizer

Right-clicking a hydroponics basin or a greenhouse with <item:infinite_horizon:ammonia_fertilizer> fertilizes it for {{value:agriculture:infinite_horizon:ammonia_fertilizer.duration_ticks|t}}. During that time the plants grow faster. You make the fertilizer at the assembly table from bone meal and ammonia.

@recipes_for infinite_horizon:ammonia_fertilizer
@endif
