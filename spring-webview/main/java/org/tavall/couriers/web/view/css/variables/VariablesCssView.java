package org.tavall.couriers.web.view.css.variables;

import org.springframework.stereotype.Component;

@Component("variablesCss")
public class VariablesCssView {

    public String tokens() {
        return VariablesCss.TOKENS;
    }

    public String join(String... classes) {
        return VariablesCss.join(classes);
    }
}


