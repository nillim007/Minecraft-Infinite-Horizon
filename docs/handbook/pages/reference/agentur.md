---
title: Deine Agentur
title_en: Your Agency
order: 25
milestone: 3
icon: infinite_horizon:engineers_handbook
summary: Agentur, Besitz von Maschinen und die Befehle dazu
---
Jeder Spieler gehört zu einer **Agentur**. Sie ist dein Raumfahrtprogramm: Später teilen alle Mitglieder Forschung, Atlas, Routen und Stationen. Spielst du allein, bist du deine eigene Agentur.

## So entsteht sie

Beim Betreten der Welt prüft der Server, ob du zu einer Agentur gehörst. Wenn nicht, gründet er eine für dich, und du bist ihr **Leiter**. Das gilt auch für Welten, die schon vor dieser Version bestanden. Solange die Agentur keinen eigenen Namen hat, heißt sie „Agentur von <dein Name>“.

Die Agentur merkt sich außerdem jede Dimension, die eines ihrer Mitglieder betreten hat. Das zählt für die Forschung: Betritt ein Mitglied den Mond, öffnet sich Stufe 2 (siehe [[forschung|Forschung]]).

## Besitz

Setzt du eine Maschine, einen Generator, einen Speicher oder einen Port, gehört er deiner Agentur. Kabel, Rohre und Gehäuseblöcke haben keinen Besitzer.

Blöcke, die nicht von einem Spieler gesetzt wurden (per Befehl, aus einem Werfer oder aus älteren Welten), sind **ohne Besitzer**. Das hat im Moment keine Folgen.

@note Noch kann jeder alle Maschinen bedienen. Rollen, Gast-Rechte und der Schutz in Kuppeln kommen, sobald man am Forschungsterminal einer Agentur beitreten kann.

## Rollen

| Rolle | Darf später |
| --- | --- |
| Leiter | alles, einladen, Rollen vergeben, Partner freigeben |
| Ingenieur | bauen, Maschinen bedienen, Raketen starten, Routen ändern, forschen |
| Gast | Kuppeln betreten, Behälter ansehen, aber nichts nehmen, setzen oder abbauen |

## Befehle

- `/infinite_horizon agency info` zeigt deine Agentur: Name, ID, Mitglieder mit Rolle und besuchte Dimensionen. Mit einem Spielernamen dahinter zeigt er dessen Agentur.
- `/infinite_horizon agency transfer <von> <an>` (nur Operatoren): Der ganze Besitz der ersten Agentur geht an die zweite über. Statt einer Agentur-ID darfst du einen Spielernamen angeben. Die alte Agentur verliert ihre Mitglieder. Kommt eines zurück, bekommt es eine neue Agentur.

@tip Spielt jemand nicht mehr auf dem Server, kann ein Operator dessen Maschinen mit `transfer` einer anderen Agentur geben. Die Blöcke wechseln den Besitzer beim nächsten Zugriff von selbst, du musst nichts neu setzen.

@warning `transfer` lässt sich nicht rückgängig machen: Die alte Agentur ist danach leer, und ihr Besitz gehört dauerhaft der neuen.
