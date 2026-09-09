package legal.shrinkwrap.api.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.time.LocalDate;
import java.util.List;

/**
 * Answer to a name that means several laws. Handing the choice back is the point - "BFG" is the
 * Bundesfinanzgesetz of 27 different years, and picking one of them would be a guess.
 */
public record NormAmbiguityDto(
        @JsonPropertyDescription("The name that was asked for")
        String query,
        @JsonPropertyDescription("How many laws it matches, which can exceed the listed candidates")
        int matches,
        @JsonPropertyDescription("Newest first; address one of them by its gesetzesnummer")
        List<Candidate> candidates
) {
    public record Candidate(
            String jurisdiction,
            String gesetzesnummer,
            String kurztitel,
            String typ,
            @JsonPropertyDescription("Which version the title and abbreviation come from")
            LocalDate stand
    ) {
    }
}
