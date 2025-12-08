#!/usr/bin/env node
import * as cdk from "aws-cdk-lib/core";
import { FbdObsidianSyncCdkStack } from "../lib/fbd-obsidian-sync__cdk-stack";

const app = new cdk.App();

const stageName = app.node.tryGetContext("stageName") || "dev";

new FbdObsidianSyncCdkStack(app, `FbdObsidianSync-${stageName}`, {
  stageName: stageName,
  env: {
    account: process.env.CDK_DEFAULT_ACCOUNT,
    region: process.env.CDK_DEFAULT_REGION,
  },
});
