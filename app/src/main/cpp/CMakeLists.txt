cmake_minimum_required(VERSION 3.22.1)
project(hiragana_jni LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(${CMAKE_CURRENT_SOURCE_DIR}/core.cmake)

add_library(hiragana_jni SHARED jni_bridge.cpp)
target_link_libraries(hiragana_jni PRIVATE hiragana_core log)
