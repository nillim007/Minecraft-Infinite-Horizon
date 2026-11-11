<!-- Erzeugt von tools/gen_wiki.py aus docs/handbook/ und spec/data. Nicht von Hand ändern. -->

[📖 Wiki](README.md) › Phase 1 · Erdindustrie › **Strom**

# Strom

Alle Maschinen laufen mit Strom, gemessen in **Joule (J)**. Wie viel eine Maschine pro Tick braucht, steht als **J/t** auf ihrer Seite. Ein Tick ist eine Zwanzigstelsekunde.

## Verbrennungsgenerator

Der Verbrennungsgenerator ist dein erstes Kraftwerk. Er verbrennt alles, was auch im Ofen brennt, und liefert 20 J/t. Lavaeimer funktionieren auch; der leere Eimer bleibt im Slot liegen.

| Steckbrief: Verbrennungsgenerator |  |
| --- | --- |
| Leistung | 20 J/t |
| Energiepuffer | 20.000 J |
| Brennstoff | alles, was im Ofen brennt (z. B. Kohle: 1.600 t (80 s)) |

![Das Fenster des Verbrennungsgenerators: Brennstoffslot und Restbrennzeit.](../images/screenshots/gui-combustion-generator.png)

<sub>Das Fenster des Verbrennungsgenerators: Brennstoffslot und Restbrennzeit.</sub>

![Rezept: Verbrennungsgenerator](images/rezepte/crafting_shaped_combustion_generator.svg)

<sub>2 × Kupferbarren, 5 × Eisenbarren, 1 × Ofen, 1 × [Maschinenrahmen](Erste-Schritte.md) → 1 × Verbrennungsgenerator</sub>

## Treibstoffgenerator

Sobald deine [Raffinerie](Raffinerie.md) läuft, lohnt sich der Treibstoffgenerator. Er verbrennt Kerosin und liefert 80 J/t – ein Vielfaches des Verbrennungsgenerators.

| Steckbrief: Treibstoffgenerator |  |
| --- | --- |
| Leistung | 80 J/t |
| Energiepuffer | 80.000 J |
| Tanks | 16.000 mB |
| Brennstoff | 1 mB [Kerosin](Raffinerie.md) je 4 t |

![Rezept: Treibstoffgenerator](images/rezepte/assembly_fuel_generator.svg)

<sub>1 × [Maschinenrahmen](Erste-Schritte.md), 1 × Verbrennungsgenerator, 4 × [Kupferrohr](Fluide-und-Rohre.md), 4 × [Eisenplatte](Presse.md) → 1 × Treibstoffgenerator</sub>

## Kupferkabel

Das Kupferkabel verbindet sich von selbst mit allem, was Strom erzeugt oder braucht. Ein Kabelnetz überträgt höchstens 256 J/t, egal wie lang es ist. Generatoren schieben ihren Strom in alle Seiten, das Netz verteilt ihn gleichmäßig auf alle Maschinen.

| Steckbrief: Kupferkabel |  |
| --- | --- |
| Durchsatz | 256 J/t |

![Rezept: Kupferkabel](images/rezepte/crafting_shaped_copper_cable.svg)

<sub>3 × Kupferbarren → 6 × Kupferkabel</sub>

## Akku

Der Akku speichert Strom für die Nacht oder für Lastspitzen. Er nimmt auf allen Seiten Strom an und gibt ihn nur über seine **Ausgangsseite** ab (Knopf „Seite“ im Fenster). So kann er zwei Netze sauber trennen.

| Steckbrief: Akku |  |
| --- | --- |
| Speicher | 500.000 J |
| Ein/Aus | 512 J/t |

![Rezept: Akku](images/rezepte/crafting_shaped_battery.svg)

<sub>4 × Redstone-Block, 4 × Kupferbarren, 1 × [Maschinenrahmen](Erste-Schritte.md) → 1 × Akku</sub>

> [!TIP]
> Ein Kupferkabel-Netz trägt 256 J/t. Reicht das nicht, teile deine Maschinen auf mehrere Netze mit eigenem Generator auf.

> [!NOTE]
> Maschinen verbrauchen nur Strom, wenn sie arbeiten. Wird der Strom knapp, pausieren sie und machen später an derselben Stelle weiter. Nichts geht verloren.

---

_Diese Seite steht auch im Spiel im **Ingenieurs-Handbuch**._

← [Rohstoffe und Erze](Rohstoffe-und-Erze.md) · [Zerkleinerer](Zerkleinerer.md) →
