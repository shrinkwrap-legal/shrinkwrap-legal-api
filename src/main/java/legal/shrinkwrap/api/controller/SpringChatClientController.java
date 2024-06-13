package legal.shrinkwrap.api.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
