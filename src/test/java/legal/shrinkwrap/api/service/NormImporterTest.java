package legal.shrinkwrap.api.service;

import at.gv.bka.ris.v26.soap.ws.client.Bundesland;
import at.gv.bka.ris.v26.soap.ws.client.NormDokumenttyp;
import legal.shrinkwrap.api.adapter.ris.dto.RisNormMetadaten;
import legal.shrinkwrap.api.persistence.entity.NormSectionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The derivations the import runs on RIS metadata. All values are taken from real documents.
 */
public class NormImporterTest {

    @Test
    public void eliLosesHostAndDocumentSegment() {
        //the same provision, delivered with three different hosts over the api
        assertThat(NormImporter.eliProvisionPath("https://www.ris.bka.gv.at/eli/bgbl/1960/159/P50/NOR40081328"))
                .isEqualTo("/eli/bgbl/1960/159/P50");
        assertThat(NormImporter.eliProvisionPath("https://ogd.ris.bka.gv.at/eli/bgbl/1960/159/P50/NOR12146644"))
                .isEqualTo("/eli/bgbl/1960/159/P50");

        //older versions of the same provision have to end up on the same key
        assertThat(NormImporter.eliProvisionPath("https://ris.bka.gv.at/eli/bgbl/1960/159/P50/NOR12151908"))
                .isEqualTo(NormImporter.eliProvisionPath("https://ris.bka.gv.at/eli/bgbl/1960/159/P50/NOR40065111"));
    }

    @Test
    public void eliToleratesWhatIsMissingOrOdd() {
        assertThat(NormImporter.eliProvisionPath(null)).isNull();
        assertThat(NormImporter.eliProvisionPath("  ")).isNull();
        //already a path, and without a document segment
        assertThat(NormImporter.eliProvisionPath("/eli/bgbl/1960/159/P50")).isEqualTo("/eli/bgbl/1960/159/P50");
    }

    @Test
    public void sectionTypeFollowsWhicheverNumberIsSet() {
        RisNormMetadaten paragraph = new RisNormMetadaten();
        paragraph.setParagraphnummer(50);
        assertThat(NormImporter.sectionTypeOf(paragraph)).isEqualTo(NormSectionType.PARAGRAPH);
        assertThat(NormImporter.nummerOf(paragraph, NormSectionType.PARAGRAPH)).isEqualTo("50");

        RisNormMetadaten artikel = new RisNormMetadaten();
        artikel.setArtikelnummer(2);
        assertThat(NormImporter.sectionTypeOf(artikel)).isEqualTo(NormSectionType.ARTIKEL);

        RisNormMetadaten anlage = new RisNormMetadaten();
        anlage.setAnlagennummer("2");
        anlage.setAnlagenteil("49");
        assertThat(NormImporter.sectionTypeOf(anlage)).isEqualTo(NormSectionType.ANLAGE);
        assertThat(NormImporter.nummerOf(anlage, NormSectionType.ANLAGE)).isEqualTo("2");
    }

    /**
     * RIS fills the article <em>and</em> the paragraph for a provision designated "Art. 4 § 1",
     * and whole laws are built that way - 76 such provisions in the Datenschutzgesetz, 42 in the
     * Angestelltengesetz. A citation names the §, so that is the designation; reading it as an
     * article would collapse every paragraph of an article onto the article number.
     */
    @Test
    public void articleWithParagraphIsAParagraph() {
        RisNormMetadaten both = new RisNormMetadaten();
        both.setArtikelParagraphAnlage("Art. 4 § 1");
        both.setArtikelnummer(4);
        both.setParagraphnummer(1);

        assertThat(NormImporter.sectionTypeOf(both)).isEqualTo(NormSectionType.PARAGRAPH);
        assertThat(NormImporter.nummerOf(both, NormSectionType.PARAGRAPH)).isEqualTo("1");
        //the article is not lost, it stays in the designation RIS wrote
        assertThat(both.getArtikelParagraphAnlage()).isEqualTo("Art. 4 § 1");
    }

    /** An article without a paragraph stays an article - "Art. 2" of the ASVG and its kind. */
    @Test
    public void articleWithoutParagraphStaysAnArticle() {
        RisNormMetadaten article = new RisNormMetadaten();
        article.setArtikelParagraphAnlage("Art. 2");
        article.setArtikelnummer(2);

        assertThat(NormImporter.sectionTypeOf(article)).isEqualTo(NormSectionType.ARTIKEL);
        assertThat(NormImporter.nummerOf(article, NormSectionType.ARTIKEL)).isEqualTo("2");
    }

    /**
     * Roughly a tenth of the corpus are head documents of a law, which RIS numbers "§ 0". Taken
     * for a paragraph they would collide with the real § 0 handling and pollute every listing.
     */
    @Test
    public void headDocumentIsNotAParagraph() {
        RisNormMetadaten head = new RisNormMetadaten();
        head.setDokumenttyp(NormDokumenttyp.NORM);
        head.setParagraphnummer(0);
        head.setArtikelParagraphAnlage("§ 0");

        assertThat(NormImporter.sectionTypeOf(head)).isEqualTo(NormSectionType.NORM);
    }

    @Test
    public void jurisdictionIsIso3166() {
        assertThat(NormImporter.jurisdictionOf(null)).isEqualTo("AT");
        assertThat(NormImporter.jurisdictionOf(Bundesland.WIEN)).isEqualTo("AT-9");
        assertThat(NormImporter.jurisdictionOf(Bundesland.BURGENLAND)).isEqualTo("AT-1");
    }

    /**
     * A caller writes whichever form comes to mind, so one abbreviation has to be findable
     * under several keys. The import stores them all, the search normalises the same way.
     */
    @Test
    public void abbreviationYieldsEveryFormItIsWritten() {
        assertThat(NormAbbreviations.normalize("StVO 1960"))
                .containsExactlyInAnyOrder("stvo 1960", "stvo1960", "stvo");

        //no trailing year, so there is nothing to strip
        assertThat(NormAbbreviations.normalize("ABGB")).containsExactly("abgb");

        //the hyphen is part of the citation and is kept: "B-VG" is not "BVG", and
        //"Bundes-Verfassungsgesetz" is not "Bundesverfassungsgesetz"
        assertThat(NormAbbreviations.normalize("B-VG")).containsExactly("b-vg");
        assertThat(NormAbbreviations.normalize("BVG")).containsExactly("bvg");
        assertThat(NormAbbreviations.normalize("Bundes-Verfassungsgesetz"))
                .containsExactly("bundes-verfassungsgesetz");
        assertThat(NormAbbreviations.normalize("Bundesverfassungsgesetz"))
                .containsExactly("bundesverfassungsgesetz");

        assertThat(NormAbbreviations.normalize("EStG 1988"))
                .containsExactlyInAnyOrder("estg 1988", "estg1988", "estg");
    }

    /**
     * A bound in a range request is whatever the caller happens to write. The number is
     * required, the designation is not - but when it is there it also says which kind of
     * provision is meant, which is what keeps "1 to 6" from mixing articles and paragraphs.
     */
    @Test
    public void rangeBoundsAcceptTheUsualSpellings() {
        record Case(String written, int number, String letters, NormSectionType typ) {
        }
        List<Case> cases = List.of(
                new Case("12", 12, "", null),
                new Case("12a", 12, "a", null),
                new Case("§ 12a", 12, "a", NormSectionType.PARAGRAPH),
                new Case("§12a", 12, "a", NormSectionType.PARAGRAPH),
                new Case("Paragraph 12", 12, "", NormSectionType.PARAGRAPH),
                new Case("Art 1", 1, "", NormSectionType.ARTIKEL),
                new Case("Art. 1", 1, "", NormSectionType.ARTIKEL),
                new Case("Artikel 1a", 1, "a", NormSectionType.ARTIKEL),
                new Case("Anlage 1", 1, "", NormSectionType.ANLAGE),
                new Case("Anl. 1", 1, "", NormSectionType.ANLAGE),
                new Case("Anl 1", 1, "", NormSectionType.ANLAGE),
                new Case("Anhang 1", 1, "", NormSectionType.ANLAGE),
                new Case("Anh 1", 1, "", NormSectionType.ANLAGE),
                new Case("Anh. 1", 1, "", NormSectionType.ANLAGE));

        for (Case c : cases) {
            assertThat(NormService.ArticleNumber.parse(c.written()))
                    .as("parsing %s", c.written())
                    .isEqualTo(new NormService.ArticleNumber(c.number(), c.letters()));
            assertThat(NormService.ArticleNumber.sectionTypeOf(c.written()))
                    .as("kind of %s", c.written())
                    .isEqualTo(c.typ());
        }
    }

    /**
     * How the bounds have to behave, in the words of the provisions themselves: § 1 precedes
     * § 1a, § 1a precedes § 1b, and all of them precede § 2.
     */
    @Test
    public void lettersSortAfterThePlainNumber() {
        //from "1" takes in 1a and 1b
        assertThat(bound("1")).isLessThan(bound("1a")).isLessThan(bound("1b"));
        //from "1b" leaves 1a behind but still reaches 1c and 1d
        assertThat(bound("1b")).isGreaterThan(bound("1a"));
        assertThat(bound("1b")).isLessThan(bound("1c")).isLessThan(bound("1d"));
        //and "1a" no longer takes in the plain § 1
        assertThat(bound("1a")).isGreaterThan(bound("1"));
        //the number still decides first
        assertThat(bound("1z")).isLessThan(bound("2"));
        assertThat(bound("2")).isLessThan(bound("10"));
    }

    private static NormService.ArticleNumber bound(String designation) {
        return NormService.ArticleNumber.parse(designation);
    }

    @Test
    public void rangeBoundWithoutANumberIsRejected() {
        //used to come back as an empty list, which reads like a law with no such provisions
        assertThat(NormService.ArticleNumber.parse("Artikel A")).isNull();
        assertThat(NormService.ArticleNumber.parse("")).isNull();
        assertThat(NormService.ArticleNumber.parse(null)).isNull();
    }

    @Test
    public void abbreviationToleratesWhatRisLeavesOut() {
        //RIS carries no abbreviation for roughly a quarter of all documents
        assertThat(NormAbbreviations.normalize(null)).isEmpty();
        assertThat(NormAbbreviations.normalize("   ")).isEmpty();
    }
}
