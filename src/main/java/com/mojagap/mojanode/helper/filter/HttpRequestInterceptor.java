package com.mojagap.mojanode.helper.filter;

import com.mojagap.mojanode.helper.AppContext;
import com.mojagap.mojanode.helper.utility.CommonUtils;
import com.mojagap.mojanode.helper.utility.DateUtils;
import com.mojagap.mojanode.model.ActionTypeEnum;
import com.mojagap.mojanode.model.http.HttpCallLog;
import com.mojagap.mojanode.model.http.HttpResponseStatusEnum;
import com.mojagap.mojanode.repository.http.HttpCallLogRepository;
import lombok.SneakyThrows;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;


public class HttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private final Logger LOG = Logger.getLogger(HttpRequestInterceptor.class.getName());

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        Long startTime = System.currentTimeMillis();
        HttpCallLog httpCallLog = logHttpRequest(request, body);
        HttpServletRequest httpServletRequest = AppContext.getBean(HttpServletRequest.class);
        httpServletRequest.setAttribute(HttpCallLog.class.getName(), httpCallLog);
        ClientHttpResponse response = execution.execute(request, body);
        logHttpResponse(response, httpCallLog);
        Long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        httpCallLog.setDuration((int) duration);


//        ActionTypeEnum actionTypeEnum = (ActionTypeEnum) httpServletRequest.getAttribute(ActionTypeEnum.class.getName());
//        HttpCallLogRepository httpCallLogRepository = AppContext.getBean(HttpCallLogRepository.class);
//        httpCallLog.setActionType(actionTypeEnum);
//        httpCallLogRepository.saveAndFlush(httpCallLog);
        return response;
    }

    @SneakyThrows
    private HttpCallLog logHttpRequest(HttpRequest request, byte[] body) {
        LOG.log(Level.INFO, "===========================Request Begin================================================");
        HttpCallLog httpCallLog = new HttpCallLog();
        httpCallLog.setActionType(ActionTypeEnum.MONEY_TRANSFER);
        httpCallLog.setCreatedOn(DateUtils.now());
        String requestBody = new String(body, StandardCharsets.UTF_8);
        httpCallLog.setRequestBody(requestBody);
        httpCallLog.setRequestUrl(request.getURI().toString());
        httpCallLog.setRequestMethod(Objects.requireNonNull(request.getMethod()).name());
        httpCallLog.setRequestHeaders(CommonUtils.OBJECT_MAPPER.writeValueAsString(request.getHeaders()));
        httpCallLog.setResponseStatus(HttpResponseStatusEnum.PENDING);
        LOG.log(Level.INFO, "Making HTTP " + request.getMethod() + " Request " + " To " + request.getURI().toString());
        LOG.log(Level.INFO, "==========================Request End================================================");
        return httpCallLog;
    }

    @SneakyThrows
    private void logHttpResponse(ClientHttpResponse response, HttpCallLog httpCallLog) {
        LOG.log(Level.INFO, "============================Response Begin==========================================");
        httpCallLog.setResponseHeaders(CommonUtils.OBJECT_MAPPER.writeValueAsString(response.getHeaders()));
        httpCallLog.setResponseStatusCode(response.getRawStatusCode());
        httpCallLog.setResponseBody(StreamUtils.copyToString(response.getBody(), Charset.defaultCharset()));
        HttpStatus statusCode = response.getStatusCode();
        if (statusCode.is4xxClientError() || statusCode.is5xxServerError()) {
            httpCallLog.setResponseStatus(HttpResponseStatusEnum.FAILED);
        } else {
            httpCallLog.setResponseStatus(HttpResponseStatusEnum.SUCCESS);
        }
        LOG.log(Level.INFO, "HTTP Request Status Text : " + response.getStatusText());
        LOG.log(Level.INFO, "=======================Response End=================================================");
    }
}
