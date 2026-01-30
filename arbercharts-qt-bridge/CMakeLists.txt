cmake_minimum_required(VERSION 3.20)
project(arbercharts_qt_bridge LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_AUTOMOC ON)

find_package(Qt6 6.8 COMPONENTS Quick REQUIRED)

if(NOT DEFINED ENV{ARBER_NATIVE_PATH})
    message(FATAL_ERROR "ARBER_NATIVE_PATH not set. Point it to dist/native (contains arbercharts-core.h and dylib).")
endif()

set(ARBER_NATIVE_PATH "$ENV{ARBER_NATIVE_PATH}")

add_library(ArberQtBridge SHARED
    src/ArberQuickItem.cpp
)

target_include_directories(ArberQtBridge PRIVATE
    ${ARBER_NATIVE_PATH}
    src
)

target_link_libraries(ArberQtBridge PRIVATE
    Qt6::Quick
    ${ARBER_NATIVE_PATH}/arbercharts-core.dylib
)

target_compile_definitions(ArberQtBridge PRIVATE
    ARBER_NATIVE_PATH="${ARBER_NATIVE_PATH}"
)
