package dev.francescolofranco.gymtracker.data.db.projections

import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.TemplateEntity

/** UI-side projection that joins a template's metadata with its ordered exercises. */
data class TemplateWithExercises(
    val template: TemplateEntity,
    val exercises: List<ExerciseEntity>,
)
