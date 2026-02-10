package org.tavall.couriers.web.view.css.page.templates;

import org.springframework.stereotype.Component;

@Component("trackingTemplateCss")
public class TrackingTemplateCssView {

    public String trackingCard() {
        return TrackingTemplateCss.TRACKING_CARD;
    }

    public String trackingActions() {
        return TrackingTemplateCss.TRACKING_ACTIONS;
    }

    public String trackingTools() {
        return TrackingTemplateCss.TRACKING_TOOLS;
    }

    public String trackingTool() {
        return TrackingTemplateCss.TRACKING_TOOL;
    }

    public String join(String... classes) {
        return TrackingTemplateCss.join(classes);
    }
}


