---
title: Titan
title_en: Titan
order: 120
milestone: 7
requires: ["infinite_horizon:titan_ice_rock"]
icon: infinite_horizon:titan_dune_sand
items: ["infinite_horizon:titan_dune_sand", "infinite_horizon:titan_ice_rock", "infinite_horizon:methane", "infinite_horizon:skimmer_leg"]
summary: Der größte Saturnmond mit Methanseen und dichter Luft
planet: infinite_horizon:titan
---
Titan hat als einziger Mond eine dichte Atmosphäre. Atmen kannst du sie nicht, aber der Druck ist normal, und es ist {{value:planet:titan.temperature.day}} °C kalt. Die Schwerkraft beträgt {{value:planet:titan.gravity}} g. Ein Druckanzug genügt.

## Gebiete

- **Methanseen**: Bis y {{value:planet:titan.sea_level}} sind die Senken mit flüssigem <item:infinite_horizon:methane> gefüllt.
- **Dünen** aus <item:infinite_horizon:titan_dune_sand>.
- **Eisgebirge** aus <item:infinite_horizon:titan_ice_rock>.

Statt Regen fällt auf Titan **Methanregen**.

## Gleiten

In der dichten Luft sinkst du mit Elytren langsamer (Sinkrate ×{{value:planet:titan.special.elytra_sink_rate_multiplier}}). Von einem Eisgebirge kommst du so weit über die Seen.

@tip Spring mit Elytren von einem hohen Gipfel ab. Auf Titan gleitest du ohne Feuerwerk weiter als auf der Erde.

## Der Methanläufer

Auf den Methanseen läuft der **Methanläufer**. Er flieht, sobald du näher als {{value:creatures:infinite_horizon:methane_skimmer.behavior_params.flee_player_distance}} Blöcke kommst. Erlegt gibt er ein <item:infinite_horizon:skimmer_leg>.

@uses infinite_horizon:skimmer_leg

@warning Wer in einen Methansee fällt, sinkt wie im Wasser. Halte dich am Ufer und nimm genug Sauerstoff mit.
