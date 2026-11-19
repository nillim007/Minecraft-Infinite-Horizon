---
title: Research
---
Research starts your space program. At the <item:infinite_horizon:research_terminal> your [[agentur|agency]] spends **research points (RP)** to unlock new components and destinations. Research belongs to the whole agency: what one member researches, everyone can build.

## The research terminal

1. Build the terminal at the assembly table (recipe below) and place it in your base. It needs no power.
2. As soon as it stands, research **tier 1** opens for your agency, and you get the advancement "Ground Station".
3. Right-click opens the **research tree**: four columns for the branches industry and energy, spaceflight, life support and logistics, six rows for the tiers. Drag the area with the mouse and scroll it (with Shift horizontally).
4. Your agency's RP are shown at the top right.

Every node shows its icon with a coloured frame:

| Frame | Meaning |
| --- | --- |
| grey | locked: tier still closed or a prerequisite is missing |
| yellow | available: one click researches it |
| green | researched |

The tooltip lists the cost, what it unlocks and missing prerequisites. Researching deducts the RP right away. Leaders and engineers of the agency may research.

## Tiers

A tier opens when the previous one is open and its **gate** is met. Open tiers never close again, even if you break the terminal. The row headers in the tree show each gate and a check mark once the tier is open.

| Tier | Gate | Default cost per node |
| --- | --- | --- |
| 1 Space program | place your own research terminal | {{value:tier:1.cost_default}} RP |
| 2 Nearby worlds | a member sets foot on the Moon | {{value:tier:2.cost_default}} RP |
| 3 Orbit | magnesium and tungsten ingots and helium made | {{value:tier:3.cost_default}} RP |
| 4 Inner system | a zero-g fabricator of the agency finishes an operation | {{value:tier:4.cost_default}} RP |

The tiers also open the chapters of this handbook: you can read the third phase ("Ring 1") from tier 2.

## Locked recipes

A node unlocks **all recipes** whose result it names. As long as your agency has not researched it, your machines and your assembly table do not make those things: the status lamp stays yellow and its tooltip says "Locked". Anything not listed in a node can always be made, for example circuits, upgrades, ports, the terminal itself and the probe.

That also affects ten things you may already have built:

| Node | Tier | Cost | Unlocks |
| --- | --- | --- | --- |
| Pressure suit | 1 | {{value:node:suit_t1.cost}} RP | <item:infinite_horizon:pressure_suit_helmet>, <item:infinite_horizon:pressure_suit_chestplate>, <item:infinite_horizon:pressure_suit_leggings>, <item:infinite_horizon:pressure_suit_boots>, <item:infinite_horizon:oxygen_canister>, <item:infinite_horizon:large_oxygen_canister> |
| Gas storage | 1 | {{value:node:gas_storage.cost}} RP | <item:infinite_horizon:pressure_bottle> |
| Dome I | 2 | {{value:node:dome_t1.cost}} RP | <item:infinite_horizon:dome_generator_t1>, <item:infinite_horizon:oxygen_generator>, <item:infinite_horizon:electric_lamp> |

@note Blocks already built and finished suits keep working. Only crafting is locked.

@warning You need the pressure suit for the first trip to the Moon. Research the **Pressure suit** node early, otherwise your assembly table builds no new suit parts.

## Research points

RP belong to the agency and are shown with two decimal places. You get them in two ways: from **samples** evaluated by the analyzer, and from **exploration targets**.

### Taking samples

1. Build a <item:infinite_horizon:sample_tool> (recipe below).
2. Right-clicking a block gives a <item:infinite_horizon:sample>. The block stays where it is. Afterwards the tool needs a {{value:research:sample_tool.cooldown_ticks|t}} break.
3. Every spot gives only one sample. Blocks placed by a player never give one: the action bar then says "No usable sample".

On Earth only ores can be sampled: <item:infinite_horizon:bauxite_ore>, <item:infinite_horizon:titanium_ore>, <item:infinite_horizon:quartz_sand> and the vanilla ores coal, iron, copper, gold, redstone, lapis and diamond. Deepslate variants count as the normal ore. On the [[mond|Moon]] every natural Moon block gives a sample. Samples from the Nether and the End are worthless.

### The analyzer

The <item:infinite_horizon:analyzer> evaluates one sample per operation and credits the RP to your agency. It needs power and only accepts samples that give RP. An analyzer that belongs to nobody does not work (red lamp), so place it yourself.

@machine infinite_horizon:analyzer

| Sample origin | RP per sample |
| --- | --- |
| Earth | {{value:research:sample_ring_0.fp}} |
| Moon | {{value:research:sample_ring_1.fp}} |

For every **sample kind** (block and location) the first {{value:research:diminishing_returns.full_count}} samples count in full, every further one only {{value:research:diminishing_returns.factor_after|%}}. On Earth you can collect up to {{value:research:sample_ring_rules.ring0_max_full_fp}} RP at full value this way.

### Exploration targets

When a member enters a **lava tube** on the Moon, the agency gets {{value:research:exploration_target.fp}} RP, once per tube. Lava tubes are straight tunnels deep in the mare, between y {{value:planet:moon.structures.1.params.y_min}} and {{value:planet:moon.structures.1.params.y_max}}. Here, too, the first {{value:research:diminishing_returns.full_count}} finds count in full.

On servers, mandatory research can be switched off (setting `research_required`). Then all recipes and handbook chapters are open, and the tree shows a notice.

## Crafting

@recipes_for infinite_horizon:research_terminal infinite_horizon:sample_tool infinite_horizon:analyzer

@tip Set up the terminal right at the start. It only costs material, opens tier 1 and shows you in the tree what comes next.

@tip Take a sample of every ore you find while mining. Samples of many different ore kinds bring more than many samples of the same kind.
