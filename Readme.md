## ZeroFs

ZeroFs is a port of the original [Jimfs](https://github.com/google/jimfs) project, with the following key differences:

- Zero dependencies
- Java 11+

The goal is to make it the "go to" Virtual FileSystem for Java, especially when used to support a WASI layer by WASM payloads.
To achieve it we build on the decade old [Jimfs](https://github.com/google/jimfs) foundation.
