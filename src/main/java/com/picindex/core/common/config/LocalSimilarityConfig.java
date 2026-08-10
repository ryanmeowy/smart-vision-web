package com.picindex.core.common.config;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.picindex.core.common.constant.EmbeddingConstant.LOCAL_HYBRID_SIMILARITY;
import static com.picindex.core.common.constant.EmbeddingConstant.LOCAL_SIMILAR_SIMILARITY;

@Component
@Profile("local")
public class LocalSimilarityConfig implements SimilarityConfig {


    @Override
    public Float forHybridSearch() {
        return LOCAL_HYBRID_SIMILARITY;
    }

    @Override
    public Float forSimilarSearch() {
        return LOCAL_SIMILAR_SIMILARITY;
    }
}
