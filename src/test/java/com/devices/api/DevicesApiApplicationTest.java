package com.devices.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DevicesApiApplication.class)
class DevicesApiApplicationTest {

    @Autowired
    private ConfigurableApplicationContext context;

    @Test
    void startsApplicationContext() {
        assertThat(context.isActive()).isTrue();
    }
}
