/**
 * PutObject.java
 *
 * This class provides functionality to upload a file to Object Lite using the AWS SDK for Java via PutObject API.
 *
 * Usage:
 * mvn clean package
 * java -cp target/aws-java-sdk-examples-1.0.jar com.examples.PutObject --endpoint <endpoint> --bucket <bucket> --key \
 * <key> -file <file>
 *
 * Command Line Options:
 * -e, --endpoint      The endpoint URL of the objects-lite service.
 * -b, --bucket        The name of the bucket where the file will be uploaded.
 * -k, --key           The key (name) of the object to be created in the bucket.
 * -f, --file          The path to the file to be uploaded.
 *
 * Example:
 * java -cp target/aws-java-sdk-examples-1.0.jar com.examples.PutObject --endpoint \
 * https://<pc-ip>:9440/api/prism/v4.0/objects/ --bucket mybucket --key myfile.txt -file /path/to/myfile.txt
 *
 * Response:
 * After the file is uploaded successfully, the script prints the ETag of the uploaded object.
 *
 * Limitations:
 * - This script only supports uploading objects smaller than 5 GB. For larger files, consider using multipart upload.
 */

package com.examples;

import com.utils.Utils;
import com.amazonaws.services.s3.AmazonS3;
import org.apache.commons.cli.*;

public class PutObject {
    private AmazonS3 s3Client;

    /**
     * Constructor to initialize the AmazonS3 client.
     *
     * @param encodedCredentials Base64 encoded credentials.
     * @param endpoint The S3 endpoint to connect to.
     * @throws Exception If an error occurs during client creation.
     */
    public PutObject(String encodedCredentials, String endpoint) throws Exception {
        // Initialize the s3Client in the constructor
        this.s3Client = Utils.CreateS3Client(encodedCredentials, endpoint);
    }

    /**
     * Uploads a file to the specified S3 bucket with the given key.
     *
     * @param bucket The name of the S3 bucket.
     * @param key The key under which to store the new object.
     * @param filePath The path to the file to upload.
     * @throws Exception If an error occurs during the upload.
     */
    public void uploadObject(String bucket, String key, String filePath) throws Exception {
        Utils.PutObject(this.s3Client, bucket, key, filePath);
    }

    public static void main(String[] args) {
        // Disable AWS SDK v1 deprecation announcement
        System.setProperty("aws.java.v1.disableDeprecationAnnouncement", "true");

        // Define command line options
        Options options = new Options();

        // Define endpoint option
        Option endpointOption = new Option("e", "endpoint", true, "Endpoint URL");
        endpointOption.setRequired(true);
        options.addOption(endpointOption);

        // Define bucket option
        Option bucketOption = new Option("b", "bucket", true, "Bucket name");
        bucketOption.setRequired(true);
        options.addOption(bucketOption);

        // Define key option
        Option keyOption = new Option("k", "key", true, "Object key");
        keyOption.setRequired(true);
        options.addOption(keyOption);

        // Define file option
        Option fileOption = new Option("f", "file", true, "File to upload");
        fileOption.setRequired(true);
        options.addOption(fileOption);

        // Create command line parser and formatter
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        String endpoint = null;
        String bucket = null;
        String key = null;
        String file = null;
        try {
            // Parse command line arguments
            CommandLine cmd = parser.parse(options, args);

            // Get values from command line arguments
            endpoint = cmd.getOptionValue("endpoint");
            bucket = cmd.getOptionValue("bucket");
            key = cmd.getOptionValue("key");
            file = cmd.getOptionValue("file");
        } catch (ParseException e) {
            System.err.println("Failed to parse command line arguments: " + e.getMessage());
            formatter.printHelp("ObjectsLitePutObject", options);
            System.exit(1);
        }

        // Get credentials from the user
        String encodedCredentials = Utils.GetCredentials();

        try {
            // Create PutObject instance and upload the file
            PutObject putObject = new PutObject(encodedCredentials, endpoint);
            putObject.uploadObject(bucket, key, file);
        } catch (Exception e) {
            System.err.println("Failed to upload object: " + e.getMessage());
            System.exit(1);
        }
    }
}
