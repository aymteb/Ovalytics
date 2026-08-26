resource "kubernetes_namespace_v1" "ovalytics" {
  metadata {
    name = var.namespace
  }
}
