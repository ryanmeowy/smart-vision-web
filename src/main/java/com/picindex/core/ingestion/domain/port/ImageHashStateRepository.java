package com.picindex.core.ingestion.domain.port;

import com.picindex.core.ingestion.domain.model.ImageHashStatus;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Repository abstraction for image hash processing state.
 */
public interface ImageHashStateRepository {

    boolean tryAcquireProcessing(String fileHash, long ttl, TimeUnit unit);

    Optional<ImageHashStatus> findStatus(String fileHash);

    void markStatus(String fileHash, ImageHashStatus status, long ttl, TimeUnit unit);

    void delete(String fileHash);
}

