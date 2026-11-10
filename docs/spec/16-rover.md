# 16. Rover

| Eigenschaft | Wert |
| --- | --- |
| Entity | `rover`, 2 Sitze, Größe 3×2×4 Blöcke |
| Aufbau | `rover_kit` auf den Boden setzen (Rechtsklick) |
| Energie | 100.000 J, Verbrauch 50 J pro gefahrenem Block, Solardach +2 J/t bei sichtbarem Himmel |
| Tempo | 0,4 Blöcke/t |
| Stufenhöhe | 2 Blöcke |
| Frachtraum | 27 Slots, Sneak-Rechtsklick öffnet |
| Module | 6 Slots, Rechtsklick mit Modul-Item |
| Laden | neben `battery`/`lithium_battery_bank` parken: 512 J/t |
| Abbau | Sneak-Rechtsklick mit leerer Hand → `rover_kit`. Fracht, Module und Energie bleiben als Data Components erhalten. |

| Modul-ID | Wirkung |
| --- | --- |
| `rover_scanner` | alle 100 t: Erzblöcke im Radius 32 werden 100 t lang durch Wände sichtbar umrandet; Erkundungsziele im Radius 128 werden im Atlas markiert |
| `rover_drill` | baut beim Fahren 3×3 Blöcke vor dem Rover ab (1 Block / 10 t), Drops in den Frachtraum; gilt als Vibrationsquelle |
| `rover_headlight` | Licht 15 vor dem Rover (gleiche Technik wie `helmet_lamp`) |
| `rover_pressure_cabin` | Insassen atmen ohne Anzug, eigener O₂-Tank 1.800 mB (30 min für 1 Person, 15 min für 2; Verbrauch wie Anzug T1 pro Insasse), Temperatur 20 °C |
| `rover_lithium_pack` | Energie × 2 (Stufe 4) |
| `rover_trailer` | +27 Slots (Anhänger-Entity folgt) |

Venus-Oberfläche: Der Rover hat dieselbe Hitzezeit von 3.600 t, danach bleibt er stehen, bis er wieder über y 180 gebracht wird (z. B. per `cloud_elevator` – Fahrzeuge in 3 Blöcken Umkreis werden mitgenommen).
