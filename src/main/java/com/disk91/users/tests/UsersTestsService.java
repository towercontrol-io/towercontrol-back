package com.disk91.users.tests;

import com.disk91.common.tests.CommonTestsService;
import com.disk91.common.tools.exceptions.ITParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class UsersTestsService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected CommonTestsService commonTestsService;


    public void runTests() throws ITParseException {

        int v = new Random().nextInt(1000);
        if ( v > 500 ) {
            throw new ITParseException("Test exception");
        }

    }

}
