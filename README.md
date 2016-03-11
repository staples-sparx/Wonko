# Wonko

Monitoring and alerting service for all Staples SparX services.

## Integration

If you're looking to integrate your service with wonko, see https://github.com/StaplesLabs/wonko-client.

## How does it work?

- A service integrating with Wonko uses the wonko-client to send metrics to Wonko.
- Wonko reads off designated topics `wonko-alerts` and `wonko-events` in the central kafka cluster.
- It sends pager-duty alerts for events coming through `wonko-alerts`. Each service needs a configured pager-duty endpoint.
- It creates prometheus objects for all metrics and exposes an endpoint per service for prometheus to poll.
- Prometheus polling and alerting is configured independently, outside of wonko.

![high-level-architecture.png](./doc/high-level-architecture.png)
