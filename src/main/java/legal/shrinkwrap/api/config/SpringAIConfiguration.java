package legal.shrinkwrap.api.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;

import java.util.List;


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
