package com.disk91;

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

}
