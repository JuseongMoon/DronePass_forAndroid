package com.ScienceFiction.DronePassAndroid.core.util

enum class DroneCategory(val label: String, val minWeight: String, val maxWeight: String) {
    TOY("250g 이하", "0g", "250g"),
    CLASS4("250g~2kg", "250g", "2kg"),
    CLASS3("2kg~7kg", "2kg", "7kg"),
    CLASS2("7kg~25kg", "7kg", "25kg")
}
