package com.felix.greengriffin.util.extensions.list

fun <T> List<T?>.isEmptyOrOnlyNulls(): Boolean {
    return filterNotNull().isEmpty()
}