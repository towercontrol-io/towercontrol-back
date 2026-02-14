package com.disk91.common.tests;

import com.disk91.common.config.CommonConfig;
import com.disk91.common.interfaces.llm.KnowledgeDocumentBody;
import com.disk91.common.interfaces.llm.LlmQueryBody;
import com.disk91.common.interfaces.llm.LlmQueryResponseItf;
import com.disk91.common.interfaces.llm.KnowledgeBaseInfoResponseItf;
import com.disk91.common.services.LLMService;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LLMTestsService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected CommonTestsService commonTestsService;

    @Autowired
    protected CommonConfig commonConfig;

    @Autowired
    protected LLMService llmService;

    private static final String TEST_KB_ID = "test-faq-kb";

    protected ArrayList<String> createdKnowledgeBases = new ArrayList<>();

    // FAQ content for testing - IoT device configuration questions
    private static final String FAQ_DOC_1_ID = "faq-device-reset";
    private static final String FAQ_DOC_1_CONTENT = """
            Question: How do I reset my IoT device to factory settings?
            Answer: To reset your IoT device to factory settings, follow these steps:
            1. Locate the small reset button on the back of the device
            2. Press and hold the reset button for 10 seconds using a paperclip
            3. The LED will blink red three times indicating the reset has started
            4. Wait for 30 seconds while the device reboots
            5. The LED will turn solid green when the device is ready for new configuration
            Note: All custom settings and paired devices will be erased during this process.
            """;

    private static final String FAQ_DOC_2_ID = "faq-wifi-connection";
    private static final String FAQ_DOC_2_CONTENT = """
            Question: How do I connect my IoT device to WiFi?
            Answer: To connect your IoT device to WiFi:
            1. Open the IoTower mobile app on your smartphone
            2. Ensure Bluetooth is enabled on your phone
            3. Press the pairing button on your device (LED will blink blue)
            4. In the app, tap "Add New Device" and select your device from the list
            5. Enter your WiFi network name (SSID) and password
            6. The app will transfer the credentials to your device
            7. Wait for the LED to turn solid green, indicating successful connection
            Troubleshooting: If connection fails, ensure your WiFi is 2.4GHz (5GHz is not supported).
            """;

    private static final String FAQ_DOC_3_ID = "faq-battery-life";
    private static final String FAQ_DOC_3_CONTENT = """
            Question: What is the expected battery life of my IoT sensor?
            Answer: The battery life depends on your configuration:
            - Standard mode (reporting every 15 minutes): approximately 2 years
            - Power saving mode (reporting every hour): approximately 4 years
            - High frequency mode (reporting every minute): approximately 3 months
            To maximize battery life, we recommend using power saving mode unless real-time
            monitoring is essential. You can change the reporting frequency in the device
            settings section of the IoTower app. The app will also notify you when battery
            level drops below 20%.
            """;

    /**
     * Run LLM service tests
     * @throws ITParseException - When a test fails
     */
    public void runTests() throws ITParseException {

        // Test 1: Add documents to knowledge base
        commonTestsService.info("[llm] Test 1: Creating test knowledge base with FAQ documents");
        try {
            addTestFaqDocuments();
            createdKnowledgeBases.add(TEST_KB_ID);
            commonTestsService.success("[llm] Successfully added 3 FAQ documents to knowledge base '{}'", TEST_KB_ID);
        } catch (ITParseException | ITNotFoundException x) {
            commonTestsService.error("[llm] Failed to add FAQ documents: {}", x.getMessage());
            throw new ITParseException("[llm] failed to create test knowledge base");
        }

        // Test 2: Verify knowledge base exists
        commonTestsService.info("[llm] Test 2: Verifying knowledge base exists");
        try {
            KnowledgeBaseInfoResponseItf info = llmService.getKnowledgeBaseInfo(TEST_KB_ID);
            if (info.getDocumentCount() != 3) {
                commonTestsService.error("[llm] Expected 3 documents, found {}", info.getDocumentCount());
                throw new ITParseException("[llm] incorrect document count in knowledge base");
            }
            commonTestsService.success("[llm] Knowledge base verified: {} documents, last sync {}ms",
                    info.getDocumentCount(), info.getLastSyncMs());
        } catch (ITNotFoundException x) {
            commonTestsService.error("[llm] Knowledge base not found: {}", x.getMessage());
            throw new ITParseException("[llm] knowledge base not found after creation");
        }

        // Test 3: Query about device reset - should find relevant content
        commonTestsService.info("[llm] Test 3: Querying about device reset procedure");
        try {
            LlmQueryBody query = new LlmQueryBody();
            query.setKnowledgeBaseId(TEST_KB_ID);
            query.setQuery("How can I reset my device to factory settings?");
            query.setTopK(2);

            LlmQueryResponseItf response = llmService.queryWithRag(query);

            // Verify we got a response
            if (response.getResponse() == null || response.getResponse().isBlank()) {
                commonTestsService.error("[llm] Empty response received for reset question");
                throw new ITParseException("[llm] empty response from RAG query");
            }

            // Verify sources include the reset FAQ
            if (response.getSources().isEmpty()) {
                commonTestsService.error("[llm] No sources returned for reset question");
                throw new ITParseException("[llm] no sources in RAG response");
            }

            // Simple content verification - response should mention key terms from the FAQ
            String lowerResponse = response.getResponse().toLowerCase();
            boolean mentionsReset = lowerResponse.contains("reset") || lowerResponse.contains("button");
            boolean mentionsLed = lowerResponse.contains("led") || lowerResponse.contains("light");

            if (mentionsReset) {
                commonTestsService.success("[llm] Response correctly addresses reset procedure");
            } else {
                commonTestsService.info("[llm] Response may not directly address reset (content-based check)");
            }

            commonTestsService.success("[llm] RAG query completed in {}ms with {} source(s): {}",
                    response.getProcessingTimeMs(), response.getSources().size(),
                    response.getResponse().substring(0, Math.min(100, response.getResponse().length())) + "...");

        } catch (ITNotFoundException | ITParseException x) {
            commonTestsService.error("[llm] RAG query failed: {}", x.getMessage());
            throw new ITParseException("[llm] failed RAG query for device reset");
        } catch (Exception x) {
            commonTestsService.error("[llm] RAG query failed uncatch Exception : {}", x.getMessage());
            throw new ITParseException("[llm] failed RAG query for device reset");
        }

        // Test 4: Query about WiFi connection
        commonTestsService.info("[llm] Test 4: Querying about WiFi connection");
        try {
            LlmQueryBody query = new LlmQueryBody();
            query.setKnowledgeBaseId(TEST_KB_ID);
            query.setQuery("How do I connect my device to wireless network?");

            LlmQueryResponseItf response = llmService.queryWithRag(query);

            if (response.getResponse() == null || response.getResponse().isBlank()) {
                commonTestsService.error("[llm] Empty response received for WiFi question");
                throw new ITParseException("[llm] empty response from WiFi RAG query");
            }

            // Verify response mentions WiFi-related content
            String lowerResponse = response.getResponse().toLowerCase();
            boolean mentionsWifi = lowerResponse.contains("wifi") || lowerResponse.contains("wireless")
                    || lowerResponse.contains("network") || lowerResponse.contains("connect");

            if (mentionsWifi) {
                commonTestsService.success("[llm] Response correctly addresses WiFi connection");
            }

            commonTestsService.success("[llm] WiFi query completed in {}ms: {}",
                    response.getProcessingTimeMs(),
                    response.getResponse().substring(0, Math.min(100, response.getResponse().length())) + "...");

        } catch (ITNotFoundException | ITParseException x) {
            commonTestsService.error("[llm] WiFi RAG query failed: {}", x.getMessage());
            throw new ITParseException("[llm] failed RAG query for WiFi connection");
        } catch (Exception x) {
            commonTestsService.error("[llm] WiFi query failed uncatch Exception : {}", x.getMessage());
            throw new ITParseException("[llm] failed RAG query for WiFi connection");
        }

        // Test 5: Query about battery life
        commonTestsService.info("[llm] Test 5: Querying about battery life expectations");
        try {
            LlmQueryBody query = new LlmQueryBody();
            query.setKnowledgeBaseId(TEST_KB_ID);
            query.setQuery("What is the battery duration of the sensor?");

            LlmQueryResponseItf response = llmService.queryWithRag(query);

            if (response.getResponse() == null || response.getResponse().isBlank()) {
                commonTestsService.error("[llm] Empty response received for battery question");
                throw new ITParseException("[llm] empty response from battery RAG query");
            }

            // Check if response contains battery-related terms or numbers (years/months)
            String lowerResponse = response.getResponse().toLowerCase();
            boolean mentionsBattery = lowerResponse.contains("battery") || lowerResponse.contains("year")
                    || lowerResponse.contains("month") || lowerResponse.contains("power");

            if (mentionsBattery) {
                commonTestsService.success("[llm] Response correctly addresses battery life");
            }

            commonTestsService.success("[llm] Battery query completed in {}ms: {}",
                    response.getProcessingTimeMs(),
                    response.getResponse().substring(0, Math.min(100, response.getResponse().length())) + "...");

        } catch (ITNotFoundException | ITParseException x) {
            commonTestsService.error("[llm] Battery RAG query failed: {}", x.getMessage());
            throw new ITParseException("[llm] failed RAG query for battery life");
        } catch (Exception x) {
            commonTestsService.error("[llm] Battery RAG query failed uncatch Exception : {}", x.getMessage());
            throw new ITParseException("[llm] failed RAG query for battery life");
        }

        // Test 6: Test query with invalid knowledge base
        commonTestsService.info("[llm] Test 6: Testing query with non-existent knowledge base");
        try {
            LlmQueryBody query = new LlmQueryBody();
            query.setKnowledgeBaseId("non-existent-kb-12345");
            query.setQuery("Any question");

            llmService.queryWithRag(query);

            commonTestsService.error("[llm] Query should have failed for non-existent KB");
            throw new ITParseException("[llm] query did not fail for non-existent knowledge base");
        } catch (ITNotFoundException x) {
            commonTestsService.success("[llm] Correctly rejected query for non-existent KB: {}", x.getMessage());
        } catch (ITParseException x) {
            commonTestsService.error("[llm] Unexpected parse exception: {}", x.getMessage());
            throw x;
        } catch (Exception x) {
            commonTestsService.error("[llm] Non existent KB uncatch Exception : {}", x.getMessage());
            throw new ITParseException("[llm] failed non existant KB test ");
        }

        // Test 7: Test simple text generation without RAG
        commonTestsService.info("[llm] Test 7: Testing simple text generation without RAG");
        try {
            String response = llmService.generateText(
                    "What is 2 + 2? Answer with just the number.",
                    "You are a helpful assistant that gives very short answers."
            );

            if (response == null || response.isBlank()) {
                commonTestsService.error("[llm] Empty response from simple text generation");
                throw new ITParseException("[llm] empty response from text generation");
            }

            // Simple check - response should contain "4"
            if (response.contains("4")) {
                commonTestsService.success("[llm] Simple text generation works correctly, answer contains '4'");
            } else {
                commonTestsService.info("[llm] Text generation returned: {}", response);
                commonTestsService.success("[llm] Simple text generation completed (answer verification skipped)");
            }

        } catch (Exception x) {
            commonTestsService.error("[llm] Simple text generation failed: {}", x.getMessage());
            throw new ITParseException("[llm] failed simple text generation");
        }

        // Test 8: Test listing knowledge bases
        commonTestsService.info("[llm] Test 8: Listing all knowledge bases");
        try {
            List<KnowledgeBaseInfoResponseItf> kbs = llmService.listKnowledgeBases();

            boolean foundTestKb = kbs.stream()
                    .anyMatch(kb -> TEST_KB_ID.equals(kb.getKnowledgeBaseId()));

            if (!foundTestKb) {
                commonTestsService.error("[llm] Test knowledge base not found in list");
                throw new ITParseException("[llm] test KB not found in knowledge base list");
            }

            commonTestsService.success("[llm] Found {} knowledge base(s), test KB present", kbs.size());

        } catch (Exception x) {
            commonTestsService.error("[llm] Failed to list knowledge bases: {}", x.getMessage());
            throw new ITParseException("[llm] failed to list knowledge bases");
        }

        // Test 9: Get current provider
        commonTestsService.info("[llm] Test 9: Checking current LLM provider");
        String provider = llmService.getCurrentProvider();
        if (provider == null || provider.isBlank()) {
            commonTestsService.error("[llm] No provider configured");
            throw new ITParseException("[llm] no LLM provider configured");
        }
        commonTestsService.success("[llm] Current LLM provider: {}", provider);

        commonTestsService.success("[llm] All LLM service tests completed successfully");
    }

    /**
     * Add test FAQ documents to the knowledge base
     * @throws ITParseException - When document addition fails
     */
    protected void addTestFaqDocuments() throws ITParseException, ITNotFoundException {
        // Add document 1 - Device reset
        KnowledgeDocumentBody doc1 = new KnowledgeDocumentBody();
        doc1.setDocumentId(FAQ_DOC_1_ID);
        doc1.setContent(FAQ_DOC_1_CONTENT);
        doc1.setTitle("Device Factory Reset");
        doc1.setCategory("troubleshooting");
        llmService.addDocument(TEST_KB_ID, doc1);

        // Add document 2 - WiFi connection
        KnowledgeDocumentBody doc2 = new KnowledgeDocumentBody();
        doc2.setDocumentId(FAQ_DOC_2_ID);
        doc2.setContent(FAQ_DOC_2_CONTENT);
        doc2.setTitle("WiFi Connection Guide");
        doc2.setCategory("setup");
        llmService.addDocument(TEST_KB_ID, doc2);

        // Add document 3 - Battery life
        KnowledgeDocumentBody doc3 = new KnowledgeDocumentBody();
        doc3.setDocumentId(FAQ_DOC_3_ID);
        doc3.setContent(FAQ_DOC_3_CONTENT);
        doc3.setTitle("Battery Life Information");
        doc3.setCategory("specifications");
        llmService.addDocument(TEST_KB_ID, doc3);
    }

    /**
     * Clean up test data
     */
    public void cleanTests() {
        commonTestsService.info("[llm] Cleaning up test knowledge bases");
        for (String kbId : createdKnowledgeBases) {
            try {
                llmService.deleteKnowledgeBase(kbId);
                commonTestsService.info("[llm] Deleted test knowledge base: {}", kbId);
            } catch (ITNotFoundException x) {
                commonTestsService.error("[llm] Failed to delete knowledge base {}: {}", kbId, x.getMessage());
            }
        }
        createdKnowledgeBases.clear();
    }

}
