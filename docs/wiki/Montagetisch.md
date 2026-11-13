<!-- Erzeugt von tools/gen_wiki.py aus docs/handbook/ und spec/data. Nicht von Hand ändern. -->

[📖 Wiki](README.md) › Phase 1 · Erdindustrie › **Montagetisch**

# Montagetisch

Der Montagetisch ist das Herz deiner Werkstatt. Fast alles, was nach der ersten Phase kommt, entsteht hier: Schaltkreise, Maschinen, Raketenteile, Raumanzüge.

Der Montagetisch hat **9 Eingabeslots** ohne festes Muster, einen Fluidtank und 3 Ausgabeslots.

| Steckbrief: Montagetisch |  |
| --- | --- |
| Verbrauch | 30 J/t |
| Dauer je Vorgang | 200 t (10 s) |
| Slots | 9 Eingabe, 3 Ausgabe |
| Tanks | 16.000 mB |
| Upgrade-Slots | 2 |

![Das Fenster des Montagetischs: 3 × 3 Eingabeslots ohne festes Muster, rechts Ausgaben, Eimerslots und Tank.](../images/screenshots/gui-assembly-table.png)

<sub>Das Fenster des Montagetischs: 3 × 3 Eingabeslots ohne festes Muster, rechts Ausgaben, Eimerslots und Tank.</sub>

## Bauen

![Rezept: Montagetisch](images/rezepte/crafting_shaped_assembly_table.svg)

<sub>2 × Kupferbarren, 4 × Eisenbarren, 1 × [Maschinenrahmen](Erste-Schritte.md), 1 × Redstone-Staub, 1 × Werkbank → 1 × Montagetisch</sub>

## Genau abmessen

Der Montagetisch ist pingelig: Ein Rezept startet nur, wenn in den Slots **genau** die Zutaten in **genau** der richtigen Menge liegen. Liegt etwas zu viel oder etwas anderes in einem Slot, passiert nichts.

- Eine Zutat darf mehrere Slots belegen. Die Mengen werden zusammengezählt.
- Verbraucht wird genau die Menge aus dem Rezept.
- Beispiel: 4 [Eisenplatte](Presse.md) und 1 [Kupferrohr](Fluide-und-Rohre.md) ergeben einen [Fluid-Port](Raffinerie.md). Mit 2 Platten und 1 Rohr bekommst du dagegen [Raffinerie-Gehäuse](Raffinerie.md).

## Der Schaltkreis

Der Schaltkreis steckt in fast jeder Maschine ab Phase 2. Bau dir früh eine kleine Produktion dafür auf.

![Rezept: Schaltkreis](images/rezepte/assembly_circuit.svg)

<sub>2 × [Silizium](Elektroofen.md), 4 × [Kupferdraht](Presse.md), 1 × [Kunststoff](Raffinerie.md) → 1 × Schaltkreis</sub>

## Alle Rezepte des Montagetischs

| Rezept | Zutaten → Ergebnis | Wo | Dauer |
| --- | --- | --- | --- |
| <img src="images/rezepte/assembly_aerostat_platform.svg" alt="Rezept" width="300"> | 2 × [Aluminiumplatte](Presse.md), 1 × [Kunststoff](Raffinerie.md) → 4 × [Aerostat-Plattform](Die-Venus.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_aluminum_cable.svg" alt="Rezept" width="300"> | 3 × [Aluminiumdraht](Presse.md), 1 × [Kunststoff](Raffinerie.md) → 8 × [Aluminiumkabel](Strom-im-All.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_analyzer.svg" alt="Rezept" width="300"> | 1 × [Maschinenrahmen](Erste-Schritte.md), 4 × [Aluminiumplatte](Presse.md), 2 × Schaltkreis, 1 × Glas → 1 × [Analysator](Forschung.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_pressure_suit_chestplate.svg" alt="Rezept" width="300"> | 10 × [Aluminiumplatte](Presse.md), 3 × [Kunststoff](Raffinerie.md), 1 × Schaltkreis → 1 × [Druckanzug-Brustplatte](Der-Druckanzug.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_pressure_suit_helmet.svg" alt="Rezept" width="300"> | 4 × [Aluminiumplatte](Presse.md), 1 × [Kunststoff](Raffinerie.md), 4 × Glas, 1 × Schaltkreis → 1 × [Druckanzug-Helm](Der-Druckanzug.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_pressure_suit_leggings.svg" alt="Rezept" width="300"> | 6 × [Aluminiumplatte](Presse.md), 2 × [Kunststoff](Raffinerie.md) → 1 × [Druckanzug-Hose](Der-Druckanzug.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_pressure_suit_boots.svg" alt="Rezept" width="300"> | 4 × [Aluminiumplatte](Presse.md), 2 × [Kunststoff](Raffinerie.md) → 1 × [Druckanzug-Stiefel](Der-Druckanzug.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_pressure_bottle.svg" alt="Rezept" width="300"> | 4 × [Aluminiumplatte](Presse.md), 1 × [Kupferrohr](Fluide-und-Rohre.md) → 1 × [Druckflasche](Sauerstoff-speichern.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_pressure_hull.svg" alt="Rezept" width="300"> | 4 × [Aluminiumplatte](Presse.md), 1 × [Titanplatte](Presse.md) → 1 × [Druckhülle](Sauerstoffgenerator.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_efficiency_upgrade.svg" alt="Rezept" width="300"> | 1 × Schaltkreis, 2 × [Golddraht](Presse.md), 2 × [Aluminiumplatte](Presse.md) → 1 × [Effizienz-Upgrade](Upgrades.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_electric_lamp.svg" alt="Rezept" width="300"> | 1 × [Aluminiumplatte](Presse.md), 1 × Glowstonestaub, 1 × [Kupferdraht](Presse.md) → 4 × [Elektrische Lampe](Leben-ohne-Luft.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_electrolyzer.svg" alt="Rezept" width="300"> | 1 × [Maschinenrahmen](Erste-Schritte.md), 4 × [Kupferdraht](Presse.md), 2 × [Eisenplatte](Presse.md), 2 × [Kupferrohr](Fluide-und-Rohre.md), 1 × Glas → 1 × [Elektrolyseur](Elektrolyseur.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_energy_port.svg" alt="Rezept" width="300"> | 4 × [Eisenplatte](Presse.md), 1 × [Kupferkabel](Strom.md) → 1 × [Energie-Port](Raffinerie.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_fluid_port.svg" alt="Rezept" width="300"> | 4 × [Eisenplatte](Presse.md), 1 × [Kupferrohr](Fluide-und-Rohre.md) → 1 × [Fluid-Port](Raffinerie.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_research_terminal.svg" alt="Rezept" width="300"> | 1 × [Maschinenrahmen](Erste-Schritte.md), 4 × [Aluminiumplatte](Presse.md), 4 × Schaltkreis, 2 × Glas, 1 × Buch → 1 × [Forschungsterminal](Forschung.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_advanced_circuit.svg" alt="Rezept" width="300"> | 2 × Schaltkreis, 4 × [Golddraht](Presse.md), 2 × [Silizium](Elektroofen.md) → 1 × [Fortgeschrittener Schaltkreis](Der-Hochtemperaturofen.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_gas_tank.svg" alt="Rezept" width="300"> | 8 × [Aluminiumplatte](Presse.md), 2 × [Druckhülle](Sauerstoffgenerator.md) → 1 × [Gastank](Helium-und-Kryotechnik.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_speed_upgrade.svg" alt="Rezept" width="300"> | 1 × Schaltkreis, 4 × Redstone-Staub, 2 × [Kupferdraht](Presse.md) → 1 × [Geschwindigkeits-Upgrade](Upgrades.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_large_oxygen_canister.svg" alt="Rezept" width="300"> | 2 × [Sauerstoffflasche](Sauerstoff-speichern.md), 1 × [Titanplatte](Presse.md) → 1 × [Große Sauerstoffflasche](Sauerstoff-speichern.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_helmet_lamp.svg" alt="Rezept" width="300"> | 2 × [Aluminiumplatte](Presse.md), 1 × Glowstonestaub, 1 × Schaltkreis → 1 × [Helmlampe](Der-Druckanzug.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_heat_suit_chestplate.svg" alt="Rezept" width="300"> | 1 × [Druckanzug-Brustplatte](Der-Druckanzug.md), 6 × [Keramikplatte](Der-Hochtemperaturofen.md), 2 × [Titanplatte](Presse.md), 2 × [Kühlsystem](Helium-und-Kryotechnik.md) → 1 × [Hitzeanzug-Brustplatte](Der-Hitzeanzug.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_heat_suit_helmet.svg" alt="Rezept" width="300"> | 1 × [Druckanzug-Helm](Der-Druckanzug.md), 3 × [Keramikplatte](Der-Hochtemperaturofen.md), 2 × [Titanplatte](Presse.md) → 1 × [Hitzeanzug-Helm](Der-Hitzeanzug.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_heat_suit_leggings.svg" alt="Rezept" width="300"> | 1 × [Druckanzug-Hose](Der-Druckanzug.md), 4 × [Keramikplatte](Der-Hochtemperaturofen.md), 2 × [Titanplatte](Presse.md) → 1 × [Hitzeanzug-Hose](Der-Hitzeanzug.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_heat_suit_boots.svg" alt="Rezept" width="300"> | 1 × [Druckanzug-Stiefel](Der-Druckanzug.md), 3 × [Keramikplatte](Der-Hochtemperaturofen.md), 2 × [Titanplatte](Presse.md) → 1 × [Hitzeanzug-Stiefel](Der-Hitzeanzug.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_heat_shield.svg" alt="Rezept" width="300"> | 4 × [Wolframplatte](Die-Rakete-T2.md), 2 × [Titanplatte](Presse.md) → 1 × [Hitzeschild](Die-Rakete-T2.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_high_temperature_furnace.svg" alt="Rezept" width="300"> | 1 × [Elektroofen](Elektroofen.md), 8 × [Titanplatte](Presse.md), 4 × Ziegel, 2 × [Fortgeschrittener Schaltkreis](Der-Hochtemperaturofen.md) → 1 × [Hochtemperaturofen](Der-Hochtemperaturofen.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_item_port.svg" alt="Rezept" width="300"> | 4 × [Eisenplatte](Presse.md), 1 × Trichter → 1 × [Item-Port](Raffinerie.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_cryo_liquefier.svg" alt="Rezept" width="300"> | 1 × [Maschinenrahmen](Erste-Schritte.md), 8 × [Aluminiumplatte](Presse.md), 4 × [Kupferrohr](Fluide-und-Rohre.md), 2 × [Fortgeschrittener Schaltkreis](Der-Hochtemperaturofen.md), 1 × [Druckhülle](Sauerstoffgenerator.md) → 1 × [Kryo-Verflüssiger](Helium-und-Kryotechnik.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_dome_generator_t1.svg" alt="Rezept" width="300"> | 16 × [Aluminiumplatte](Presse.md), 8 × [Titanplatte](Presse.md), 4 × Schaltkreis, 4 × Glas → 1 × [Kuppelgenerator I](Die-Kuppel.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_cooling_system.svg" alt="Rezept" width="300"> | 4 × [Kupferrohr](Fluide-und-Rohre.md), 1 × [Druckhülle](Sauerstoffgenerator.md), 1.000 mB [Flüssiges Helium](Helium-und-Kryotechnik.md) → 1 × [Kühlsystem](Helium-und-Kryotechnik.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_landing_beacon.svg" alt="Rezept" width="300"> | 2 × [Aluminiumplatte](Presse.md), 1 × Schaltkreis, 1 × Redstone-Lampe → 1 × [Landefeuer](Das-Landefeuer.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_sample_tool.svg" alt="Rezept" width="300"> | 2 × [Titanstange](Presse.md), 1 × [Eisenplatte](Presse.md) → 1 × [Probenzange](Forschung.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_refinery_controller.svg" alt="Rezept" width="300"> | 1 × [Maschinenrahmen](Erste-Schritte.md), 16 × [Eisenplatte](Presse.md), 8 × [Kupferrohr](Fluide-und-Rohre.md), 4 × Redstone-Staub → 1 × [Raffinerie](Raffinerie.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_refinery_casing.svg" alt="Rezept" width="300"> | 2 × [Eisenplatte](Presse.md), 1 × [Kupferrohr](Fluide-und-Rohre.md) → 4 × [Raffinerie-Gehäuse](Raffinerie.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_rocket_cargo_bay.svg" alt="Rezept" width="300"> | 16 × [Aluminiumplatte](Presse.md), 2 × [Titanplatte](Presse.md), 1 × Truhe → 1 × [Raketen-Frachtraum](Die-Rakete.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_rocket_fin_t1.svg" alt="Rezept" width="300"> | 8 × [Aluminiumplatte](Presse.md) → 1 × [Raketenflosse T1](Die-Rakete.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_rocket_fin_t2.svg" alt="Rezept" width="300"> | 3 × [Leichtbau-Platte](Der-Hochtemperaturofen.md) → 1 × [Raketenflosse T2](Die-Rakete-T2.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_rocket_capsule_t1.svg" alt="Rezept" width="300"> | 40 × [Aluminiumplatte](Presse.md), 16 × [Titanplatte](Presse.md), 16 × Schaltkreis → 1 × [Raketenkapsel T1](Die-Rakete.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_rocket_capsule_t2.svg" alt="Rezept" width="300"> | 32 × [Leichtbau-Platte](Der-Hochtemperaturofen.md), 16 × [Hitzeschild](Die-Rakete-T2.md), 16 × [Fortgeschrittener Schaltkreis](Der-Hochtemperaturofen.md) → 1 × [Raketenkapsel T2](Die-Rakete-T2.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_rocket_tank_t1.svg" alt="Rezept" width="300"> | 30 × [Aluminiumplatte](Presse.md), 4 × [Titanplatte](Presse.md), 2 × Schaltkreis → 1 × [Raketentank T1](Die-Rakete.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_rocket_tank_t2.svg" alt="Rezept" width="300"> | 12 × [Leichtbau-Platte](Der-Hochtemperaturofen.md), 4 × [Hitzeschild](Die-Rakete-T2.md) → 1 × [Raketentank T2](Die-Rakete-T2.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_rocket_engine_t1.svg" alt="Rezept" width="300"> | 8 × [Aluminiumplatte](Presse.md), 32 × [Titanplatte](Presse.md), 8 × Schaltkreis → 1 × [Raketentriebwerk T1](Die-Rakete.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_rocket_engine_t2.svg" alt="Rezept" width="300"> | 16 × [Leichtbau-Platte](Der-Hochtemperaturofen.md), 24 × [Hitzeschild](Die-Rakete-T2.md), 16 × [Fortgeschrittener Schaltkreis](Der-Hochtemperaturofen.md), 4 × [Kühlsystem](Helium-und-Kryotechnik.md) → 1 × [Raketentriebwerk T2](Die-Rakete-T2.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_regolith_heater.svg" alt="Rezept" width="300"> | 1 × [Maschinenrahmen](Erste-Schritte.md), 8 × [Aluminiumplatte](Presse.md), 4 × [Kupferdraht](Presse.md), 2 × Schaltkreis, 1 × [Druckflasche](Sauerstoff-speichern.md) → 1 × [Regolith-Erhitzer](Helium-und-Kryotechnik.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_oxygen_canister.svg" alt="Rezept" width="300"> | 2 × [Aluminiumplatte](Presse.md), 1 × [Kupferrohr](Fluide-und-Rohre.md) → 1 × [Sauerstoffflasche](Sauerstoff-speichern.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_oxygen_generator.svg" alt="Rezept" width="300"> | 8 × [Aluminiumplatte](Presse.md), 4 × [Kupferrohr](Fluide-und-Rohre.md), 2 × Schaltkreis, 1 × [Druckhülle](Sauerstoffgenerator.md) → 1 × [Sauerstoffgenerator](Sauerstoffgenerator.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_circuit.svg" alt="Rezept" width="300"> | 2 × [Silizium](Elektroofen.md), 4 × [Kupferdraht](Presse.md), 1 × [Kunststoff](Raffinerie.md) → 1 × Schaltkreis | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_solar_panel.svg" alt="Rezept" width="300"> | 4 × [Silizium](Elektroofen.md), 2 × [Aluminiumplatte](Presse.md), 4 × [Kupferdraht](Presse.md), 1 × Glas → 1 × [Solarpanel](Strom-im-All.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_probe.svg" alt="Rezept" width="300"> | 8 × [Aluminiumplatte](Presse.md), 2 × Schaltkreis, 1 × [Solarpanel](Strom-im-All.md), 16.000 mB [Kerosin](Raffinerie.md) → 1 × [Sonde](Startrampe-und-Sonde.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_launch_control.svg" alt="Rezept" width="300"> | 1 × [Maschinenrahmen](Erste-Schritte.md), 4 × Schaltkreis, 4 × [Aluminiumplatte](Presse.md), 2 × Glas → 1 × [Startkontrolle](Startrampe-und-Sonde.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_launch_pad.svg" alt="Rezept" width="300"> | 2 × [Eisenplatte](Presse.md), 1 × [Titanplatte](Presse.md) → 2 × [Startrampe](Startrampe-und-Sonde.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_launch_tower.svg" alt="Rezept" width="300"> | 4 × [Eisenstange](Presse.md), 1 × [Eisenplatte](Presse.md) → 2 × [Startturm](Startrampe-und-Sonde.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_titanium_pipe.svg" alt="Rezept" width="300"> | 3 × [Titanplatte](Presse.md), 1 × Glas → 4 × [Titanrohr](Der-Hochtemperaturofen.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_transport_tank.svg" alt="Rezept" width="300"> | 4 × [Aluminiumplatte](Presse.md), 1 × [Druckhülle](Sauerstoffgenerator.md) → 1 × [Transporttank](Sauerstoff-speichern.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_fuel_generator.svg" alt="Rezept" width="300"> | 1 × [Maschinenrahmen](Erste-Schritte.md), 1 × [Verbrennungsgenerator](Strom.md), 4 × [Kupferrohr](Fluide-und-Rohre.md), 4 × [Eisenplatte](Presse.md) → 1 × [Treibstoffgenerator](Strom.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_fuel_pump.svg" alt="Rezept" width="300"> | 1 × [Maschinenrahmen](Erste-Schritte.md), 4 × [Kupferrohr](Fluide-und-Rohre.md), 2 × [Aluminiumplatte](Presse.md), 1 × Schaltkreis → 1 × [Treibstoffpumpe](Startrampe-und-Sonde.md) | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_cloud_elevator.svg" alt="Rezept" width="300"> | 8 × [Aluminiumplatte](Presse.md), 4 × [Titanplatte](Presse.md), 2 × Schaltkreis, 1 × [Kunststoff](Raffinerie.md) → 1 × [Wolkenaufzug](Die-Venus.md) | Montagetisch | 200 t (10 s)<br>30 J/t |

> [!TIP]
> Wenn der Pfeil nicht losläuft, obwohl alles da zu sein scheint: Nimm alles heraus und lege die Zutaten in genau der Rezeptmenge wieder hinein.

---

_Diese Seite steht auch im Spiel im **Ingenieurs-Handbuch**._

← [Fluide und Rohre](Fluide-und-Rohre.md) · [Elektrolyseur](Elektrolyseur.md) →
