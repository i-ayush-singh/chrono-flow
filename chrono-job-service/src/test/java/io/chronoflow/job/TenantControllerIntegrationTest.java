package io.chronoflow.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class TenantControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("chronoflow")
            .withUsername("chronoflow")
            .withPassword("chronoflow");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    @SuppressWarnings("rawtypes")
    private KafkaTemplate kafkaTemplate;

    @Test
    void createTenantAndApiKey_flowWorksWithMigratedSchema() throws Exception {
        MvcResult tenantResult = mockMvc.perform(post("/api/v1/tenants")
                        .contentType("application/json")
                        .content("""
                                {"name":"tenant-it"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode tenantJson = objectMapper.readTree(tenantResult.getResponse().getContentAsString());
        String tenantId = tenantJson.get("id").asText();
        assertThat(tenantId).isNotBlank();

        MvcResult keyResult = mockMvc.perform(post("/api/v1/tenants/{tenantId}/api-keys", tenantId))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode keyJson = objectMapper.readTree(keyResult.getResponse().getContentAsString());
        assertThat(keyJson.get("keyId").asText()).startsWith("ck_");
        assertThat(keyJson.get("keySecret").asText()).startsWith("cs_");
    }
}
