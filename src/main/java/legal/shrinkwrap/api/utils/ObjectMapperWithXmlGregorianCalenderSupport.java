package legal.shrinkwrap.api.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import jakarta.xml.bind.JAXBElement;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class ObjectMapperWithXmlGregorianCalenderSupport extends ObjectMapper {

    public ObjectMapperWithXmlGregorianCalenderSupport() {
        super();
        SimpleModule module = new SimpleModule();
        module.addSerializer(JAXBElement.class, new XmlCalenderSerializer());
        this.registerModule(module);

    }

    public static class XmlCalenderSerializer extends JsonSerializer<JAXBElement> {
        @Override
        public void serialize(JAXBElement value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value.getValue() instanceof XMLGregorianCalendar) {
                XMLGregorianCalendar calendar = (XMLGregorianCalendar) value.getValue();

                if (calendar.getHour() == DatatypeConstants.FIELD_UNDEFINED &&
                        calendar.getMinute() == DatatypeConstants.FIELD_UNDEFINED &&
                        calendar.getSecond() == DatatypeConstants.FIELD_UNDEFINED) {
                    // Only date is present
                    LocalDate localDate = calendar.toGregorianCalendar().toZonedDateTime().toLocalDate();
                    gen.writeString(localDate.toString());
                } else {
                    // Date and time are present
                    LocalDateTime localDateTime = calendar.toGregorianCalendar().toZonedDateTime().toLocalDateTime();
                    gen.writeString(localDateTime.toString());
                }
            } else {
                serializers.defaultSerializeValue(value, gen);
            }
        }
    }
}