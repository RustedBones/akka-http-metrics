# Changelog

## Unreleased

## v1.7.1 (2022-06-07)

- Fix prometheus dimension ordering

## v1.7.0 (2022-04-11)

- Update dependencies
- Add support for custom dimensions
- Change from sequence to varagrs API in settings

## v1.6.0 (2021-05-07)

- Update dependencies
- Throw explicit exception when trace-id not found in by the MereStage
- Seal and convert routes internally
- Move HttpMetricsServerBuilder to http.metrics.core package
- Remove deprecated HttpMetricsRoute

## v1.5.1 (2021-02-16)

- Update dependencies
- Add API to create HttpMetricsSettings

## v1.5.0 (2021-01-12)

- Update dependencies
- Split dropwizard v4 and v5. v4 being the default.
- Fix bug when HttpEntity.Default is used with HTTP/1.0 protocol

## 1.4.1 (2020-12-14)

- Fix regression due to automatic transformation of entity into streams
- Fix regression in meterFunction APIs

## 1.4.0 (2020-12-12)

- Split HttpMetricsHandler API with separated callbacks
- Add requests failures counter for unserved requests
- Compute sizes and durations metrics on end of entity stream
- Remove deprecated API

## 1.3.0 (2020-11-09)

- Fix Metrics BidiFlow closing
- Adding support for custom server-level dimensions

## 1.2.0 (2020-08-29)

- Update to akka-http 10.2
- Update libraries
- Add metrics names to settings
- Streamline metrics, name and doc

## 1.1.1 (2020-06-10)

- Update libraries
- Fix implicit execution context regression

## 1.1.0 (2020-04-18)

- Fix implicits for HTTP/2 API
- Explicit path labelling

## 1.0.0 (2020-03-14)

- Update libraries
- Decorrelate routes from registry
- Add namespace setting
- Add histogram config for prometheus registry
- Add method dimension

## 0.6.0 (2019-08-25)

- Use static path dimension to for unhandled requests
- Add graphite carbon support

## 0.5.0 (2019-07-27)

- Update to akka-http 10.1.9
- Add path label support
- Add async handler api

## 0.4.0 (2019-07-06)

- Add scala 2.13 support
- Add TCP connection metrics

## 0.3.0 (2019-04-12)

- Use status group dimension on responses and duration
- Change response metric names for more consistency

## 0.2.1 (2019-03-23)

- Fix prometheus time conversion [#4](https://github.com/RustedBones/akka-http-metrics/issues/4)

## 0.2.0 (2018-12-28)

- Initial release

## [Deprecated] [akka-http-prometheus](https://github.com/RustedBones/akka-http-prometheus)

See original's project [changelog](https://github.com/RustedBones/akka-http-prometheus/blob/master/CHANGELOG.md)