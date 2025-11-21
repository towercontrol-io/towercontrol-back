package com.disk91.common.tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CommonTestsService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_BLUE = "\u001B[34m";

    public void info(String message, Object... args) {
        log.info(ANSI_BLUE+"[test] "+ANSI_RESET+message, args);
    }

    public void success(String message, Object... args) {
        log.info(ANSI_BLUE+"[test] "+ANSI_GREEN+message+ANSI_RESET, args);
    }

    public void error(String message, Object... args) {
        log.error(ANSI_BLUE+"[test] "+ANSI_RED+message+ANSI_RESET, args);
    }


}
