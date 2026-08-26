variable "namespace" {
  type    = string
  default = "ovalytics"
}

variable "db_name" {
  type    = string
  default = "ovalytics"
}

variable "db_user" {
  type    = string
  default = "ovalytics"
}

variable "db_password" {
  type      = string
  default   = "ovalytics"
  sensitive = true
}

variable "backend_image" {
  type    = string
  default = "ovalytics-backend:local"
}
