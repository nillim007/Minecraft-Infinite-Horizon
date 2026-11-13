<!-- Erzeugt von tools/gen_wiki.py aus docs/handbook/ und spec/data. Nicht von Hand ändern. -->

[📖 Wiki](README.md) › Phase 3 · Ring 1 › **Helium und Kryotechnik**

# Helium und Kryotechnik

Der Mondboden enthält Helium, das der Sonnenwind über Milliarden Jahre dort abgelegt hat. Mit Hitze holst du es heraus, mit Kälte machst du es flüssig.

## Regolith-Erhitzer

Der Regolith-Erhitzer erhitzt [Mond-Regolith](Der-Mond.md) und gibt Helium in seinen Tank ab. Er arbeitet **nur auf dem Mond**. An jedem anderen Ort leuchtet seine Lampe rot, und der Tooltip sagt dir, warum.

| Steckbrief: Regolith-Erhitzer |  |
| --- | --- |
| Verbrauch | 40 J/t |
| Dauer je Vorgang | 100 t (5 s) |
| Slots | 1 Eingabe, 0 Ausgabe |
| Tanks | 16.000 mB |
| Upgrade-Slots | 2 |
| Ort | {"dimension_type": null, "dimensions": ["infinite_horizon:moon"], "excluded_dimensions": []} |

## Kryo-Verflüssiger

Der Kryo-Verflüssiger kühlt Helium so stark, dass es flüssig wird. Flüssiges Helium braucht das Kühlsystem der Hitzeanzüge.

| Steckbrief: Kryo-Verflüssiger |  |
| --- | --- |
| Verbrauch | 120 J/t |
| Dauer je Vorgang | 20 t (1 s) |
| Tanks | 16.000 mB, 16.000 mB |
| Upgrade-Slots | 2 |

## Gastank

Gase lagerst du im Gastank. Er nimmt nur Gase auf und gibt sie über eine wählbare Seite ab, wie der Flüssigkeitstank.

| Steckbrief: Gastank |  |
| --- | --- |
| Speicher | 64.000 mB |

## Herstellung

| Rezept | Zutaten → Ergebnis | Wo | Dauer |
| --- | --- | --- | --- |
| <img src="images/rezepte/assembly_cooling_system.svg" alt="Rezept" width="300"> | 4 × [Kupferrohr](Fluide-und-Rohre.md), 1 × [Druckhülle](Sauerstoffgenerator.md), 1.000 mB Flüssiges Helium → 1 × Kühlsystem | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_regolith_heater.svg" alt="Rezept" width="300"> | 1 × [Maschinenrahmen](Erste-Schritte.md), 8 × [Aluminiumplatte](Presse.md), 4 × [Kupferdraht](Presse.md), 2 × [Schaltkreis](Montagetisch.md), 1 × [Druckflasche](Sauerstoff-speichern.md) → 1 × Regolith-Erhitzer | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_cryo_liquefier.svg" alt="Rezept" width="300"> | 1 × [Maschinenrahmen](Erste-Schritte.md), 8 × [Aluminiumplatte](Presse.md), 4 × [Kupferrohr](Fluide-und-Rohre.md), 2 × [Fortgeschrittener Schaltkreis](Der-Hochtemperaturofen.md), 1 × [Druckhülle](Sauerstoffgenerator.md) → 1 × Kryo-Verflüssiger | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_gas_tank.svg" alt="Rezept" width="300"> | 8 × [Aluminiumplatte](Presse.md), 2 × [Druckhülle](Sauerstoffgenerator.md) → 1 × Gastank | Montagetisch | 200 t (10 s)<br>30 J/t |

> [!TIP]
> Stell den Regolith-Erhitzer direkt in deine Mondbasis und leite das Helium mit Rohren in einen Gastank. Den Verflüssiger kannst du dann auch auf der Erde betreiben.

---

_Diese Seite steht auch im Spiel im **Ingenieurs-Handbuch**._

← [Der Hochtemperaturofen](Der-Hochtemperaturofen.md) · [Die Rakete T2](Die-Rakete-T2.md) →
