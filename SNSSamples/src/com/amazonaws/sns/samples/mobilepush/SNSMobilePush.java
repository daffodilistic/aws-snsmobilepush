package com.amazonaws.sns.samples.mobilepush;

/*
 * Copyright 2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreatePlatformApplicationRequest;
import com.amazonaws.services.sns.model.CreatePlatformApplicationResult;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sns.model.DeletePlatformApplicationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;


public class SNSMobilePush {
    // This message is delivered if a platform specific message is not
    // specified for the end point. It must be set. It is received by the device
    // as the value of the key "default".
    private static final String defaultMessage = "This is the default message";

    private final static ObjectMapper objectMapper = new ObjectMapper();

    public static enum Platform {
        // Apple Push Notification Service
        APNS,
        // Sandbox version of Apple Push Notification Service
        APNS_SANDBOX,
        // Amazon Device Messaging
        ADM,
        // Google Cloud Messaging
        GCM
    }

    private final AmazonSNS snsClient;
    
	public SNSMobilePush(AmazonSNS sns) {
        snsClient = sns;
    }

    public static void main(String[] args) throws IOException{
        /*
         * TODO: Be sure to fill in your AWS access credentials in the
         * AwsCredentials.properties file before you try to run this sample.
         * http://aws.amazon.com/security-credentials
         */
        AmazonSNS sns = new AmazonSNSClient(new PropertiesCredentials(
                SNSMobilePush.class
                        .getResourceAsStream("AwsCredentials.properties")));

        sns.setEndpoint("https://sns.us-west-2.amazonaws.com");
        System.out.println("===========================================\n");
        System.out.println("Getting Started with Amazon SNS");
        System.out.println("===========================================\n");
        try {
            SNSMobilePush sample = new SNSMobilePush(sns);
            // TODO: Uncomment the services you wish to use.
            //sample.demoAndroidAppNotification(Platform.GCM);
            //sample.demoKindleAppNotification(Platform.ADM);
            //sample.demoAppleAppNotification(Platform.APNS);
            //sample.demoAppleAppNotification(Platform.APNS_SANDBOX);
        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon SNS, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out
                    .println("Caught an AmazonClientException, which means the client encountered "
                            + "a serious internal problem while trying to communicate with SNS, such as not "
                            + "being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
    }

    public void demoAppleAppNotification(Platform platform){
        // TODO: Please fill in following values for your application.
        String deviceToken = ""; // This is 64 hex characters.
        String certificate = "";  // This should be in pem format with \n at the end of each line.
        String privateKey = "";  // This should be in pem format with \n at the end of each line.
        String applicationName = "";
        demoNotification(platform, certificate, privateKey, deviceToken,
                applicationName);
    }

    public void demoAndroidAppNotification(Platform platform){
        // TODO: Please fill in following values for your application
        String registrationId = "";
        String principal = ""; // principal is not applicable for GCM
        String serverAPIKey = "";
        String applicationName = "";
        demoNotification(platform, principal, serverAPIKey, registrationId,
                applicationName);
    }

    public void demoKindleAppNotification(Platform platform){
        // TODO: Please fill in following values for your application
        String registrationId = "";
        String clientId = "";
        String clientSecret = "";
        String applicationName = "";
        demoNotification(platform, clientId, clientSecret, registrationId,
                applicationName);
    }

    private void demoNotification(Platform platform, String principal,
            String credential, String platformToken, String applicationName){
        // Create Platform Application. This corresponds to an app on a platform.
        CreatePlatformApplicationResult platformApplicationResult = createPlatformApplication(
                applicationName, platform, principal, credential);
        System.out.println(platformApplicationResult);

        // The Platform Application Arn can be used to uniquely identify the Platform Application.
        String platformApplicationArn = platformApplicationResult.getPlatformApplicationArn();

        // Create an Endpoint. This corresponds to an app on a device.
        CreatePlatformEndpointResult platformEndpointResult = createPlatformEndpoint(
                "CustomData - Useful to store endpoint specific data", platformToken, platformApplicationArn);
        System.out.println(platformEndpointResult);

        // Publish a push notification to an Endpoint.
        PublishResult publishResult = publish(platformEndpointResult.getEndpointArn(), platform);
        System.out.println("Published.  MessageId="+ publishResult.getMessageId());
        // Delete the Platform Application since we will no longer be using it.
        deletePlatformApplication(platformApplicationArn);
    }

    private CreatePlatformApplicationResult createPlatformApplication(
            String applicationName, Platform platform, String principal, String credential) {
        CreatePlatformApplicationRequest platformApplicationRequest = new CreatePlatformApplicationRequest();
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("PlatformPrincipal", principal);
        attributes.put("PlatformCredential", credential);
        platformApplicationRequest.setAttributes(attributes);
        platformApplicationRequest.setName(applicationName);
        platformApplicationRequest.setPlatform(platform.name());
        return snsClient.createPlatformApplication(platformApplicationRequest);
    }

    private CreatePlatformEndpointResult createPlatformEndpoint(
            String customData, String platformToken, String applicationArn) {
        CreatePlatformEndpointRequest platformEndpointRequest = new CreatePlatformEndpointRequest();
        platformEndpointRequest.setCustomUserData(customData);
        platformEndpointRequest.setToken(platformToken);
        platformEndpointRequest.setPlatformApplicationArn(applicationArn);
        return snsClient.createPlatformEndpoint(platformEndpointRequest);
    }

    private String getPlatformSampleMessage(Platform platform) {
        switch (platform) {
        case APNS:
            return getSampleAppleMessage();
        case APNS_SANDBOX:
            return getSampleAppleMessage();
        case GCM:
            return getSampleAndroidMessage();
        case ADM:
            return getSampleKindleMessage();
        default:
            throw new IllegalArgumentException("Platform Not supported : " + platform.name());
        }
    }

    private String getSampleAppleMessage() {
        Map<String, Object> appleMessageMap = new HashMap<String, Object>();
        Map<String, Object> appMessageMap = new HashMap<String, Object>();
        appMessageMap.put("alert", "You have got email.");
        appMessageMap.put("badge", 9);
        appMessageMap.put("sound", "default");
        appleMessageMap.put("aps", appMessageMap);
        return jsonify(appleMessageMap);
    }

    private String getSampleKindleMessage() {
        Map<String, Object> kindleMessageMap = new HashMap<String, Object>();
        kindleMessageMap.put("data", getData());
        kindleMessageMap.put("consolidationKey", "Welcome");
        kindleMessageMap.put("expiresAfter", 1000);
        return jsonify(kindleMessageMap);
    }

    private String getSampleAndroidMessage() {
        Map<String, Object> androidMessageMap = new HashMap<String, Object>();
        androidMessageMap.put("collapse_key", "Welcome");
        androidMessageMap.put("data", getData());
        androidMessageMap.put("delay_while_idle", true);
        androidMessageMap.put("time_to_live", 125);
        androidMessageMap.put("dry_run", false);
        return jsonify(androidMessageMap);
    }

    private Map<String, String> getData() {
        Map<String, String> payload = new HashMap<String, String>();
        payload.put("message", "Hello World!");
        return payload;
    }

    private PublishResult publish(String endpointArn, Platform platform) {
        PublishRequest publishRequest = new PublishRequest();
        Map<String, String> messageMap = new HashMap<String, String>();
        String message;
        messageMap.put("default", defaultMessage);
        messageMap.put(platform.name(), getPlatformSampleMessage(platform));
        // For direct publish to mobile end points, topicArn is not relevant.
        publishRequest.setTargetArn(endpointArn);
        publishRequest.setMessageStructure("json");
        message = jsonify(messageMap);

        // Display the message that will be sent to the endpoint/
        System.out.println(message);

        publishRequest.setMessage(message);
        return snsClient.publish(publishRequest);
    }

    private void deletePlatformApplication(String applicationArn) {
        DeletePlatformApplicationRequest request = new DeletePlatformApplicationRequest();
        request.setPlatformApplicationArn(applicationArn);
        snsClient.deletePlatformApplication(request);  
    }

    private static String jsonify(Object message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            e.printStackTrace();
            throw (RuntimeException) e;
        }
    }
}
