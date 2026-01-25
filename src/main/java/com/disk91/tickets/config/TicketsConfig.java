package com.disk91.tickets.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = {"file:configuration/tickets.properties"}, ignoreResourceNotFound = true)
public class TicketsConfig {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // ----------------------------------------------
    // Integration setup
    // ----------------------------------------------

    // How the audit service is connected to the different service instances
    @Value("${tickets.integration.medium:memory}")
    protected String ticketsIntegrationMedium;
    public String getTicketsIntegrationMedium() {
        return ticketsIntegrationMedium;
    }

    @Value("${tickets.integration.timeout.ms:10000}")
    protected long ticketsIntegrationTimeout;
    public long getTicketsIntegrationTimeout() {
        return ticketsIntegrationTimeout;
    }

    // ----------------------------------------------
    // Public access setup
    // ----------------------------------------------
    @Value("${tickets.allow.public.creation:true}")
    protected boolean ticketsAllowPublicCreation;
    public boolean isTicketsAllowPublicCreation() {
        return ticketsAllowPublicCreation;
    }

    @Value("${tickets.allow.public.view:true}")
    protected boolean ticketsAllowPublicView;
    public boolean isTicketsAllowPublicView() {
        return ticketsAllowPublicView;
    }

    @Value("${tickets.manager.email:}")
    protected String ticketsManagerEmail;
    public String getTicketsManagerEmail() {
        return ticketsManagerEmail;
    }

    @Value("${tickets.public.creation.email.filters:}")
    protected String ticketsPublicCreationEmailFilters;
    public String getTicketsPublicCreationEmailFilters() {
        return ticketsPublicCreationEmailFilters;
    }

    @Value("${tickets.public.view.key.expiration:172800}") // 2 days
    protected long ticketsPublicViewKeyExpiration;
    public long getTicketsPublicViewKeyExpiration() {
        return ticketsPublicViewKeyExpiration;
    }

    @Value("${tickets.allow.anonymous.view:true}")
    protected boolean ticketsAllowAnonymousView;
    public boolean isTicketsAllowAnonymousView() {
        return ticketsAllowAnonymousView;
    }

    @Value("${tickets.anonymous.view.key.expiration:172800}") // 2 days
    protected long ticketsAnonymousViewKeyExpiration;
    public long getTicketsAnonymousViewKeyExpiration() {
        return ticketsAnonymousViewKeyExpiration;
    }

    @Value("${tickets.anonymous.encryption.iv:4fee91822bce48331d6ac0d69d978492")
    protected String ticketsAnonymousEncryptionIv;
    public String getTicketsAnonymousEncryptionIv() {
        return ticketsAnonymousEncryptionIv;
    }

    @Value("${ticket.anonymous.content.filters:}")
    protected String ticketAnonymousContentFilters;
    public String getTicketAnonymousContentFilters() {
        return ticketAnonymousContentFilters;
    }

    @Value("${ticket.anonymous.hash.salt:6dea91831bce6b331d6ac0d69d973692}")
    protected String ticketAnonymousHashSalt;
    public String getTicketAnonymousHashSalt() {
        return ticketAnonymousHashSalt;
    }

    // Param !0! ticketId !1! authKey
    @Value("${ticket.anonymous.response.link:}")
    protected String ticketAnonymousResponseLink;
    public String getTicketAnonymousResponseLink(String ticketId, String authKey) {
        String _ticketAnonymousResponseLink = this.ticketAnonymousResponseLink;
        _ticketAnonymousResponseLink = _ticketAnonymousResponseLink.replace("!0!", ticketId);
        _ticketAnonymousResponseLink = _ticketAnonymousResponseLink.replace("!1!", authKey);
        return _ticketAnonymousResponseLink;
    }

    @Value("${tickets.discord.webhook.url:}")
    protected String ticketsDiscordWebhookUrl;
    public String getTicketsDiscordWebhookUrl() {
        return ticketsDiscordWebhookUrl;
    }

    @Value("${tickets.email.user.update:true}")
    protected boolean ticketsEmailUserUpdate;
    public boolean isTicketsEmailUserUpdate() {
        return ticketsEmailUserUpdate;
    }

}
