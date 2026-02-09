package org.tavall.couriers.web.view.controller.dsahboard.admin.model;

public final class AdminUserView {

    private final String name;
    private final String role;

    public AdminUserView(String name, String role) {
        this.name = name;
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }
}
