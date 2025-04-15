package legal.shrinkwrap.api.config;

import org.springframework.context.annotation.Configuration;


@Configuration
public class SpringAIConfiguration {


    /*
    @Profile("openai")
    @Bean
    VectorStore vectorStore(EmbeddingModel openAiEmbeddingModel, @Value("classpath:ASVG-01.06.2024.pdf") Resource pdf) {
        SimpleVectorStore vectorStore = new SimpleVectorStore(openAiEmbeddingModel);
        PagePdfDocumentReader reader = new PagePdfDocumentReader(pdf);
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> documents = splitter.apply(reader.get());
        vectorStore.accept(documents);
        return vectorStore;
    }

     */

    /*
    @Profile("!openai")
    @Bean("vectorStore")
    VectorStore vectorStoreDummy(EmbeddingModel openAiEmbeddingModel) {
        SimpleVectorStore vectorStore = new SimpleVectorStore(openAiEmbeddingModel);
        return vectorStore;
    }

     */


}
