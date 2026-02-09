package org.tavall.couriers.web.view.css.page.templates;

import org.springframework.stereotype.Component;

@Component("dashboardTemplateCss")
public class DashboardTemplateCssView {

    public String opsGrid() {
        return DashboardTemplateCss.OPS_GRID;
    }

    public String opsCard() {
        return DashboardTemplateCss.OPS_CARD;
    }

    public String opsCardAlt() {
        return DashboardTemplateCss.OPS_CARD_ALT;
    }

    public String scanQueueList() {
        return DashboardTemplateCss.SCAN_QUEUE_LIST;
    }

    public String scanQueueCard() {
        return DashboardTemplateCss.SCAN_QUEUE_CARD;
    }

    public String scanQueueProcessing() {
        return DashboardTemplateCss.SCAN_QUEUE_PROCESSING;
    }

    public String scanQueueError() {
        return DashboardTemplateCss.SCAN_QUEUE_ERROR;
    }

    public String routeModalPanel() {
        return DashboardTemplateCss.ROUTE_MODAL_PANEL;
    }

    public String routeActions() {
        return DashboardTemplateCss.ROUTE_ACTIONS;
    }

    public String routeAction() {
        return DashboardTemplateCss.ROUTE_ACTION;
    }

    public String join(String... classes) {
        return DashboardTemplateCss.join(classes);
    }
}


