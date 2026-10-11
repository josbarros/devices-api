package com.devices.api.domain;

public class DeviceUpdateNotAllowedException extends RuntimeException {

    public DeviceUpdateNotAllowedException() {
        super("The name or brand of an in-use device cannot be changed");
    }
}
