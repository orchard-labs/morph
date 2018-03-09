#!/bin/bash

set -x

# install apache ant
export ANT_HOME=${HOME}/ant
export PATH=${HOME}/ant/bin:${PATH}

# run the tests
lein test2junit :all
TEST_EXIT_CODE=$?

# create docs
lein codox

# collect test results
if [ -d ${HOME}/test_output ]; then
    find target/test2junit -type f -name "*.xml" -exec cp {} ${HOME}/test_output \;
fi

if [ -d ${HOME}/artifacts ]; then
    cp -r target/test2junit/html ${HOME}/artifacts
    cp -r target/default/doc ${HOME}/codox
fi

exit $TEST_EXIT_CODE
