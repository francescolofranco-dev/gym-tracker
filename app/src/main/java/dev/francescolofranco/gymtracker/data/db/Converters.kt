package dev.francescolofranco.gymtracker.data.db

import androidx.room.TypeConverter
import dev.francescolofranco.gymtracker.domain.ExerciseSide
import dev.francescolofranco.gymtracker.domain.Muscle
import java.time.Instant

class Converters {

    @TypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun musclesToString(set: Set<Muscle>?): String =
        set.orEmpty().joinToString(",") { it.name }

    @TypeConverter
    fun stringToMuscles(s: String?): Set<Muscle> =
        if (s.isNullOrBlank()) emptySet()
        else s.split(",").mapNotNull { token ->
            runCatching { Muscle.valueOf(token) }.getOrNull()
        }.toSet()

    @TypeConverter
    fun exerciseSideToString(side: ExerciseSide?): String = (side ?: ExerciseSide.BOTH).name

    @TypeConverter
    fun stringToExerciseSide(value: String?): ExerciseSide =
        runCatching { ExerciseSide.valueOf(value.orEmpty()) }.getOrDefault(ExerciseSide.BOTH)
}
