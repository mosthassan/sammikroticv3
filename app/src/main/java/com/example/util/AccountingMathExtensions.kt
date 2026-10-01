package com.example.util

import java.math.BigDecimal

operator fun BigDecimal.compareTo(other: Int): Int = this.compareTo(BigDecimal.valueOf(other.toLong()))
operator fun BigDecimal.compareTo(other: Long): Int = this.compareTo(BigDecimal.valueOf(other))
operator fun BigDecimal.compareTo(other: Double): Int = this.compareTo(BigDecimal.valueOf(other))

operator fun Double.plus(other: BigDecimal): Double = this + other.toDouble()
operator fun Double.minus(other: BigDecimal): Double = this - other.toDouble()
operator fun BigDecimal.plus(other: Double): BigDecimal = this.add(BigDecimal.valueOf(other))
operator fun BigDecimal.minus(other: Double): BigDecimal = this.subtract(BigDecimal.valueOf(other))

operator fun BigDecimal.times(other: Double): BigDecimal = this.multiply(BigDecimal.valueOf(other))
operator fun BigDecimal.times(other: Int): BigDecimal = this.multiply(BigDecimal.valueOf(other.toLong()))
