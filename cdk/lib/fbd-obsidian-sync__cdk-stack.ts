import * as cdk from "aws-cdk-lib/core";
import { Construct } from "constructs";
import * as lambda from "aws-cdk-lib/aws-lambda";
import * as apigateway from "aws-cdk-lib/aws-apigateway";
import * as path from "path";

export class FbdObsidianSyncCdkStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const springBootLambda = new lambda.Function(this, "SpringBootFunction", {
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
        SPRING_PROFILES_ACTIVE: "prod",
        // Add your environment variables
      },
    });

    const api = new apigateway.LambdaRestApi(this, "SpringBootApi", {
      handler: springBootLambda,
      proxy: true,
    });
  }
}
