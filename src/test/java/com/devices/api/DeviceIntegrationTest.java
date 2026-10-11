package com.devices.api;

import com.devices.api.controller.DeviceDto;
import com.devices.api.domain.DeviceState;
import com.devices.api.persistence.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DevicesApiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@AutoConfigureRestTestClient
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DeviceIntegrationTest {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private DeviceRepository repository;

    @BeforeEach
    void clearDevices() {
        repository.deleteAll();
    }

    @Test
    void listsAllCreatedDevices() {
        var created = new ArrayList<DeviceDto>();
        for (var state : DeviceState.values()) {
            var device = restTestClient.post().uri("/devices")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new DeviceDto(null, "Device " + state, "Brand " + state, state, null))
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(DeviceDto.class)
                    .returnResult().getResponseBody();
            assertThat(device).isNotNull();
            created.add(device);
        }

        restTestClient.get().uri("/devices")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(DeviceDto[].class)
                .value(devices -> assertThat(devices).containsExactlyInAnyOrderElementsOf(created));
    }

    @Test
    void listsNoDevicesWhenDatabaseIsEmpty() {
        restTestClient.get().uri("/devices")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody().json("[]");
    }

    @ParameterizedTest
    @CsvSource({"available, AVAILABLE", "in-use, IN_USE", "inactive, INACTIVE"})
    void createsPersistsAndRetrievesDevice(String state, DeviceState domainState) {
        var before = Instant.now().minusSeconds(1);

        var result = restTestClient.post().uri("/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"name":" Phone ","brand":" Brand ","state":"%s"}
                        """.formatted(state))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(DeviceDto.class)
                .returnResult();

        var created = result.getResponseBody();
        assertThat(created).isNotNull();
        assertThat(created.id()).isPositive();
        assertThat(created.name()).isEqualTo(" Phone ");
        assertThat(created.brand()).isEqualTo(" Brand ");
        assertThat(created.state()).isEqualTo(domainState);
        assertThat(created.creationTime()).isBetween(before, Instant.now());
        var location = URI.create("/devices/" + created.id());
        assertThat(result.getResponseHeaders().getLocation()).isEqualTo(location);
        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findById(created.id()).map(DeviceDto::from)).contains(created);

        restTestClient.get().uri(location)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(DeviceDto.class).isEqualTo(created);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{",
            "{}",
            "{\"brand\":\"Brand\",\"state\":\"available\"}",
            "{\"name\":\"Phone\",\"state\":\"available\"}",
            "{\"name\":\"Phone\",\"brand\":\"Brand\"}",
            "{\"name\":\" \",\"brand\":\"Brand\",\"state\":\"available\"}",
            "{\"name\":\"Phone\",\"brand\":\" \",\"state\":\"available\"}",
            "{\"name\":\"Phone\",\"brand\":\"Brand\",\"state\":null}",
            "{\"name\":\"Phone\",\"brand\":\"Brand\",\"state\":\"unknown\"}",
            "{\"name\":\"Phone\",\"brand\":\"Brand\",\"state\":0}",
            "{\"name\":\"Phone\",\"brand\":\"Brand\",\"state\":\"available\",\"extra\":true}",
            "{\"name\":\"Phone\",\"brand\":\"Brand\",\"state\":\"available\",\"id\":123}",
            "{\"name\":\"Phone\",\"brand\":\"Brand\",\"state\":\"available\",\"creationTime\":\"2020-01-01T00:00:00Z\"}"
    })
    void rejectsInvalidCreationWithoutPersistingDevice(String body) {
        restTestClient.post().uri("/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.title").isEqualTo("Bad Request")
                .jsonPath("$.detail").isNotEmpty()
                .jsonPath("$.instance").isEqualTo("/devices");

        assertThat(repository.count()).isZero();
    }

    @Test
    void returnsProblemDetailsForMissingDevice() {
        restTestClient.get().uri("/devices/42")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.title").isEqualTo("Not Found")
                .jsonPath("$.detail").isEqualTo("Device 42 not found")
                .jsonPath("$.instance").isEqualTo("/devices/42");
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-an-id", "9223372036854775808"})
    void rejectsInvalidDeviceId(String id) {
        restTestClient.get().uri("/devices/" + id)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.title").isEqualTo("Bad Request")
                .jsonPath("$.detail").isNotEmpty();
    }
}
