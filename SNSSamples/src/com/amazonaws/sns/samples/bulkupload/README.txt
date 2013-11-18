Amazon SNS Mobile Push Bulk Endpoint Creation Sample
----------------------------------------------------

This sample creates Amazon SNS Mobile Push endpoints in bulk. ARNs for endpoints that are successfully created are stored in a log file. Failures are recorded into a separate log file.

To use this sample:

1- Specify your AWS Access Key and AWS Secret Key in AwsCredentials.properties.
2- Specify the following values in MobilePush.properties:
    'applicationarn' is the Platform Application ARN,
    'csvfilename' is the absolute path to the CSV file containing endpoint tokens and user data,
    'goodfilename' is the absolute path to the log file which will contain endpoint ARNs that were created successfully, 
    'badfilename' is the absolute path to the log file which will contain endpoint tokens that failed to create,
    'delimiterchar' is the character used as a delimiter in the CSV file,
    'quotechar' is the character used for quoting values in the CSV file.
    'numofthreads' is the number of threads concurrently creating endpoints.
3- Compile and run BatchCreatePlatformEndpointSample.java. The sample requires the OpenCSV library. Obtain a copy of the library from http://sourceforge.net/projects/opencsv/. 
   The relevant javadocs may be obtained at http://opencsv.sourceforge.net/apidocs/index.html

For more information about Amazon SNS Mobile Push, please see http://docs.aws.amazon.com/sns/latest/dg/SNSMobilePush.html

For licensing information about this sample, please see the included LICENSE.txt.