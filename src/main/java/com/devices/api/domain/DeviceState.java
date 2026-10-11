package com.devices.api.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum DeviceState {
    @JsonProperty("available")
    AVAILABLE,
    @JsonProperty("in-use")
    IN_USE,
    @JsonProperty("inactive")
    INACTIVE
}
