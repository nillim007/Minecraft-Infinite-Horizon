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

## Festlegungen aus der Umsetzung (M1–M8)

Annahmen aus der Umsetzung, die übernommen wurden. Die Nummer verweist auf den Eintrag in `docs/spec-gaps.md`, dort steht auch die Code-Stelle.

- **#91 (M5e, 16):** Rover: Lenkung, Länge der Hitbox, Startenergie, Frachtraum vs. Abbau, Scanner-Darstellung, Druckkabine, Anhänger → Gelenkt wird wie beim Pferd über die Blickrichtung des Fahrers, gefahren auf dessen Client (Server rechnet Energie). Hitbox quadratisch mit `size.width` × `size.height` (Länge 4 nicht darstellbar). Neuer Bausatz ohne Energie. Schleichen + Rechtsklick mit leerer Hand baut ab, mit Item öffnet den Frachtraum; Inventartaste im Sitz öffnet ihn ebenfalls. Scanner nennt Erzzahl und Erkundungsziele in der Aktionsleiste; Umrandung durch Wände und Atlas-Markierung folgen mit dem Feinschliff. Druckkabine wirkt wie eine Kuppel T1 mit Kabinentemperatur; Tank füllt sich per Rechtsklick mit einem Sauerstoffbehälter. Anhänger erweitert nur den Frachtraum (Anhänger-Entity später); Lithium-Paket kommt mit Lithium. Erz = Block-ID endet auf `_ore`
