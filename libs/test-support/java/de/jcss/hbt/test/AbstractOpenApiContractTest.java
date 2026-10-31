package de.jcss.hbt.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Guards the REST contract shared with the frontend: the generated OpenAPI document of a service must match its file
 * in {@code packages/shared-types/openapi/}, from which the frontend generates its TypeScript types. Each service
 * runs this check through an {@link IntegrationTest} subclass named {@code OpenApiContractTest}; after an intended API
 * change, regenerate the file with {@code ./mvnw verify -Dit.test=OpenApiContractTest -Dopenapi.update=true}.
 */
public abstract class AbstractOpenApiContractTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final Path contract;

    @Autowired
    private WebApplicationContext webApplicationContext;

    /**
     * @param contractFileName file name in {@code packages/shared-types/openapi/}, e.g. {@code plans.json}; resolved
     *                         against the service directory, the working directory of the tests
     */
    protected AbstractOpenApiContractTest(String contractFileName) {
        this.contract = Path.of("..", "..", "packages", "shared-types", "openapi", contractFileName);
    }

    @Test
    void generatedSpecMatchesCheckedInContract() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        String actual = mockMvc.perform(get("/v3/api-docs"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        if (Boolean.getBoolean("openapi.update")) {
            Files.createDirectories(contract.getParent());
            Files.writeString(contract, actual.strip() + "\n");
        }

        assertThat(Files.exists(contract))
                .as("%s is missing, generate it with -Dopenapi.update=true", contract)
                .isTrue();
        assertThat(JSON.readTree(actual))
                .as("API changed; if intended, regenerate %s with -Dopenapi.update=true", contract)
                .isEqualTo(JSON.readTree(Files.readString(contract)));
    }
}
