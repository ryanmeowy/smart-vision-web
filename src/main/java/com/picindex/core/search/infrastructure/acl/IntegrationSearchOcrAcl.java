package com.picindex.core.search.infrastructure.acl;

import com.picindex.core.integration.ai.port.ImageOcrService;
import com.picindex.core.search.domain.port.SearchOcrPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * ACL adapter from search OCR port to integration OCR service.
 */
@Component
@RequiredArgsConstructor
public class IntegrationSearchOcrAcl implements SearchOcrPort {

    private final ImageOcrService imageOcrService;

    @Override
    public String extractText(String imageInput) {
        return imageOcrService.extractText(imageInput);
    }
}
