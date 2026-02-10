package org.tavall.couriers.web.view.css.utils;

import org.springframework.stereotype.Component;

@Component("utilsCss")
public class UtilsCssView {

    public String grid() {
        return UtilsCss.GRID;
    }

    public String grid2() {
        return UtilsCss.GRID_2;
    }

    public String grid3() {
        return UtilsCss.GRID_3;
    }

    public String textCenter() {
        return UtilsCss.TEXT_CENTER;
    }

    public String textLink() {
        return UtilsCss.TEXT_LINK;
    }

    public String mt2() {
        return UtilsCss.MT_2;
    }

    public String mb2() {
        return UtilsCss.MB_2;
    }

    public String hiddenMobile() {
        return UtilsCss.HIDDEN_MOBILE;
    }

    public String fadeIn() {
        return UtilsCss.FADE_IN;
    }

    public String isHidden() {
        return UtilsCss.IS_HIDDEN;
    }

    public String textNowrap() {
        return UtilsCss.TEXT_NOWRAP;
    }

    public String join(String... classes) {
        return UtilsCss.join(classes);
    }
}


