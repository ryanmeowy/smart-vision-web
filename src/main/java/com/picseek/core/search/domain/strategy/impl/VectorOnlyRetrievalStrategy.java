package com.picseek.core.search.domain.strategy.impl;

import com.picseek.core.search.domain.model.ImageSearchResultDTO;
import com.picseek.core.search.interfaces.rest.dto.SearchQueryDTO;
import com.picseek.core.search.domain.model.StrategyTypeEnum;
import com.picseek.core.search.infrastructure.persistence.es.repository.ImageRepository;
import com.picseek.core.search.domain.strategy.RetrievalStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.picseek.core.common.constant.EmbeddingConstant.DEFAULT_TOP_K;

/**
 * Pure vector retrieval strategy (KNN only).
 */
@Component
@RequiredArgsConstructor
public class VectorOnlyRetrievalStrategy implements RetrievalStrategy {

    private final ImageRepository imageRepository;

    @Override
    public List<ImageSearchResultDTO> search(SearchQueryDTO query, List<Float> queryVector) {
        Integer topK = query == null || query.getTopK() == null ? DEFAULT_TOP_K : query.getTopK();
        return imageRepository.vectorSearch(queryVector, topK);
    }

    @Override
    public StrategyTypeEnum getType() {
        return StrategyTypeEnum.VECTOR_ONLY;
    }
}

