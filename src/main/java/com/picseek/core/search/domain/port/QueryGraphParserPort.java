package com.picseek.core.search.domain.port;

import com.picseek.core.common.model.GraphTriple;

import java.util.List;

/**
 * Domain port for parsing graph triples from user query text.
 */
public interface QueryGraphParserPort {

    /**
     * Parse text query into graph triples.
     *
     * @param keyword user query keyword
     * @return parsed triples, empty when nothing matched
     */
    List<GraphTriple> parseFromKeyword(String keyword);
}
