package com.felix.greengriffin.extensions.list

import kotlin.collections.indexOfFirst


fun <T> List<T?>.replaceByNull(predicate: (T?) -> Boolean): List<T?> = toMutableList().apply {
    val indexOnBoard = indexOfFirst(predicate)
    if (indexOnBoard > -1) this[indexOnBoard] = null
}

fun <T> List<T>.add(element: T, ifNone: (T) -> Boolean): List<T> = toMutableList().apply {
    if (this.none { ifNone(it) }) {
        add(element)
    }
}

fun <T> List<T?>.replace(index: Int, element: T?): List<T?> = toMutableList().apply {
    this[index] = element
}