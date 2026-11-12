package de.infinitehorizon.agency;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Aktueller Besitzer als reine Funktion (23.1): Geht eine Agentur in einer anderen auf ({@code merged_into}), gehört
 * ihr Besitz der Ziel-Agentur. Folgt der Kette A → B → C; ein Kreis bricht an der ersten Wiederholung ab.
 */
public final class OwnershipResolver {

    private OwnershipResolver() {
    }

    /**
     * @param owner      gespeicherte Besitzer-Agentur
     * @param mergedInto liefert zu einer Agentur ihr {@code merged_into} (leer, wenn sie nicht aufgegangen ist)
     * @return die Agentur am Ende der Kette
     */
    public static UUID current(UUID owner, Function<UUID, Optional<UUID>> mergedInto) {
        UUID cur = owner;
        Set<UUID> seen = new HashSet<>();
        while (seen.add(cur)) {
            Optional<UUID> next = mergedInto.apply(cur);
            if (next.isEmpty()) {
                return cur;
            }
            cur = next.get();
        }
        return cur;
    }
}
