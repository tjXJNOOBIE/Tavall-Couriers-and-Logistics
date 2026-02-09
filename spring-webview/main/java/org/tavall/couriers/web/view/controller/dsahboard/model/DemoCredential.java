package org.tavall.couriers.web.view.controller.dsahboard.model;

public final class DemoCredential {

    private final String label;
    private final String username;
    private final String password;

    public DemoCredential(String label, String username, String password) {
        this.label = label;
        this.username = username;
        this.password = password;
    }

    public String getLabel() {
        return label;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
