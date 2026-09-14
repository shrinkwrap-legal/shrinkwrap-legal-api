package legal.shrinkwrap.api.persistence.entity;

/**
 * The kind of provision a document holds. {@link #NORM} is the head document of a law, which
 * RIS delivers as "§ 0" and which makes up roughly a tenth of the corpus.
 */
public enum NormSectionType {
    PARAGRAPH,
    ARTIKEL,
    ANLAGE,
    NORM
}
