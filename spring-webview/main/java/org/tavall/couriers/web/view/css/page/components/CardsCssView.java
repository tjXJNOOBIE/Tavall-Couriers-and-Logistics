package org.tavall.couriers.web.view.css.page.components;

import org.springframework.stereotype.Component;

@Component("cardsCss")
public class CardsCssView {

    public String card() {
        return CardsCss.CARD;
    }

    public String cardHeader() {
        return CardsCss.CARD_HEADER;
    }

    public String cardTitle() {
        return CardsCss.CARD_TITLE;
    }
}
