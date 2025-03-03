package legal.shrinkwrap.api.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class PandocTextWrapper {

    public static String convertHtmlToText(String content) {
        try {
            // Prepare the pandoc command
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "pandoc", "-f", "html", "-t", "plain", "--wrap", "preserve"
            );
            processBuilder.redirectErrorStream(true);

            // Start the process
            Process process = processBuilder.start();

            // Write the content to the process's input stream
            process.getOutputStream().write(content.getBytes(StandardCharsets.UTF_8));
            process.getOutputStream().close();

            // Read the output
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

                //remove empty lines
                List<String> lines = reader.lines().collect(Collectors.toList()); /*
                        .map(String::strip)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList()); */

                // Wait for the process to complete
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    PandocTextWrapper.log.error("Pandoc process exited with code " + exitCode);
                    return null;
                }

                return String.join("\n", lines);
            }

        } catch (IOException | InterruptedException e) {
            PandocTextWrapper.log.error("An error occurred while running pandoc: " + e.getMessage());
            return null;
        }
    }
}
