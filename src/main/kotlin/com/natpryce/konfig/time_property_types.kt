package com.natpryce.konfig

import java.time.*
import java.time.format.DateTimeParseException


val durationType = propertyType<Duration, DateTimeParseException>(Duration::parse)
val localTimeType = propertyType<LocalTime, DateTimeParseException>(LocalTime::parse)
val localDateType = propertyType<LocalDate, DateTimeParseException>(LocalDate::parse)
val localDateTimeType = propertyType<LocalDateTime, DateTimeParseException>(LocalDateTime::parse)
val instantType = propertyType<Instant, DateTimeParseException>(Instant::parse)
