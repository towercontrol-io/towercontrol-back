package com.disk91.capture.drivers.standard;

import com.disk91.capture.interfaces.AbstractProcessor;
import com.disk91.capture.interfaces.CaptureDataPivot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Service
public class DefaultProcessorForPivot extends AbstractProcessor {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ObjectMapper mapper = new ObjectMapper();

    // ================================================================================
    // Static processor registry
    // ================================================================================

    private static final Logger staticLog = LoggerFactory.getLogger(CaptureDataPivot.class);

    // Thread-safe map of registered processors, keyed by a string id
    private static final ConcurrentHashMap<String, Consumer<CaptureDataPivot>> staticProcessors = new ConcurrentHashMap<>();

    /**
     * Register a processor for CaptureDataPivot under the given id.
     * If a processor already exists for that id, it is replaced.
     * @param id        - Unique identifier for the processor
     * @param processor - Function consuming a CaptureDataPivot instance
     */
    public static void addProcessor(String id, Consumer<CaptureDataPivot> processor) {
        staticProcessors.put(id, processor);
        staticLog.info("[capture] Static processor registered with id '{}'", id);
    }

    /**
     * Remove a previously registered processor by its id.
     * @param id - Unique identifier of the processor to remove
     */
    public static void removeProcessor(String id) {
        Consumer<CaptureDataPivot> removed = staticProcessors.remove(id);
        if (removed != null) {
            staticLog.info("[capture] Static processor with id '{}' removed", id);
        } else {
            staticLog.warn("[capture] Attempted to remove unknown static processor with id '{}'", id);
        }
    }

    /**
     * Invoke all registered static processors on the given pivot, in ascending id order.
     * Exceptions thrown by individual processors are caught and logged but do not interrupt the chain.
     * @param pivot - CaptureDataPivot instance to pass to each processor
     */
    public void process(CaptureDataPivot pivot) {

        // In case no processor exists, just dump
        if ( staticProcessors.isEmpty() ) {
            try {
                String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(pivot);
                log.info(json);
            } catch (JsonProcessingException e) {
                log.error("[capture] Failed to dump pivot data: {}", e.getMessage());
            }
            return;
        }

        // Sort processors by id (ascending string order) before invocation
        new TreeMap<>(staticProcessors).forEach((id, processor) -> {
            try {
                processor.accept(pivot);
            } catch (Exception e) {
                staticLog.warn("[capture] Static processor '{}' threw an exception and was skipped: {}", id, e.getMessage());
            }
        });

    }

}
