variable "bucket_name" {
  description = "The name of the Google Cloud Storage bucket"
  type        = string
}

variable "location" {
  description = "The location/region where the storage bucket will be created"
  type        = string
}

variable "storage_class" {
  description = "The storage class of the bucket (e.g., STANDARD, NEARLINE, COLDLINE, ARCHIVE)"
  type        = string
}

variable "cloud_run_sa_email" {
  description = "The Cloud Run service account email that needs access to the storage bucket"
  type        = string
}