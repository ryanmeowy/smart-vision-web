package com.picseek.core.search.escli.interfaces.rest;

import com.picseek.core.common.exception.GlobalExceptionHandler;
import com.picseek.core.search.escli.application.EsCliQueryService;
import com.picseek.core.search.escli.interfaces.rest.dto.EsClusterHealthDTO;
import com.picseek.core.search.escli.interfaces.rest.dto.EsDocSearchDTO;
import com.picseek.core.search.escli.interfaces.rest.dto.EsIndexListDTO;
import com.picseek.core.search.escli.interfaces.rest.dto.EsIndexSummaryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EsCliControllerTest {

    @Mock
    private EsCliQueryService service;

    private MockMvc mockMvc;
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new EsCliController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void clusterHealth_shouldReturnResultEnvelope() throws Exception {
        when(service.getClusterHealth()).thenReturn(EsClusterHealthDTO.builder()
                .status("green")
                .clusterName("picseek-es")
                .numberOfNodes(3)
                .activePrimaryShards(24)
                .activeShards(48)
                .unassignedShards(0)
                .timedOut(false)
                .timestamp("2026-04-10T10:00:00Z")
                .build());

        mockMvc.perform(get("/api/es/cluster/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("green"))
                .andExpect(jsonPath("$.data.clusterName").value("picseek-es"));
    }

    @Test
    void listIndices_shouldReturnPagedData() throws Exception {
        when(service.listIndices(eq("picseek_*"), eq("green"), eq(1), eq(20)))
                .thenReturn(EsIndexListDTO.builder()
                        .items(List.of(EsIndexSummaryDTO.builder()
                                .name("picseek_images_local_v2")
                                .health("green")
                                .docsCount(100L)
                                .storeSize("1mb")
                                .pri("1")
                                .rep("1")
                                .build()))
                        .page(1)
                        .size(20)
                        .total(1)
                        .build());

        mockMvc.perform(get("/api/es/indices")
                        .param("pattern", "picseek_*")
                        .param("status", "green")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("picseek_images_local_v2"));
    }

    @Test
    void docSearch_shouldReturnHits() throws Exception {
        when(service.searchDocuments(eq("picseek_images_local_read"), any()))
                .thenReturn(EsDocSearchDTO.builder()
                        .took(12L)
                        .timedOut(false)
                        .total(2L)
                        .hits(List.of())
                        .build());

        String body = """
                {
                  "query": "fileName:cat",
                  "from": 0,
                  "size": 10
                }
                """;

        mockMvc.perform(post("/api/es/indices/picseek_images_local_read/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.took").value(12))
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    void docSearch_shouldRejectWhenQueryMissing() throws Exception {
        mockMvc.perform(post("/api/es/indices/picseek_images_local_read/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request parameters."));
    }
}
