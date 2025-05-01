package legal.shrinkwrap.api.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * https://entwickler.de/reader/reading-collection/6662dda8be48cf002cbd6200/a1d1eb6d925500dcee7482fb
 *
 * https://spring.io/blog/2024/03/06/function-calling-in-java-and-spring-ai-using-the-latest-mistral-ai-api
 */
@RestController
public class SpringChatClientController {

    private final ChatClient chatClient;

    public SpringChatClientController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/client")
    public String generation(String question) {
        String answer = chatClient.prompt().user(question).call().content();
        return answer;
    }

    @GetMapping("/pdf")
    public String infoPdf(String question) {
        /*
        List<Document> documents = vectorStore.similaritySearch(question);

        String inlined = documents.stream()
                .map(Document::getContent)
                .collect(Collectors.joining(System.lineSeparator()));

        String prompt = """
                Du bist ein Rechtsanwalt im Bereich des Sozialversicherungsrechts.
                Zur Beantwortung kannst Du in den Daten in DOCUMENTS nachsehen.
                Es ist nur erlaubt antworten von DOCUMENTS zu verwenden.
                Wenn du nicht sicher bist, dann schreib es als Antwort.
                
                DOCUMENTS:
                {documents}
                """;

        Message system = new SystemPromptTemplate(prompt)
                .createMessage(Map.of("documents", inlined));
        UserMessage user = new UserMessage(question);
        Prompt combinedPrompt = new Prompt(List.of(system, user));

        String answer = chatClient.prompt(combinedPrompt).call().content();
        return answer;

         */

        return "TEST";
    }

}
