/**
 * ConcurrentMultipartUpload.java
 *
 * This class provides functionality to upload a file to Object Lite using the AWS SDK for Java via parallel multipart
 * upload.
 *
 * Parallel multipart upload allows you to upload a large object as a set of smaller parts concurrently, which can
 * significantly speed up the upload process. This script uses multiple threads to upload parts of the file concurrently
 * and then completes the multipart upload.
 *
 * Usage:
 * mvn clean package
 * java -cp target/aws-java-sdk-examples-1.0.jar com.examples.ConcurrentMultipartUpload --endpoint <endpoint> --bucket \
 * <bucket> --key <key> -file <file> [-part-size <part-size>] [-max-concurrency <max-concurrency>]
 *
 * Command Line Options:
 * -e, --endpoint      The endpoint URL of the objects-lite service.
 * -b, --bucket        The name of the bucket where the file will be uploaded.
 * -k, --key           The key (name) of the object to be created in the bucket.
 * -f, --file          The path to the file to be uploaded.
 * -p, --part-size     The size of each part in bytes (default is 8 MB).
 * -c, --max-concurrency The maximum number of concurrent uploads (default is 5).
 *
 * Example:
 * java -cp target/aws-java-sdk-examples-1.0.jar com.examples.ConcurrentMultipartUpload --endpoint \
 * https://<pc-ip>:9440/api/prism/v4.0/objects/ --bucket mybucket --key myfile.txt -file /path/to/myfile.txt \
 * -part-size 1048576 -max-concurrency 8
 *
 * Response:
 * After the file is uploaded successfully, the script prints the ETag of the uploaded object.
 *
 * Limitations:
 * - This script does not handle resumable uploads or retries in case of failure. User needs to consider re-uploading
 *   the file in case of failure.
 */

package com.examples;

import com.utils.Utils;
import com.amazonaws.services.s3.AmazonS3;
import org.apache.commons.cli.*;

public class ConcurrentMultipartUpload {
    private AmazonS3 s3Client;

    // Default part size
    private static final long DEFAULT_PART_SIZE = 8 * 1024 * 1024; // 8 MB

    // Default max concurrency
    private static final int DEFAULT_MAX_CONCURRENCY = 5;

    /**
     * Constructor to initialize the AmazonS3 client.
     *
     * @param encodedCredentials Base64 encoded credentials.
     * @param endpoint The S3 endpoint to connect to.
     * @throws Exception If an error occurs during client creation.
     */
    public ConcurrentMultipartUpload(String encodedCredentials, String endpoint) throws Exception {
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
     * @param maxConcurrency The maximum number of concurrent uploads.
     * @throws Exception If an error occurs during the upload.
     */
    public void uploadObject(String bucket, String key, String filePath, long partSize, int maxConcurrency) throws Exception {
        Utils.ConcurrentMultipartUpload(this.s3Client, bucket, key, filePath, partSize, maxConcurrency);
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
        Option partSizeOption = new Option("p", "part-size", true, "Part size in bytes (default is 8 MB)");
        partSizeOption.setRequired(false);
        options.addOption(partSizeOption);

        // Define max concurrency option
        Option maxConcurrencyOption = new Option("c", "max-concurrency", true,
            "The maximum number of concurrent uploads (default is 5)");
        maxConcurrencyOption.setRequired(false);
        options.addOption(maxConcurrencyOption);

        // Create command line parser and formatter
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        String endpoint = null;
        String bucket = null;
        String key = null;
        String file = null;
        long partSize = DEFAULT_PART_SIZE;
        int maxConcurrency = DEFAULT_MAX_CONCURRENCY;
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

            // Set the default max concurrency if not provided
            String maxConcurrencyStr = cmd.getOptionValue("max-concurrency");
            if (maxConcurrencyStr != null) {
                maxConcurrency = Integer.parseInt(maxConcurrencyStr);
            }
        } catch (ParseException e) {
            System.err.println("Failed to parse command line arguments: " + e.getMessage());
            formatter.printHelp("ObjectsLiteConcurrentMultipartUpload", options);
            System.exit(1);
        }

        // Get credentials from the user
        String encodedCredentials = Utils.GetCredentials();

        try {
            // Create ConcurrentMultipartUpload instance and upload the file
            ConcurrentMultipartUpload concurrentMultipartUpload = new ConcurrentMultipartUpload(encodedCredentials, endpoint);
            concurrentMultipartUpload.uploadObject(bucket, key, file, partSize, maxConcurrency);
        } catch (Exception e) {
            System.err.println("Failed to upload object: " + e.getMessage());
            System.exit(1);
        }
    }
}
