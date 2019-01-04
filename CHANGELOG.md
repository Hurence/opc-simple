# Changelog
All notable changes to this project will be documented in this file.

## Table of contents

- [[Unreleased]](#unreleased)
	- [Features](#features)
	- [Bugfixes](#bugfixes)
	- [Breaking changes](#breaking-changes)
- [[2.0.1] (2018-12-28)](#201-2018-12-28)
	- [Bugfixes](#bugfixes-1)
- [[2.0.0] (2018-07-05)](#200-2018-07-05)
	- [Features](#features-1)
	- [Bugfixes](#bugfixes-2)
	- [[1.2.0] (2018-06-29)](#120-2018-06-29)
	- [Features](#features-2)
	- [Breaking changes](#breaking-changes-1)
	- [[1.1.2] (2018-05-02)](#112-2018-05-02)
	- [Features](#features-3)
	- [[1.0.0] (2018-04-16)](#100-2018-04-16)
	- [Features](#features-4)

## [Unreleased]

### Features
- **OPC Simple goes ReactiveX**

### Bugfixes
- Browse next tree level may be incomplete for OPC-UA

### Breaking changes
- Breaking changes on all APIs in order to support reactive patterns.
      
## [2.0.1] (2018-12-28)

### Bugfixes
- Migrate jinterop artifacts to maven central

## [2.0.0] (2018-07-05)
### Features
- Use an improved threading model for subscriptions
### Bugfixes
- Make opc-simple compliant with KEP. MinimumSamplingRate rate is returned as integer and not double as supposed.
- Improve general stability

### [1.2.0] (2018-06-29)

### Features
- OPC-UA implementation with Eclipse Milo.
- Support to Anonymous, UserPass and X509
- Support to both tag refresh rate and publication window
- Improved testing and documentation.

### Breaking changes
- OpcOperations become an interface. Implementations are now OpcUaTemplate and OpcDaTemplate
- Opc Auto connect now removes unnecessary cast thanks to dynamic proxies.

### [1.1.2] (2018-05-02)

### Features

- Support try with resources to automatically close Sessions and Connections
- Support error codes in OpcData
- Support stateful sessions. Multiplex Groups in a single connection.
- Use native java types instead of JIVariant.

### [1.0.0] (2018-04-16)

### Features

- OPC-DA implementation with JInterop and Utgard


[Unreleased]: https://github.com/Hurence/opc-simple/compare/master...develop
[1.0.0]: https://github.com/Hurence/opc-simple/compare/7051ad30c02643a50827668b6f50066744e1204c...1.0.0
[1.1.2]: https://github.com/Hurence/opc-simple/compare/1.0.0...1.1.2
[1.2.0]: https://github.com/Hurence/opc-simple/compare/1.1.2...1.2.0
[2.0.0]: https://github.com/Hurence/opc-simple/compare/2.0.0...1.2.0
[2.0.1]: https://github.com/Hurence/opc-simple/compare/2.0.0...2.0.1


