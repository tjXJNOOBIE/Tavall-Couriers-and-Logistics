package org.tavall.couriers.web.view.css.base;

import org.springframework.stereotype.Component;

@Component("baseCss")
public class BaseCssView {

    public String base() {
        return BaseCss.BASE;
    }

    public String join(String... classes) {
        return BaseCss.join(classes);
    }
}


