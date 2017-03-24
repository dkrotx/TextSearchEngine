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
