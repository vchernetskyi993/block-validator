# Block Validator

Simple CLI app that validates **already validated** Bitcoin block by its version (height).

Personal study project. For steps used [this YouTube video](https://www.youtube.com/watch?v=qLI8Y0961zk).

```shell
# build binary
./gradlew clean installDist
# "validate" 660000 block; prints each step details 
./build/install/block-validator/bin/block-validator 660000
```
