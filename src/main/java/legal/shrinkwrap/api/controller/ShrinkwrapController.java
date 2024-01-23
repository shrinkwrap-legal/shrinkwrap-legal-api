package legal.shrinkwrap.api.controller;


import legal.shrinkwrap.api.dto.GeneralDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShrinkwrapController {

    private static final Logger LOG = LoggerFactory.getLogger(ShrinkwrapController.class);

    @GetMapping("/hello")
    public String hello() {
        return "Hello, I am a shrink wrapper";
    }


    @GetMapping("/general")
    public GeneralDto getGeneralInfo() {
        LOG.info("general info called");
        return new GeneralDto("Shringwrap.Legal", "0.0.1");
    }
}
