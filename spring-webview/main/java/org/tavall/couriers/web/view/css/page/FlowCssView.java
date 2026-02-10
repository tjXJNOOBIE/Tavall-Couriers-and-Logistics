package org.tavall.couriers.web.view.css.page;

import org.springframework.stereotype.Component;

@Component("flowCss")
public class FlowCssView {

    public String flexCenter() {
        return FlowCss.FLEX_CENTER;
    }

    public String flexBetween() {
        return FlowCss.FLEX_BETWEEN;
    }

    public String textCenter() {
        return FlowCss.TEXT_CENTER;
    }

    public String gridTwo() {
        return FlowCss.GRID_TWO;
    }

    public String gridThree() {
        return FlowCss.GRID_THREE;
    }

    public String join(String... classes) {
        return FlowCss.join(classes);
    }
}


