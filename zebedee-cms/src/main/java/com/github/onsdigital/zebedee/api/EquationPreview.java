package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.service.EquationService;
import com.github.onsdigital.zebedee.service.EquationServiceResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Given some Tex equation input, return the equation in SVG format.
 */
@RestController
public class EquationPreview {

    @RequestMapping(value = "/equationPreview", method = RequestMethod.POST)
    public String renderEquation(HttpServletRequest request, HttpServletResponse response, String input) throws IOException {
        EquationServiceResponse equationServiceResponse = EquationService.render(input);
        String rendered = equationServiceResponse == null ? input : equationServiceResponse.svg;
        return rendered;
    }
}