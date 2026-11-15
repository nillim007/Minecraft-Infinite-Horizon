---
title: Lithium
title_en: Lithium
order: 30
milestone: 7
requires: ["infinite_horizon:lithium_ingot"]
icon: infinite_horizon:lithium_battery
items: ["infinite_horizon:lithium_salt", "infinite_horizon:lithium_dust", "infinite_horizon:lithium_ingot", "infinite_horizon:lithium_battery", "infinite_horizon:lithium_battery_bank", "infinite_horizon:rover_lithium_pack"]
summary: Lithiumsalz, Lithium-Akku, Akkubank, Rover-Akku
---
Lithium kommt als Salzkruste vom [[merkur|Merkur]]. Der Zerkleinerer macht daraus Staub, der Elektroofen Barren. Aus den Barren baust du den <item:infinite_horizon:lithium_battery>, ein Bauteil fast aller Geräte der vierten Phase.

@recipes_for infinite_horizon:lithium_dust infinite_horizon:lithium_ingot infinite_horizon:lithium_battery

## Lithium-Akkubank

Die <item:infinite_horizon:lithium_battery_bank> speichert {{value:storage:infinite_horizon:lithium_battery_bank.capacity}} J und gibt bis {{value:storage:infinite_horizon:lithium_battery_bank.io_per_tick}} J/t ab, über ihre Ausgangsseite wie der Akku.

@machine infinite_horizon:lithium_battery_bank

## Rover-Akku

Der <item:infinite_horizon:rover_lithium_pack> ist ein Modul für den [[rover|Rover]]: Er fasst damit das {{value:vehicles:infinite_horizon:rover_lithium_pack.energy_multiplier}}-Fache an Energie.

@recipes_for infinite_horizon:lithium_battery_bank infinite_horizon:rover_lithium_pack

@tip Lithiumsalz gibt im Zerkleinerer und Elektroofen gleich viel Lithium. Spar dir den Zerkleinerer, solange du ihn für Erze brauchst.
