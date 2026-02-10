package org.tavall.couriers.web.view.css.abstracted;

import org.springframework.stereotype.Component;

@Component("abstractedCss")
public class AbstractedCssView {

    public String entrypoint() {
        return AbstractedCss.ENTRYPOINT;
    }

    public String join(String... classes) {
        return AbstractedCss.join(classes);
    }
}


