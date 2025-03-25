module "api_services" {
  source     = "./modules/api-services"
  project_id = local.project_id
}

module "service_accounts" {
  source     = "./modules/service-accounts"
  project_id = local.project_id
  depends_on = [module.api_services]
}

module "cloud_run" {
  source               = "./modules/cloud-run"
  service_name         = local.backup_service_name
  region               = local.region
  container_image      = "${local.region}-docker.pkg.dev/${local.project_id}/${local.backup_container_repo_name}/${local.backup_service_name}:latest"
  project_id           = local.project_id
  service_account_email = module.service_accounts.cloud_run_sa_email
  depends_on           = [module.api_services, module.service_accounts]
}

module "cloud_scheduler_backup" {
  source           = "./modules/cloud-scheduler"
  project_id       = local.project_id
  name             = "trigger-bigquery-backup"
  description      = "Triggers BigQuery backup via Cloud Run Service"
  schedule         = local.backup_scheduler_cron
  backend_uri      = "${module.cloud_run.service_url}/api/v1/backup"
  message_body     = local.backup_scheduler_message
  scheduler_sa_email = module.service_accounts.scheduler_sa_email
  depends_on       = [module.api_services, module.service_accounts]
}

module "cloud_scheduler_restore" {
  source           = "./modules/cloud-scheduler"
  project_id       = local.project_id
  name             = "trigger-restore-backup"
  description      = "Triggers BigQuery restore via Cloud Run Service"
  schedule         = local.restore_scheduler_cron
  backend_uri      = "${module.cloud_run.service_url}/api/v1/restore"
  message_body     = local.restore_scheduler_message
  scheduler_sa_email = module.service_accounts.scheduler_sa_email
  depends_on       = [module.api_services, module.service_accounts]
}

module "storage" {
  source             = "./modules/storage"
  bucket_name        = local.backup_bucket_name
  location           = local.region
  storage_class      = local.backup_bucket_storage_class
  cloud_run_sa_email = module.service_accounts.cloud_run_sa_email
  depends_on         = [module.api_services, module.service_accounts]
}

module "artifact_registry" {
  source          = "./modules/artifact-registry"
  project_id      = local.project_id
  repository_name = local.backup_container_repo_name
  location        = local.region
  depends_on      = [module.api_services]
}