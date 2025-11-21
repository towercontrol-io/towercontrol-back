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

/**
 * UserPending -  This class stores account creation requests awaiting email validation and handles confirmations.
 * Entries are automatically removed either upon activation or after a specified period.
 */

package com.disk91.users.mdb.entities;

import com.disk91.common.tools.EncryptionHelper;
import com.disk91.common.tools.HexCodingTools;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users_pending")
@CompoundIndexes({
        @CompoundIndex(name = "activation_key", def = "{'activation': 'hashed'}"),
})
public class UserRegistration {

    @Transient
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Id
    protected String id;

    // Encrypted email to be verified
    protected String email;

    // Requestor IP
    protected String requestorIP;

    // Random String for validation
    protected String validationId;

    // Entry creation date
    protected long creationDate;

    // Entry Expiration date
    protected long expirationDate;

    // Registration code - not mandatory - this is an invitation / registration code given by the admin, allowing
    // certain access & feature
    protected String registrationCode;

    // ===========================


    @Transient
    private static final String __iv = "4fee88822bce7d331d6db0d69d978492";

    public static String getEncodedEmail(String _email, String encryptionKey) throws ITParseException {
        return EncryptionHelper.encrypt(_email, __iv, encryptionKey);
    }

    /**
     * Init the UserPending Structure with the given email ; protect it with encryption and setup the expiration
     * time. Create a random validation string to be used for email validation.
     * @param _email
     * @param expirationMs
     * @throws ITParseException
     */
    public void init(String _email, String _regCode, String _ip, long expirationMs, String encryptionKey)  throws ITParseException {

        this.email = EncryptionHelper.encrypt(_email, __iv, encryptionKey);

        if ( _ip == null || _ip.isEmpty()) {
            _ip = "0.0.0.0";
        }
        this.requestorIP = EncryptionHelper.encrypt(_ip, __iv, encryptionKey);
        if ( this.email == null || this.requestorIP == null ) {
            log.error("[users] Error while encrypting email/IP");
            throw new ITParseException("Email encryption failed");
        }
        this.validationId = HexCodingTools.getRandomHexString(128);
        this.creationDate = Now.NowUtcMs();
        this.expirationDate = this.creationDate + expirationMs;
        this.registrationCode = (_regCode==null)?"":_regCode;
    }

    /**
     * Return the email is the validation key is correct and returned in the right time frame.
     * @param _validationId
     * @return
     * @throws ITParseException
     * @throws ITNotFoundException
     */
    public String verify(String _validationId, String encryptionKey) throws ITParseException, ITNotFoundException {
        if ( expirationDate > Now.NowUtcMs() && this.validationId.equals(_validationId) ) {
            String decryptedEmail = EncryptionHelper.decrypt(this.email, __iv, encryptionKey);
            if (decryptedEmail == null) {
                log.error("[users] Error while decrypting email");
                throw new ITParseException("Email decryption failed");
            }
            return decryptedEmail;
        } else throw new ITNotFoundException("Validation ID is not valid");
    }

    /**
     * Same just to get the email ...
     * @param encryptionKey
     * @return
     * @throws ITParseException
     * @throws ITNotFoundException
     */
    public String getEncEmail(String encryptionKey) throws ITParseException, ITNotFoundException {
        return verify(this.validationId, encryptionKey);
    }


    // ============================
    // GETTER & SETTER

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getValidationId() {
        return validationId;
    }

    public void setValidationId(String validationId) {
        this.validationId = validationId;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public long getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(long expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getRequestorIP() {
        return requestorIP;
    }

    public void setRequestorIP(String requestorIP) {
        this.requestorIP = requestorIP;
    }

    public String getRegistrationCode() {
        return registrationCode;
    }

    public void setRegistrationCode(String registrationCode) {
        this.registrationCode = registrationCode;
    }
}
