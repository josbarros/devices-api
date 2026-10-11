package com.devices.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import javax.sql.DataSource;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DevicesApiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@AutoConfigureRestTestClient
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DevicesApiHttpTest {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private DataSource dataSource;

    @Test
    void respondsOverHttpUsingAnIsolatedSQLiteFile() throws Exception {
        restTestClient.get().uri("/missing")
                .exchange()
                .expectStatus().isNotFound();
        try (var connection = dataSource.getConnection()) {
            var databaseUrl = connection.getMetaData().getURL();
            assertThat(databaseUrl).startsWith("jdbc:sqlite:");
            assertThat(Path.of(databaseUrl.substring("jdbc:sqlite:".length()))).isRegularFile();
        }
    }
}
