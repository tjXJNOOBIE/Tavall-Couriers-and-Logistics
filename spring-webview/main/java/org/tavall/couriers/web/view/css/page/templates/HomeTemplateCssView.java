package org.tavall.couriers.web.view.css.page.templates;

import org.springframework.stereotype.Component;

@Component("homeTemplateCss")
public class HomeTemplateCssView {

    public String body() {
        return HomeTemplateCss.BODY;
    }

    public String mainContent() {
        return HomeTemplateCss.MAIN_CONTENT;
    }

    public String container() {
        return HomeTemplateCss.CONTAINER;
    }

    public String heroCard() {
        return HomeTemplateCss.HERO_CARD;
    }

    public String heroHeader() {
        return HomeTemplateCss.HERO_HEADER;
    }

    public String heroBrand() {
        return HomeTemplateCss.HERO_BRAND;
    }

    public String heroIconWrap() {
        return HomeTemplateCss.HERO_ICON_WRAP;
    }

    public String heroIcon() {
        return HomeTemplateCss.HERO_ICON;
    }

    public String heroTitle() {
        return HomeTemplateCss.HERO_TITLE;
    }

    public String heroDesc() {
        return HomeTemplateCss.HERO_DESC;
    }

    public String actions() {
        return HomeTemplateCss.ACTIONS;
    }

    public String join(String... classes) {
        return HomeTemplateCss.join(classes);
    }
}


