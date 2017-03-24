# BUILD
```bash
./gradlew installApp
```
Installs all needed jars and scripts for tests

# TESTS
```bash
./gradlew test
```
Simple unit tests

```bash
./examples/parallel_index/idx.sh
./examples/parallel_index/search.sh
```
Integration test - index real documents from examples/parallel_index/text.parsed and search for given tokens in result

# DOCS FOR INTEGRATION TEST
```bash
./examples/prepare_docs/prepare_basic.sh
```
Downloads documents from net and prepares file for input in integration test. Needs protoc in path and python-protobuf
