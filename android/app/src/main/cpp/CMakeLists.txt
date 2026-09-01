cmake_minimum_required(VERSION 3.22.1)
project("ocengine")

add_library(ocengine SHARED native-lib.cpp)

find_library(log-lib log)

target_link_libraries(ocengine ${log-lib})