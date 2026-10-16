# Host (Windows/MSVC) tests of the shared conversion engine, from Hiragana IME
# for Windows tests/. Run with tools/host-test.ps1.
cmake_minimum_required(VERSION 3.20)
project(HiraganaCoreTests LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")

set(ROOT_DIR ${CMAKE_CURRENT_SOURCE_DIR}/..)
set(UPSTREAM_DIR ${ROOT_DIR}/ibus-hiragana)
include(${ROOT_DIR}/app/src/main/cpp/core.cmake)

enable_testing()
add_executable(core_tests core_tests.cpp EngineTests.cpp AndroidEngineTests.cpp test_main.cpp)
target_link_libraries(core_tests PRIVATE hiragana_core)
target_compile_definitions(core_tests PRIVATE UPSTREAM_DIR="${UPSTREAM_DIR}")
add_test(NAME core_tests COMMAND core_tests)

add_executable(golden_test golden_test.cpp)
target_link_libraries(golden_test PRIVATE hiragana_core)
add_executable(engine_replay engine_replay.cpp)
target_link_libraries(engine_replay PRIVATE hiragana_core)
add_executable(assisted_golden_test assisted_golden_test.cpp)
target_link_libraries(assisted_golden_test PRIVATE hiragana_core)

find_package(Python3 COMPONENTS Interpreter)
if(Python3_FOUND)
    set(GOLDEN_DIR ${CMAKE_CURRENT_BINARY_DIR}/golden)
    set(GEN ${CMAKE_COMMAND} -E env PYTHONUTF8=1 ${Python3_EXECUTABLE})

    add_test(NAME golden_generate COMMAND ${GEN} ${CMAKE_CURRENT_SOURCE_DIR}/golden/gen_golden.py ${GOLDEN_DIR})
    set_tests_properties(golden_generate PROPERTIES FIXTURES_SETUP golden)
    add_test(NAME engine_golden_generate
        COMMAND ${GEN} ${CMAKE_CURRENT_SOURCE_DIR}/golden/gen_engine_golden.py ${GOLDEN_DIR}/engine.txt 40)
    set_tests_properties(engine_golden_generate PROPERTIES FIXTURES_SETUP engine_golden)
    add_test(NAME engine_golden COMMAND engine_replay ${UPSTREAM_DIR} ${GOLDEN_DIR}/engine.txt)
    set_tests_properties(engine_golden PROPERTIES FIXTURES_REQUIRED engine_golden)
    add_test(NAME assisted_golden_generate COMMAND ${GEN} ${CMAKE_CURRENT_SOURCE_DIR}/golden/gen_llm_golden.py ${GOLDEN_DIR})
    set_tests_properties(assisted_golden_generate PROPERTIES FIXTURES_SETUP assisted_golden)

    foreach(system restrained.9 restrained.1)
        foreach(kind romazi kana)
            add_test(NAME golden_${system}_${kind}
                COMMAND golden_test ${UPSTREAM_DIR}/dic ${system}.dic ${kind} ${GOLDEN_DIR}/lookup.${system}.${kind}.tsv)
            set_tests_properties(golden_${system}_${kind} PROPERTIES FIXTURES_REQUIRED golden)
            add_test(NAME assisted_golden_${system}_${kind}
                COMMAND assisted_golden_test ${UPSTREAM_DIR}/dic ${system}.dic ${kind} ${GOLDEN_DIR}/assisted.${system}.${kind}.tsv)
            set_tests_properties(assisted_golden_${system}_${kind} PROPERTIES FIXTURES_REQUIRED assisted_golden)
        endforeach()
    endforeach()
endif()
