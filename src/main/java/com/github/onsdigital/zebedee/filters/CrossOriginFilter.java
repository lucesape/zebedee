package com.github.onsdigital.zebedee.filters;

import com.github.davidcarboni.restolino.framework.Filter;
import com.github.onsdigital.zebedee.model.Sessions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CrossOriginFilter implements Filter {
    /**
     * This filter adds required headers to the Http response allowing cross origin calls.
     * There is no configuration to enable this filter, it is recognised automatically at run time.
     *
     * @param request
     * @param response
     * @return
     */
    @Override
    public boolean filter(HttpServletRequest request, HttpServletResponse response) {
        response.addHeader("Access-Control-Allow-Origin", "http://localhost:8081");
        response.addHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, " + Sessions.TOKEN_HEADER);
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
        return true;
    }
}