package org.tavall.couriers.web.view.css.page.components;

import org.springframework.stereotype.Component;

@Component("formsCss")
public class FormsCssView {

    public String formGroup() {
        return FormsCss.FORM_GROUP;
    }

    public String formLabel() {
        return FormsCss.FORM_LABEL;
    }

    public String formInput() {
        return FormsCss.FORM_INPUT;
    }
}
