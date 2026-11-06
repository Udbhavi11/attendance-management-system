
package com.workforce.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TeamDTO {

    private Long id;

    @NotBlank(message = "Team name is required")
    @Size(max = 100, message = "Team name cannot exceed 100 characters")
    private String name;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    private Long managerId;

    private String managerName;

    public TeamDTO() {
    }

    public TeamDTO(
            Long id,
            String name,
            String description,
            Long managerId,
            String managerName) {

        this.id = id;
        this.name = name;
        this.description = description;
        this.managerId = managerId;
        this.managerName = managerName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }
}
