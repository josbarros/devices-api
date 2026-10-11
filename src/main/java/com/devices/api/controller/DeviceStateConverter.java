package com.devices.api.controller;

import com.devices.api.domain.DeviceState;
import org.springframework.core.convert.converter.Converter;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

import java.beans.PropertyEditorSupport;

@ControllerAdvice(assignableTypes = DeviceController.class)
public class DeviceStateConverter implements Converter<String, DeviceState> {

    @InitBinder
    public void registerStateConversion(WebDataBinder binder) {
        // Prevent fallback conversion from accepting enum constant names or blank input.
        binder.registerCustomEditor(DeviceState.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(convert(text));
            }
        });
    }

    @Override
    public DeviceState convert(String source) {
        return switch (source) {
            case "available" -> DeviceState.AVAILABLE;
            case "in-use" -> DeviceState.IN_USE;
            case "inactive" -> DeviceState.INACTIVE;
            default -> throw new IllegalArgumentException("Invalid device state: " + source);
        };
    }
}
