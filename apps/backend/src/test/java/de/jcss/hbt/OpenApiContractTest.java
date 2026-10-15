package de.jcss.hbt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Guards the REST contract shared with the frontend: the generated OpenAPI document must
 * match {@code packages/shared-types/openapi/plans.json}, from which the frontend
 * generates its TypeScript types. After an intended API change, regenerate the file with
 * {@code ./mvnw test -Dtest=OpenApiContractTest -Dopenapi.update=true}.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OpenApiContractTest {

    private static final Path CONTRACT = Path.of("..", "..", "packages", "shared-types", "openapi", "plans.json");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    void generatedSpecMatchesCheckedInContract() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        String actual = mockMvc.perform(get("/v3/api-docs"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        if (Boolean.getBoolean("openapi.update")) {
            Files.createDirectories(CONTRACT.getParent());
            Files.writeString(CONTRACT, actual.strip() + "\n");
        }

        assertThat(Files.exists(CONTRACT))
                .as("%s is missing, generate it with -Dopenapi.update=true", CONTRACT)
                .isTrue();
        assertThat(JSON.readTree(actual))
                .as("API changed; if intended, regenerate %s with -Dopenapi.update=true", CONTRACT)
                .isEqualTo(JSON.readTree(Files.readString(CONTRACT)));
    }
}
