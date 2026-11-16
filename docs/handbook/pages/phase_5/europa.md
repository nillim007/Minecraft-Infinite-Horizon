---
title: Europa
title_en: Europa
order: 90
milestone: 7
requires: ["infinite_horizon:europa_ice"]
icon: infinite_horizon:europa_ice
items: ["infinite_horizon:europa_ice", "infinite_horizon:europa_seabed", "infinite_horizon:hydrothermal_vent", "infinite_horizon:microbe_culture", "infinite_horizon:ice_drill", "infinite_horizon:diving_module"]
summary: Ein Ozean unter dem Eis, hydrothermale Quellen und Mikroben
---
Europa ist von einer dicken Eisdecke umhüllt. Darunter liegt ein Ozean, auf dessen Grund warme Quellen sprudeln. Die Schwerkraft beträgt {{value:planet:europa.gravity}} g, an der Oberfläche ist es {{value:planet:europa.temperature.day}} °C kalt, und die Strahlung des Jupiter hat Stufe {{value:planet:europa.radiation}}.

## Aufbau

- **Eisdecke** aus <item:infinite_horizon:europa_ice> zwischen y {{value:planet:europa.features.0.params.y_min}} und {{value:planet:europa.features.0.params.y_max}}. In den **Eisspalten** reißt sie auf; dort kommst du direkt ans Wasser.
- **Ozean** darunter. Unter der Eisdecke ist das Wasser {{value:planet:europa.zones.0.temperature}} °C warm, der Druck normal und die Strahlung abgeschirmt.
- **Meeresboden** aus <item:infinite_horizon:europa_seabed>.

## Ausrüstung

- An der Oberfläche: ein Raumanzug mit <item:infinite_horizon:radiation_shield>.
- Im Ozean ab y {{value:planet:europa.zones.1.y_max}} abwärts herrscht Tiefseedruck. Dafür brauchst du das <item:infinite_horizon:diving_module> an der Hose. Es macht dich außerdem beim Schwimmen schneller (×{{value:suits:infinite_horizon:diving_module.swim_speed_multiplier}}).

@recipes_for infinite_horizon:diving_module

@warning Ohne Tauchmodul zerdrückt dich der Tiefseedruck. Bleib über der Grenze, bis du eines hast.

## Durch das Eis

Europa-Eis ist so hart wie Obsidian. Mit der Spitzhacke dauert ein Schacht durch die Eisdecke sehr lange. Der <item:infinite_horizon:ice_drill> schafft es schneller: Halte die rechte Maustaste gedrückt, und er bohrt unter dir einen Schacht von {{value:items:infinite_horizon:ice_drill.shaft.width}} × {{value:items:infinite_horizon:ice_drill.shaft.width}} Blöcken nach unten, eine Schicht alle {{value:items:infinite_horizon:ice_drill.ticks_per_layer|t}}. Jeder Block kostet {{value:items:infinite_horizon:ice_drill.energy_per_block}} J aus dem Akku (voll: {{value:items:infinite_horizon:ice_drill.energy_capacity}} J). Lade ihn im Ladeslot eines Akkus.

@recipes_for infinite_horizon:ice_drill

@tip Bohr nur dort, wo du wieder herauskommst. Eine Treppe aus Blöcken oder ein Jetpack bringt dich zurück auf die Eisdecke.

## Hydrothermale Quellen

Auf dem Meeresboden liegen **Quellenfelder**: Gruppen von <item:infinite_horizon:hydrothermal_vent>, aus denen Blasen aufsteigen. Ein Quellenfeld ist ein Erkundungsziel. Rechtsklick mit einer leeren Glasflasche auf eine Quelle gibt eine <item:infinite_horizon:microbe_culture>. Danach ruht diese Quelle {{value:items:infinite_horizon:hydrothermal_vent.cooldown_ticks|t}}.

@uses infinite_horizon:microbe_culture

## Leben im Ozean

- Die **Eisqualle** leuchtet und treibt langsam zur nächsten Quelle. Folge ihr, wenn du ein Quellenfeld suchst.
- Jedes Quellenfeld bewacht ein **Riffwächter**. Er ist friedlich, bis jemand in seiner Nähe (Umkreis {{value:creatures:infinite_horizon:reef_keeper.behavior_params.aggro_radius}} Blöcke) Blöcke abbaut oder Proben nimmt. Dann greift er an ({{value:creatures:infinite_horizon:reef_keeper.damage}} Schaden).

@tip Gib dem Riffwächter eine Weltraumration (Rechtsklick). Dann bleibt er dir gegenüber {{value:creatures:infinite_horizon:reef_keeper.behavior_params.pacify_ticks|t}} friedlich, und du kannst in Ruhe Proben nehmen.
