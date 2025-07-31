package legal.shrinkwrap.api.service;

import legal.shrinkwrap.api.persistence.repo.CommonSentencesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class CommonSentenceServiceTest {

    private CommonSentenceService commonSentenceService;
    private CommonSentencesRepository commonSentencesRepository;

    @BeforeEach
    public void setUp() {
        commonSentencesRepository = Mockito.mock(CommonSentencesRepository.class);
        Mockito.when(commonSentencesRepository.findAllSentenceHash()).thenReturn(List.of("123456789","asdfasdf","fghfgh") );
        commonSentenceService = new CommonSentenceService(commonSentencesRepository);
    }

    @Test
    public void testFindContainedSentences() {
        List<String> result = commonSentenceService.findContainedSentences("9876 01234567890 asdfasdf ujklkj");
        assertEquals(2,result.size());
        assertEquals("123456789",result.get(0));
        assertEquals("asdfasdf",result.get(1));
    }

}