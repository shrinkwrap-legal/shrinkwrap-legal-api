package legal.shrinkwrap.api.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.xml.bind.JAXBElement;
import org.junit.jupiter.api.Test;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import static org.assertj.core.api.Assertions.assertThat;

public class ObjectMapperWithXmlGregorianCalenderSupportTest {

    private final ObjectMapper mapper = new ObjectMapperWithXmlGregorianCalenderSupport();

    @Test
    public void writesTheDateOfACalendarElement() throws Exception {
        XMLGregorianCalendar calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar("2026-09-04");
        JAXBElement<XMLGregorianCalendar> element =
                new JAXBElement<>(new QName("Inkrafttretensdatum"), XMLGregorianCalendar.class, calendar);

        assertThat(mapper.writeValueAsString(element)).isEqualTo("\"2026-09-04\"");
    }

    /**
     * Anything other than a calendar used to be handed back to the mapper as the element rather
     * than as its value, which landed in this serializer again and recursed until the stack was
     * gone. Judicature never noticed because it wraps nothing but calendars; norm metadata
     * carries {@code JAXBElement<Integer>} for the paragraph number, and every single document
     * lost its metadata json to a caught exception.
     */
    @Test
    public void writesTheValueOfAnyOtherElement() throws Exception {
        JAXBElement<Integer> number =
                new JAXBElement<>(new QName("Paragraphnummer"), Integer.class, 50);
        assertThat(mapper.writeValueAsString(number)).isEqualTo("50");

        JAXBElement<String> text =
                new JAXBElement<>(new QName("Abkuerzung"), String.class, "StVO 1960");
        assertThat(mapper.writeValueAsString(text)).isEqualTo("\"StVO 1960\"");
    }

    @Test
    public void writesNullForAnEmptyElement() throws Exception {
        JAXBElement<Integer> empty =
                new JAXBElement<>(new QName("Artikelnummer"), Integer.class, null);
        assertThat(mapper.writeValueAsString(empty)).isEqualTo("null");
    }
}
