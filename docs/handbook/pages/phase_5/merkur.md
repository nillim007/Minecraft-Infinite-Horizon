---
title: Der Merkur
title_en: Mercury
order: 20
milestone: 7
requires: ["infinite_horizon:mercury_rock"]
icon: infinite_horizon:mercury_rock
items: ["infinite_horizon:mercury_rock", "infinite_horizon:mercury_lithium_salt", "infinite_horizon:mercury_iron_ore", "infinite_horizon:mercury_titanium_ore", "infinite_horizon:mirror_shell", "infinite_horizon:heat_reflector", "infinite_horizon:radiation_shield"]
summary: Glut auf der Tagseite, Frost auf der Nachtseite, Lithium dazwischen
planet: infinite_horizon:mercury
---
Der Merkur dreht sich so langsam, dass eine Seite immer glüht und die andere immer friert. Wo du landest, hängt von der X-Koordinate ab:

- **Glutebenen** (Tagseite, X über 2.000): {{value:planet:mercury.zones.0.temperature}} °C, mit <item:infinite_horizon:mercury_titanium_ore>.
- **Frostebenen** (Nachtseite, X unter −2.000): {{value:planet:mercury.zones.1.temperature}} °C, mit <item:infinite_horizon:mercury_iron_ore>.
- **Dämmerungsgürtel** dazwischen: angenehme {{value:planet:mercury.zones.2.temperature}} °C. Hier liegt <item:infinite_horizon:mercury_lithium_salt> als Kruste an der Oberfläche.
- **Schattenkrater**: selten, {{value:planet:mercury.zones.3.temperature}} °C, mit Eis im Gestein.

Die Schwerkraft beträgt {{value:planet:mercury.gravity}} g, die Strahlung Stufe {{value:planet:mercury.radiation}}.

@ores infinite_horizon:mercury

## Ausrüstung

Auf der Tagseite hilft nur der [[hitzeanzug|Hitzeanzug]]. Gegen die Strahlung schützt der Anzug allein nicht: Bau den <item:infinite_horizon:radiation_shield> in den Helm, er hält Strahlung bis Stufe {{value:suits:infinite_horizon:radiation_shield.radiation_max}} ab. Der <item:infinite_horizon:heat_reflector> in der Brustplatte verlängert die Hitzezeit auf der Venus um das {{value:suits:infinite_horizon:heat_reflector.venus_heat_timer_multiplier}}-Fache.

@recipes_for infinite_horizon:radiation_shield infinite_horizon:heat_reflector

## Der Spiegelkäfer

Im Dämmerungsgürtel krabbelt der Spiegelkäfer. Er greift nur an, wenn du ihn angreifst. Sein Panzer spiegelt das Sonnenlicht: Wer ihn bei Tageslicht aus weniger als {{value:creatures:infinite_horizon:mirror_beetle.behavior_params.gaze_distance}} Blöcken ansieht, ist {{value:creatures:infinite_horizon:mirror_beetle.behavior_params.blindness_ticks|t}} lang geblendet. Er lässt eine <item:infinite_horizon:mirror_shell> fallen, aus der der Hitzereflektor entsteht.

@warning Lande nicht auf der Tagseite ohne Hitzeanzug. Der Dämmerungsgürtel um X 0 ist der sichere Landeplatz.

@tip Schau den Spiegelkäfer nicht direkt an, sondern nähere dich von der Seite. Ein Schlag aus kurzer Entfernung genügt meist.
