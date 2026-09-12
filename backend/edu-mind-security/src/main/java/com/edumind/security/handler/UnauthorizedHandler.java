package com.edumind.security.handler;

import com.edumind.common.api.ApiResponseWriter;
import com.edumind.common.api.ResultCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class UnauthorizedHandler {

    public void handle(HttpServletResponse response) throws IOException {
        ApiResponseWriter.write(response, ResultCode.UNAUTHORIZED);
    }
}
