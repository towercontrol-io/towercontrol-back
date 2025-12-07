package com.disk91.capture.drivers.standard;

import com.disk91.capture.interfaces.AbstractProcessor;
import com.disk91.capture.interfaces.CaptureDataPivot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DefaultProcessorForPivot extends AbstractProcessor {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void process(CaptureDataPivot pivot) {
        try {
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(pivot);
            log.info(json);
        } catch (JsonProcessingException e) {
            log.error("[capture] Failed to dump pivot data: {}", e.getMessage());
        }
    }
}
