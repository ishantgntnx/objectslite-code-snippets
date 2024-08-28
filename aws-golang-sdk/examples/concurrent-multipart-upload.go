/*
This script uploads a file to Objectslite using the AWS SDK for Go via parallel multipart upload.

Parallel multipart upload allows you to upload a large object as a set of smaller parts concurrently, which can
significantly speed up the upload process. This script uses goroutines to upload parts concurrently and then completes
the multipart upload.

Usage:
    go run concurrent-multipart-upload.go --endpoint <endpoint> --bucket <bucket> --key <key> --file <file> \
    [--part-size <part-size>] [--max-concurrency <max-concurrency>]

Flags:
    --endpoint: Objectslite endpoint
    --bucket: Bucket name
    --key: Key name
    --file: File to upload
    --part-size: Part size for multipart upload (default: 8 MB)
    --max-concurrency: Concurrency level for multipart upload (default: 5)

The script prompts for username and password to authenticate with Objectslite. The credentials will be encoded in Base64
and used as the access key and secret key for the session.

Example:
    go run concurrent-multipart-upload.go --endpoint https://<pc-ip>:9440/api/prism/v4.0/objects/ --bucket mybucket \
    --key mykey --file myfile.txt --part-size 5242880 --max-concurrency 8

Response details:
    After the file is uploaded successfully, the script prints the ETag of the uploaded object.

Limitations:
    - This script does not handle resumable uploads or retries in case of failure. User needs to consider re-uploading
      the file in case of failure.
*/

package main

import (
	"flag"
	"fmt"
	"log"
	"os"

	"github.com/ishantgntnx/objectslite-code-snippets/aws-golang-sdk/utils"
)

const (
	defaultConcurrency = 5
	defaultPartSize    = 8 * 1024 * 1024 // 8 MB
)

func main() {
	// Define flags
	endpoint := flag.String("endpoint", "", "Objectslite endpoint")
	bucket := flag.String("bucket", "", "Bucket name")
	key := flag.String("key", "", "Key name")
	file := flag.String("file", "", "File to upload")
	partSize := flag.Int64("part-size", defaultPartSize, "Part size for multipart upload")
	maxConcurrency := flag.Int("max-concurrency", defaultConcurrency, "Concurrency level for multipart upload")
	flag.Parse()

	// Check if required flags are provided
	if *endpoint == "" || *bucket == "" || *key == "" || *file == "" {
		fmt.Println("Error: --endpoint, --bucket, --key, and --file flags are required")
		flag.Usage()
		os.Exit(1)
	}

	// Get encoded credentials
	encodedCredentials, err := utils.GetCredentials()
	if err != nil {
		log.Fatalf("Error reading and encoding credentials: %v", err)
	}

	// Create an S3 client
	svc, err := utils.CreateS3Client(*endpoint, encodedCredentials)
	if err != nil {
		log.Fatalf("Failed to create session: %v", err)
	}

	// Upload the file via concurrent multipart upload
	result, err := utils.ConcurrentMultipartUpload(svc, *bucket, *key, *file, *partSize, *maxConcurrency)
	if err != nil {
		log.Fatalf("Failed to upload file to objectslite with error: %v", err)
	}
	fmt.Println("File uploaded successfully to objectslite")
	if result.ETag != nil {
		fmt.Printf("ETag: %s\n", *result.ETag)
	}
}
