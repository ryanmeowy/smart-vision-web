package com.picindex.core.ingestion.infrastructure.acl;

import com.picindex.core.common.model.GraphTriple;
import com.picindex.core.ingestion.domain.port.IngestionContentPort;
import com.picindex.core.integration.ai.port.ContentGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * ACL adapter from ingestion content port to integration generation service.
 */
@Component
@RequiredArgsConstructor
public class ContentIngestionAcl implements IngestionContentPort {

    private final ContentGenerationService contentGenerationService;

    @Override
    public String generateFileName(String imageInput) {
        return contentGenerationService.generateFileName(imageInput);
    }

    @Override
    public List<String> generateTags(String imageInput) {
        return contentGenerationService.generateTags(imageInput);
    }

    @Override
    public List<GraphTriple> generateGraph(String imageInput) {
        List<GraphTriple> triples = contentGenerationService.generateGraph(imageInput);
        if (triples == null || triples.isEmpty()) {
            return Collections.emptyList();
        }
        return triples.stream()
                .filter(Objects::nonNull)
                .toList();
    }
}
