package org.tavall.couriers.web.view.css.page.components;

import org.springframework.stereotype.Component;

@Component("badgesCss")
public class BadgesCssView {

    public String badge() {
        return BadgesCss.BADGE;
    }

    public String badgeSuccess() {
        return BadgesCss.BADGE_SUCCESS;
    }

    public String badgeNeutral() {
        return BadgesCss.BADGE_NEUTRAL;
    }
}
