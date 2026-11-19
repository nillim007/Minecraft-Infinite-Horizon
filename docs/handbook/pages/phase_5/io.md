---
title: Io
title_en: Io
order: 80
milestone: 7
requires: ["infinite_horizon:io_crust"]
icon: infinite_horizon:io_sulfur_ore
items: ["infinite_horizon:io_crust", "infinite_horizon:io_sulfur_ore", "infinite_horizon:heat_scale"]
summary: Der Vulkanmond des Jupiter mit Schwefel und Lavaseen
planet: infinite_horizon:io
---
Io ist der innerste große Mond des Jupiter und voller Vulkane. Die Oberfläche aus <item:infinite_horizon:io_crust> ist kalt ({{value:planet:io.temperature.day}} °C), aber überall gibt es Lava. Die Schwerkraft beträgt {{value:planet:io.gravity}} g, und die Strahlung des Jupiter hat Stufe {{value:planet:io.radiation}}.

## Ausrüstung

- Überall: ein Raumanzug mit <item:infinite_horizon:radiation_shield>.
- In der Nähe von Lava steigt die Temperatur auf {{value:planet:io.zones.0.temperature}} °C. Dort brauchst du einen [[hitzeanzug|Hitzeanzug]] mit Strahlenschild.

## Gebiete

- **Schwefelebenen**, **Lavaseen** und **Vulkanfelder**. In den Vulkanfeldern stehen Vulkanschlote; wer einen betritt, bringt seiner Agentur Forschungspunkte als Erkundungsziel.
- <item:infinite_horizon:io_sulfur_ore> steckt überall in der Kruste und gibt Schwefel.

## Der Schwefelsalamander

In den Lavaseen und an den Vulkanschloten lebt der Schwefelsalamander. Er ist feuerfest und beschießt dich aus der Ferne mit Schwefelklumpen ({{value:creatures:infinite_horizon:sulfur_salamander.damage}} Schaden je Treffer). Besiegt lässt er manchmal eine <item:infinite_horizon:heat_scale> fallen.

@uses infinite_horizon:heat_scale

## Vulkanausbruch

Steht ein Vulkanschlot in der Nähe eines Spielers, kann er ausbrechen. Du wirst vorher gewarnt. Während des Ausbruchs regnet es im Umkreis von {{value:events:infinite_horizon:volcanic_eruption.effects.radius}} Blöcken um den Schlot Brocken aus Schwefelerz. Abstand ist der einzige Schutz.

@warning Die Brocken treffen hart. Verlass den Umkreis des Schlots, sobald die Warnung kommt.

@tip Nach dem Ausbruch liegt frisches Schwefelerz offen herum. Sammle es ein, bevor du weiterziehst.
