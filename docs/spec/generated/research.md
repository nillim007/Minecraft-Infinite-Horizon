<!-- Erzeugt von tools/render_docs.py aus spec/data. Nicht von Hand ändern. -->

# Forschung

## Stufen

| Stufe | Name | Kosten je Knoten | Tor |
| --- | --- | --- | --- |
| 1 | Raumfahrtprogramm | 10 | `{'type': 'has_block', 'block': 'infinite_horizon:research_terminal'}` |
| 2 | Nahe Welten | 25 | `{'type': 'enter_dimension', 'dimension': 'infinite_horizon:moon'}` |
| 3 | Orbit | 50 | `{'type': 'crafted_items', 'items': ['infinite_horizon:magnesium_ingot', 'infinite_horizon:tungsten_ingot'], 'fluids': ['infinite_horizon:helium']}` |
| 4 | Inneres System | 80 | `{'type': 'machine_completed', 'block': 'infinite_horizon:zero_g_fabricator', 'recipe': None}` |
| 5 | Äußeres System | 120 | `{'type': 'enter_dimension', 'dimension': 'infinite_horizon:saturn_orbit'}` |
| 6 | Endgame | 250 | `{'type': 'all', 'gates': [{'type': 'crafted_items', 'items': ['infinite_horizon:uranium_ingot'], 'fluids': []}, {'type': 'machine_completed', 'block': 'infinite_horizon:magnetic_trap', 'recipe': None}]}` |

## Stufe 1 (8 Knoten)

| Knoten | Name | Ast | Kosten | Voraussetzung | Schaltet frei |
| --- | --- | --- | --- | --- | --- |
| `solar_power` | Solarenergie | industry | 10 | – | `solar_panel` |
| `space_food` | Raumfahrtnahrung | life_support | 10 | – | `food_packer` |
| `suit_t1` | Druckanzug | life_support | 10 | – | `pressure_suit_helmet`, `pressure_suit_chestplate`, `pressure_suit_leggings`, `pressure_suit_boots`, `oxygen_canister`, `large_oxygen_canister`, `helmet_lamp` |
| `aluminum_wiring` | Aluminiumverkabelung | logistics | 10 | – | `aluminum_cable` |
| `gas_storage` | Gasspeicher | logistics | 10 | – | `pressure_bottle`, `transport_tank` |
| `launch_pad` | Startrampe | spaceflight | 10 | – | `launch_pad`, `launch_tower`, `fuel_pump`, `launch_control` |
| `moon_probe` | Mondsonde | spaceflight | 10 | `launch_pad`, `solar_power`, `gas_storage` | `probe`, probe_target: `moon` |
| `rocket_t1` | Rakete T1 | spaceflight | 10 | `moon_probe` | `rocket_capsule_t1`, `rocket_tank_t1`, `rocket_engine_t1`, `rocket_fin_t1`, `rocket_cargo_bay` |

## Stufe 2 (13 Knoten)

| Knoten | Name | Ast | Kosten | Voraussetzung | Schaltet frei |
| --- | --- | --- | --- | --- | --- |
| `cryogenics` | Kryotechnik | industry | 25 | `regolith_heater` | `cryo_liquefier`, `cooling_system` |
| `high_temperature` | Hochtemperatur | industry | 25 | – | `high_temperature_furnace`, `ceramic_plate`, `titanium_pipe` |
| `magnesium_alloy` | Magnesium-Legierung | industry | 25 | – | `magnesium_alloy`, `lightweight_plate` |
| `regolith_heater` | Regolith-Erhitzer | industry | 25 | – | `regolith_heater` |
| `dome_t1` | Kuppel I | life_support | 25 | – | `dome_generator_t1`, `oxygen_generator`, `dome_range_module`, `dome_efficiency_module`, `dome_shield_module`, `electric_lamp` |
| `dome_t2` | Kuppel II | life_support | 25 | `dome_t1`, `magnesium_alloy`, `cryogenics` | `dome_generator_t2` |
| `hydroponics` | Hydrokultur | life_support | 25 | – | `hydroponics_basin`, `compost_soil`, `compost` |
| `suit_t2` | Hitzeanzug | life_support | 25 | `high_temperature`, `cryogenics` | `heat_suit_helmet`, `heat_suit_chestplate`, `heat_suit_leggings`, `heat_suit_boots`, `cooling_module`, `armor_plating` |
| `gas_tanks` | Gastanks | logistics | 25 | – | `gas_tank` |
| `landing_beacon` | Landefeuer | spaceflight | 25 | – | `landing_beacon`, `aerostat_platform`, `cloud_elevator` |
| `planet_probes` | Planetensonden | spaceflight | 25 | – | probe_target: `mars`, probe_target: `venus`, probe_target: `mars_orbit`, probe_target: `venus_orbit` |
| `rocket_t2` | Rakete T2 | spaceflight | 25 | `magnesium_alloy`, `cryogenics`, `high_temperature` | `rocket_capsule_t2`, `rocket_tank_t2`, `rocket_engine_t2`, `rocket_fin_t2`, multiblock: `launch_pad_t2` |
| `rover` | Rover | spaceflight | 25 | – | `rover_kit`, `rover_scanner`, `rover_drill`, `rover_headlight`, `rover_pressure_cabin`, `rover_trailer` |

## Stufe 3 (9 Knoten)

| Knoten | Name | Ast | Kosten | Voraussetzung | Schaltet frei |
| --- | --- | --- | --- | --- | --- |
| `fusion` | Kernfusion | industry | 50 | `superconductors` | `fusion_reactor`, `fusion_casing` |
| `space_weather` | Weltraumwetter | industry | 50 | – | `space_weather_station` |
| `superconductors` | Supraleiter | industry | 50 | `station_modules` | `superconductor_wire`, `superconductor_cable` |
| `gravity` | Künstliche Schwerkraft | life_support | 50 | `fusion` | `gravity_plate` |
| `greenhouse` | Gewächshaus | life_support | 50 | `station_modules`, `hydroponics` | `greenhouse` |
| `tank_depot` | Tankdepot | logistics | 50 | – | `tank_depot` |
| `lander` | Landefähre | spaceflight | 50 | `station_core` | `lander` |
| `station_core` | Stationskern | spaceflight | 50 | `dome_t1`, `magnesium_alloy` | `station_core`, `station_hull`, `station_window`, `docking_bay`, `magnet_boots` |
| `station_modules` | Stationsmodule | spaceflight | 50 | `station_core` | `habitation_module`, `research_lab`, `zero_g_fabricator`, `solar_wing` |

## Stufe 4 (12 Knoten)

| Knoten | Name | Ast | Kosten | Voraussetzung | Schaltet frei |
| --- | --- | --- | --- | --- | --- |
| `gas_collection` | Gasgewinnung | industry | 80 | – | `gas_collector` |
| `lithium_storage` | Lithiumspeicher | industry | 80 | – | `lithium_battery`, `lithium_battery_bank`, `rover_lithium_pack` |
| `magnetic_trap` | Magnetfalle | industry | 80 | `gas_collection`, `iridium_chips` | `magnetic_trap`, `antimatter_cell` |
| `clone_chamber` | Klon-Kammer | life_support | 80 | `iridium_chips` | `clone_chamber` |
| `dome_t3` | Kuppel III | life_support | 80 | `dome_t2`, `iridium_chips` | `dome_generator_t3` |
| `eva` | Außeneinsatz | life_support | 80 | `lithium_storage` | `jetpack`, `heat_reflector` |
| `ice_drilling` | Eisbohrung | life_support | 80 | `lithium_storage` | `ice_drill`, `diving_module` |
| `radiation_shield` | Strahlenschutz | life_support | 80 | – | `radiation_shield` |
| `iridium_chips` | Iridium-Chips | logistics | 80 | – | `iridium_chip` |
| `advanced_probe` | Fortgeschrittene Sonde | spaceflight | 20 | – | `advanced_probe`, probe_target: `mercury_orbit`, probe_target: `mercury`, probe_target: `asteroid_belt`, probe_target: `ceres`, probe_target: `jupiter_orbit`, probe_target: `io`, probe_target: `europa`, probe_target: `saturn_orbit`, probe_target: `titan`, probe_target: `enceladus`, probe_target: `uranus_orbit`, probe_target: `neptune_orbit`, probe_target: `triton`, probe_target: `pluto` |
| `asteroid_anchor` | Asteroiden-Anker | spaceflight | 80 | – | `asteroid_anchor` |
| `long_range` | Fernflug | spaceflight | 80 | `lithium_storage`, `iridium_chips` | `long_range_module` |

## Stufe 5 (7 Knoten)

| Knoten | Name | Ast | Kosten | Voraussetzung | Schaltet frei |
| --- | --- | --- | --- | --- | --- |
| `drilling_rig` | Bohrturm | industry | 120 | – | `drilling_rig` |
| `fission` | Kernspaltung | industry | 120 | `iridium_chips` | `fission_reactor`, `fission_casing`, `fuel_rod` |
| `pump_station` | Pumpstation | industry | 120 | – | `pump_station`, `pontoon` |
| `fertilizer` | Dünger | life_support | 120 | `pump_station` | `ammonia_fertilizer` |
| `suit_t3` | Tiefraumanzug | life_support | 120 | `suit_t2`, `superconductors`, `lithium_storage` | `deep_space_suit_helmet`, `deep_space_suit_chestplate`, `deep_space_suit_leggings`, `deep_space_suit_boots`, `insulation_fabric`, `heating_module` |
| `cargo_logistics` | Frachtlogistik | logistics | 120 | `iridium_chips`, `lithium_storage` | `cargo_terminal`, `cargo_rocket`, `logistics_computer`, `trading_post` |
| `atomic_drive` | Atomantrieb | spaceflight | 120 | `fission` | `atomic_drive` |

## Stufe 6 (3 Knoten)

| Knoten | Name | Ast | Kosten | Voraussetzung | Schaltet frei |
| --- | --- | --- | --- | --- | --- |
| `heavy_freighter` | Schwerlast-Frachter | logistics | 250 | `cargo_logistics`, `atomic_drive` | `heavy_freighter` |
| `antimatter_drive` | Antimaterie-Antrieb | spaceflight | 250 | `magnetic_trap`, `fission` | `antimatter_drive` |
| `orbital_shipyard` | Orbitalwerft | spaceflight | 250 | `antimatter_drive` | `shipyard_terminal`, `shipyard_dock` |

