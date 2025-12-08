package com.fbdco.fbd_obsidian_sync_dashboard_api;

import com.amazonaws.serverless.proxy.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.amazonaws.services.lambda.runtime.Context;

class LambdaHandlerTest {

    private Context lambdaContext;

    @BeforeEach
    void setup() {
        lambdaContext = new TestContext(); // your mock context
    }

    @Test
    void testEndpointShouldWork() {
        LambdaHandler lambdaHandler = new LambdaHandler();

        // Manually build the AWS proxy request
        AwsProxyRequest request = new AwsProxyRequest();
        request.setHttpMethod("GET");
        request.setPath("/test");
        SingleValueHeaders headers = new SingleValueHeaders();
        headers.put("Content-Type", "application/json");
        request.setHeaders(headers);

        AwsProxyRequestContext requestContext = new AwsProxyRequestContext();
        requestContext.setRequestId("test-request-id");
        ApiGatewayRequestIdentity identity = new ApiGatewayRequestIdentity();
        identity.setSourceIp("127.0.0.1");
        requestContext.setIdentity(identity);
        request.setRequestContext(requestContext);

        AwsProxyResponse response = lambdaHandler.handleRequest(request, lambdaContext);

        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(200, response.getStatusCode());
    }
}


