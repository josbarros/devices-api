package com.devices.api.controller;

import com.devices.api.DevicesApiApplication;
import com.devices.api.domain.Device;
import com.devices.api.domain.DeviceState;
import com.devices.api.service.DeviceService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = DevicesApiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@AutoConfigureRestTestClient
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DeviceControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private DeviceService service;

    @ParameterizedTest
    @CsvSource({"available, AVAILABLE", "in-use, IN_USE", "inactive, INACTIVE"})
    void createsDeviceOverHttp(String state, DeviceState domainState) {
        var created = new Device(" Phone ", " Brand ", domainState);
        ReflectionTestUtils.setField(created, "id", 42L);
        ReflectionTestUtils.setField(created, "creationTime", Instant.parse("2026-10-11T10:00:00Z"));
        when(service.create(" Phone ", " Brand ", domainState)).thenReturn(created);

        restTestClient.post().uri("/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"name":" Phone ","brand":" Brand ","state":"%s"}
                        """.formatted(state))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueEquals(HttpHeaders.LOCATION, "/devices/42")
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo(42)
                .jsonPath("$.name").isEqualTo(" Phone ")
                .jsonPath("$.brand").isEqualTo(" Brand ")
                .jsonPath("$.state").isEqualTo(state)
                .jsonPath("$.creationTime").isEqualTo("2026-10-11T10:00:00Z");

        verify(service).create(" Phone ", " Brand ", domainState);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "null",
            "{",
            "{\"brand\":\"Brand\",\"state\":\"available\"}",
            "{\"name\":null,\"brand\":\"Brand\",\"state\":\"available\"}",
            "{\"name\":\"\",\"brand\":\"Brand\",\"state\":\"available\"}",
            "{\"name\":\" \\t\\n\\r \",\"brand\":\"Brand\",\"state\":\"available\"}",
            "{\"name\":\"Phone\",\"state\":\"available\"}",
            "{\"name\":\"Phone\",\"brand\":null,\"state\":\"available\"}",
            "{\"name\":\"Phone\",\"brand\":\"\",\"state\":\"available\"}",
            "{\"name\":\"Phone\",\"brand\":\" \\t\\n\\r \",\"state\":\"available\"}",
            "{\"name\":\"Phone\",\"brand\":\"Brand\"}",
            "{\"name\":\"Phone\",\"brand\":\"Brand\",\"state\":null}",
            "{\"name\":\"Phone\",\"brand\":\"Brand\",\"state\":\"unknown\"}",
            "{\"name\":\"Phone\",\"brand\":\"Brand\",\"state\":\"AVAILABLE\"}",
            "{\"name\":\"Phone\",\"brand\":\"Brand\",\"state\":\"in_use\"}",
            "{\"name\":\"Phone\",\"brand\":\"Brand\",\"state\":0}",
            "{\"name\":\"Phone\",\"brand\":\"Brand\",\"state\":\"available\",\"extra\":true}",
            "{\"name\":\"Phone\",\"brand\":\"Brand\",\"state\":\"available\",\"id\":123}",
            "{\"name\":\"Phone\",\"brand\":\"Brand\",\"state\":\"available\",\"creationTime\":\"2020-01-01T00:00:00Z\"}"
    })
    void rejectsInvalidCreationBeforeCallingService(String body) {
        restTestClient.post().uri("/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.title").isEqualTo("Bad Request")
                .jsonPath("$.detail").isNotEmpty();

        verifyNoInteractions(service);
    }
}
