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

/*
 * OpenCSV is licensed under Apache 2.0. Please see more details at 
 * http://opencsv.sourceforge.net/#using-commercially
 */

package com.amazonaws.sns.samples.bulkupload;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import au.com.bytecode.opencsv.CSVReader;

public class BatchCreatePlatformEndpointSample {

    /*
     * Fields which are to be stored in BulkUpload.properties
     */
    private String applicationArn;
    private String csvFileName;
    private String goodFileName;
    private String badFileName;
    private char delimiterChar = ',';
    private char quoteChar = '\"';
    private int numOfThreads = 1;

    private static final String APPLICATION_ARN = "applicationarn";
    private static final String CSV_FILE_NAME = "csvfilename";
    private static final String GOOD_FILE_NAME = "goodfilename";
    private static final String BAD_FILE_NAME = "badfilename";
    private static final String DELIMITER_CHAR = "delimiterchar";
    private static final String QUOTE_CHAR = "quotechar";
    private static final String NUM_OF_THREADS = "numofthreads";

    static final int MALFORMED_PROPERTIES_ERROR_CODE = 1;
    static final int CREDENTIAL_RETRIEVAL_FAILURE_ERROR_CODE = 2;
    static final int FILE_ACCESS_FAILURE_ERROR_CODE = 3;
    static final int NOT_FOUND_ERROR_CODE = 4;

    private CSVReader csvReader;
    private Writer goodFileWriter;
    private Writer badFileWriter;

    /*
     * The properties files
     */
    static final String AWSCREDENTIALSPROPERTIES_FILE = "AwsCredentials.properties";
    static final String MOBILEPUSHPROPERTIES_FILE = "BulkUpload.properties";

    static final List<String> listOfRegions = Collections
            .unmodifiableList(new ArrayList<String>() {
                private static final long serialVersionUID = 1L;

                {
                    add("us-east-1");
                    add("us-west-1");
                    add("us-west-2");
                    add("sa-east-1");
                    add("eu-west-1");
                    add("ap-southeast-1");
                    add("ap-southeast-2");
                    add("ap-northeast-1");
                    add("us-gov-west-1");
                }
            });

    long lineNumber;

    public BatchCreatePlatformEndpointSample(Properties mapOfProperties) {
        lineNumber = 0;
        String region = "us-east-1";
        if (mapOfProperties.containsKey(APPLICATION_ARN)) {
            this.applicationArn = (String) mapOfProperties.get(APPLICATION_ARN);
            try {
                String[] applicationParts = applicationArn.split(":");
                if (!listOfRegions.contains(region = applicationParts[3])) {
                    System.err.println("[ERROR] The region " + region
                            + " is invalid");
                    System.exit(MALFORMED_PROPERTIES_ERROR_CODE);
                }
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                System.err.println("[ERROR] The ARN " + this.applicationArn
                        + " is malformed");
                System.exit(MALFORMED_PROPERTIES_ERROR_CODE);
            }
        } else {
            System.err.println("[ERROR] The " + MOBILEPUSHPROPERTIES_FILE
                    + " file is missing the field " + APPLICATION_ARN);
            System.exit(MALFORMED_PROPERTIES_ERROR_CODE);
        }
        if (mapOfProperties.containsKey(CSV_FILE_NAME)) {
            this.csvFileName = (String) mapOfProperties.get(CSV_FILE_NAME);
        } else {
            System.err.println("[ERROR] The " + MOBILEPUSHPROPERTIES_FILE
                    + " file is missing the field " + CSV_FILE_NAME);
            System.exit(MALFORMED_PROPERTIES_ERROR_CODE);
        }
        if (mapOfProperties.containsKey(BAD_FILE_NAME)) {
            this.badFileName = (String) mapOfProperties.get(BAD_FILE_NAME);
        } else {
            System.err.println("[ERROR] The " + MOBILEPUSHPROPERTIES_FILE
                    + " file is missing the field " + BAD_FILE_NAME);
            System.exit(MALFORMED_PROPERTIES_ERROR_CODE);
        }
        if (mapOfProperties.containsKey(GOOD_FILE_NAME)) {
            this.goodFileName = (String) mapOfProperties.get(GOOD_FILE_NAME);
        } else {
            System.err.println("[ERROR] The " + MOBILEPUSHPROPERTIES_FILE
                    + " file is missing the field " + GOOD_FILE_NAME);
            System.exit(MALFORMED_PROPERTIES_ERROR_CODE);
        }
        if (mapOfProperties.containsKey(DELIMITER_CHAR)) {
            try {
                this.delimiterChar = ((String) mapOfProperties
                        .get(DELIMITER_CHAR)).charAt(0);
            } catch (StringIndexOutOfBoundsException sioobe) {
            }
        }
        if (mapOfProperties.containsKey(QUOTE_CHAR)) {
            try {
                this.quoteChar = ((String) mapOfProperties.get(QUOTE_CHAR))
                        .charAt(0);
            } catch (StringIndexOutOfBoundsException sioobe) {
            }
        }
        if(mapOfProperties.containsKey(NUM_OF_THREADS)) {
            try {
                this.numOfThreads = Integer.parseInt((String) mapOfProperties.get(NUM_OF_THREADS));
            }
            catch (Exception e) {
                System.out.println("Could not set");
            }
            if(this.numOfThreads <= 0) {
                this.numOfThreads = 1;
            }
        }

        try {
            goodFileWriter = new PrintWriter(new FileWriter(this.goodFileName,
                    true));
        } catch (IOException ioe) {
            System.err.println("[ERROR] Error initiating write to "
                    + this.goodFileName + ": " + ioe.getMessage());
            System.exit(FILE_ACCESS_FAILURE_ERROR_CODE);
        }
        try {
            badFileWriter = new PrintWriter(new FileWriter(this.badFileName,
                    true));
        } catch (IOException ioe) {
            System.err.println("[ERROR] Error initiating write to "
                    + this.badFileName + ": " + ioe.getMessage());
            System.exit(FILE_ACCESS_FAILURE_ERROR_CODE);
        }
    }

    /**
     * @param numberOfThreads
     *            - Number of concurrently operating threads
     */
    public void readCsv(int numberOfThreads) {
        ExecutorService executor = Executors
                .newFixedThreadPool(numberOfThreads);

        String[] lineBeingProcessed;

        try {
            csvReader = new CSVReader(new BufferedReader(new FileReader(
                    this.csvFileName)), this.delimiterChar, this.quoteChar);


            while ((lineBeingProcessed = csvReader.readNext()) != null) {
                lineNumber++;
                /*
                 * If the csv reader reads two fields, the first is read as the
                 * token and the second is read as the customUserData
                 */
                if (lineBeingProcessed.length == 2) {
                    if (lineBeingProcessed[0].length() == 0) {
                        System.err
                                .println("<"
                                        + lineNumber
                                        + ">"
                                        + "[ERROR: MALFORMED CSV FILE] Null token found in "
                                        + this.csvFileName);
                        ((PrintWriter) badFileWriter).println("<"
                                + lineNumber
                                + ">"
                                + Arrays.toString(lineBeingProcessed)
                                        .substring(
                                                1,
                                                Arrays.toString(
                                                        lineBeingProcessed)
                                                        .length() - 1));
                        badFileWriter.flush();
                        continue;
                    }
                    CreateEndpointJob worker = new CreateEndpointJob();
                    worker.setThreadProperties(lineNumber,
                            lineBeingProcessed[0], lineBeingProcessed[1],
                            this.applicationArn, this.goodFileName,
                            this.badFileName, this.goodFileWriter,
                            this.badFileWriter);
                    executor.execute(worker);
                }
                /*
                 * If the csv reader reads one field, it is read as the token
                 * and the customUserData is null
                 */
                else if (lineBeingProcessed.length == 1) {
                    if (lineBeingProcessed[0].length() == 0) {
                        System.err
                                .println("<"
                                        + lineNumber
                                        + ">"
                                        + "[ERROR: MALFORMED CSV FILE] Null token found in "
                                        + this.csvFileName);
                        ((PrintWriter) badFileWriter).println("<"
                                + lineNumber
                                + ">"
                                + Arrays.toString(lineBeingProcessed)
                                        .substring(
                                                1,
                                                Arrays.toString(
                                                        lineBeingProcessed)
                                                        .length() - 1));
                        badFileWriter.flush();
                        continue;
                    }
                    CreateEndpointJob worker = new CreateEndpointJob();
                    worker.setThreadProperties(numberOfThreads,
                            lineBeingProcessed[0], "", this.applicationArn,
                            this.goodFileName, this.badFileName,
                            this.goodFileWriter, this.badFileWriter);
                    executor.execute(worker);
                } else {
                    System.err
                            .println("<" + lineNumber + ">"
                                    + "[ERROR: MALFORMED CSV FILE] "
                                    + this.csvFileName);
                    ((PrintWriter) badFileWriter).println("<"
                            + lineNumber
                            + ">"
                            + Arrays.toString(lineBeingProcessed).substring(
                                    1,
                                    Arrays.toString(lineBeingProcessed)
                                            .length() - 1));
                    badFileWriter.flush();
                    continue;
                }
            }
            executor.shutdown();
        } catch (IOException e) {
            System.err.println("[ERROR] Error initiating read from file "
                    + this.csvFileName);
            System.exit(FILE_ACCESS_FAILURE_ERROR_CODE);
        }
    }

    public static void main(String[] args) {
        try {
            Properties properties = new Properties();
            properties.load(new InputStreamReader(
                    BatchCreatePlatformEndpointSample.class
                            .getResourceAsStream(MOBILEPUSHPROPERTIES_FILE)));
            BatchCreatePlatformEndpointSample sample = new BatchCreatePlatformEndpointSample(
                    properties);
            sample.readCsv(sample.numOfThreads);
        } catch (IOException ioe) {
            System.err.println("[ERROR] Error initiating read from  "
                    + MOBILEPUSHPROPERTIES_FILE);
            System.exit(FILE_ACCESS_FAILURE_ERROR_CODE);
        }
    }
}
