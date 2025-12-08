import * as cdk from "aws-cdk-lib/core";
import { Construct } from "constructs";
import * as lambda from "aws-cdk-lib/aws-lambda";
import * as path from "path";
import { Stack, StackProps, Duration } from "aws-cdk-lib";
import * as s3 from "aws-cdk-lib/aws-s3";
import * as dynamodb from "aws-cdk-lib/aws-dynamodb";
import * as cognito from "aws-cdk-lib/aws-cognito";
import * as apigw from "aws-cdk-lib/aws-apigateway";
import * as iam from "aws-cdk-lib/aws-iam";
export interface FbdObsidianSyncCdkStackProps extends StackProps {
  stageName: string; // "staging" | "prod"
}

export class FbdObsidianSyncCdkStack extends Stack {
  constructor(
    scope: Construct,
    id: string,
    props: FbdObsidianSyncCdkStackProps
  ) {
    super(scope, id, props);

    const stage = props.stageName;
    const appName = "fbd-obsidian-sync";

    // S3 bucket
    const bucket = new s3.Bucket(this, "AppBucket", {
      removalPolicy: cdk.RemovalPolicy.RETAIN,
      bucketName: `${appName}--${stage}--bucket`.toLowerCase(),
      cors: [
        {
          allowedHeaders: ["*"],
          allowedMethods: [
            s3.HttpMethods.GET,
            s3.HttpMethods.PUT,
            s3.HttpMethods.POST,
            s3.HttpMethods.HEAD,
            s3.HttpMethods.DELETE,
          ],
          allowedOrigins: ["*"],
          exposedHeaders: ["ETag"],
          maxAge: 3000,
        },
      ],
    });

    // const bucket = s3.Bucket.fromBucketName(
    //   this,
    //   "AppBucket",
    //   `${appName}--${stage}--bucket`.toLowerCase()
    // );

    // DynamoDB
    const table = new dynamodb.Table(this, "AppTable", {
      partitionKey: { name: "user_id", type: dynamodb.AttributeType.STRING },
      billingMode: dynamodb.BillingMode.PAY_PER_REQUEST,
      removalPolicy: cdk.RemovalPolicy.RETAIN,
    });

    // Cognito
    const userPool = new cognito.UserPool(this, "UserPool", {
      userPoolName: `${appName}--${stage}--userpool`,
      selfSignUpEnabled: true,
      signInAliases: { email: true },
      passwordPolicy: {
        minLength: 8,
        requireUppercase: true,
        requireLowercase: true,
        requireDigits: true,
        requireSymbols: true,
        tempPasswordValidity: Duration.days(7),
      },
      deletionProtection: false,
      accountRecovery: cognito.AccountRecovery.EMAIL_ONLY,
    });

    const userPoolClient = new cognito.UserPoolClient(this, "UserPoolClient", {
      userPool,
      userPoolClientName: `${appName}--${stage}--userpool-client`,
      generateSecret: true,
      idTokenValidity: Duration.hours(1),
      accessTokenValidity: Duration.hours(1),
      refreshTokenValidity: Duration.days(5),
      authSessionValidity: Duration.minutes(5),
      authFlows: {
        adminUserPassword: true,
      },
      oAuth: {
        scopes: [cognito.OAuthScope.EMAIL],
      },
    });

    const fn = new lambda.Function(this, "SpringBootFunction", {
      functionName: `${appName}--${stage}--dashboard-api-lambda`,
      runtime: lambda.Runtime.JAVA_21, // Wow
      handler:
        "com.fbdco.fbd_obsidian_sync_dashboard_api.LambdaHandler::handleRequest",
      code: lambda.Code.fromAsset(
        path.join(
          __dirname,
          "../../dashboard-api-lambda/.aws-sam/build/SpringBootFunction"
        )
      ),
      memorySize: 1024,
      timeout: cdk.Duration.seconds(30),
      environment: {
        COGNITO_REGION: this.region,
        COGNITO_CLIENT_SECRET:
          userPoolClient.userPoolClientSecret.unsafeUnwrap(), // So we don't have to use AWS Secrets Manager
        COGNITO_CLIENT_ID: userPoolClient.userPoolClientId,
        COGNITO_USERPOOL_ID: userPool.userPoolId,
        S3_REGION: this.region,
        S3_BUCKET: bucket.bucketName,
        DYNAMODB_REGION: this.region,
        DYNAMODB_TABLE: table.tableName,
        CONFIG_MAX_VAULT_NAME_LENGTH: "50",
        CONFIG_MAX_VAULTS_PER_USER: "5",
      },
    });

    // Grant permissions
    bucket.grantReadWrite(fn);
    table.grantFullAccess(fn); // or fine-grained permissions as needed

    // Cognito access: add policy statements
    fn.addToRolePolicy(
      new iam.PolicyStatement({
        actions: ["cognito-idp:*"],
        resources: [userPool.userPoolArn],
      })
    );

    // API Gateway (REST)
    const api = new apigw.LambdaRestApi(this, "ApiGateway", {
      restApiName: `${appName}--${stage}--api`,
      handler: fn,
      deployOptions: {
        stageName: stage,
      },
    });

    // Output
    new cdk.CfnOutput(this, "ApiUrl", { value: api.url });
    new cdk.CfnOutput(this, "BucketName", { value: bucket.bucketName });
    new cdk.CfnOutput(this, "TableName", { value: table.tableName });
    new cdk.CfnOutput(this, "UserPoolId", { value: userPool.userPoolId });
  }
}
