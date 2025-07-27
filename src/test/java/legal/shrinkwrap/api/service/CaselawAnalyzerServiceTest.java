package legal.shrinkwrap.api.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharSequenceNodeFactory;
import com.googlecode.concurrenttrees.solver.LCSubstringSolverForMinLength;
import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.dto.CaseLawRequestDto;
import legal.shrinkwrap.api.dto.CaselawSummaryCivilCase;
import legal.shrinkwrap.api.persistence.entity.CaseLawAnalysisEntity;
import legal.shrinkwrap.api.persistence.entity.CaseLawEntity;
import legal.shrinkwrap.api.utils.SentenceHashingTools;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import legal.shrinkwrap.api.adapter.HtmlDownloadService;
import legal.shrinkwrap.api.dataset.CaseLawDataset;


@SpringBootTest
@Tag("integration")
class CaselawAnalyzerServiceTest {

    @Autowired
    private CaselawAnalyzerService caselawAnalyzerService;

    @Autowired
    private FileHandlingService fileHandlingService;

    @Autowired
    private HtmlDownloadService htmlDownloadService;

    @Autowired
    private CaselawTextService caselawTextService;

    @Autowired
    private DocumentService documentService;

    @Test
    public void singleCaseLaw() {
        String ecli = "ECLI:AT:OGH0002:2024:008OBA00004";
        CaseLawDataset caselawDatasetForECLI = documentService.getCaselawDatasetForECLI(ecli);
        caselawAnalyzerService.summarizeCaselaw(caselawDatasetForECLI.sentences());
        caselawAnalyzerService.analyzeCaselaw(caselawDatasetForECLI);
    }

    @Test
    public void singleCaseLawSummary() throws InterruptedException {
        String ecli = "ECLI:AT:OGH0002:2024:008OBA00004";
        CaseLawRequestDto dto = new CaseLawRequestDto(ecli, null, RisCourt.Justiz, false);
        CaseLawEntity entity = documentService.downloadCaseLaw(dto);
        CaseLawAnalysisEntity analysisEntity = DocumentServiceImpl.createTextConversion(entity);
        CaselawSummaryCivilCase caselawSummaryCivilCase = caselawAnalyzerService.summarizeCaselaw(analysisEntity.getFullText(), entity).summary();
        System.out.println(caselawSummaryCivilCase.toString());

    }

    @Test
    public void testSentenceSplitSimilarity() throws InterruptedException {
        String ecli1 = "ECLI:AT:BVWG:2025:I424.2304252.1.00";
        String ecli2 = "ECLI:AT:BVWG:2025:I423.2305904.1.00";
        CaseLawRequestDto dto1 = new CaseLawRequestDto(ecli1, null, RisCourt.BVwG, false);
        CaseLawEntity entity1 = documentService.downloadCaseLaw(dto1);
        CaseLawAnalysisEntity analysisEntity1 = DocumentServiceImpl.createTextConversion(entity1);
        List<SentenceHashingTools.HashedSentence> sentenceModel1 = SentenceHashingTools.getSentenceModel(analysisEntity1.getFullText());
        String sentence1Hash = sentenceModel1.stream().map(SentenceHashingTools.HashedSentence::getCharacter).collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();

        CaseLawRequestDto dto2 = new CaseLawRequestDto(ecli2, null, RisCourt.BVwG, false);
        CaseLawEntity entity2 = documentService.downloadCaseLaw(dto2);
        CaseLawAnalysisEntity analysisEntity2 = DocumentServiceImpl.createTextConversion(entity2);
        List<SentenceHashingTools.HashedSentence> sentenceModel2 = SentenceHashingTools.getSentenceModel(analysisEntity2.getFullText());
        String sentence2Hash = sentenceModel2.stream().map(SentenceHashingTools.HashedSentence::getCharacter).collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();

        LCSubstringSolverForMinLength solver = new LCSubstringSolverForMinLength(new DefaultCharSequenceNodeFactory());
        solver.add(sentence1Hash);
        solver.add(sentence2Hash);
        List<CharSequence> longestCommonSubstring = solver.getLongestCommonSubstringsForLength(6);

        List<List<SentenceHashingTools.HashedSentence>> sentencesToReplace1 = new ArrayList<>();
        List<List<SentenceHashingTools.HashedSentence>> sentencesToReplace2 = new ArrayList<>();
        for (CharSequence common : longestCommonSubstring) {
            {
                int startPos = sentence1Hash.indexOf(common.toString());
                int endPos = startPos + common.length();
                SentenceHashingTools.HashedSentence sentence1 = sentenceModel1.get(startPos);
                SentenceHashingTools.HashedSentence sentence2 = sentenceModel1.get(endPos);
                sentencesToReplace1.add(List.of(sentence1, sentence2));
            }
            {
                int startPos = sentence2Hash.indexOf(common.toString());
                int endPos = startPos + common.length();
                SentenceHashingTools.HashedSentence sentence1 = sentenceModel2.get(startPos);
                SentenceHashingTools.HashedSentence sentence2 = sentenceModel2.get(endPos);
                sentencesToReplace2.add(List.of(sentence1, sentence2));
            }
        }

        String fullText1 = SentenceHashingTools.replaceCommonSentence(analysisEntity1.getFullText(), sentencesToReplace1);
        String fullText2 = SentenceHashingTools.replaceCommonSentence(analysisEntity2.getFullText(), sentencesToReplace2);

        System.out.println(1);

        //print out duplicate fragments


        //CaselawSummaryCivilCase caselawSummaryCivilCase = caselawAnalyzerService.summarizeCaselaw(analysisEntity.getFullText(), entity).summary();
        //System.out.println(caselawSummaryCivilCase.toString());

    }

    @Test
    public void singleCaseLawSummaryEuGH() throws InterruptedException {
        String ecli = "ECLI:AT:OGH0002:2025:0080OB00021.25H.0526.000";
        CaseLawRequestDto dto = new CaseLawRequestDto(ecli, null, RisCourt.Justiz, false);
        CaseLawEntity entity = documentService.downloadCaseLaw(dto);
        CaseLawAnalysisEntity analysisEntity = DocumentServiceImpl.createTextConversion(entity);
        CaselawSummaryCivilCase caselawSummaryCivilCase = caselawAnalyzerService.summarizeCaselaw(analysisEntity.getFullText(), entity).summary();
        System.out.println(caselawSummaryCivilCase.toString());

    }
    @Test
    public void singleCaseLawSummaryVfGH() throws InterruptedException {
        String ecli = "ECLI:AT:VFGH:2016:G7.2016";
        CaseLawRequestDto dto = new CaseLawRequestDto(ecli, null, RisCourt.VfGH, false);
        CaseLawEntity entity = documentService.downloadCaseLaw(dto);
        CaseLawAnalysisEntity analysisEntity = DocumentServiceImpl.createTextConversion(entity);
        CaselawSummaryCivilCase caselawSummaryCivilCase = caselawAnalyzerService.summarizeCaselaw(analysisEntity.getFullText(), entity).summary();
        System.out.println(caselawSummaryCivilCase.toString());

    }

    @Test
    void analyzeCaselaw() {
        Path directoryPath = Paths.get("c:\\tmp\\shrinkwrap");

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, "*.html")) {
            for (Path entry : stream) {
                System.out.println("Reading file: " + entry.getFileName());
                String sentenceContent = null;// Files.readString((Paths.get("c:\\tmp\\shrinkwrap\\ECLI_AT_OGH0002_2024_008OBA00004.24G.0826.000.html.sentences.txt")), StandardCharsets.UTF_8);
                String htmlContent = Files.readString((Paths.get("c:\\tmp\\shrinkwrap\\ECLI_AT_OGH0002_2024_008OBA00004.24G.0826.000.html")), StandardCharsets.UTF_8);
                CaseLawDataset ds = new CaseLawDataset(null,null,null,null,null,null,null,null,null,null,null,null,htmlContent,sentenceContent);
                caselawAnalyzerService.summarizeCaselaw(ds.sentences());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}