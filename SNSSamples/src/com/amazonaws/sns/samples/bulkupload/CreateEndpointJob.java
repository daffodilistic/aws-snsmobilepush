package com.amazonaws.sns.samples.bulkupload;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.GetPlatformApplicationAttributesRequest;
import com.amazonaws.services.sns.model.GetPlatformApplicationAttributesResult;
import com.amazonaws.services.sns.model.InvalidParameterException;
import com.amazonaws.services.sns.model.NotFoundException;

public class CreateEndpointJob implements Runnable {

    private static Lock lock = new ReentrantLock();

    private AmazonSNS client;

    long lineNumber;
    private String token;
    private String userData;
    private String applicationArn;
    private String goodFileName;
    private String badFileName;
    private Writer goodFileWriter;
    private Writer badFileWriter;
    private String region;

    public CreateEndpointJob() {

        try {
            client = new AmazonSNSClient(
                    new PropertiesCredentials(
                            BatchCreatePlatformEndpointSample.class
                                    .getResourceAsStream(BatchCreatePlatformEndpointSample.AWSCREDENTIALSPROPERTIES_FILE)));
        } catch (IOException ioe) {
            System.err
                    .print("[ERROR] Error opening file"
                            + BatchCreatePlatformEndpointSample.AWSCREDENTIALSPROPERTIES_FILE
                            + ": " + ioe.getMessage());
            System.exit(BatchCreatePlatformEndpointSample.CREDENTIAL_RETRIEVAL_FAILURE_ERROR_CODE);
        }
    }

    public void setThreadProperties(long lineNumber, String token,
            String userData, String applicationArn, String goodFileName,
            String badFileName, Writer goodFileWriter, Writer badFileWriter) {
        this.lineNumber = lineNumber;
        this.token = token;
        this.userData = userData;
        this.applicationArn = applicationArn;
        this.goodFileName = goodFileName;
        this.badFileName = badFileName;
        this.goodFileWriter = goodFileWriter;
        this.badFileWriter = badFileWriter;
    }

    public void verifyPlatformApplication(AmazonSNS client) {
        try {
            if (!BatchCreatePlatformEndpointSample.listOfRegions
                    .contains(this.region = this.applicationArn.split(":")[3])) {
                System.err.println("[ERROR] The region " + region
                        + " is invalid");
                System.exit(BatchCreatePlatformEndpointSample.MALFORMED_PROPERTIES_ERROR_CODE);
            }
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            System.err.println("[ERROR] The ARN " + this.applicationArn
                    + " is malformed");
            System.exit(BatchCreatePlatformEndpointSample.MALFORMED_PROPERTIES_ERROR_CODE);
        }
        client.setEndpoint("https://sns." + this.region + ".amazonaws.com/");
        try {
            GetPlatformApplicationAttributesRequest applicationAttributesRequest = new GetPlatformApplicationAttributesRequest();
            applicationAttributesRequest
                    .setPlatformApplicationArn(this.applicationArn);
            @SuppressWarnings("unused")
            GetPlatformApplicationAttributesResult getAttributesResult = client
                    .getPlatformApplicationAttributes(applicationAttributesRequest);
        } catch (NotFoundException nfe) {
            System.err
                    .println("[ERROR: APP NOT FOUND] The application ARN provided: "
                            + this.applicationArn
                            + " does not correspond to any existing platform applications. "
                            + nfe.getMessage());
            System.exit(BatchCreatePlatformEndpointSample.NOT_FOUND_ERROR_CODE);
        } catch (InvalidParameterException ipe) {
            System.err
                    .println("[ERROR: APP ARN INVALID] The application ARN provided: "
                            + this.applicationArn
                            + " is malformed"
                            + ipe.getMessage());
            System.exit(BatchCreatePlatformEndpointSample.NOT_FOUND_ERROR_CODE);
        }
    }

    @Override
    public void run() {
        verifyPlatformApplication(this.client);
        try {
            CreatePlatformEndpointResult createResult = client
                    .createPlatformEndpoint(new CreatePlatformEndpointRequest()
                            .withPlatformApplicationArn(this.applicationArn)
                            .withToken(this.token)
                            .withCustomUserData(this.userData));
            try {
                System.out.println("<" + lineNumber + ">"
                        + "[SUCCESS] The endpoint was created with Arn "
                        + createResult.getEndpointArn());
                lock.lock();
                ((PrintWriter) goodFileWriter).println("<" + lineNumber + "> "
                        + createResult.getEndpointArn() + "," + this.token
                        + "," + this.userData);
                goodFileWriter.flush();
            } catch (IOException ioe) {
                System.err.println("[ERROR] Error initiating write to"
                        + this.goodFileName + ": " + ioe.getMessage());
                System.exit(BatchCreatePlatformEndpointSample.FILE_ACCESS_FAILURE_ERROR_CODE);
            } finally {
                lock.unlock();
            }
        } catch (AmazonServiceException ase) {
            try {

                System.err
                        .println("<"
                                + lineNumber
                                + ">"
                                + "[ERROR] The endpoint could not be created because of an AmazonServiceException. "
                                + ase.getMessage());
                lock.lock();
                ((PrintWriter) badFileWriter).println("<" + lineNumber + "> "
                        + this.token + "," + this.userData);
                badFileWriter.flush();
            } catch (IOException ioe) {
                System.err.println("[ERROR] Error initiating write to"
                        + this.badFileName + ": " + ioe.getMessage());
                System.exit(BatchCreatePlatformEndpointSample.FILE_ACCESS_FAILURE_ERROR_CODE);
            } finally {
                lock.unlock();
            }
            return;
        } catch (AmazonClientException ace) {
            try {
                System.err
                        .println("<"
                                + lineNumber
                                + ">"
                                + "[ERROR] The endpoint could not be created because of an AmazonClientException. "
                                + ace.getMessage());
                ((PrintWriter) badFileWriter).println("<" + lineNumber + "> "
                        + ace.getMessage() + " " + this.token + ","
                        + this.userData);
                badFileWriter.flush();
            } catch (IOException ioe) {
                System.err.println("[ERROR] Error initiating write to"
                        + this.badFileName + ": " + ioe.getMessage());
                System.exit(BatchCreatePlatformEndpointSample.FILE_ACCESS_FAILURE_ERROR_CODE);
            }
            return;
        }
    }

}
