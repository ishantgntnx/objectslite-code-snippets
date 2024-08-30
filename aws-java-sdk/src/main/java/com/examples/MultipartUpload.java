/**
 * MultipartUpload.java
 *
 * This class provides functionality to upload a file to Object Lite using the AWS SDK for Java via multipart upload.
 *
 * Multipart upload allows you to upload large objects in parts. This script reads the file in parts and uploads each
 * part individually. After all parts are uploaded, the script completes the multipart upload.
 *
 * Usage:
 * mvn clean package
 * java -cp target/aws-java-sdk-examples-1.0.jar com.examples.MultipartUpload --endpoint <endpoint> --bucket <bucket> \
 * --key <key> -file <file> [-part-size <part-size>]
 *
 * Command Line Options:
 * -e, --endpoint      The endpoint URL of the objects-lite service.
 * -b, --bucket        The name of the bucket where the file will be uploaded.
 * -k, --key           The key (name) of the object to be created in the bucket.
 * -f, --file          The path to the file to be uploaded.
 * -p, --part-size     The size of each part in bytes (default is 8 MB).
 *
 * Example:
 * java -cp target/aws-java-sdk-examples-1.0.jar com.examples.MultipartUpload --endpoint \
 * https://<pc-ip>:9440/api/prism/v4.0/objects/ --bucket mybucket --key myfile.txt -file /path/to/myfile.txt \
 * -part-size 1048576
 *
 * Response:
 * After the file is uploaded successfully, the script prints the ETag of the uploaded object.
 *
 * Limitations:
 * - This script does not handle resumable uploads or retries in case of failure. User needs to consider re-uploading
 *   the file in case of failure.
 * - This script does not support concurrent/parallel uploads. Each part is uploaded sequentially.
 */

package com.examples;

import com.utils.Utils;
import com.amazonaws.services.s3.AmazonS3;
import org.apache.commons.cli.*;

public class MultipartUpload {
    private AmazonS3 s3Client;

    // Default part size
    private static final long DEFAULT_PART_SIZE = 8 * 1024 * 1024; // 8 MB

    /**
     * Constructor to initialize the AmazonS3 client.
     *
     * @param encodedCredentials Base64 encoded credentials.
     * @param endpoint The S3 endpoint to connect to.
     * @throws Exception If an error occurs during client creation.
     */
    public MultipartUpload(String encodedCredentials, String endpoint) throws Exception {
        // Initialize the s3Client in the constructor
        this.s3Client = Utils.CreateS3Client(encodedCredentials, endpoint);
    }

    /**
     * Uploads a file to the specified S3 bucket with the given key.
     *
     * @param bucket The name of the S3 bucket.
     * @param key The key under which to store the new object.
     * @param filePath The path to the file to upload.
     * @param partSize The size of each part in bytes.
     * @throws Exception If an error occurs during the upload.
     */
    public void uploadObject(String bucket, String key, String filePath, long partSize) throws Exception {
        Utils.MultipartUpload(this.s3Client, bucket, key, filePath, partSize);
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

        // Define part size option
        Option partSizeOption = new Option("p", "part-size", true, "Part size in bytes");
        partSizeOption.setRequired(false);
        options.addOption(partSizeOption);

        // Create command line parser and formatter
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        String endpoint = null;
        String bucket = null;
        String key = null;
        String file = null;
        long partSize = DEFAULT_PART_SIZE;
        try {
            // Parse command line arguments
            CommandLine cmd = parser.parse(options, args);

            // Get values from command line arguments
            endpoint = cmd.getOptionValue("endpoint");
            bucket = cmd.getOptionValue("bucket");
            key = cmd.getOptionValue("key");
            file = cmd.getOptionValue("file");

            // Set the default part size if not provided
            String partSizeStr = cmd.getOptionValue("part-size");
            if (partSizeStr != null) {
                partSize = Long.parseLong(partSizeStr);
            }
        } catch (ParseException e) {
            System.err.println("Failed to parse command line arguments: " + e.getMessage());
            formatter.printHelp("ObjectsLiteMultipartUpload", options);
            System.exit(1);
        }

        // Get credentials from the user
        String encodedCredentials = Utils.GetCredentials();

        try {
            // Create MultipartUpload instance and upload the file
            MultipartUpload multipartUpload = new MultipartUpload(encodedCredentials, endpoint);
            multipartUpload.uploadObject(bucket, key, file, partSize);
        } catch (Exception e) {
            System.err.println("Failed to upload object: " + e.getMessage());
            System.exit(1);
        }
    }
}
