package com.devices.api.persistence;

import com.devices.api.DevicesApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DevicesApiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class SQLiteConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void connectsToSQLite() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.isValid(1)).isTrue();
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:sqlite:");
        }
    }
}
