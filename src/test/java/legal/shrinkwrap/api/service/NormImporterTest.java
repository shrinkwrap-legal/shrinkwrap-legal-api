package legal.shrinkwrap.api.service;

import at.gv.bka.ris.v26.soap.ws.client.Bundesland;
import at.gv.bka.ris.v26.soap.ws.client.NormDokumenttyp;
import legal.shrinkwrap.api.adapter.ris.dto.RisNormMetadaten;
import legal.shrinkwrap.api.adapter.ris.dto.RisNormResult;
import legal.shrinkwrap.api.persistence.entity.NormEntity;
import legal.shrinkwrap.api.persistence.entity.NormSectionType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
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
    public void bundeslandFoldsTheUmlautTheSchemaTransliterates() {
        //the response spells them out, the schema does not - all nine have to resolve, because
        //an unresolved one used to be filed as federal law
        assertThat(NormImporter.bundeslandOf("Kärnten")).isEqualTo(Bundesland.KAERNTEN);
        assertThat(NormImporter.bundeslandOf("Niederösterreich")).isEqualTo(Bundesland.NIEDEROESTERREICH);
        assertThat(NormImporter.bundeslandOf("Oberösterreich")).isEqualTo(Bundesland.OBEROESTERREICH);
        assertThat(NormImporter.bundeslandOf("Burgenland")).isEqualTo(Bundesland.BURGENLAND);
        assertThat(NormImporter.bundeslandOf("Salzburg")).isEqualTo(Bundesland.SALZBURG);
        assertThat(NormImporter.bundeslandOf("Steiermark")).isEqualTo(Bundesland.STEIERMARK);
        assertThat(NormImporter.bundeslandOf("Tirol")).isEqualTo(Bundesland.TIROL);
        assertThat(NormImporter.bundeslandOf("Vorarlberg")).isEqualTo(Bundesland.VORARLBERG);
        assertThat(NormImporter.bundeslandOf("Wien")).isEqualTo(Bundesland.WIEN);

        //null is the key of federal law, so only a missing or unknown state may produce it
        assertThat(NormImporter.bundeslandOf(null)).isNull();
        assertThat(NormImporter.bundeslandOf("  ")).isNull();
        assertThat(NormImporter.bundeslandOf("Südtirol")).isNull();
    }

    /**
     * Real values of the Telekommunikationsgesetz 2021, which had lost its long title: RIS puts
     * it on § 0 alone, and the import used to read the whole law off whichever provision came
     * into force last - § 44, on a date still in the future.
     */
    @Test
    public void onlyTheHeadInForceCarriesLongTitleAndEli() {
        LocalDate today = LocalDate.of(2026, 9, 11);

        RisNormResult head = norm(NormDokumenttyp.NORM, LocalDate.of(2024, 1, 1), null,
                "Bundesgesetz, mit dem ein Telekommunikationsgesetz erlassen wird",
                "https://ris.bka.gv.at/eli/bgbl/i/2021/190/P0/NOR40260238");
        RisNormResult paragraph = norm(NormDokumenttyp.PARAGRAPH, LocalDate.of(2026, 10, 1), null,
                null, "https://ris.bka.gv.at/eli/bgbl/i/2021/190/P44/NOR40274117");

        NormEntity law = new NormEntity();
        NormImporter.applyHead(law, List.of(paragraph, head), today);
        assertThat(law.getLangtitel()).startsWith("Bundesgesetz, mit dem ein Telekommunikations");
        assertThat(law.getEli()).endsWith("/P0/NOR40260238");
    }

    @Test
    public void aHeadThatIsNotCurrentLeavesTheStoredTitleAlone() {
        LocalDate today = LocalDate.of(2026, 9, 11);

        NormEntity law = new NormEntity();
        law.setLangtitel("der gespeicherte Titel");
        law.setEli("https://ris.bka.gv.at/eli/bgbl/i/2021/190/P0/NOR40260238");

        //superseded: RIS keeps it and may still correct it
        RisNormResult old = norm(NormDokumenttyp.NORM, LocalDate.of(2021, 11, 1),
                LocalDate.of(2023, 12, 31), "der alte Titel", "…/NOR40238676");
        NormImporter.applyHead(law, List.of(old), today);
        assertThat(law.getLangtitel()).isEqualTo("der gespeicherte Titel");

        //not in force yet
        RisNormResult future = norm(NormDokumenttyp.NORM, LocalDate.of(2027, 1, 1), null,
                "der kuenftige Titel", "…/NOR40999999");
        NormImporter.applyHead(law, List.of(future), today);
        assertThat(law.getLangtitel()).isEqualTo("der gespeicherte Titel");

        //no head at all: the ordinary in-place update of a single provision
        RisNormResult paragraph = norm(NormDokumenttyp.PARAGRAPH, LocalDate.of(2026, 1, 1), null,
                null, "…/NOR40274117");
        NormImporter.applyHead(law, List.of(paragraph), today);
        assertThat(law.getLangtitel()).isEqualTo("der gespeicherte Titel");
        assertThat(law.getEli()).endsWith("/P0/NOR40260238");
    }

    /** RIS marks "never" as 9000-01-01, and such a version must not become the title source. */
    @Test
    public void aVersionNotYetInForceIsNoTitleSource() {
        LocalDate today = LocalDate.of(2026, 9, 11);

        RisNormMetadaten current = new RisNormMetadaten();
        current.setInkrafttreten(LocalDate.of(2026, 1, 1));
        assertThat(NormImporter.inForceAlready(current, today)).isTrue();

        RisNormMetadaten sentinel = new RisNormMetadaten();
        sentinel.setInkrafttreten(LocalDate.of(9000, 1, 1));
        assertThat(NormImporter.inForceAlready(sentinel, today)).isFalse();

        assertThat(NormImporter.inForceAlready(new RisNormMetadaten(), today)).isFalse();
    }

    /**
     * A version resolved later may come into force earlier than one resolved before it - a law
     * starting 1.1.2028 gets an amendment starting 1.10.2027. Neither may become the source of
     * the current values while both are still ahead of us.
     */
    @Test
    public void aVersionStillToComeIsNeverTheTitleSource() {
        LocalDate today = LocalDate.of(2026, 9, 11);

        RisNormResult inForce = norm(NormDokumenttyp.PARAGRAPH, LocalDate.of(2025, 12, 24), null,
                null, "…/P217");
        RisNormResult startsIn2028 = norm(NormDokumenttyp.PARAGRAPH, LocalDate.of(2028, 1, 1), null,
                null, "…/P44");
        RisNormResult amendedToStartIn2027 = norm(NormDokumenttyp.PARAGRAPH, LocalDate.of(2027, 10, 1),
                null, null, "…/P44neu");

        NormEntity law = new NormEntity();
        law.setKurztitel("Telekommunikationsgesetz 2021");

        //the whole law: the newest that actually applies wins, whatever is queued behind it
        assertThat(NormImporter.titleSource(law,
                List.of(startsIn2028, inForce, amendedToStartIn2027), today)).isSameAs(inForce);

        //the nightly update touched the future versions only - then there is nothing current to
        //say, and the stored values stay rather than being overwritten by a date not yet reached
        assertThat(NormImporter.titleSource(law,
                List.of(startsIn2028, amendedToStartIn2027), today)).isNull();
    }

    /** A law before its own start has nothing else to offer, so the first one to apply fills it. */
    @Test
    public void aLawNotYetInForceFallsBackToTheVersionStartingFirst() {
        LocalDate today = LocalDate.of(2026, 9, 11);

        RisNormResult startsIn2028 = norm(NormDokumenttyp.PARAGRAPH, LocalDate.of(2028, 1, 1), null,
                null, "…/P1");
        RisNormResult startsIn2027 = norm(NormDokumenttyp.PARAGRAPH, LocalDate.of(2027, 10, 1), null,
                null, "…/P2");

        assertThat(NormImporter.titleSource(new NormEntity(),
                List.of(startsIn2028, startsIn2027), today)).isSameAs(startsIn2027);
    }

    /**
     * The type is free text in state law - 33 spellings across the corpus, typos included - so
     * the filter matches leniently, and the wildcard has to keep a complete mirror complete.
     */
    @Test
    public void theTypeFilterTakesEverythingUnlessItIsNarrowed() {
        NormImporter importer = new NormImporter(null, null, null, null);

        for (String everything : List.of("*", "", "   ")) {
            importer.importTypes = everything;
            assertThat(importer.importsType("BG")).isTrue();
            assertThat(importer.importsType("V")).isTrue();
            assertThat(importer.importsType(null)).isTrue();
        }

        importer.importTypes = "BG,LG,Gesetz,Landesgesetz,LVG,VG,G";
        assertThat(importer.importsType("BG")).isTrue();
        assertThat(importer.importsType("LVG")).isTrue();
        //RIS pads and varies the case of the state types
        assertThat(importer.importsType(" lg ")).isTrue();
        assertThat(importer.importsType("Gesetz")).isTrue();

        assertThat(importer.importsType("V")).isFalse();
        assertThat(importer.importsType("Verordnung")).isFalse();
        assertThat(importer.importsType("Vertrag – Schweiz")).isFalse();
        assertThat(importer.importsType(null)).isFalse();
    }

    /** Real heads: the Kesselgesetz ended 2016, TKG 2003 in 2021, a law in force has no end. */
    @Test
    public void repealedLawsAreTakenFromTheCutOffOn() {
        NormImporter importer = new NormImporter(null, null, null, null);
        RisNormMetadaten kesselgesetz = new RisNormMetadaten();
        kesselgesetz.setAusserkrafttreten(LocalDate.of(2016, 4, 19));
        RisNormMetadaten longGone = new RisNormMetadaten();
        longGone.setAusserkrafttreten(LocalDate.of(1995, 6, 30));
        RisNormMetadaten inForce = new RisNormMetadaten();

        importer.repealedSince = "2000-01-01";
        assertThat(importer.importsPeriod(kesselgesetz)).isTrue();
        assertThat(importer.importsPeriod(longGone)).isFalse();
        assertThat(importer.importsPeriod(inForce)).isTrue();

        //the day itself still counts
        RisNormMetadaten onTheDay = new RisNormMetadaten();
        onTheDay.setAusserkrafttreten(LocalDate.of(2000, 1, 1));
        assertThat(importer.importsPeriod(onTheDay)).isTrue();

        importer.repealedSince = "";
        assertThat(importer.importsPeriod(longGone)).isTrue();
    }

    private static RisNormResult norm(NormDokumenttyp typ, LocalDate from, LocalDate until,
                                      String titel, String eli) {
        RisNormMetadaten metadaten = new RisNormMetadaten();
        metadaten.setDokumenttyp(typ);
        metadaten.setInkrafttreten(from);
        metadaten.setAusserkrafttreten(until);
        metadaten.setTitel(titel);
        metadaten.setEli(eli);
        return new RisNormResult(null, metadaten, null, List.of());
    }

    /** Real values: RIS pads the short title of the APG and nearly every Kundmachungsorgan. */
    @Test
    public void onlyTheEdgesAreStripped() {
        assertThat(NormImporter.stripped("Allgemeines Pensionsgesetz ")).isEqualTo("Allgemeines Pensionsgesetz");
        assertThat(NormImporter.stripped("BGBl. I Nr. 142/2004 zuletzt geändert durch BGBl. I Nr. 25/2025 "))
                .isEqualTo("BGBl. I Nr. 142/2004 zuletzt geändert durch BGBl. I Nr. 25/2025");
        assertThat(NormImporter.stripped("BG\n")).isEqualTo("BG");
        //the line breaks inside the long title are content
        assertThat(NormImporter.stripped("Allgemeines Pensionsgesetz (APG)<br/>StF: BGBl. I Nr. 142/2004<br/>"))
                .isEqualTo("Allgemeines Pensionsgesetz (APG)<br/>StF: BGBl. I Nr. 142/2004<br/>");
        assertThat(NormImporter.stripped(null)).isNull();
    }

    /** The year separates TKG 2003 from TKG 2021; only the full key set drops it. */
    @Test
    public void theNameAsWrittenKeepsItsYear() {
        assertThat(NormAbbreviations.asWritten("TKG 2003")).containsExactly("tkg 2003", "tkg2003");
        assertThat(NormAbbreviations.asWritten("TKG\u00a02003")).containsExactly("tkg 2003", "tkg2003");
        assertThat(NormAbbreviations.asWritten("StVO")).containsExactly("stvo");
        assertThat(NormAbbreviations.normalize("TKG 2003")).contains("tkg");
        assertThat(NormAbbreviations.asWritten(" ")).isEmpty();
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
