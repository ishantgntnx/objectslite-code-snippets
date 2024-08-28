/*
This script uploads a file to Objectslite using the AWS SDK for Go via s3manager package.

Package s3manager provides a wrapper around the upload APIs to make it easier to upload objects using the SDK. This
includes support for multipart uploads on large objects and concurrent uploads for increased performance.

Usage:
    go run uploader.go --endpoint <endpoint> --bucket <bucket> --key <key> --file <file> [--part-size <part-size>] \
    [--max-concurrency <max-concurrency>]

Flags:
    --endpoint: Objectslite endpoint
    --bucket: Bucket name
    --key: Key name
    --file: File to upload
    --part-size: Part size for multipart upload (default: 8MB)
    --max-concurrency: Concurrency level for multipart upload (default: 5)

The script prompts for username and password to authenticate with Objectslite. The credentials will be encoded in Base64
and used as the access key and secret key for the session.

Example:
    go run uploader.go --endpoint https://<pc-ip>:9440/api/prism/v4.0/objects/ --bucket mybucket --key mykey --file \
    myfile.txt --part-size 8*1024*1024 --max-concurrency 5

Response details:
    After the file is uploaded successfully, the script prints the ETag of the uploaded object.

Limitations:
    - This script does not handle resuming uploads if they are interrupted. User needs to consider re-uploading the file
      in case of failure.
    - In AWS Golang SDK's s3manager, threshold for multipart upload and part size are defined under single flag
      'PartSize' and cannot be set separately.
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
	defaultPartSize    = 8 * 1024 * 1024 // 8MB
	defaultConcurrency = 5
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

	// Create S3 uploader
	uploader, err := utils.CreateUploader(*endpoint, encodedCredentials, *partSize, *maxConcurrency)
	if err != nil {
		log.Fatalf("Failed to create uploader: %v", err)
	}

	// Upload the file
	result, err := utils.UploadFile(uploader, *bucket, *key, *file)
	if err != nil {
		log.Fatalf("Failed to upload file to objectslite with error: %v", err)
	}
	fmt.Println("File uploaded successfully to objectslite")
	if result.ETag != nil {
		fmt.Printf("ETag: %s\n", *result.ETag)
	}
}
