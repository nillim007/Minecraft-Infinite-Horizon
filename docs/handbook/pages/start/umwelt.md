---
title: Umwelt, Belastung und Schwerkraft
title_en: Environment, Strain and Gravity
order: 30
milestone: 3
icon: infinite_horizon:engineers_handbook
summary: Was fremde Welten gefährlich macht und wie lange du durchhältst
---
Auf der Erde musst du dir um Luft, Druck und Temperatur keine Gedanken machen. Auf anderen Himmelskörpern schon: Jede Welt hat eigene **Umweltwerte**, und jede Gefahr, gegen die du nicht geschützt bist, baut eine **Belastung** auf.

## Die Umweltwerte

Der Server prüft alle {{value:environment:environment_update.interval_ticks|t}}, wo du stehst, und berechnet:

- **Atemluft:** Gibt es eine atembare Atmosphäre? Auf dem Mond nicht.
- **Temperatur:** Sie folgt dem Tageslicht. Auf dem Mond steigt sie mittags auf {{value:planet:moon.temperature.day}} °C und fällt nachts auf {{value:planet:moon.temperature.night}} °C. In der Dämmerung liegt sie dazwischen.
- **Druck:** von Vakuum über niedrig und normal bis hoch.
- **Strahlung:** Stufe {{value:environment:radiation_rules.min}} bis {{value:environment:radiation_rules.max}}. Stehen mindestens {{value:environment:radiation_rules.cover_blocks}} feste Blöcke über dir, sinkt sie um {{value:environment:radiation_rules.cover_reduction}} Stufen.

Bestimmte Bereiche haben eigene Werte. Im Polarkrater des Mondes bleibt es immer bei {{value:planet:moon.biome_temperature_overrides.moon_polar_crater}} °C, und Sonnenenergie gibt es dort nicht.

Dimensionen ohne eigene Umweltwerte, etwa der Nether, das End oder Welten anderer Mods, gelten wie die Erde.

## Was du ohne Anzug verträgst

| Gefahr | Ohne Anzug sicher |
| --- | --- |
| Temperatur | {{value:environment:no_suit_tolerance.temp_min}} bis {{value:environment:no_suit_tolerance.temp_max}} °C |
| Druck | niedrig bis normal |
| Strahlung | bis Stufe {{value:environment:no_suit_tolerance.radiation_max}} |
| Atemluft | nur in atembarer Luft |

## Die Belastung

Jede Gefahr hat ihre eigene Belastung. Bist du ungeschützt, füllt sie sich in {{value:environment:oxygen.buildup_ticks|t}} von 0 auf 100 %. Erst bei 100 % nimmst du Schaden, und Rüstung hilft dagegen nicht. Ohne Sauerstoff oder im Vakuum kostet das {{value:environment:oxygen.damage}} Lebenspunkt alle {{value:environment:oxygen.damage_interval|t}}.

Ist die Gefahr vorbei, sinkt die Belastung in {{value:environment:oxygen.decay_ticks|t}} wieder auf 0. Mehrere Gefahren zählen getrennt: Ohne Anzug auf dem Mond steigen Sauerstoff und Druck gleichzeitig, dazu je nach Tageszeit Hitze oder Kälte.

Im Kreativ- und im Zuschauermodus gibt es keine Belastung. Nach dem Tod beginnt sie wieder bei 0.

@warning Die Belastung kündigt sich nur kurz an: Nach {{value:environment:oxygen.buildup_ticks|t}} im Vakuum beginnt der Schaden. Geh nie ohne Plan nach draußen.

## Die Anzeige im HUD

Über der Hungerleiste erscheint für jede Gefahr eine eigene Leiste, sobald ihre Belastung steigt. Sie ist grün, wird ab {{value:environment:hud.yellow_from|%}} gelb und ab {{value:environment:hud.red_from|%}} rot. Beim Erscheinen ertönt ein Warnton.

Trägst du einen vollständigen [[raumanzug|Druckanzug]] außerhalb atembarer Luft, zeigt eine weitere Leiste seinen Sauerstoffvorrat. Liegen Temperatur oder Strahlung außerhalb dessen, was du ohne Anzug verträgst, stehen ihre Werte darüber.

## Schwerkraft

Jede Welt hat ihre eigene Schwerkraft. Auf dem Mond beträgt sie das {{value:planet:moon.gravity}}-Fache der Erde. Du fällst langsamer, springst viel höher und überstehst tiefe Stürze ohne Schaden: Die sichere Fallhöhe ({{value:misc:gravity.safe_fall_distance_numerator}} Blöcke auf der Erde) wird durch die Schwerkraft geteilt, und Fallschaden wird im selben Maß kleiner.

Die Sprungkraft selbst bleibt gleich. Darum springst du auf dem Mond ein Vielfaches höher als auf der Erde.

@tip Strahlung lässt sich abschirmen: Schon {{value:environment:radiation_rules.cover_blocks}} feste Blöcke über dem Kopf senken sie um {{value:environment:radiation_rules.cover_reduction}} Stufen. Eine Höhle oder ein Dach ist auf fremden Welten oft die schnellste Deckung.
