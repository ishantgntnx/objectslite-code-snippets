/*
This script uploads a file to Objectslite using the AWS SDK for Go via put-object API.

Usage:
    go run put-object.go --endpoint <endpoint> --bucket <bucket> --key <key> --file <file>

Flags:
    --endpoint: Objectslite endpoint
    --bucket: Bucket name
    --key: Key name
    --file: File to upload

The script prompts for username and password to authenticate with Objectslite. The credentials will be encoded in Base64
and used as the access key and secret key for the session.

Example:
    go run put-object.go --endpoint https://<pc-ip>:9440/api/prism/v4.0/objects/ --bucket mybucket --key mykey --file \
    myfile.txt

Response details:
    After the file is uploaded successfully, the script prints the ETag of the uploaded object.

Limitations:
    - This script only supports uploading objects smaller than 5 GB. For larger files, consider using multipart upload.
*/

package main

import (
	"flag"
	"fmt"
	"log"
	"os"

	"github.com/ishantgntnx/objectslite-code-snippets/aws-golang-sdk/utils"
)

func main() {
	// Define flags
	endpoint := flag.String("endpoint", "", "Objectslite endpoint")
	bucket := flag.String("bucket", "", "Bucket name")
	key := flag.String("key", "", "Key name")
	file := flag.String("file", "", "File to upload")
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

	// Upload the file via put-object operation
	result, err := utils.PutObject(svc, *bucket, *key, *file)
	if err != nil {
		log.Fatalf("Failed to upload file to objectslite with error: %v", err)
	}
	fmt.Println("File uploaded successfully to objectslite")
	if result.ETag != nil {
		fmt.Printf("ETag: %s\n", *result.ETag)
	}
}
