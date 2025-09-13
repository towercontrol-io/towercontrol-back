/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2024.
 *
 *    Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 *    and associated documentation files (the "Software"), to deal in the Software without restriction,
 *    including without limitation the rights to use, copy, modify, merge, publish, distribute,
 *    sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 *    furnished to do so, subject to the following conditions:
 *
 *    The above copyright notice and this permission notice shall be included in all copies or
 *    substantial portions of the Software.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *    FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 *    OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *    WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 *    IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.disk91.users.services;

import com.disk91.common.config.CommonConfig;
import com.disk91.common.pdb.entities.Param;
import com.disk91.common.pdb.repositories.ParamRepository;
import com.disk91.common.tools.HexCodingTools;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.users.config.UsersConfig;
import com.disk91.users.mdb.entities.User;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Predicate;

@Service
public class UsersInit {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected CommonConfig commonConfig;

    @Autowired
    protected UsersConfig usersConfig;

    @Autowired
    protected  UserCreationService userCreationService;

    @Autowired
    protected ParamRepository paramRepository;

    @PostConstruct
    public void init() {
        log.debug("[users] Init");

        // Ensure the encryption keys are correctly set
        byte[] serverKey = HexCodingTools.getByteArrayFromHexString(commonConfig.getEncryptionKey());
        byte[] applicationKey = HexCodingTools.getByteArrayFromHexString(commonConfig.getApplicationKey());
        if ( serverKey.length != 16 || applicationKey.length != 16 ) {
            log.error("[users] ************************************");
            log.error("[users] Encryption key are not 16 bytes long");
            log.error("[users] ************************************");
            System.exit(1);
        }
        byte[] sessionKey = HexCodingTools.getByteArrayFromHexString(usersConfig.getUsersSessionKey());
        if ( sessionKey.length != 32 ) {
            log.error("[users] ************************************");
            log.error("[users] Session key is not 32 bytes long");
            log.error("[users] ************************************");
            System.exit(1);
        }

        // Init the default parameters
        Param p = paramRepository.findByParamKey("users.condition.version");
        if ( p == null ) {
            p = new Param();
            p.setParamKey("users.condition.version");
            p.setStringValue("initial");
            paramRepository.save(p);
        }

        // We need to create the default ROLES when not existing

        // Create the super admin user if not existing (executed only a single time)
        p = paramRepository.findByParamKey("users.superadmin.creation");
        if ( p == null ) {
            userCreationService.createSuperAdmin();
        }

        // Database migration scripts
        p = paramRepository.findByParamKey("users.mongo.schema.version");
        if ( p == null ) {
            p = new Param();
            p.setParamKey("users.mongo.schema.version");
            p.setLongValue(1);
            paramRepository.save(p);
        }
        boolean migrationCompleted = false;
        while ( !migrationCompleted ) {
            switch ((int)p.getLongValue()) {
                case 1:
                    log.info("[users] [migration] [Mongo] Migrating Mongo database schema from version 1 to version 2");
                    try {

                        processAllUsers(u->{
                            if ( u.getUserSearch() == null ) {
                                u.setKeys(commonConfig.getEncryptionKey(), commonConfig.getApplicationKey());
                                try {
                                    u.setEncLoginSearch(u.getEncEmail());
                                } catch ( ITParseException x ) {
                                    // skip, the user email is not accessible
                                    return false;
                                } finally {
                                    u.cleanKeys();
                                }
                                return true;
                            } else return false;
                        });
                    } catch (Exception e) {
                        log.error("[users] [migration] [Mongo] Migration failed, please check the database is available and retry later", e);
                        System.exit(1);
                    }
                    p.setLongValue(2);
                    paramRepository.save(p);
                    break;
                case 2: // Current version
                    migrationCompleted = true;
                    log.info("[users] [migration] [Mongo] Migration completed");
                    break;
                default:
                    log.error("[users] [migration] [Mongo] Database schema version is unknown, something is wrong !");
                    break;
            }
        }

    }

    // ------------------------------------------------------------------
    // DB Migration functions
    // ------------------------------------------------------------------

    @Autowired
    protected MongoTemplate mongoTemplate;

    public void processAllUsers(Predicate<User> action) {
        final int batchSize = 100;
        int proceeded = 0;
        int skipped = 0;
        List<User> users = null;
        do {
            Query query = new Query()
                    .with(Sort.by("login").ascending())
                    .skip(proceeded)
                    .limit(batchSize);
            users = mongoTemplate.find(query, User.class);
            for ( User u : users ) {
                if ( action.test(u) ) {
                    mongoTemplate.save(u);
                } else skipped++;
            }
            proceeded += users.size();
        } while (users.size() == batchSize);
        log.info("[users] [migration] [Mongo] Processed {} Users, skipped {}", proceeded, skipped);
    }

}
