# 26. Registry-Übersicht

**Dimensionen:** `moon`, `mars`, `venus`, `mercury`, `asteroid_belt`, `ceres`, `io`, `europa`, `titan`, `enceladus`, `triton`, `pluto`, `earth_orbit`, `venus_orbit`, `mars_orbit`, `mercury_orbit`, `jupiter_orbit`, `saturn_orbit`, `uranus_orbit`, `neptune_orbit`.

**Entities:** `rocket`, `lander`, `rover`, `rover_trailer`, `probe_entity`, `meteor`, `starship`, `seat` (unsichtbarer Sitz, 12.5), `sulfur_glob` (Projektil des Schwefelsalamanders), alle 15 Kreaturen aus Kapitel 18.

**Block-Entities:** alle Maschinen (auch `dome_generator_*` und `electric_lamp`), Generatoren, Speicher, Multiblock-Controller, Ports, `research_terminal`, `launch_control`, `landing_beacon`, `cloud_elevator`, `hydroponics_basin`, Stationsmodule, `cargo_terminal`, `logistics_computer`, `trading_post`, `shipyard_terminal`, `shipyard_dock`, `transmitter_core`. Raketenteile sind normale Blöcke ohne Block-Entity, außer `rocket_cargo_bay`, `rocket_tank_*` und `long_range_module` (speichern Fracht bzw. Treibstoff). Rover-Teile sind Items. Kabel, Rohre und Gehäuse haben keine Block-Entity (die Netze verwaltet `GridManager`).

**Weitere Blöcke ohne eigenes Kapitel:** `sulfur_trail` (18), `extinguished_torch` (8.5), `electric_lamp` (8.5), `pontoon` (18), `meteorite_ore` (19), `ancient_alloy` (13.4), `compost_soil` und `compost_farmland` (17).

**Menüs (GUI):** je Maschine eines, dazu `research_terminal`, `launch_control`, `star_map`, `rocket`, `rover`, `logistics_computer`, `shipyard_terminal`, `trading_post`, `planetary_atlas`, `engineers_handbook`.

**Data Components:** `sternenwerft:fluid` (Fluid + Menge), `oxygen`, `energy`, `suit_modules`, `sample` (Block + Dimension), `charge`, `nutrition`, `linked_pos`, `drive` (Antrieb einer Frachtrakete), `rover_data` (Fracht, Module und Energie eines abgebauten Rovers), `flights_left` (`heavy_freighter`), `chart_owner` (`star_chart`).

**Tags:** `sternenwerft:gases`, `needs_atmosphere` (Block-Tag, 8.5), `sampleable/ring0`, `sampleable/planet`, `analyzable` (die 15 Kreaturen aus 18), `basalt`, `crawler_food`, `vibrating`, `compostable`, `planet_soil`, alle `c:`-Materialtags.
