package legal.shrinkwrap.api.controller;


import jakarta.annotation.PostConstruct;
import legal.shrinkwrap.api.dto.GeneralDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
public class ShrinkwrapController {
    private static final Logger LOG = LoggerFactory.getLogger(ShrinkwrapController.class);

    @GetMapping("/hello")
    public String hello() {
        return "Hello, I am a shrink wrapper";
    }


    //on extension install, redirect to RIS
    @GetMapping("/extension-install")
    public RedirectView extensionInstall() {
        RedirectView redirect = new RedirectView("https://www.ris.bka.gv.at/Judikatur/#shrinkwrap");
        redirect.setStatusCode(HttpStatusCode.valueOf(303)); //always GET
        return redirect;
    }

    @GetMapping(value = "/general", produces = MediaType.APPLICATION_JSON_VALUE)
    public GeneralDto getGeneralInfo() {
        LOG.info("general info called");
        return new GeneralDto("Shringwrap.Legal", "0.0.1");
    }
}
