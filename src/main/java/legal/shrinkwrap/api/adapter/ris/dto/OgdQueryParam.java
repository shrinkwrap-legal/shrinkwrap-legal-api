package legal.shrinkwrap.api.adapter.ris.dto;

import java.time.format.DateTimeFormatter;

public class OgdQueryParam {

    public static final String APPLIKATION = "Applikation";

    public static final String RECHTSSATZNUMMER = "Rechtssatznummer";

    public static final DateTimeFormatter QUERY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
}
