package com.picseek.core.ingestion.infrastructure.acl;

import com.picseek.core.ingestion.domain.port.IngestionOcrPort;
import com.picseek.core.integration.ai.port.ImageOcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * ACL adapter from ingestion OCR port to integration OCR service.
 */
@Component
@RequiredArgsConstructor
public class OcrIngestionAcl implements IngestionOcrPort {

    private final ImageOcrService imageOcrService;

    @Override
    public String extractText(String imageInput) {
        return imageOcrService.extractText(imageInput);
    }
}
