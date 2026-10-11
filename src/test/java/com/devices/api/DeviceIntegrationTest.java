package com.devices.api;

import com.devices.api.controller.DeviceDto;
import com.devices.api.domain.DeviceState;
import com.devices.api.persistence.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
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

    @ParameterizedTest
    @EnumSource(value = DeviceState.class, names = {"AVAILABLE", "INACTIVE"})
    void deletesEligibleCreatedDevice(DeviceState state) {
        var created = restTestClient.post().uri("/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new DeviceDto(null, "Phone", "Brand", state, null))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(DeviceDto.class)
                .returnResult().getResponseBody();
        assertThat(created).isNotNull();
        var retained = restTestClient.post().uri("/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new DeviceDto(null, "Tablet", "Other", DeviceState.IN_USE, null))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(DeviceDto.class)
                .returnResult().getResponseBody();
        assertThat(retained).isNotNull();
        var location = URI.create("/devices/" + created.id());

        restTestClient.delete().uri(location)
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        assertThat(repository.findById(created.id())).isEmpty();
        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findById(retained.id()).map(DeviceDto::from)).contains(retained);

        restTestClient.get().uri(location)
                .exchange()
                .expectStatus().isNotFound();
        restTestClient.delete().uri(location)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void rejectsDeletionOfInUseDeviceWithoutChangingIt() {
        var created = restTestClient.post().uri("/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new DeviceDto(null, "Phone", "Brand", DeviceState.IN_USE, null))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(DeviceDto.class)
                .returnResult().getResponseBody();
        assertThat(created).isNotNull();
        var location = URI.create("/devices/" + created.id());

        restTestClient.delete().uri(location)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(409)
                .jsonPath("$.title").isEqualTo("Conflict")
                .jsonPath("$.detail").isEqualTo("Device " + created.id() + " is in use and cannot be deleted")
                .jsonPath("$.instance").isEqualTo(location.toString());

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findById(created.id()).map(DeviceDto::from)).contains(created);
        restTestClient.get().uri(location)
                .exchange()
                .expectStatus().isOk()
                .expectBody(DeviceDto.class).isEqualTo(created);
    }

    @Test
    void returnsProblemDetailsForMissingDeviceDeletion() {
        restTestClient.delete().uri("/devices/42")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.title").isEqualTo("Not Found")
                .jsonPath("$.detail").isEqualTo("Device 42 not found")
                .jsonPath("$.instance").isEqualTo("/devices/42");

        assertThat(repository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-an-id", "9223372036854775808"})
    void rejectsInvalidDeletionId(String id) {
        restTestClient.delete().uri("/devices/" + id)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.title").isEqualTo("Bad Request")
                .jsonPath("$.detail").isNotEmpty();

        assertThat(repository.count()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"available, AVAILABLE", "in-use, IN_USE", "inactive, INACTIVE"})
    void listsCreatedDevicesByStateAndByBrandAndState(String state, DeviceState domainState) {
        var created = new ArrayList<DeviceDto>();
        for (var brand : new String[]{"Brand", "brand", " Brand "}) {
            for (var deviceState : DeviceState.values()) {
                var device = restTestClient.post().uri("/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new DeviceDto(null, "Device " + created.size(), brand, deviceState, null))
                        .exchange()
                        .expectStatus().isCreated()
                        .expectBody(DeviceDto.class)
                        .returnResult().getResponseBody();
                assertThat(device).isNotNull();
                created.add(device);
            }
        }
        var expectedByState = created.stream().filter(device -> device.state() == domainState).toList();

        restTestClient.get().uri(builder -> builder.path("/devices").queryParam("state", state).build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(DeviceDto[].class)
                .value(devices -> assertThat(devices).containsExactlyInAnyOrderElementsOf(expectedByState));

        for (var brand : new String[]{"Brand", "brand", " Brand ", "Missing", ""}) {
            var expected = expectedByState.stream().filter(device -> device.brand().equals(brand)).toList();

            restTestClient.get().uri(builder -> builder.path("/devices")
                            .queryParam("brand", brand).queryParam("state", state).build())
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                    .expectBody(DeviceDto[].class)
                    .value(devices -> assertThat(devices).containsExactlyInAnyOrderElementsOf(expected));
        }
    }

    @Test
    void listsNoDevicesWhenStateHasNoMatches() {
        restTestClient.post().uri("/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new DeviceDto(null, "Phone", "Brand", DeviceState.AVAILABLE, null))
                .exchange()
                .expectStatus().isCreated();

        for (var uri : new String[]{"/devices?state=inactive", "/devices?brand=Brand&state=inactive"}) {
            restTestClient.get().uri(uri)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                    .expectBody().json("[]");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "AVAILABLE", "IN_USE", "in_use", "0", "true", "", " ",
            " available "})
    void rejectsInvalidStateFilters(String state) {
        for (var includeBrand : new boolean[]{false, true}) {
            restTestClient.get().uri(builder -> {
                        builder.path("/devices").queryParam("state", state);
                        if (includeBrand) {
                            builder.queryParam("brand", "Brand");
                        }
                        return builder.build();
                    })
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(400)
                    .jsonPath("$.title").isEqualTo("Bad Request")
                    .jsonPath("$.detail").isNotEmpty()
                    .jsonPath("$.instance").isEqualTo("/devices");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Brand", "brand", " Brand ", "Missing", ""})
    void listsCreatedDevicesByExactBrand(String brand) {
        var created = new ArrayList<DeviceDto>();
        var brands = new String[]{"Brand", "Brand", "brand", " Brand "};
        for (int index = 0; index < brands.length; index++) {
            var device = restTestClient.post().uri("/devices")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new DeviceDto(null, "Device " + index, brands[index],
                            DeviceState.values()[index % DeviceState.values().length], null))
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(DeviceDto.class)
                    .returnResult().getResponseBody();
            assertThat(device).isNotNull();
            created.add(device);
        }
        var expected = created.stream().filter(device -> device.brand().equals(brand)).toList();

        restTestClient.get().uri(builder -> builder.path("/devices").queryParam("brand", brand).build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(DeviceDto[].class)
                .value(devices -> assertThat(devices).containsExactlyInAnyOrderElementsOf(expected));
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
