package org.tavall.couriers.web.view.css.page.components;

import org.springframework.stereotype.Component;

@Component("footerCss")
public class FooterCssView {

    public String appFooter() {
        return FooterCss.APP_FOOTER;
    }

    public String footerNote() {
        return FooterCss.FOOTER_NOTE;
    }
}
