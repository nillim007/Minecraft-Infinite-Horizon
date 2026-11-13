---
title: Weltraumwetter und Ereignisse
title_en: Space Weather and Events
order: 130
milestone: 5
requires: ["infinite_horizon:meteorite_ore"]
icon: infinite_horizon:meteorite_ore
items: ["infinite_horizon:meteorite_ore", "infinite_horizon:raw_platinum", "infinite_horizon:raw_iridium"]
summary: Sonnenstürme, Meteoritenschauer, Staubstürme und Säureregen
---
Fremde Welten haben ihr eigenes Wetter. Alle {{value:events:event_planning.check_interval_ticks|t}} würfelt der Server für jede Welt mit Spielern, ob ein Ereignis kommt. Es beginnt {{value:events:event_planning.schedule_ahead_ticks|t}} später, und {{value:events:event_planning.warning_ticks_without_station|t}} vorher bekommst du eine Warnung im Chat, einen Warnton und eine Anzeige im HUD. Wie oft Ereignisse kommen, stellt der Server mit `event_frequency` ein.

## Sonnensturm

Auf allen Welten außer der Erde. Die Strahlung steigt um {{value:events:infinite_horizon:solar_storm.effects.radiation_bonus}}. Maschinen ohne Dach aus mindestens {{value:events:infinite_horizon:solar_storm.effects.machine_pause.exempt_cover_blocks}} Blöcken halten alle {{value:events:infinite_horizon:solar_storm.effects.machine_pause.interval_ticks|t}} für {{value:events:infinite_horizon:solar_storm.effects.machine_pause.pause_ticks|t}} an. Dafür liefern Solarpanels das {{value:events:infinite_horizon:solar_storm.reward.solar_multiplier}}-Fache.

## Meteoritenschauer

Auf Mond und Mars. Alle {{value:events:infinite_horizon:meteor_shower.effects.impact_interval_ticks|t}} schlägt bei jedem Spieler im Umkreis von {{value:events:infinite_horizon:meteor_shower.effects.impact_radius}} Blöcken ein Meteorit ein. Er explodiert ohne Feuer (ob Blöcke zerstört werden, stellt der Server ein) und hinterlässt <item:infinite_horizon:meteorite_ore>. Im [[zerkleinerer|Zerkleinerer]] gibt das Eisen, Nickel und mit Glück <item:infinite_horizon:raw_platinum> oder <item:infinite_horizon:raw_iridium>.

@recipes_for infinite_horizon:raw_platinum infinite_horizon:raw_iridium

## Staubsturm (Mars)

Statt Regen ziehen auf dem Mars Staubstürme auf. Solarpanels liefern nur noch das {{value:events:infinite_horizon:dust_storm.effects.solar_multiplier}}-Fache, Monster kommen doppelt so oft, und Staubgeister tauchen auf, die dich zu Erkundungszielen führen können (siehe [[kreaturen|Kreaturen]]).

## Säureregen (Venus)

Auf der Venus regnet es Säure. Wer ungeschützt unter freiem Himmel steht, verliert alle {{value:events:infinite_horizon:acid_rain.effects.interval_ticks|t}} Haltbarkeit an Rüstung und Werkzeugen in der Hand. Der [[hitzeanzug|Hitzeanzug]] schützt dich.

@warning Stell auf dem Mars und dem Mond keine wertvollen Maschinen ungeschützt unter freien Himmel. Ein Dach aus drei Blöcken schützt sie im Sonnensturm.

@tip Ein Meteoritenschauer ist eine gute Gelegenheit: Sammle die Einschläge ein, solange er dauert.
