---
title: Helium and Cryogenics
---
The Moon's soil contains helium that the solar wind deposited there over billions of years. Heat gets it out, cold makes it liquid.

## Regolith heater

The <item:infinite_horizon:regolith_heater> heats <item:infinite_horizon:moon_regolith> and releases helium into its tank. It works **only on the Moon**. Anywhere else its lamp glows red, and the tooltip tells you why.

@machine infinite_horizon:regolith_heater

@recipes_type infinite_horizon:regolith_heating

## Cryo liquefier

The <item:infinite_horizon:cryo_liquefier> cools helium so much that it turns liquid. The <item:infinite_horizon:cooling_system> of the heat suits needs liquid helium.

@machine infinite_horizon:cryo_liquefier

@recipes_type infinite_horizon:cryo

## Gas tank

You store gases in the <item:infinite_horizon:gas_tank>. It only takes gases and outputs them through a selectable side, like the fluid tank.

@machine infinite_horizon:gas_tank

## Crafting

@recipes_for infinite_horizon:regolith_heater infinite_horizon:cryo_liquefier infinite_horizon:gas_tank infinite_horizon:cooling_system

@tip Put the regolith heater right into your Moon base and pipe the helium into a gas tank. You can then run the liquefier on Earth as well.
