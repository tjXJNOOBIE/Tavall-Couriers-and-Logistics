package org.tavall.couriers.web.view.controller.dsahboard.admin.model;

public final class AdminUserView {

    private final String userUUID;
    private final String name;
    private final String role;

    public AdminUserView(String userUUID, String name, String role) {
        this.userUUID = userUUID;
        this.name = name;
        this.role = role;
    }

    public String getUserUUID() {
        return userUUID;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }
}
