package com.picindex.core.ingestion.infrastructure.acl;

import com.picindex.core.ingestion.domain.port.IngestionObjectStoragePort;
import com.picindex.core.integration.oss.OssManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.picindex.core.common.constant.AliyunConstant.X_OSS_PROCESS_EMBEDDING;
import static com.picindex.core.integration.oss.domain.model.PresignedValidityEnum.SHORT_TERM_VALIDITY;

/**
 * ACL adapter from ingestion storage port to integration OSS manager.
 */
@Component
@RequiredArgsConstructor
public class OssIngestionAcl implements IngestionObjectStoragePort {

    private final OssManager ossManager;

    @Override
    public String buildAiImageInput(String objectKey) {
        return ossManager.getAiPresignedUrl(objectKey, SHORT_TERM_VALIDITY.getValidity(), X_OSS_PROCESS_EMBEDDING);
    }
}
