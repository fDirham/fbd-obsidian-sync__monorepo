# Overview
[Website](https://da8re62zge7z0.cloudfront.net)

The backend for this [obsidian plugin](https://github.com/fDirham/fbd-obsidian-sync__plugin)

Contains:
- Website code that gets auto deployed to S3 when modified
- CDK to deploy your own stack to your own account if you so choose
- REST API built for AWS Lambda, writting in Java 21. A gradle project.

## Website
Under `fbd-obsidian-sync-website`. A simple react vite app.

## REST API
Under `dashboard-api-lambda`. Needs Java 21 to run. Use `sam build` and read the README in that directory to find out how to local test.

## CDK
Under `cdk`. Basically provisions:
- S3 bucket
- DynamoDB table
- Cognito user pool and client
- A lambda function using built files from REST API
- API Gateway
