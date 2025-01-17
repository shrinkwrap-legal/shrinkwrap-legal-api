package legal.shrinkwrap.api.service;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.CheckForNull;
import java.io.File;
import java.util.List;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

@Service
public class FileHandlingService {
    private static final Logger LOG = LoggerFactory.getLogger(FileHandlingService.class);

    @Value("${files.output-directory}")
    private String outputDirectory;


    @CheckForNull
    public String loadFile(String ecli, String postfix) {
        FileNameAndFolder fileAndFolder = ecliToFolder(ecli);
        String qualifiedName = fileAndFolder.folder + File.separatorChar + fileAndFolder.filename + postfix;
        try {
            return Files.readString(Paths.get(qualifiedName), StandardCharsets.UTF_8);
        } catch (IOException e) {
            //Files.createDirectories(Paths.get(ecliToFolder(ecli)));
            return null;
        }
    }


    public String loadJsonFile(String ECLI, String filename) {
        throw new NotImplementedException("to be done");
    }


    public void saveFile(String ecli, String postfix, String content) {
        FileNameAndFolder fileAndFolder = ecliToFolder(ecli);
        String qualifiedName = fileAndFolder.folder + File.separatorChar + fileAndFolder.filename + postfix;
        try {
            Paths.get(fileAndFolder.folder).toFile().mkdirs();
            Files.writeString(Paths.get(qualifiedName),content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private FileNameAndFolder ecliToFolder(String ecli) {
        //ECLI:AT:OGH0002:2024:0150OS00082.24H.1209.000
        //https://e-justice.europa.eu/175/DE/european_case_law_identifier_ecli

        List<String> parts = Arrays.asList(ecli.split(":",5));
        FileNameAndFolder fileAndFolder = new FileNameAndFolder(outputDirectory + File.separatorChar + Strings.join(parts.subList(0,parts.size()-1),File.separatorChar), parts.getLast());
        return fileAndFolder;
    }

    public static record FileNameAndFolder(
            String folder,
            String filename) {
    }

    ;

}
