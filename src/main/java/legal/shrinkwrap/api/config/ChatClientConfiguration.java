package legal.shrinkwrap.api.config;

import org.springframework.ai.autoconfigure.chat.client.ChatClientBuilderConfigurer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * include both dependencies for openai and ollama
 * https://github.com/spring-projects/spring-ai/issues/1226
 *
 * https://www.innoq.com/de/blog/2024/02/ollama-llm-spring-ai-rag/
 * https://spring.io/blog/2024/07/26/spring-ai-with-ollama-tool-support
 */
@Configuration
public class ChatClientConfiguration {

    @Bean
    @Scope("prototype")
    @Qualifier("openAiChatClientBuilder")
    ChatClient.Builder openAiChatClientBuilder(ChatClientBuilderConfigurer chatClientBuilderConfigurer,
                                               @Qualifier("openAiChatModel") ChatModel chatModel) {
        ChatClient.Builder builder = ChatClient.builder(chatModel);
        return chatClientBuilderConfigurer.configure(builder);
    }


}
