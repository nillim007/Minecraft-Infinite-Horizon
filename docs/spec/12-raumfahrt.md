# 12. Raumfahrt

## 12.1 Ziele und Raketenstufen

> **Maßgebliche Daten:** `spec/data/planets.json` (je Ziel: `ring`, `distance_points`, `required_rocket_tier`, `requires_long_range`, `landing_requires_fuel`, `launch_cost_override`).
> **Lesefassung:** `docs/spec/generated/planets.md`.

Es gibt 21 Ziele: die Erde, 12 weitere Oberflächen (Mond, Mars, Venus, Merkur, Asteroidengürtel, Ceres, Io, Europa, Titan, Enceladus, Triton, Pluto) und 8 Orbits (Erde, Venus, Mars, Merkur, Jupiter, Saturn, Uranus, Neptun). T1 erreicht Ring 1 (ohne Orbits), T2 Ring 1 mit Orbits und das innere System bis Jupiter, T2 mit `long_range_module` das äußere System.

Ein Ziel ist wählbar, wenn (1) die Agentur dort eine Sonde ankommen ließ, (2) die Rakete die Stufe hat und (3) der Treibstoff reicht. Erde und Erdorbit sind immer bekannt.

## 12.2 Treibstoffformel

Kosten in mB **je Treibstoffkomponente** (also gleich viel Brennstoff und Sauerstoff):

```
kosten(A → B) = start(A) + transfer(A, B) + landung(B)
start(A)      = 32.000 × g(A)              (Orbit: 0; Venus aus der Wolkenschicht: 16.000)
transfer(A,B) = 6.000 × |DP(A) − DP(B)| + (A ≠ B ? 2.000 : 0)
landung(B)    = 0,25 × 32.000 × g(B)        (0, wenn landing_requires_fuel = false in planets.json)
```

Beispiele: Erde → Mond = 32.000 + 2.000 + 1.360 = 35.360 mB. Mond → Erde = 5.440 + 2.000 + 0 = 7.440 mB.

**Tankgrößen je Komponente:** T1 3–5 Tanks = 48.000–80.000 mB. T2 4–8 Tanks = 64.000–128.000 mB, mit `long_range_module` +128.000 mB (max. 256.000 mB).

**Hin- und Rückflug:** Die Tabelle aller Ziele (ab Erdoberfläche und ab Station im Erdorbit, mit Mindestausstattung) wird aus der Formel und `planets.json` berechnet: `docs/spec/generated/fuel_table.md` (erzeugt von `tools/render_docs.py`, geprüft von `tools/validate_spec.py`). Beispiele: Mond 42.800 mB (T1, 3 Tanks), Mars 69.200 mB (T1, 5 Tanks), Pluto 254.400 mB ab Erde bzw. 222.400 mB ab Station (T2 + Modul).

**Nachtanken und Rückflug:**

- Der Raketen-Computer zeigt jedes Ziel **grün** (Hin- und Rückflug reichen), **gelb** (nur Hinflug, Warnung „Rückflug nur mit Nachtanken“) oder **grau** (Hinflug reicht nicht, nicht wählbar).
- Nachtanken ist möglich:
  - über das Slotpaar im Raketen-GUI (4.1) oder den Knopf „Aus Frachtraum tanken“, der passende `transport_tank` aus dem Frachtraum leert,
  - an einer `docking_bay` aus angrenzenden Tanks,
  - an einer `fuel_pump` im Umkreis von 3 Blöcken um das gelandete Raketen-Entity.
- Ab Stufe 3 ist die Station im Erdorbit das vorgesehene Drehkreuz: Start von der Station spart 32.000 mB je Komponente.

## 12.3 Sonde

| ID | Stufe | Ziele | Flugzeit |
| --- | --- | --- | --- |
| `probe` | 1 (Mond), 2 (Mars, Venus) | Ring 1 | 1.200 + 1.200 × DP Ticks |
| `advanced_probe` | 4 | alle Ziele in Ring 2 | 1.200 + 1.200 × DP Ticks |

Ablauf: Sonde als Block in die Mitte der Startplattform setzen → in `launch_control` Ziel wählen → Start (Countdown 200 t, Sonde fliegt als Entity nach oben und verschwindet). Nach der Flugzeit: Ziel für die Agentur freigeschaltet, +10 FP, Nachricht an die Agentur, Atlas-Eintrag. Eine Sonde kehrt nie zurück. Orbit-Ziele brauchen eine eigene Sonde.

## 12.4 Raketenbau

Eine Rakete wird aus **Raketenblöcken** auf der Startplattform gebaut. Alle Teile sind 1 Block groß.

**Aufbau (von unten nach oben, über der Plattformmitte):**

1. Triebwerk direkt auf dem mittleren `launch_pad`
2. 4 Flossen waagerecht neben dem Triebwerk (N, O, S, W)
3. Treibstofftanks
4. optional Frachtraum
5. optional `long_range_module` (nur T2, direkt unter der Kapsel)
6. Kapsel ganz oben

> **Maßgebliche Daten:** `spec/data/rocket_parts.json` (Teile mit Masse, Treibstoffkapazität, Schub, Frachtslots, Sitzen; Regeln je Stufe mit erlaubter Teileanzahl).

Kurzfassung: Kapsel (1), Tanks (T1 3–5, T2 4–8, je 16.000 mB + 16.000 mB), Triebwerk (1), Flossen (4), Frachtraum (T1 0–1, T2 0–2), Fernflug-Modul (nur T2, 0–1, +128.000 mB je Komponente).

- Schub: T1 = 30, T2 = 48. **Start nur bei Schub / (Masse × g(Start)) ≥ 1,0.** Orbit: immer erlaubt.
  - Beispiel T1: 4 Tanks + voller Frachtraum = 27,35 t → 1,10 ✓. 5 Tanks + voller Frachtraum = 31,35 t → 0,96 ✗ (5 Tanks nur ohne oder mit leerem Frachtraum).
  - Beispiel T2: 6 Tanks + Modul + 2 volle Frachträume = 44,7 t → 1,07 ✓. 8 Tanks + Modul = 47 t → 1,02 ✓, mit Fracht ✗.
- T1 startet von der 5×5-Plattform (`launch_pad_t1`), T2 nur von der 7×7-Plattform (`launch_pad_t2`), einer `docking_bay` oder als gelandetes Raketen-Entity.
- Treibstoff T1: `kerosene` + `oxygen`. T2: `hydrogen` + `oxygen`.
- Stufen dürfen nicht gemischt werden (außer Frachtraum).
- **Raketen-Computer** (GUI der Kapsel und von `launch_control`): zeigt Teileliste, Masse, Schub-Gewicht-Verhältnis, Treibstoff, erreichbare Ziele (nach 12.1/12.2).
- **Checkliste** in `launch_control`: Struktur gültig ✓, Schub ≥ Gewicht ✓, Treibstoff für mindestens ein Ziel ✓, mindestens ein Ziel freigeschaltet ✓, Spieler mit vollständigem Anzug sitzt in der Kapsel ✓.
- **Betanken:** `fuel_pump` zieht aus angrenzenden Tanks und Rohren und verteilt gleichmäßig auf alle Raketentanks.

## 12.5 Flugablauf

1. Spieler sitzt in der Kapsel, startet über `launch_control` oder das Kapsel-GUI.
2. **Server:** prüft die Checkliste, wandelt die Raketenblöcke in ein `rocket`-Entity um (Data: Teileliste, Treibstoff, Fracht, Stufe), entfernt die Blöcke.
3. **Client:** Countdown 200 t mit Ton, Aufstieg 600 t mit Kamera von außen, Bildschirmbeben.
4. **Navigationskarte** (GUI `star_map`; nicht verwechseln mit dem Item `star_chart`): Ziele mit Treibstoffkosten, gesperrte Ziele grau. Auswahl des Landeorts: Landefeuer der Agentur in dieser Dimension oder Koordinaten X/Z (Standard: Spawn der Dimension = X/Z 0/0 mit der Landeplatzsuche unten; in der Overworld ihr Weltspawn).
5. **Server:** zieht den Treibstoff ab, teleportiert Rakete und Passagiere in die Zieldimension 200 Blöcke über dem Landepunkt. Landung in 200 t auf dem obersten festen Block (Landefeuer: innerhalb 3 Blöcken).
6. Danach steht das `rocket`-Entity am Boden. Rechtsklick öffnet das Raketen-GUI (Treibstoff aus `transport_tank`, Fracht, Start). Sneak-Rechtsklick mit leerer Hand zerlegt die Rakete in ihre Teil-Items (Treibstoff geht verloren).
**Sonderfälle (MUSS):**

- **Reihenfolge:** Ziel und Landeort wählt man **vor** dem Start. Die Navigationskarte öffnet sich beim Klick auf „Start“, danach laufen Countdown, Aufstieg und Landung ohne Eingabe.
- **Mehrere Insassen:** Ziel und Landeort wählt der Spieler, der „Start“ drückt.
- **Disconnect, Tod oder Schließen des Bildschirms während des Flugs:** Der Server schließt den Flug sofort ab. Rakete und verbliebene Insassen landen am gewählten Ort; ein abgemeldeter Spieler erscheint beim nächsten Login in der Rakete.
- **Sitzen:** Rechtsklick auf den Kapselblock setzt den Spieler auf ein unsichtbares Sitz-Entity. Nach der Umwandlung sitzt er im `rocket`-Entity.
- **Landepunkt:**
  - Gesucht wird spiralförmig im Radius 32 um die gewählte X/Z-Position ein fester Block ohne Fluid darüber mit 3 Luftblöcken über sich.
  - Findet sich keiner (Lava, Methan, Wasser), entsteht eine 3×3-Plattform aus dem Gestein des Planeten auf der Fluidoberfläche.
  - Venus: immer in der Wolkenschicht (12.8); Landefeuer auf der Venus gelten nur in der Wolkenschicht.
- **Neustart:** Ein gelandetes `rocket`-Entity darf überall erneut starten (ohne Rampe).
- **Besitz:** Das `rocket`-Entity gehört der Agentur des Spielers, der den Start ausgelöst hat (23.2).
- **Mehrere Rampen:** `launch_control` steuert genau die nächstgelegene gültige Rampe im Umkreis von 8 Blöcken.
- **Asteroidengürtel:** Es gibt keinen Boden. Gelandet wird auf dem höchsten Asteroidenblock der Spalte; gibt es keinen, schwebt die Rakete auf y 100 wie im Orbit (Schritt 7).

7. **Orbit ohne `docking_bay` als Ziel:** Die Rakete schwebt auf y 100 an der gewählten X/Z-Position (Standard 0/0). Ist dort keine freie Säule von 5×5×12 Blöcken oder liegt ein fremder `station_core` näher als 64 Blöcke, sucht der Server spiralförmig bis Radius 256 weiter. Klickt der Spieler in der Kapsel mit einem `station_core` in der Hand, wird der Kern direkt unter die Rakete gesetzt. Die 5×5-Plattform entsteht, die Rakete steht darauf, und der Spieler kann sicher aussteigen.

## 12.6 Landefeuer

`landing_beacon`: Block, Rechtsklick → Namen vergeben. Erscheint in Atlas und Landeortwahl der Agentur. Rotes Leuchtsignal bis Höhe 256.

## 12.7 Landefähre

`lander`: Entity, 2 Sitze, Tank 32.000 mB je Komponente (`hydrogen` + `oxygen`). Fliegt nur zwischen einer Orbit-Dimension und den Körpern darunter (Erdorbit ↔ Erde/Mond, Jupiter-Orbit ↔ Io/Europa, Saturn-Orbit ↔ Titan/Enceladus, Neptun-Orbit ↔ Triton, sonst Planetenorbit ↔ Planet). Kosten nach 12.2 ohne `transfer`. Start von der Erdoberfläche ist verboten. Das Ziel wählt man in der Navigationskarte, die nur die erlaubten Ziele zeigt. Sonst gilt der Flugablauf aus 12.5. Das `lander`-Item lässt sich nur in Orbit-Dimensionen oder auf einer `docking_bay` setzen (Rechtsklick wie ein Boot).

## 12.8 Venus-Wolkenschicht

- Die Wolkenschicht ist y 180–240 in `venus`. Dort: 20 °C, Druck `normal`, Atmosphäre `toxic` (nicht atembar).
- Landet eine Rakete auf der Venus und es gibt am Landepunkt keinen festen Block zwischen y 180 und 240, erzeugt der Server dort einmalig eine 5×5-Plattform aus `aerostat_platform` auf y 200.
- `aerostat_platform` ist ein normaler Block, der in der Luft gesetzt werden darf, wenn er an einen anderen `aerostat_platform` angrenzt.
- `cloud_elevator`: Rechtsklick auf die obere Station → nach 100 t Teleport auf die Oberfläche: erster fester Block unter y 180 an gleicher X/Z, der kein Fluid ist und 2 Luftblöcke über sich hat. Liegt an dieser Stelle Lava, entsteht eine 3×3-Plattform aus `venus_rock` auf der Lavaoberfläche. Dort entsteht automatisch eine verknüpfte untere Station. Rechtsklick unten → zurück.

## 12.9 Schwerkraft

Beim Betreten einer Dimension und alle 10 t:

- `minecraft:gravity` = 0,08 × g
- `minecraft:fall_damage_multiplier` = g
- `minecraft:safe_fall_distance` = 3 / max(g, 0,05)
- Umsetzung als `AttributeModifier` mit fester ID `infinite_horizon:planet_gravity` (nie den Basiswert ändern), damit andere Mods ihre eigenen Modifikatoren behalten.
- Operationen: `minecraft:gravity` und `minecraft:fall_damage_multiplier` als `add_multiplied_total` mit g − 1, `minecraft:safe_fall_distance` als `add_value` mit 3 / max(g, 0,05) − 3 (Basiswerte 0,08, 1 und 3). Angewendet beim Dimensionswechsel, Respawn, Login und alle 10 t.
- Gilt für Spieler (ab M3), Rover und Raketen (`applies_to` in `misc.json`). Monster und Items fallen mit Vanilla-Schwerkraft. Die Mixins für Projektile und Explosionen (3.7) kommen mit M5.
- Mit g = 0,17 springt ein Spieler rund 4,9 Blöcke hoch (Vanilla 1,25). Das ist gewollt; `minecraft:jump_strength` bleibt unverändert.
- In Orbit-Dimensionen g = 0,005 (fast schwerelos). Mit `magnet_boots` auf festem Grund oder über einer `gravity_plate`: g = 1.
- Rover und Raketen-Entities nutzen dasselbe g.

## 12.10 Befehl `goto` (ab M3)

`/infinite_horizon goto <planet> [<x> <z>]` (Rechtestufe 2) bringt einen Spieler ohne Rakete in eine Planetendimension: in M3 als einziger Weg zum Mond, später für Tests. Der Landeplatz entsteht wie in 12.5 (`LandingService`, `landing_search` in `spaceflight.json`), Standard X/Z 0/0. `goto overworld` führt zum Weltspawn. Werte in `misc.json` (`commands`).
