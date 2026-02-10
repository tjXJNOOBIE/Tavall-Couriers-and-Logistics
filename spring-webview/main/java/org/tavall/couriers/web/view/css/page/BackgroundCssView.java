package org.tavall.couriers.web.view.css.page;

import org.springframework.stereotype.Component;

@Component("backgroundCss")
public class BackgroundCssView {

    public String mainContent() {
        return BackgroundCss.MAIN_CONTENT;
    }

    public String container() {
        return BackgroundCss.CONTAINER;
    }

    public String loginWrapper() {
        return BackgroundCss.LOGIN_WRAPPER;
    }

    public String cardBlue() {
        return BackgroundCss.CARD_BLUE;
    }

    public String cardDark() {
        return BackgroundCss.CARD_DARK;
    }

    public String join(String... classes) {
        return BackgroundCss.join(classes);
    }
}


