<!-- Erzeugt von tools/gen_wiki.py aus docs/handbook/ und spec/data. Nicht von Hand ändern. -->

[Startseite](Home.md) › Phase 1 · Erdindustrie › **Raffinerie**

# Raffinerie

Die Raffinerie ist dein erster **Multiblock**: ein Turm aus 45 Blöcken, der aus Erdöl **Kerosin** und **Kunststoff** macht. Kerosin ist Treibstoff, Kunststoff steckt in jedem Schaltkreis.

![Raffinerie](../images/refinery.svg)

| Steckbrief: Raffinerie |  |
| --- | --- |
| Verbrauch | 40 J/t |
| Dauer je Vorgang | 200 t (10 s) |
| Tanks | 16.000 mB, 16.000 mB |
| Upgrade-Slots | 2 |

## Was sie macht

| Ergebnis | Zutaten | Wo | Dauer | Energie |
| --- | --- | --- | --- | --- |
| 1 × Kunststoff, 600 mB Kerosin | 1.000 mB Erdöl | Raffinerie | 200 t (10 s) | 40 J/t |

## Der Aufbau

Die Raffinerie ist **3 Blöcke breit, 3 Blöcke tief und 5 Blöcke hoch**, ganz massiv.

1. Setz den Controller (Raffinerie) auf den Boden. Seine Vorderseite zeigt zu dir.
2. Der Controller ist die **Mitte der untersten Reihe auf der Vorderseite**. Der Turm reicht von ihm einen Block nach links und rechts, zwei Blöcke nach hinten und vier Blöcke nach oben.
3. Füll alle übrigen 44 Plätze mit Raffinerie-Gehäuse.
4. Ersetze einzelne Gehäuseblöcke durch **Ports**, wo Rohre und Kabel andocken sollen (siehe unten).
5. Sobald alles stimmt, springt die Statuslampe von Rot auf Gelb.

> [!TIP]
> **Sneak-Rechtsklick** mit leerer Hand auf den Controller zeigt dir 200 t (10 s) lang mit roten Partikeln, wo noch Blöcke fehlen oder falsch sind.

## Ports

Ports ersetzen Gehäuseblöcke und leiten nach außen an den Controller weiter. Rechtsklick mit leerer Hand schaltet zwischen **Eingang** und **Ausgang** um.

- Fluid-Port – Erdöl hinein (Eingang), Kerosin heraus (Ausgang).
- Item-Port – Kunststoff heraus (Ausgang).
- Energie-Port – Strom hinein.

| Ergebnis | Zutaten | Wo | Dauer | Energie |
| --- | --- | --- | --- | --- |
| 1 × Raffinerie | 1 × [Maschinenrahmen](Erste-Schritte.md), 16 × [Eisenplatte](Presse.md), 8 × [Kupferrohr](Fluide-und-Rohre.md), 4 × Redstone-Staub | Montagetisch | 200 t (10 s) | 30 J/t |
| 4 × Raffinerie-Gehäuse | 2 × [Eisenplatte](Presse.md), 1 × [Kupferrohr](Fluide-und-Rohre.md) | Montagetisch | 200 t (10 s) | 30 J/t |
| 1 × Item-Port | 4 × [Eisenplatte](Presse.md), 1 × Trichter | Montagetisch | 200 t (10 s) | 30 J/t |
| 1 × Fluid-Port | 4 × [Eisenplatte](Presse.md), 1 × [Kupferrohr](Fluide-und-Rohre.md) | Montagetisch | 200 t (10 s) | 30 J/t |
| 1 × Energie-Port | 4 × [Eisenplatte](Presse.md), 1 × [Kupferkabel](Strom.md) | Montagetisch | 200 t (10 s) | 30 J/t |

## Materialbedarf

Für 44 Gehäuseblöcke brauchst du 11 Montagevorgänge, also 22 Eisenplatten und 11 Kupferrohre. Jeder Port spart dir einen Gehäuseblock.

> [!WARNING]
> Andere Blöcke im Aufbau sind nicht erlaubt. Auch ein einzelner falscher Block lässt die Lampe rot leuchten. Fahr im Fenster mit der Maus über die Lampe: Dort steht der erste fehlende Block mit Koordinate.

---

_Diese Seite steht auch im Spiel im **Ingenieurs-Handbuch**._

← [Elektrolyseur](Elektrolyseur.md) · [Upgrades](Upgrades.md) →
