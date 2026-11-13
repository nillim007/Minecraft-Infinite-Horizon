<!-- Erzeugt von tools/gen_wiki.py aus docs/handbook/ und spec/data. Nicht von Hand ändern. -->

[📖 Wiki](README.md) › Phase 2 · Raumfahrtprogramm › **Forschung**

# Forschung

Mit der Forschung beginnt dein Raumfahrtprogramm. Am Forschungsterminal gibt deine [Agentur](Deine-Agentur.md) **Forschungspunkte (FP)** aus und schaltet damit neue Bauteile und Ziele frei. Die Forschung gehört der ganzen Agentur: Was ein Mitglied erforscht, können alle bauen.

## Das Forschungsterminal

1. Bau das Terminal am Montagetisch (Rezept unten) und stell es in deine Basis. Es braucht keinen Strom.
2. Sobald es steht, öffnet sich **Stufe 1** der Forschung für deine Agentur, und du bekommst den Erfolg „Bodenstation“.
3. Rechtsklick öffnet den **Forschungsbaum**: vier Spalten für die Äste Industrie und Energie, Raumfahrt, Lebenserhaltung und Logistik, sechs Zeilen für die Stufen. Die Fläche lässt sich mit der Maus ziehen und scrollen (mit Umschalt waagerecht).
4. Oben rechts stehen die FP deiner Agentur.

Jeder Knoten zeigt sein Symbol mit einem farbigen Rahmen:

| Rahmen | Bedeutung |
| --- | --- |
| grau | gesperrt: Stufe noch zu oder Voraussetzung fehlt |
| gelb | verfügbar: ein Klick erforscht ihn |
| grün | erforscht |

Der Tooltip nennt Kosten, Freischaltungen und fehlende Voraussetzungen. Erforschen zieht die FP sofort ab. Forschen dürfen Leiter und Ingenieure der Agentur.

## Stufen

Eine Stufe öffnet sich, wenn die vorige offen ist und ihr **Tor** erfüllt ist. Offene Stufen schließen sich nie wieder, auch wenn du das Terminal abbaust. Die Zeilenköpfe im Baum zeigen jedes Tor und einen Haken, sobald die Stufe offen ist.

| Stufe | Tor | Standardkosten je Knoten |
| --- | --- | --- |
| 1 Raumfahrtprogramm | ein eigenes Forschungsterminal setzen | 10 FP |
| 2 Nahe Welten | ein Mitglied betritt den Mond | 25 FP |

Die Stufen öffnen auch die Kapitel dieses Handbuchs: Die dritte Phase („Ring 1“) liest du ab Stufe 2.

## Gesperrte Rezepte

Ein Knoten schaltet **alle Rezepte** frei, deren Ergebnis er nennt. Solange deine Agentur ihn nicht erforscht hat, bauen deine Maschinen und dein Montagetisch diese Dinge nicht: Die Statuslampe bleibt gelb, und ihr Tooltip meldet „Gesperrt“. Was in keinem Knoten steht, kannst du immer herstellen, zum Beispiel Schaltkreise, Upgrades, Ports, das Terminal selbst und die Sonde.

Das betrifft auch zehn Dinge, die du vielleicht schon gebaut hast:

| Knoten | Stufe | Kosten | Schaltet frei |
| --- | --- | --- | --- |
| Druckanzug | 1 | 10 FP | [Druckanzug-Helm](Der-Druckanzug.md), [Druckanzug-Brustplatte](Der-Druckanzug.md), [Druckanzug-Hose](Der-Druckanzug.md), [Druckanzug-Stiefel](Der-Druckanzug.md), [Sauerstoffflasche](Sauerstoff-speichern.md), [Große Sauerstoffflasche](Sauerstoff-speichern.md) |
| Gasspeicher | 1 | 10 FP | [Druckflasche](Sauerstoff-speichern.md) |
| Kuppel I | 2 | 25 FP | [Kuppelgenerator I](Die-Kuppel.md), [Sauerstoffgenerator](Sauerstoffgenerator.md), [Elektrische Lampe](Leben-ohne-Luft.md) |

> [!NOTE]
> Schon gebaute Blöcke und fertige Anzüge laufen weiter. Gesperrt ist nur das Herstellen.

> [!WARNING]
> Für die erste Reise zum Mond brauchst du den Druckanzug. Erforsche den Knoten **Druckanzug** früh, sonst baut dein Montagetisch keine neuen Anzugteile.

## Forschungspunkte

FP gehören der Agentur und werden mit zwei Nachkommastellen angezeigt. Du bekommst sie auf zwei Wegen: aus **Proben**, die der Analysator auswertet, und aus **Erkundungszielen**.

### Proben nehmen

1. Bau eine Probenzange (Rezept unten).
2. Rechtsklick auf einen Block gibt eine Probe. Der Block bleibt stehen. Danach braucht die Zange 40 t (2 s) Pause.
3. Jede Stelle gibt nur eine Probe. Blöcke, die ein Spieler gesetzt hat, geben nie eine: Die Aktionsleiste meldet dann „Keine verwertbare Probe“.

Auf der Erde lassen sich nur Erze beproben: [Bauxiterz](Rohstoffe-und-Erze.md), [Titanerz](Rohstoffe-und-Erze.md), [Quarzsand](Rohstoffe-und-Erze.md) und die Vanilla-Erze Kohle, Eisen, Kupfer, Gold, Redstone, Lapis und Diamant. Tiefenschiefer-Varianten zählen wie das normale Erz. Auf dem [Mond](Der-Mond.md) gibt jeder natürliche Mondblock eine Probe. Proben aus dem Nether und dem End sind wertlos.

### Der Analysator

Der Analysator wertet je Vorgang eine Probe aus und schreibt die FP deiner Agentur gut. Er braucht Strom und nimmt nur Proben an, die FP geben. Ein Analysator, der niemandem gehört, arbeitet nicht (rote Lampe); setz ihn deshalb selbst.

| Steckbrief: Analysator |  |
| --- | --- |
| Verbrauch | 20 J/t |
| Dauer je Vorgang | 100 t (5 s) |
| Slots | 1 Eingabe, 0 Ausgabe |
| Upgrade-Slots | 2 |

| Herkunft der Probe | FP je Probe |
| --- | --- |
| Erde | 1 |
| Mond | 5 |

Von jeder **Probenart** (Block und Fundort) zählen die ersten 10 Proben voll, jede weitere nur noch 10 %. Auf der Erde holst du so bis zu 100 FP mit vollem Wert.

### Erkundungsziele

Betritt ein Mitglied eine **Lavaröhre** auf dem Mond, bekommt die Agentur 20 FP, einmal je Röhre. Lavaröhren sind gerade Tunnel tief im Mare, zwischen y 30 und 50. Auch hier zählen die ersten 10 Funde voll.

Auf Servern kann die Forschungspflicht abgeschaltet sein (Einstellung `research_required`). Dann sind alle Rezepte und Handbuch-Kapitel frei, und der Baum zeigt einen Hinweis.

## Herstellung

| Rezept | Zutaten → Ergebnis | Wo | Dauer |
| --- | --- | --- | --- |
| <img src="images/rezepte/assembly_sample_tool.svg" alt="Rezept" width="300"> | 2 × [Titanstange](Presse.md), 1 × [Eisenplatte](Presse.md) → 1 × Probenzange | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_analyzer.svg" alt="Rezept" width="300"> | 1 × [Maschinenrahmen](Erste-Schritte.md), 4 × [Aluminiumplatte](Presse.md), 2 × [Schaltkreis](Montagetisch.md), 1 × Glas → 1 × Analysator | Montagetisch | 200 t (10 s)<br>30 J/t |
| <img src="images/rezepte/assembly_research_terminal.svg" alt="Rezept" width="300"> | 1 × [Maschinenrahmen](Erste-Schritte.md), 4 × [Aluminiumplatte](Presse.md), 4 × [Schaltkreis](Montagetisch.md), 2 × Glas, 1 × Buch → 1 × Forschungsterminal | Montagetisch | 200 t (10 s)<br>30 J/t |

> [!TIP]
> Stell das Terminal gleich am Anfang auf. Es kostet nur Material, öffnet Stufe 1 und zeigt dir im Baum, was als Nächstes kommt.

> [!TIP]
> Nimm von jedem Erz, das du beim Bergbau findest, eine Probe. Proben vieler verschiedener Erzarten bringen mehr als viele Proben derselben Art.

---

_Diese Seite steht auch im Spiel im **Ingenieurs-Handbuch**._

← [Erste Schritte ins All](Erste-Schritte-ins-All.md) · [Der Mond](Der-Mond.md) →
