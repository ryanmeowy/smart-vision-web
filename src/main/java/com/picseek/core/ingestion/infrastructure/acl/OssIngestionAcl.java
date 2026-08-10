package com.picseek.core.ingestion.infrastructure.acl;

import com.picseek.core.ingestion.domain.port.IngestionObjectStoragePort;
import com.picseek.core.integration.oss.OssManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.picseek.core.common.constant.AliyunConstant.X_OSS_PROCESS_EMBEDDING;
import static com.picseek.core.integration.oss.domain.model.PresignedValidityEnum.SHORT_TERM_VALIDITY;

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
