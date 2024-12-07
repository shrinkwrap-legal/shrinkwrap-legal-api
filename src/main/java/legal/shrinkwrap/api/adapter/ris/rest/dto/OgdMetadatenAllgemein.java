package legal.shrinkwrap.api.adapter.ris.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record OgdMetadatenAllgemein(@JsonProperty("Veroeffentlicht")
                                    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
                                    LocalDate veroeffentlicht,
                                    @JsonProperty("Geaendert")
                                    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
                                    LocalDate geaendert) {
}
