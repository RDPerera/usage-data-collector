# WSO2 Usage Data Collector

[![Build Status](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fwso2.org%2Fjenkins%2Fview%2Fproducts%2Fjob%2Fproducts%2Fjob%2Fproduct-apim%2F)](<To be added>)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![stackoverflow](https://img.shields.io/badge/stackoverflow-wso2mi-orange)](https://stackoverflow.com/tags/wso2-am/)
[![stackoverflow](https://img.shields.io/badge/stackoverflow-wso2am-orange)](https://stackoverflow.com/tags/wso2-micro-integrator/)

---

# Overview

This repository contains the **WSO2 Usage Data Collector** - a component of the Consumption Tracker system designed to effectively track and record product consumption data within the **WSO2 Micro Integrator (MI)** environment.

### Description

During the initial sale of a product (or multiple products), agreements are made regarding the number of cores or transaction volumes. However, after deployment, there is currently no proper visibility into whether customers are consuming more than their allocated limits.

Without an existing mechanism to **record or track consumption** (cores, transaction volumes, or automation runtime), we rely solely on customer-provided information.

The **Usage Data Collector** addresses this challenge by providing a **system to record product consumption data** and **generate usage reports** when required.


---

### Architecture

The consumption tracking system consists of three main components deployed across MI nodes:

<img width="800" alt="Overall Architecture" src="./docs/counter_highlevel_arch.png" />

## MI-Side Components

The system introduces three main components on the MI side:

- **Data Collector** - Gathers usage and deployment information
- **Data Publisher** - Publishes collected data to configured destinations
- **Data Receiver** - Receives and secures data before forwarding to endpoints


### Data Collector

The **Usage Data Collector** is the component housed in this repository. 

The broader Consumption Tracker system includes two types of data collectors:

| Subcomponent | Description | Frequency / Trigger | Status |
|--------------|-------------|---------------------|--------|
| **Deployment Data Collector** | Collects environment-level data such as CPU core count, JDK version, OS, and update level | Daily / On restart | Separate component (not in this repo) |
| **Usage Data Collector** | Captures the number of transactions per hour processed by MI nodes and measures the total runtime of automation tasks executed | Hourly | **This repository** |

> **Note**: This repository contains only the **Usage Data Collector** component. The Deployment Data Collector is a separate component designed for reuse across multiple WSO2 products.

### How It Works

1. Each collector operates periodically or in an event-driven manner
2. Collected data is sent to the **Data Publisher**
3. Data is aggregated and prepared for transmission

---

### Data Publisher

The **Data Publisher** component:

- Receives aggregated data from the collectors


### Data Access Options

- **Local Storage (ICP)**: If data is stored locally, it can be exported using the **Integration Control Plane (ICP)**
- **Public API**: If data is published to the public API, WSO2 can directly monitor customer usage

---

### Data Receiver
The **Data Receiver** component:

- Secures incoming data from MI nodes
- Forwards data to designated endpoints for processing and storage
- Publishes data based on the available destination:
  - **Choreo-hosted WSO2 API** (for cloud-based monitoring) [Priority]
  - **Integration Control Plane (ICP)** (for customers preferring on-premise storage)

## Installation

1. Copy the generated JAR to your MI installation:
   ```bash
   cp counter/target/org.wso2.micro.integrator.usage.data.collector-0.1.0.jar <MI_HOME>/lib/
   ```

2. Update the `deployment.toml` configuration as shown above

3. Restart the MI server

## Configuration

### Basic Setup

Add the following configuration to your MI's `deployment.toml` file:

**Location**: `<MI_HOME>/conf/deployment.toml`

```toml
[integration.transaction_counter]
enable = true
server_id = "MI-Node-1"
producer_counting_thread_pool_size = 10
producer_scheduled_interval = 10
max_transaction_count_per_record = 20
min_transaction_count_per_record = 5
record_queue_size = 1000
publisher_scheduled_interval = 5
publisher_max_batch_size = 100
publisher_max_retries = 3
store_impl = "org.wso2.integration.transaction.counter.store.TransactionRecordStoreImpl"
service_url = "https://your-api-endpoint/transactions/records"
service_username = "admin"
service_password = "admin"
```

### Configuration Parameters

#### Core Settings

| Parameter | Description | Required |
|-----------|-------------|----------|
| `enable` | Enable/disable the usage data collector | Yes |
| `server_id` | Unique identifier for this MI node | Yes |

#### Data Collection Settings

| Parameter | Description | Default |
|-----------|-------------|---------|
| `producer_counting_thread_pool_size` | Number of threads for counting operations | 10 |
| `producer_scheduled_interval` | Interval (seconds) for scheduled data collection | 10 |
| `max_transaction_count_per_record` | Maximum transactions per record before publishing | 20 |
| `min_transaction_count_per_record` | Minimum transactions to trigger record creation | 5 |

#### Data Publishing Settings

| Parameter | Description | Default |
|-----------|-------------|---------|
| `record_queue_size` | Maximum records in queue before dropping | 1000 |
| `publisher_scheduled_interval` | Interval (seconds) for publishing records | 5 |
| `publisher_max_batch_size` | Maximum records per batch | 100 |
| `publisher_max_retries` | Retry attempts for failed transmissions | 3 |

#### Service Connection

| Parameter | Description |
|-----------|-------------|
| `service_url` | URL of the data receiving endpoint |
| `service_username` | Authentication username |
| `service_password` | Authentication password |

> **Note**: The handler class is automatically registered at `org.wso2.micro.integrator.usage.data.collector.TransactionCountHandler`

---

## Building from Source

### Prerequisites

- Java 8 or higher
- Maven 3.6+
- Git

### Build Steps

```bash
# Clone the repository
git clone https://github.com/RDPerera/integration-transaction-counter.git
cd integration-transaction-counter

# Build the project
mvn clean install

# The JAR will be created at:
# counter/target/org.wso2.micro.integrator.usage.data.collector-0.1.0.jar
```

---

## Project Structure

```
integration-transaction-counter/
├── counter/                          # Usage Data Collector source code
│   ├── src/main/java/
│   │   └── org/wso2/micro/integrator/usage/data/collector/
│   │       ├── config/               # Configuration management
│   │       ├── publisher/            # Data publishing logic
│   │       ├── record/               # Record data structures
│   │       └── exception/            # Exception classes
│   └── pom.xml
├── docs/                             # Documentation and diagrams
├── pom.xml                           # Parent POM
└── README.md                         # This file
```

---

## Data Collected

### Usage Data (Hourly)

- Transaction count per hour
- Automation task runtime duration

---

## Reusability

The Usage Data Collector is designed for reuse across multiple WSO2 products:

- **WSO2 Micro Integrator (MI)** - Primary target (current implementation)
- **WSO2 API Manager (APIM)** - Planned with necessary adjustments for API gateway usage tracking
- **WSO2 Identity Server (IS)** - Future integration for identity transaction tracking
- **WSO2 Identity & Access Management (IAM)** - Planned for access management metrics

The modular architecture allows teams to adapt the collector to product-specific requirements while maintaining core functionality.

---

## Contributing

We welcome contributions from the community! To contribute:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Guidelines

- Follow existing code style and conventions
- Add unit tests for new functionality
- Update documentation as needed
- Ensure compatibility with Apache 2.0 License

---

## Issues and Support

Please report issues at: [GitHub Issues](https://github.com/RDPerera/integration-transaction-counter/issues)

For WSO2 product support, visit:
- [WSO2 Support Portal](https://support.wso2.com/)
- [WSO2 Community](https://wso2.com/community/)

---

## License

This project is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).

---

## Additional Resources

- [WSO2 Micro Integrator Documentation](https://mi.docs.wso2.com/)
- [WSO2 API Manager Documentation](https://apim.docs.wso2.com/)
- [Apache Synapse](https://synapse.apache.org/)
- [WSO2 Integration Control Plane](https://wso2.com/integration/integration-control-plane/)

---

---

(c) 2025, [WSO2 LLC](http://www.wso2.org/). All Rights Reserved.