---
title: Montagetisch
title_en: Assembly Table
order: 80
milestone: 2
requires: ["sternenwerft:assembly_table"]
icon: sternenwerft:assembly_table
items: ["sternenwerft:assembly_table", "sternenwerft:circuit"]
summary: Hier entstehen alle wichtigen Bauteile
---
Der <item:sternenwerft:assembly_table> ist das Herz deiner Werkstatt. Fast alles, was nach der ersten Phase kommt, entsteht hier: Schaltkreise, Maschinen, Raketenteile, Raumanzüge.

Der Montagetisch hat **9 Eingabeslots** ohne festes Muster, einen Fluidtank und 3 Ausgabeslots.

@machine sternenwerft:assembly_table

## Bauen

@recipes_for sternenwerft:assembly_table

## Genau abmessen

Der Montagetisch ist pingelig: Ein Rezept startet nur, wenn in den Slots **genau** die Zutaten in **genau** der richtigen Menge liegen. Liegt etwas zu viel oder etwas anderes in einem Slot, passiert nichts.

- Eine Zutat darf mehrere Slots belegen. Die Mengen werden zusammengezählt.
- Verbraucht wird genau die Menge aus dem Rezept.
- Beispiel: 4 <item:sternenwerft:iron_plate> und 1 <item:sternenwerft:copper_pipe> ergeben einen <item:sternenwerft:fluid_port>. Mit 2 Platten und 1 Rohr bekommst du dagegen <item:sternenwerft:refinery_casing>.

## Der Schaltkreis

Der <item:sternenwerft:circuit> steckt in fast jeder Maschine ab Phase 2. Bau dir früh eine kleine Produktion dafür auf.

@recipes_for sternenwerft:circuit

## Alle Rezepte des Montagetischs

@recipes_type assembly

@tip Wenn der Pfeil nicht losläuft, obwohl alles da zu sein scheint: Nimm alles heraus und lege die Zutaten in genau der Rezeptmenge wieder hinein.
