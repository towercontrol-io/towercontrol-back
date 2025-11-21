package com.disk91;

import com.disk91.common.config.CommonConfig;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.users.tests.UsersTestsService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@EnableScheduling
@EnableWebMvc
@Configuration
@EnableAsync
@SpringBootApplication
public class ItcApplication implements CommandLineRunner, ExitCodeGenerator {

    public static boolean requestingExitForStartupFailure = false;

    public static ApplicationContext context;

    public static void main(String[] args) {
        context = SpringApplication.run(ItcApplication.class, args);
        if (ItcApplication.requestingExitForStartupFailure) {
            exit();
        }
    }


    @Override
    public void run(String... args) throws Exception {
        long pid = ProcessHandle.current().pid();
        System.out.println("-------------- GO ("+pid+")--------------");
        testsExecution();
    }

    public static void exit() {

        int exitCode = SpringApplication.exit(context, new ExitCodeGenerator() {
            @Override
            public int getExitCode() {
                return 0;
            }
        });
        // Bug in springboot, calling exit is create a deadlock
        //System.exit(exitCode);
        System.out.println("------------- GONE --------------");
    }

    public int getExitCode() {
        return 0;
    }

    @Autowired
    protected CommonConfig commonConfig;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_BLUE = "\u001B[34m";

    @Autowired
    protected UsersTestsService usersTestsService;

    protected void testsExecution() {
        if ( commonConfig.isCommonTestEnabled() ) {
            System.out.println(ANSI_BLUE+"================ Running Tests ========================"+ANSI_RESET);
            try {
                usersTestsService.runTests();



                System.out.println(ANSI_GREEN+"================ TESTS SUCCESS ========================"+ANSI_RESET);
            } catch (ITParseException e) {
                System.out.println(ANSI_RED+"[ERROR] "+e.getMessage()+ANSI_RESET);
                System.out.println(ANSI_RED+"================ TESTS FAILED ========================"+ANSI_RESET);
                exit();
            }
        }
    }

}
