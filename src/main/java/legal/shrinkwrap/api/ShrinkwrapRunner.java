package legal.shrinkwrap.api;

import legal.shrinkwrap.api.service.CaseLawImporter;
import legal.shrinkwrap.api.service.NormImporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShrinkwrapRunner implements ApplicationRunner {

    private final CaseLawImporter caseLawImporter;
    private final NormImporter normImporter;

    /**
     * Started here rather than in a @PostConstruct: the imports run for hours, and the case law
     * one would otherwise block the startup until it is done.
     */
    @Value("${do-initial-import:false}")
    boolean doInitialImport;

    @Value("${enable-judicature:true}")
    boolean judicatureEnabled = true;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Shrinkwrap started");

        if (!doInitialImport) {
            return;
        }
        if (judicatureEnabled) {
            new Thread(caseLawImporter::initDB).start();
        }
        //starts its own thread and checks enable-norms itself
        normImporter.importEverything();
    }
}
