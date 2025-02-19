package legal.shrinkwrap.api.data;


import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import legal.shrinkwrap.api.persistence.entity.CaseLawAnalysisEntity;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessJudikaturTest {

    private String resultFolder = "C:\\Tools\\Data\\RIS\\ECLI";

    private String selectedResults = "C:\\Tools\\Data\\RIS\\ECLI\\AT\\VFGH\\2020";

    @Test
    public void testProcessJudikatur() throws IOException {

        ObjectMapper objectMapper = YamlMapper.getMapper();

        log.info("iterate over all files");

        Iterator<File> iterator = FileUtils.iterateFiles(new File(selectedResults),null,true);

        while (iterator.hasNext()) {
            File file = iterator.next();
            log.info("File {}",file.getAbsolutePath());

            CaseLawAnalysisEntity entity = objectMapper.readValue(file, CaseLawAnalysisEntity.class);
            log.info("CaseLawAnalysisEntity {}",entity.getCaseLaw().getCaseNumber());

        }


    }
}
