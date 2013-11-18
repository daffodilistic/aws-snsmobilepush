Amazon SNS Mobile Push
----------------------------------------------------

This sample will send a notification to an application on a mobile device. Currently supported
are GCM(android/Google), ADM(Kindle), and APNS(Apple).

To use this sample:

1- Specify your AWS Access Key and AWS Secret Key in AwsCredentials.properties.
2- Undo the comment for the relevant platform, e.g.
                sample.demoAndroidAppNotification(Platform.GCM);
                for android.
3- Enter the relevant registration information e.g.
                registrationId
                ServerAPIKey
                applicationName
                for android.
4- *OPTIONAL* Comment out the line to delete the platform application to continue using the test platform application
i.e.
                //deletePlatformApplication(platformApplicationArn);
5- Make sure to have the AWS SDK for Java, found here http://aws.amazon.com/sdkforjava/
	and configure your build path.

For more information about Amazon SNS Mobile Push, please see docs.aws.amazon.com/sns/latest/dg/SNSMobilePush.html
For licensing information about this sample, please see the included LICENSE.txt.