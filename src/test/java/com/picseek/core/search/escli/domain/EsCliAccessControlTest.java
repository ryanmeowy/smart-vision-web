package com.picseek.core.search.escli.domain;

import com.picseek.core.common.config.VectorConfig;
import com.picseek.core.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EsCliAccessControlTest {

    @Test
    void isAllowed_shouldUseConfiguredPatterns() {
        EsCliAccessConfig config = new EsCliAccessConfig();
        config.setAllowedIndexPatterns(List.of("picseek_images_*", "logs-*"));

        VectorConfig vectorConfig = new VectorConfig();
        EsCliAccessControl control = new EsCliAccessControl(config, vectorConfig);

        assertThat(control.isAllowed("picseek_images_local_v2")).isTrue();
        assertThat(control.isAllowed("logs-2026-04")).isTrue();
        assertThat(control.isAllowed("other-index")).isFalse();
    }

    @Test
    void isAllowed_shouldFallbackToVectorDefaultsWhenNotConfigured() {
        EsCliAccessConfig config = new EsCliAccessConfig();

        VectorConfig vectorConfig = new VectorConfig();
        vectorConfig.setReadAlias("picseek_images_local_read");
        vectorConfig.setWriteAlias("picseek_images_local_write");
        vectorConfig.setIndexName("picseek_images_local");
        vectorConfig.setIndexVersion("v2");

        EsCliAccessControl control = new EsCliAccessControl(config, vectorConfig);

        assertThat(control.isAllowed("picseek_images_local_read")).isTrue();
        assertThat(control.isAllowed("picseek_images_local_v2")).isTrue();
        assertThat(control.isAllowed("random-index")).isFalse();
    }

    @Test
    void assertIndexAllowed_shouldThrowWhenDenied() {
        EsCliAccessConfig config = new EsCliAccessConfig();
        config.setAllowedIndexPatterns(List.of("picseek_images_*"));

        EsCliAccessControl control = new EsCliAccessControl(config, new VectorConfig());

        assertThatThrownBy(() -> control.assertIndexAllowed("secret-index"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Index access denied");
    }
}
