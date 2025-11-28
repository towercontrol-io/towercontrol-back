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
package com.disk91.capture.services;

import com.disk91.capture.mdb.entities.Protocols;
import com.disk91.capture.mdb.repositories.ProtocolsRepository;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CaptureProtocolsCache {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // The idea is to have a cache that can be extended, similar to roles, and that allows defining protocols with subtypes
    // to properly represent injections and to adapt to protocols for decoding, metadata interpretation and other tasks.

    // global cache of roles
    protected ConcurrentHashMap<String,Protocols> protocolsCache = new ConcurrentHashMap<>();

    @Autowired
    protected ProtocolsRepository protocolsRepository;

    // Platform protocols definitions, the database only stores custom protocols
    private final String [] pfProtocols = {
        "{'id':'system-lorawan-helium-chirpstack-v4'," +
                "'protocolFamily':'protocol-lorawan', 'protocolType':'protocol-helium', 'protocol-version':'protocol-version-chirpstack-v4', 'description':'lorawan-helium-chirpstack-v4','enDescription':'Helium running Chirpstack V4', " +
                "'processingClassName':'com.disk91.capture.drivers.LoraWanHeliumChirpstackV4Driver', 'creationBy':'system', 'creationMs':0, 'defaultWideOpen':false, " +
                "'mandatoryFields':[ " +
                  "{ 'name':'protocol-server-api-endpoint', 'valueType':'string,^http[s]://', 'description':'protocol-lorawan-helium-chirp-v4-api-endpoint', 'enDescription': 'Helium Chirpstack Api Endpoint'}" +
                "]" +
        "}",
    };

    /**
     * Return the number of pfProtocols expected in the cache from the initialization list above
     * This methods is mostly for tests
     */
    public int __countPfProtocols() { return pfProtocols.length; }

    /**
     * Returns the number of Protocols in cache, also mostly used by tests.
     */
    public int __countProtocolsInCache() { return this.protocolsCache.size(); }

    @PostConstruct
    public void initProtocolCache() {
        log.debug("[capture] Protocols init");
        this.loadProtocols();
        if ( this.verifyDefaultProtocols() ) {
            // some modification have been made, better reloading.
            this.loadProtocols();
        }
    }

    /**
     * Search a Protocol in the Cache and return a clone of it to make sure it will not
     * be modified by the caller
     */
    public Protocols getProtocol(
        String id
    ) throws ITNotFoundException {
        Protocols r = protocolsCache.get(id);
        if ( r == null ) {
            log.warn("[capture] Protocol {} searched but not found", id);
            throw new ITNotFoundException("capture-protocol-not-found");
        }
        return r.clone();
    }

    /**
     * Return the list of protocols available, clone it to avoid modification by the caller
     * @return list of protocols
     */
    public List<Protocols> getProtocols() {
        ArrayList<Protocols> _proto = new ArrayList<>();
        for ( Protocols r : this.protocolsCache.values() ) {
            _proto.add(r.clone());
        }
        return _proto;
    }

    /**
     * Add one Protocol, internal API. When the protocol is created, it is added to the database and
     * added to the cache. We need to update the cache of the different instances and for this
     * we need to call the message bus entity.
     * It's not possible to add an already existing protocol, in this cas it returns ITTooManyException
     * the protocol can exist with ID but mainly because the protocol family/type/version tree.
     */
    public void addProtocol(
       Protocols protocol
    ) throws ITTooManyException, ITParseException {
        if ( protocol.getId() != null && protocolsCache.containsKey(protocol.getId()) ) {
            log.error("[capture] Protocol creation impossible: {} already exists", protocol.getId());
            throw new ITTooManyException("capture-protocol-already-exists");
        }
        for ( Protocols _p : protocolsCache.values() ) {
            if ( _p.getProtocolFamily().compareTo(protocol.getProtocolFamily()) == 0 &&
                 _p.getProtocolType().compareTo(protocol.getProtocolType()) == 0 &&
                 _p.getProtocolVersion().compareTo(protocol.getProtocolVersion()) == 0 ) {
                log.error("[capture] Protocol creation impossible: protocol family/type/version combination already exists");
                throw new ITTooManyException("capture-protocol-already-exists");
            }
        }

        if ( ! protocol.getProtocolFamily().matches("^[a-z\\-]*$") ) {
            log.error("[capture] Protocol family must be lowercase a-z and - only, was {}",protocol.getProtocolFamily());
            throw new ITParseException("protocol-family-invalid-format");
        }
        if ( ! protocol.getProtocolType().matches("^[a-z\\-]*$") ) {
            log.error("[capture] Protocol type must be lowercase a-z and - only, was {}",protocol.getProtocolType());
            throw new ITParseException("protocol-type-invalid-format");
        }
        if ( ! protocol.getProtocolVersion().matches("^[a-z\\-]*$") ) {
            log.error("[capture] Protocol version must be lowercase a-z and - only, was {}",protocol.getProtocolVersion());
            throw new ITParseException("protocol-version-invalid-format");
        }
        if ( ! protocol.getDescription().matches("^[a-z\\-]*$") ) {
            log.error("[capture] Protocol description must be lowercase a-z and - only, was {}",protocol.getDescription());
            throw new ITParseException("protocol-description-invalid-format");
        }

        // database will set the Id.
        if ( protocol.getId() != null ) protocol.setId(null);
        Protocols p = protocolsRepository.save(protocol);
        protocolsCache.put(p.getId(), p);
        // @TODO - send message to other instances to reload roles.
    }



    /**
     * Load or reload the protocols from the database
     * This method is synchronized to avoid multiple reload at the same time
     */
    synchronized private void loadProtocols() {
        List<Protocols> all = protocolsRepository.findAll();
        protocolsCache.clear();
        // load the platform protocol from String structure
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        for ( String r : pfProtocols ) {
            try {
                Protocols p = mapper.readValue(r.replace("'", "\""), Protocols.class);
                p.setCreationMs(Now.NowUtcMs());
                protocolsCache.putIfAbsent(p.getId(), p);
            } catch (JsonProcessingException e) {
                log.error("[capture] Error loading platform protocols {} due to {}", r, e.getMessage());
            }
        }
        // load the custom roles if any
        all.forEach((r) -> protocolsCache.putIfAbsent(r.getId(), r));
        log.info("[capture] {} Roles (re)loaded", protocolsCache.size());
    }

    /**
     * Resync the protocol database with the static definition in pfProtocols
     * this is used when the static definition has changed, and we need to update it.
     * It uses the version for this.
     * The static protocols defined in pfProtocols are not persisting in the database so this is
     * normally not doing many things ... but the function is here for later update
     * structure and may be improved when necessary.
     * Return true if at least one role has been updated and the cache need to be reloaded
     */
    synchronized private boolean verifyDefaultProtocols() {

        boolean modified = false;
        List<Protocols> all = protocolsRepository.findAll();

        // load the platform role from String structure
        HashMap<String,Protocols> defaultProtocols = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        for ( String r : pfProtocols ) {
            try {
                Protocols p = mapper.readValue(r.replace("'", "\""), Protocols.class);
                p.setCreationMs(Now.NowUtcMs());
                defaultProtocols.put(p.getId(),p);
            } catch (JsonProcessingException e) {
                log.error("[capture] Error processing platform protocols {} due to {}", r, e.getMessage());
            }
        }

        // process the db comparison
        for (Protocols r : all ) {
            Protocols _r = defaultProtocols.get(r.getId());
            if ( _r != null ) {
                // role exists in default, check versions
                if (r.getVersion() < _r.getVersion()) {
                    // need to update the role
                    protocolsRepository.save(_r);
                    log.info("[capture] Protocol {} updated to version {}", r.getDescription(), r.getVersion());
                    modified = true;
                }
            }
        }
        return modified;
    }


}
