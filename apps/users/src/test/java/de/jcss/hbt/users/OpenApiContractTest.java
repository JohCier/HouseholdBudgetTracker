package de.jcss.hbt.users;

import de.jcss.hbt.test.AbstractOpenApiContractTest;
import de.jcss.hbt.test.IntegrationTest;

/** Checks the OpenAPI document of this service against {@code packages/shared-types/openapi/users.json}. */
@IntegrationTest
class OpenApiContractTest extends AbstractOpenApiContractTest {

    OpenApiContractTest() {
        super("users.json");
    }
}
