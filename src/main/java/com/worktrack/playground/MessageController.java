package com.worktrack.playground;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;


@RestController
@Profile("local-config")
@RefreshScope
public class MessageController {

    @Value("${message}")
    private String message;

    @GetMapping("/message")
    // Demo endpoint for dynamic config loading via refresh scope.
    public String getMessage() {
        return message;
    }
}
