#!/bin/bash

set -x

# install apache ant
export ANT_HOME=/ant
export PATH=/ant/bin:${PATH}

# run the tests
lein test2junit :all
TEST_EXIT_CODE=$?

# create docs
lein codox

# collect test results
if [ -d /test_output ]; then
    find target/test2junit -type f -name "*.xml" -exec cp {} /test_output \;
fi

if [ -d /artifacts ]; then
    cp -r target/test2junit/html /artifacts
    cp -r target/default/doc /codox
fi

exit $TEST_EXIT_CODE
