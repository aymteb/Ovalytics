output "namespace" {
  value = kubernetes_namespace_v1.ovalytics.metadata[0].name
}

output "api_url" {
  value = "http://localhost:8080/api/health"
}

output "check_pods" {
  value = "kubectl get pods -n ${var.namespace}"
}
