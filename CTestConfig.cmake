## This file should be placed in the root directory of your project.
## Then modify the CMakeLists.txt file in the root directory of your
## project to incorporate the testing dashboard.
## # The following are required to uses Dart and the Cdash dashboard
##   ENABLE_TESTING()
##   INCLUDE(CTest)
set (CTEST_PROJECT_NAME "HDFView")
set (CTEST_NIGHTLY_START_TIME "18:00:00 CST")

set (CTEST_DROP_METHOD "https")
if (CDASH_LOCAL)
  set (CTEST_DROP_SITE "10.10.10.82")
else ()
  set (CTEST_DROP_SITE "cdash.hdfgroup.org")
endif ()
set (CTEST_DROP_LOCATION "/submit.php?project=HDFJava")
set (CTEST_DROP_SITE_CDASH TRUE)

set (UPDATE_TYPE svn)

set (CTEST_TEST_TIMEOUT 3600 CACHE STRING
    "Maximum time allowed before CTest will kill the test.")
set (DART_TESTING_TIMEOUT 3600 CACHE STRING
    "Maximum time allowed before CTest will kill the test.")

SET(CTEST_SUBMIT_RETRY_DELAY 20 CACHE STRING
    "How long to wait between timed-out CTest submissions.")
