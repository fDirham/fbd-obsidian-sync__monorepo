package com.fbdco.fbd_obsidian_sync_dashboard_api;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

public class TestContext implements Context {

    @Override
    public String getAwsRequestId() { return "test-request-id"; }

    @Override
    public String getLogGroupName() { return "test-log-group"; }

    @Override
    public String getLogStreamName() { return "test-log-stream"; }

    @Override
    public String getFunctionName() { return "test-function"; }

    @Override
    public String getFunctionVersion() { return "1"; }

    @Override
    public String getInvokedFunctionArn() { return "arn:aws:lambda:region:account-id:function:test-function"; }

    @Override
    public CognitoIdentity getIdentity() {
        return new CognitoIdentity() {
            @Override
            public String getIdentityId() { return "test-identity-id"; }

            @Override
            public String getIdentityPoolId() { return "test-identity-pool-id"; }
        };
    }

    @Override
    public ClientContext getClientContext() { return null; }

    @Override
    public int getRemainingTimeInMillis() { return 30000; }

    @Override
    public int getMemoryLimitInMB() { return 512; }

    @Override
    public LambdaLogger getLogger() { return null; }
}
