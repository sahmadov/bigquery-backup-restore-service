variable "schedule" {
  description = "The cron schedule for Cloud Scheduler"
  type        = string
}

variable "project_id" {
  description = "The GCP Project ID"
  type        = string
}

variable "message_body" {
  description = "JSON message body to send with the Cloud Scheduler job (will be base64 encoded)"
  type        = string
  default     = "{}"
}

variable "backend_uri" {
  description = "The Uri of Cloud Run Application which is going to handle request"
  type        = string
}

variable "name" {
  description = "The name of Cloud Scheduler"
  type        = string
}

variable "description" {
  description = "The description of Cloud Scheduler"
  type        = string
}

variable "scheduler_sa_email" {
  description = "The service account email to use for the scheduler"
  type        = string
}