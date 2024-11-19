package legal.shrinkwrap.api.python;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.python.embedding.utils.GraalPyResources;
import org.springframework.stereotype.Service;

import java.util.List;
//https://www.youtube.com/watch?v=F8GoDqTtSOE
@Service
public class SimplePythonService {


    public SimplePythonService() {

    }

    public void sayHello() {
        String question = "Was ist die Hauptstadt von Österreich?";

        try (Context context = GraalPyResources.createContext()) {
            CreateChatCompletionFunction completionFunction = context.eval("python",
                    // language=python
                    """
                    import os
                    from openapi import OpenAI
                    
                    client = OpenAI(
                        # This is the default and can be omitted
                        api_key=os.environ.get("OPENAPI_API_KEY")
                    )
                    
                    def create_chat_completion(user_input):
                        return client.chat.completions.create(
                            messages=[
                                {
                                    "role": "user",
                                    "content": user_input,
                                }
                            ],
                            model="gpt-3.5-turbo"
                        )
                    create_chat_completion
                    """).as(CreateChatCompletionFunction.class);
            ChatCompletion chatCompletion = completionFunction.apply(question);
            for(Choice choice : chatCompletion.choices()) {
                System.out.println(choice.message().content());
            }
        } catch (PolyglotException e) {
            e.printStackTrace();
        }

    }



    @FunctionalInterface
    public interface CreateChatCompletionFunction {
        ChatCompletion apply(String message);
    }

    public interface ChatCompletion {
        List<Choice> choices();
    }

    public interface Choice {
        ChatCompletionMessage message();
    }
    public interface ChatCompletionMessage {
        String content();
    }
}
