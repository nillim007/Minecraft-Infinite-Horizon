---
title: Deine Agentur
title_en: Your Agency
order: 25
milestone: 3
icon: infinite_horizon:engineers_handbook
summary: Agentur, Beitritt, Rechte, Besitz von Maschinen und die Befehle dazu
---
Jeder Spieler gehört zu einer **Agentur**. Sie ist dein Raumfahrtprogramm: Alle Mitglieder teilen Forschungspunkte und Forschung, später auch Atlas, Routen und Stationen. Spielst du allein, bist du deine eigene Agentur.

## So entsteht sie

Beim Betreten der Welt prüft der Server, ob du zu einer Agentur gehörst. Wenn nicht, gründet er eine für dich, und du bist ihr **Leiter**. Das gilt auch für Welten, die schon vor dieser Version bestanden. Solange die Agentur keinen eigenen Namen hat, heißt sie „Agentur von <dein Name>“.

Die Agentur merkt sich außerdem jede Dimension, die eines ihrer Mitglieder betreten hat. Das zählt für die Forschung: Betritt ein Mitglied den Mond, öffnet sich Stufe 2 (siehe [[forschung|Forschung]]).

## Besitz

Setzt du eine Maschine, einen Generator, einen Speicher oder einen Port, gehört er deiner Agentur. Kabel, Rohre und Gehäuseblöcke haben keinen Besitzer.

Blöcke, die nicht von einem Spieler gesetzt wurden (per Befehl, aus einem Werfer oder aus älteren Welten), sind **ohne Besitzer**. Jeder darf sie benutzen. Wer ihr GUI als Erster öffnet, wird mit seiner Agentur Besitzer.

## Beitreten und Austreten

Alles läuft am <item:infinite_horizon:research_terminal> im Reiter **Agentur**. Dort siehst du die Mitglieder mit ihrer Rolle.

1. **Einladen:** Der Leiter sieht rechts alle Spieler, die gerade online und nicht Mitglied sind, und lädt sie per Knopf ein. Der Eingeladene bekommt eine Nachricht im Chat.
2. **Annehmen oder Ablehnen:** Unten rechts stehen deine offenen Einladungen. Annehmen kannst du an **jedem** Forschungsterminal, auch an einem fremden.
3. **Beitritt:** Du verlässt deine alte Agentur und wirst Ingenieur der neuen. Wird die alte dadurch leer, gehört ihr ganzer Besitz (Maschinen, Kuppeln, Terminals) ab sofort der neuen. Forschungspunkte und Forschung der alten Agentur werden **nicht** übertragen.
4. **Austreten:** Du gründest sofort eine neue eigene Agentur und bist ihr Leiter. Allein kannst du nicht austreten.

Tritt der Leiter aus, wird der Ingenieur Leiter, der am längsten dabei ist.

@warning Wer einer anderen Agentur beitritt, nimmt seine Forschungspunkte und seine Forschung nicht mit. Es gilt der Stand der neuen Agentur.

## Rechte

| Rolle | Darf |
| --- | --- |
| Leiter | alles, dazu einladen |
| Ingenieur | bauen, Maschinen bedienen und einstellen, forschen, später Raketen starten |
| Nicht-Mitglied | fremde Maschinen und Behälter öffnen und ansehen, aber nichts herausnehmen, hineinlegen, einstellen oder abbauen |

- **Kuppelschutz:** In einer aktiven fremden [[kuppel|Kuppel]] setzen und bauen Nicht-Mitglieder keinen Block ab, auch keine Erde oder Steine. Atmen darf dort jeder.
- **Operatoren** des Servers sind von diesen Regeln ausgenommen, wie beim Spawnschutz.

@note Rollen wechseln, die Agentur umbenennen und Partnerschaften kommen in einer späteren Version.

## Befehle

- `/infinite_horizon agency info` zeigt deine Agentur: Name, ID, Mitglieder mit Rolle und besuchte Dimensionen. Mit einem Spielernamen dahinter zeigt er dessen Agentur.
- `/infinite_horizon agency transfer <von> <an>` (nur Operatoren): Der ganze Besitz der ersten Agentur geht an die zweite über. Statt einer Agentur-ID darfst du einen Spielernamen angeben. Die alte Agentur verliert ihre Mitglieder. Kommt eines zurück, bekommt es eine neue Agentur.

@tip Spielt jemand nicht mehr auf dem Server, kann ein Operator dessen Maschinen mit `transfer` einer anderen Agentur geben. Die Blöcke wechseln den Besitzer beim nächsten Zugriff von selbst, du musst nichts neu setzen.

@warning `transfer` lässt sich nicht rückgängig machen: Die alte Agentur ist danach leer, und ihr Besitz gehört dauerhaft der neuen.
