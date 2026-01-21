package com.disk91.tickets.tests;

import com.disk91.common.tests.CommonTestsService;
import com.disk91.common.tools.exceptions.ITParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Service;

@Service
public class TicketsTestsService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected CommonTestsService commonTestsService;

    @Autowired(required = false)
    private AutowireCapableBeanFactory beanFactory;

    Object privTicketsTestsService = null;

    /**
     * This function launch the tests if the NCE edition is present
     * @throws ITParseException
     */
    public void runTests() throws ITParseException {

        try {
            Class<?> clazz = Class.forName("com.disk91.tickets.tests.PrivTicketsTestsService");
            privTicketsTestsService = beanFactory.createBean(clazz);
            commonTestsService.info("[tickets] Run NCE tests");
            privTicketsTestsService.getClass()
                    .getMethod("runTests")
                    .invoke(privTicketsTestsService);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            commonTestsService.info("[tickets] tests skipped (Community Edition) {} ",e.getClass());
        } catch (Exception e) {
            throw new ITParseException(e.getMessage());
        }
    }

    public void cleanTests() throws ITParseException {
        if ( privTicketsTestsService == null ) return;
        try {
            privTicketsTestsService.getClass()
                    .getMethod("cleanTests")
                    .invoke(privTicketsTestsService);
        } catch (NoSuchMethodException | IllegalAccessException ignored) {
        } catch (Exception e) {
            throw new ITParseException(e.getMessage());
        }
    }
}

