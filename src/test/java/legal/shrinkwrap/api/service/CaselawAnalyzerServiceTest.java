package legal.shrinkwrap.api.service;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.google.common.io.Resources;
import legal.shrinkwrap.api.adapter.ris.RisSearchParameterCaseLaw;
import legal.shrinkwrap.api.adapter.ris.RisSoapAdapter;
import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.adapter.ris.dto.RisSearchResult;
import legal.shrinkwrap.api.dto.CaseLawRequestDto;
import legal.shrinkwrap.api.dto.CaselawSummaryCivilCase;
import legal.shrinkwrap.api.persistence.entity.CaseLawAnalysisEntity;
import legal.shrinkwrap.api.persistence.entity.CaseLawEntity;
import legal.shrinkwrap.api.utils.SentenceHashingTools;
import net.mezzdev.suffixtree.GeneralizedSuffixTree;
import net.mezzdev.suffixtree.Pair;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import legal.shrinkwrap.api.adapter.HtmlDownloadService;
import legal.shrinkwrap.api.dataset.CaseLawDataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


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

    @Autowired
    private CommonSentenceService commonSentenceService;

    @Autowired
    private RisSoapAdapter risSoapAdapter;

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

        GeneralizedSuffixTree<CaseLawEntity> solver = new GeneralizedSuffixTree<>();
        solver.put(sentence1Hash, entity1);
        solver.put(sentence2Hash, entity2);
        List<Pair<CharSequence, Collection<CaseLawEntity>>> longestCommonSubstring = new ArrayList<>();
        solver.findAllCommonSubstringsOfSizeInMinKeys(4, 2, false, true, (c, i) -> {
            longestCommonSubstring.add(new Pair<>(c, i));
        });
        GeneralizedSuffixTree.filterSubstrings(longestCommonSubstring);


        List<List<SentenceHashingTools.HashedSentence>> sentencesToReplace1 = new ArrayList<>();
        List<List<SentenceHashingTools.HashedSentence>> sentencesToReplace2 = new ArrayList<>();
        for (Pair<CharSequence, Collection<CaseLawEntity>> common : longestCommonSubstring) {
            {
                int startPos = sentence1Hash.indexOf(common.first().toString());
                int endPos = startPos + common.first().length();
                SentenceHashingTools.HashedSentence sentence1 = sentenceModel1.get(startPos);
                SentenceHashingTools.HashedSentence sentence2 = sentenceModel1.get(endPos-1);
                sentencesToReplace1.add(List.of(sentence1, sentence2));
            }
            {
                int startPos = sentence2Hash.indexOf(common.first().toString());
                int endPos = startPos + common.first().length();
                SentenceHashingTools.HashedSentence sentence1 = sentenceModel2.get(startPos);
                SentenceHashingTools.HashedSentence sentence2 = sentenceModel2.get(endPos-1);
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

    @Test
    public void testGetCommonSentences() throws InterruptedException, IOException {
        URL resource = getClass().getClassLoader().getResource("commonSentences.txt");
        String all = Resources.toString(resource, StandardCharsets.UTF_8);
        String output = "";
        int num = 0;
        for (String sentence : all.split("\n")) {
            String ecli = sentence.split(";")[0].trim();
            String fragment = sentence.split(";",2)[1].trim();

            CaseLawRequestDto dto1 = new CaseLawRequestDto(ecli, null, RisCourt.BVwG, false);
            CaseLawEntity entity1 = documentService.downloadCaseLaw(dto1);
            CaseLawAnalysisEntity analysisEntity1 = DocumentServiceImpl.createTextConversion(entity1);
            String commonSentence = SentenceHashingTools.getCommonSentence(analysisEntity1.getFullText(), fragment);
            if (commonSentence == null) {
                continue;
            }
            System.out.println(sentence + " ==>  " + commonSentence.replaceAll("[\r\n]"," "));
            output += fragment + " ==>  " + commonSentence.replaceAll("[\r\n]"," ");
            num++;
        }
        System.out.println(1);
    }

    @Test
    public void testAnalyzeCaseLawWithCommonSentences() {
        //import documents first
        RisSearchResult result1 = risSoapAdapter.findCaseLawDocuments(
                RisSearchParameterCaseLaw.builder()
                        .ecli("ECLI:AT:BVWG:2024:W287.2293603.1.00")
                        .court(RisCourt.BVwG)
                        .judikaturTyp(new RisSearchParameterCaseLaw.JudikaturTyp(false, true))
                        .build()
        );

        CaseLawEntity entity = documentService.importJudikaturResult(result1.getJudikaturResults().getFirst());

        String importString = "ECLI:AT:BVWG:2024:W287.2293603.1.00;Ba'V3?!]6%56p=,t\n" +
                "ECLI:AT:BVWG:2024:W287.2293603.1.00;qeY }~;0P$V@MX#a[8n_Q'7B b@eW)<&FO*gU]JqaD'\"#8z(,6Z#`W+V@~3DC~#?8\n" +
                "ECLI:AT:BVWG:2024:W287.2293603.1.00;/9[$o\"aP+2NTg>Q4^%L?KW*lMJ>d-0(uv&Cf1.5Q";

        commonSentenceService.importFromECLITextFile(importString);

        CaseLawAnalysisEntity analysisEntity = DocumentServiceImpl.createTextConversion(entity);
        CaselawSummaryCivilCase caselawSummaryCivilCase = caselawAnalyzerService.summarizeCaselaw(analysisEntity.getFullText(), entity).summary();

        System.out.println(caselawSummaryCivilCase.toString());
    }

}