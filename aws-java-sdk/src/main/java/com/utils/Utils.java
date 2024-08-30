/**
 * Utils.java
 *
 * This utility class provides helper methods for uploading files to Objectslite using the AWS SDK for Java.
 * It includes functions for obtaining credentials, creating sessions, and uploading files to S3 buckets using various
 * methods such as single put-object, multipart upload, and concurrent multipart upload. These utilities simplify the
 * process of uploading files to Objects-Lite by handling session creation, credential management, and upload
 * operations.
 *
 * Key Methods:
 * - GetCredentials: Prompts the user for a username and password, then encodes them in Base64.
 * - CreateS3Client: Creates and configures an S3 client with custom SSL settings.
 * - PutObject: Uploads a file to a specified S3 bucket using the putObject API.
 * - MultipartUpload: Uploads a large file to a specified S3 bucket using the multipart upload APIs.
 * - ConcurrentMultipartUpload: Uploads a large file to a specified S3 bucket using concurrent multipart uploads.
 *
 * Usage:
 * Import the package and call the desired functions with appropriate arguments.
 *
 * Example:
 * AmazonS3 s3Client = Utils.CreateS3Client(encodedCredentials, endpoint);
 * Utils.PutObject(s3Client, bucket, key, filePath);
 *
 */

package com.utils;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;

public class Utils {
    public static final Regions DEFAULT_REGION = Regions.US_EAST_1;

    /**
     * Prompts the user for a username and password, then encodes them in Base64.
     *
     * @return Base64 encoded credentials in the format "username:password".
     */
    public static String GetCredentials() {
        Scanner scanner = new Scanner(System.in);

        // Prompt for username
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        // Prompt for password
        System.out.print("Enter password: ");
        String password = new String(System.console().readPassword()).trim();

        // Encode username:password in Base64
        String stringCredentials = username + ":" + password;
        String encodedCredentials = Base64.getEncoder().encodeToString(stringCredentials.getBytes());

        return encodedCredentials;
    }

    /**
     * Creates an AmazonS3 client with the provided credentials and endpoint.
     * SSL certificate verification is disabled.
     *
     * @param encodedCredentials Base64 encoded credentials.
     * @param endpoint The S3 endpoint to connect to.
     * @return An AmazonS3 client.
     * @throws Exception If an error occurs during client creation.
     */
    public static AmazonS3 CreateS3Client(String encodedCredentials, String endpoint) throws Exception {
        // Create AWS credentials using the provided Base64 encoded credentials
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(encodedCredentials, encodedCredentials);

        // Initialize SSL context to disable SSL certificate verification
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {}

            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        }}, new java.security.SecureRandom());

        // Create an SSLConnectionSocketFactory with the SSLContext
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                sslContext, NoopHostnameVerifier.INSTANCE);

        // Configure the client to use the custom SSL socket factory
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.getApacheHttpClientConfig().setSslSocketFactory(sslSocketFactory);

        // Build and return the AmazonS3 client
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, DEFAULT_REGION.getName()))
                .withPathStyleAccessEnabled(true)
                .withClientConfiguration(clientConfiguration)
                .build();
    }

    /**
     * Uploads a file to the specified S3 bucket with the given key via putObject API.
     *
     * @param s3Client The S3 client.
     * @param bucket The name of the S3 bucket.
     * @param key The key under which to store the new object.
     * @param filePath The path to the file to upload.
     * @throws Exception If an error occurs during the upload.
     */
    public static void PutObject(AmazonS3 s3Client, String bucket, String key, String filePath) throws Exception {
        // Check if the file exists
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("File does not exist or is not a valid file: " + filePath);
        }

        // Create PutObjectRequest
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, key, file);

        // Upload the file
        PutObjectResult putObjectResult = s3Client.putObject(putObjectRequest);
        System.out.println("ETag: " + putObjectResult.getETag());
    }

    /**
     * Uploads a large file to the specified S3 bucket with the given key via multipart upload API.
     *
     * @param s3Client The S3 client.
     * @param bucket The name of the S3 bucket.
     * @param key The key under which to store the new object.
     * @param filePath The path to the file to upload.
     * @param partSize The size of each part in bytes.
     * @throws Exception If an error occurs during the upload.
     */
    public static void MultipartUpload(AmazonS3 s3Client, String bucket, String key, String filePath, long partSize)
        throws Exception {

        // Check if the file exists
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("File does not exist or is not a valid file: " + filePath);
        }

        // Create a list to hold the part ETags
        List<PartETag> partETags = new ArrayList<>();

        // Initiate the multipart upload
        InitiateMultipartUploadRequest initMultipartRequest = new InitiateMultipartUploadRequest(bucket, key);
        InitiateMultipartUploadResult initMultipartResponse = s3Client.initiateMultipartUpload(initMultipartRequest);

        long contentLength = file.length();

        // Upload the file parts
        long filePosition = 0;
        for (int i = 1; filePosition < contentLength; i++) {
            // Calculate the size of the part
            long currentPartSize = Math.min(partSize, (contentLength - filePosition));

            // Create the request to upload a part
            UploadPartRequest uploadPartRequest = new UploadPartRequest()
                    .withBucketName(bucket)
                    .withKey(key)
                    .withUploadId(initMultipartResponse.getUploadId())
                    .withPartNumber(i)
                    .withFileOffset(filePosition)
                    .withFile(file)
                    .withPartSize(currentPartSize);

            // Upload the part and add the response's ETag to our list
            UploadPartResult uploadPartResult = s3Client.uploadPart(uploadPartRequest);
            partETags.add(uploadPartResult.getPartETag());

            filePosition += currentPartSize;
        }

        // Complete the multipart upload
        CompleteMultipartUploadRequest compMultipartRequest = new CompleteMultipartUploadRequest(bucket, key,
            initMultipartResponse.getUploadId(), partETags);
        CompleteMultipartUploadResult compMultipartResponse = s3Client.completeMultipartUpload(compMultipartRequest);
        System.out.println("ETag: " + compMultipartResponse.getETag());
    }

    /**
     * Uploads a large file to the specified S3 bucket with the given key via multipart upload API using concurrent uploads.
     *
     * @param s3Client The S3 client.
     * @param bucket The name of the S3 bucket.
     * @param key The key under which to store the new object.
     * @param filePath The path to the file to upload.
     * @param partSize The size of each part in bytes.
     * @param maxConcurrency The number of concurrent uploads to perform.
     * @throws Exception If an error occurs during the upload.
     */
    public static void ConcurrentMultipartUpload(AmazonS3 s3Client, String bucket, String key, String filePath,
        long partSize, int maxConcurrency) throws Exception {

        // Assert that concurrency is less than or equal to 8
        if (maxConcurrency > 8) {
            throw new IllegalArgumentException("Concurrency level must be less than or equal to 8");
        }

        // Check if the file exists
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("File does not exist or is not a valid file: " + filePath);
        }

        // Create a list to hold the part ETags
        List<PartETag> partETags = new ArrayList<>();

        // Initiate the multipart upload
        InitiateMultipartUploadRequest initMultipartRequest = new InitiateMultipartUploadRequest(bucket, key);
        InitiateMultipartUploadResult initMultipartResponse = s3Client.initiateMultipartUpload(initMultipartRequest);

        long contentLength = file.length();

        // Create an ExecutorService to manage the threads
        ExecutorService executorService = Executors.newFixedThreadPool(maxConcurrency);
        List<Future<PartETag>> futures = new ArrayList<>();

        // Upload the file parts concurrently
        long filePosition = 0;
        for (int i = 1; filePosition < contentLength; i++) {
            final int partNumber = i;
            final long offset = filePosition;
            final long currentPartSize = Math.min(partSize, (contentLength - filePosition));

            // Print the part number and offset
            System.out.println("Part Number: " + partNumber + " Offset: " + offset + " Part Size: " + currentPartSize);

            // Submit a task to upload the part
            futures.add(executorService.submit(() -> {
                UploadPartRequest uploadPartRequest = new UploadPartRequest()
                        .withBucketName(bucket)
                        .withKey(key)
                        .withUploadId(initMultipartResponse.getUploadId())
                        .withPartNumber(partNumber)
                        .withFileOffset(offset)
                        .withFile(file)
                        .withPartSize(currentPartSize);

                // Upload the part and return the ETag
                UploadPartResult uploadPartResult = s3Client.uploadPart(uploadPartRequest);
                return uploadPartResult.getPartETag();
            }));

            filePosition += currentPartSize;
        }

        // Wait for all parts to be uploaded and collect the ETags
        for (Future<PartETag> future : futures) {
            partETags.add(future.get());
            // Print all info of the part
            // System.out.println("Part ETag: " + future.get().getETag() + " Part Number: " + future.get().getPartNumber());
        }

        // Complete the multipart upload
        CompleteMultipartUploadRequest compMultipartRequest = new CompleteMultipartUploadRequest(bucket, key,
            initMultipartResponse.getUploadId(), partETags);
        CompleteMultipartUploadResult compMultipartResponse = s3Client.completeMultipartUpload(compMultipartRequest);
        System.out.println("ETag: " + compMultipartResponse.getETag());

        // Shutdown the executor service
        executorService.shutdown();
    }
}
