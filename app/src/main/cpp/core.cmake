# The conversion engine shared with Hiragana IME for Windows (see CORE_ORIGIN.md).
# Included by app/src/main/cpp/CMakeLists.txt (Android) and core-tests/CMakeLists.txt (host).
# OnnxBert.cpp is not copied: the LLM-assisted conversion is not part of the Android port.

set(HIRAGANA_CORE_DIR ${CMAKE_CURRENT_LIST_DIR}/core)

add_library(hiragana_core STATIC
    ${HIRAGANA_CORE_DIR}/Text.cpp
    ${HIRAGANA_CORE_DIR}/KanaTables.cpp
    ${HIRAGANA_CORE_DIR}/Log.cpp
    ${HIRAGANA_CORE_DIR}/Dictionary.cpp
    ${HIRAGANA_CORE_DIR}/Json.cpp
    ${HIRAGANA_CORE_DIR}/Keysyms.cpp
    ${HIRAGANA_CORE_DIR}/KeyboardController.cpp
    ${HIRAGANA_CORE_DIR}/LookupTable.cpp
    ${HIRAGANA_CORE_DIR}/Engine.cpp
    ${HIRAGANA_CORE_DIR}/Settings.cpp
)
target_include_directories(hiragana_core PUBLIC ${HIRAGANA_CORE_DIR})
target_compile_features(hiragana_core PUBLIC cxx_std_17)
if(MSVC)
    target_compile_options(hiragana_core PUBLIC /utf-8 /EHsc /permissive-)
    target_compile_definitions(hiragana_core PUBLIC UNICODE _UNICODE NOMINMAX WIN32_LEAN_AND_MEAN)
endif()
