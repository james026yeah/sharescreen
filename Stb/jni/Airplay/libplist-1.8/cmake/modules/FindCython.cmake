FIND_PROGRAM(CYTHON_EXECUTABLE cython)

INCLUDE(FindPackageHandleStandardArgs)
FIND_PACKAGE_HANDLE_STANDARD_ARGS(Cython DEFAULT_MSG CYTHON_EXECUTABLE)

MARK_AS_ADVANCED(CYTHON_EXECUTABLE)

IF(CYTHON_FOUND)
      SET(CYTHON_USE_FILE ${CMAKE_SOURCE_DIR}/cmake/modules/UseCython.cmake)
ENDIF(CYTHON_FOUND)
