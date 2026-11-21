---
title: Montagetisch
title_en: Assembly Table
order: 80
milestone: 2
requires: ["infinite_horizon:assembly_table"]
icon: infinite_horizon:assembly_table
items: ["infinite_horizon:assembly_table", "infinite_horizon:circuit"]
summary: Hier entstehen alle wichtigen Bauteile
---
Der <item:infinite_horizon:assembly_table> ist das Herz deiner Werkstatt. Fast alles, was nach der ersten Phase kommt, entsteht hier: Schaltkreise, Maschinen, Raketenteile, Raumanzüge.

Der Montagetisch hat **9 Eingabeslots** ohne festes Muster, einen Fluidtank und 3 Ausgabeslots.

@machine infinite_horizon:assembly_table

@image ../images/screenshots/gui-assembly-table.png | Das Fenster des Montagetischs: 3 × 3 Eingabeslots ohne festes Muster, rechts Ausgaben, Eimerslots und Tank.

## Bauen

@recipes_for infinite_horizon:assembly_table

## Genug hineinlegen

Ein Rezept startet, sobald jede Zutat **mindestens** in der Rezeptmenge da ist. Mehr ist erlaubt; fremde Items in den Slots verhindern den Start.

- Eine Zutat darf mehrere Slots belegen. Die Mengen werden zusammengezählt.
- Verbraucht wird genau die Menge aus dem Rezept, der Rest bleibt liegen.
- Passen mehrere Rezepte, nimmt der Tisch das, das am meisten verbraucht. Beispiel: 4 <item:infinite_horizon:iron_plate> und 1 <item:infinite_horizon:copper_pipe> ergeben einen <item:infinite_horizon:fluid_port>, 2 oder 3 Platten und 1 Rohr <item:infinite_horizon:refinery_casing>.

## Der Schaltkreis

Der <item:infinite_horizon:circuit> steckt in fast jeder Maschine ab Phase 2. Bau dir früh eine kleine Produktion dafür auf.

@recipes_for infinite_horizon:circuit

## Alle Rezepte des Montagetischs

@recipes_type assembly

@tip Wenn der Pfeil nicht losläuft, obwohl alles da zu sein scheint: Prüfe, ob ein fremdes Item in einem Slot liegt und ob die Energieleiste für den ganzen Vorgang reicht.
