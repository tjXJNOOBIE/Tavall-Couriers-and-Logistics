package org.tavall.couriers.web.view.css.style;

import org.springframework.stereotype.Component;

@Component("styleCss")
public class StyleCssView {

    public String manifest() {
        return StyleCss.MANIFEST;
    }

    public String join(String... classes) {
        return StyleCss.join(classes);
    }
}


