package com.ledgertracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 50, message = "Category name must be between 2 and 50 characters")
    private String name;

    @NotBlank(message = "Category type is required")
    @Pattern(regexp = "EXPENSE|INCOME", message = "Type must be either EXPENSE or INCOME")
    private String type;

    @NotBlank(message = "Color code is required")
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Color must be a valid hex code (e.g. #1B4332)")
    private String color;

    @NotBlank(message = "Icon name is required")
    private String icon;

    public CategoryRequest() {
    }

    public CategoryRequest(String name, String type, String color, String icon) {
        this.name = name;
        this.type = type;
        this.color = color;
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name.trim() : null;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color != null ? color.trim().toUpperCase() : null;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon != null ? icon.trim().toLowerCase() : null;
    }
}
