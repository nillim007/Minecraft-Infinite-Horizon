<!-- Erzeugt von tools/gen_wiki.py aus docs/handbook/ und spec/data. Nicht von Hand ändern. -->

[📖 Wiki](README.md) › Nachschlagen › **Deine Agentur**

# Deine Agentur

Jeder Spieler gehört zu einer **Agentur**. Sie ist dein Raumfahrtprogramm: Alle Mitglieder teilen Forschungspunkte und Forschung, später auch Atlas, Routen und Stationen. Spielst du allein, bist du deine eigene Agentur.

## So entsteht sie

Beim Betreten der Welt prüft der Server, ob du zu einer Agentur gehörst. Wenn nicht, gründet er eine für dich, und du bist ihr **Leiter**. Das gilt auch für Welten, die schon vor dieser Version bestanden. Solange die Agentur keinen eigenen Namen hat, heißt sie „Agentur von <dein Name>“.

Die Agentur merkt sich außerdem jede Dimension, die eines ihrer Mitglieder betreten hat. Das zählt für die Forschung: Betritt ein Mitglied den Mond, öffnet sich Stufe 2 (siehe [Forschung](Forschung.md)).

## Besitz

Setzt du eine Maschine, einen Generator, einen Speicher oder einen Port, gehört er deiner Agentur. Kabel, Rohre und Gehäuseblöcke haben keinen Besitzer.

Blöcke, die nicht von einem Spieler gesetzt wurden (per Befehl, aus einem Werfer oder aus älteren Welten), sind **ohne Besitzer**. Jeder darf sie benutzen. Wer ihr GUI als Erster öffnet, wird mit seiner Agentur Besitzer.

## Beitreten und Austreten

Alles läuft am [Forschungsterminal](Forschung.md) im Reiter **Agentur**. Dort siehst du die Mitglieder mit ihrer Rolle.

1. **Einladen:** Der Leiter sieht rechts alle Spieler, die gerade online und nicht Mitglied sind, und lädt sie per Knopf ein. Der Eingeladene bekommt eine Nachricht im Chat.
2. **Annehmen oder Ablehnen:** Unten rechts stehen deine offenen Einladungen. Annehmen kannst du an **jedem** Forschungsterminal, auch an einem fremden.
3. **Beitritt:** Du verlässt deine alte Agentur und wirst Ingenieur der neuen. Wird die alte dadurch leer, gehört ihr ganzer Besitz (Maschinen, Kuppeln, Terminals) ab sofort der neuen. Forschungspunkte und Forschung der alten Agentur werden **nicht** übertragen.
4. **Austreten:** Du gründest sofort eine neue eigene Agentur und bist ihr Leiter. Allein kannst du nicht austreten.

Tritt der Leiter aus, wird der Ingenieur Leiter, der am längsten dabei ist.

> [!WARNING]
> Wer einer anderen Agentur beitritt, nimmt seine Forschungspunkte und seine Forschung nicht mit. Es gilt der Stand der neuen Agentur.

## Rechte

| Rolle | Darf |
| --- | --- |
| Leiter | alles, dazu einladen, Rollen vergeben und umbenennen |
| Ingenieur | bauen, Maschinen bedienen und einstellen, forschen, Raketen starten, Routen ändern |
| Gast | Maschinen und Behälter der Agentur öffnen und ansehen, aber nichts herausnehmen, hineinlegen, einstellen oder abbauen |
| Nicht-Mitglied | wie ein Gast, bei fremden Agenturen |

- **Kuppelschutz:** In einer aktiven fremden [Kuppel](Die-Kuppel.md) setzen und bauen Nicht-Mitglieder keinen Block ab, auch keine Erde oder Steine. Atmen darf dort jeder.
- **Operatoren** des Servers sind von diesen Regeln ausgenommen, wie beim Spawnschutz.

## Rollen und Name

Der Leiter verwaltet die Agentur im Reiter **Agentur**:

- **Rolle wechseln:** Neben jedem Mitglied steht ein Knopf mit seiner Rolle. Ein Klick wechselt zwischen Ingenieur und Gast. Das Mitglied bekommt eine Nachricht.
- **Leitung übergeben:** Der Knopf „Leiter“ macht das Mitglied zum Leiter. Du selbst wirst dann Ingenieur.
- **Umbenennen:** Trag unten links einen Namen ein und klick auf „Umbenennen“. Ein leerer Name stellt wieder „Agentur von <Gründer>“ her.

> [!TIP]
> Gib Mitspielern, die nur zuschauen sollen, die Rolle Gast. Sie können dann nichts aus Versehen abbauen.

## Befehle

- `/infinite_horizon agency info` zeigt deine Agentur: Name, ID, Mitglieder mit Rolle und besuchte Dimensionen. Mit einem Spielernamen dahinter zeigt er dessen Agentur.
- `/infinite_horizon agency transfer <von> <an>` (nur Operatoren): Der ganze Besitz der ersten Agentur geht an die zweite über. Statt einer Agentur-ID darfst du einen Spielernamen angeben. Die alte Agentur verliert ihre Mitglieder. Kommt eines zurück, bekommt es eine neue Agentur.

> [!TIP]
> Spielt jemand nicht mehr auf dem Server, kann ein Operator dessen Maschinen mit `transfer` einer anderen Agentur geben. Die Blöcke wechseln den Besitzer beim nächsten Zugriff von selbst, du musst nichts neu setzen.

> [!WARNING]
> `transfer` lässt sich nicht rückgängig machen: Die alte Agentur ist danach leer, und ihr Besitz gehört dauerhaft der neuen.

---

_Diese Seite steht auch im Spiel im **Ingenieurs-Handbuch**._

← [Alle Items und Blöcke](Alle-Items-und-Bloecke.md) · [Tipps und häufige Fragen](Tipps-und-haeufige-Fragen.md) →
