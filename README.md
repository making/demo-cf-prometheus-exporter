
Access index-0

```
while true;do curl -k -s -w '\n' https://0-demo.<app domain>/prometheus-cf | grep '"/prometheus"';sleep 1;done
```

Access index-1

```
while true;do curl -k -s -w '\n' https://1-demo.<app domain>/prometheus-cf | grep '"/prometheus"';sleep 1;done
```

exporter example

``` yaml
scrape_configs:
- job_name: demo
  scrape_interval: 60s
  scrape_timeout: 10s
  metrics_path: /prometheus-cf
  scheme: https
  static_configs:
  - targets:
    - 0-demo.<app domain>:443
    - 1-demo.<app domain>:443
    - 2-demo.<app domain>:443
```