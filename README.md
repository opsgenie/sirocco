# SIROCCO

[![License](https://img.shields.io/github/license/opsgenie/sirocco.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Lifecycle](https://img.shields.io/osslifecycle/opsgenie/sirocco.svg)]()

## Get Sirocco

Binaries are available from [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Ccom.opsgenie.sirocco).

|Group|Artifact|Latest Stable Version|
|-----------|---------------|---------------------|
|com.opsgenie.sirocco|sirocco-*|[![Maven Central](https://img.shields.io/maven-central/v/com.opsgenie.sirocco/sirocco-oss-parent.svg)]()|

Below are the various artifacts published:

|Artifact|Description|
|-----------|---------------|
|[sirocco-api](sirocco-api)|API module|
|[sirocco-warmup](sirocco-warmup)|Warmup module|

## Builds

Sirocco builds are run on OpsGenie's internal Jenkins.

|  Branch |                                                     Build                                                     |                                                                         Coverage                                                                         |                                                                         Tests                                                                         |
|:-------:|:-------------------------------------------------------------------------------------------------------------:|:--------------------------------------------------------------------------------------------------------------------------------------------------------:|:-----------------------------------------------------------------------------------------------------------------------------------------------------:|
|  Master | [![Build Status](https://jenkins.opsgeni.us/buildStatus/buildIcon?job=SiroccoOSSBuild)]() | [![Code Coverage](https://jenkins.opsgeni.us/buildStatus/coverageIcon?job=SiroccoOSSBuild)]() | [![Test Status](https://jenkins.opsgeni.us/buildStatus/testIcon?job=SiroccoOSSBuild)]() |

## Versioning

Artifact versions are in `X.Y.Z` format
- `X`: Major version number. 
  - Increases only when there are big API and/or architectural changes. 
  - Breaks backward compatibility.
- `Y`: Minor version number. 
  - Increases for small API changes and big improvements. 
  - Breaks backward compatibility.
- `Z`: Patch version number. 
  - Increases for bug fixes and small improvements. 
  - Doesn’t break backward compatibility. 

## Requirements

* JDK 1.8+ (for build and execution)
* Maven 3.x (for build)

## Build

To build:

```
$ git clone git@github.com:opsgenie/sirocco.git
$ cd sirocco/
$ mvn clean install
```

## Issues and Feedback

[![Issues](https://img.shields.io/github/issues/opsgenie/sirocco.svg)](https://github.com/opsgenie/sirocco/issues?q=is%3Aopen+is%3Aissue)
[![Closed issues](https://img.shields.io/github/issues-closed/opsgenie/sirocco.svg)](https://github.com/opsgenie/sirocco/issues?q=is%3Aissue+is%3Aclosed)

Please use [GitHub Issues](https://github.com/opsgenie/sirocco/issues) for any bug report, feature request and support.

## Contribution

[![Pull requests](https://img.shields.io/github/issues-pr/opsgenie/sirocco.svg)](https://github.com/opsgenie/sirocco/pulls?q=is%3Aopen+is%3Apr)
[![Closed pull requests](https://img.shields.io/github/issues-pr-closed/opsgenie/sirocco.svg)](https://github.com/opsgenie/sirocco/pulls?q=is%3Apr+is%3Aclosed)
[![Contributors](https://img.shields.io/github/contributors/opsgenie/sirocco.svg)]()

If you would like to contribute, please 
- Fork the repository on GitHub and clone your fork.
- Create a branch for your changes and make your changes on it.
- Send a pull request by explaining clearly what is your contribution.

> Tip: Please check the existing pull requests for similar contributions and consider submit an issue to discuss the proposed feature before writing code.

## LICENSE

Copyright (c) 2017 OpsGenie, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
