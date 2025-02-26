package com.example.myFitHololenzApp

enum class ActivityLevel {
    BriskWalking,
    Jogging,
    Running
}

data class GameLevel(
    val activity: ActivityLevel,
    val stepsRequired: Int
)

class PlayerGoals(totalStepGoal: Int) {

    val totalStepGoal: Int = totalStepGoal
    val stepGoals: Map<ActivityLevel, Int> = mapOf(
        ActivityLevel.BriskWalking to (totalStepGoal * 0.33).toInt(),
        ActivityLevel.Jogging to (totalStepGoal * 0.33).toInt(),
        ActivityLevel.Running to (totalStepGoal * 0.34).toInt()
    )

    fun getStepsRequiredForLevel(level: ActivityLevel): Int {
        return stepGoals[level] ?: 0
    }

    fun getStepsRequiredForPreviousLevels(level: ActivityLevel): Int {
        var steps = 0
        for ((key, value) in stepGoals) {
            if (key.ordinal < level.ordinal) {
                steps += value
            }
        }
        return steps
    }

    fun getCurrentLevel(stepsTaken: Int): ActivityLevel {
        var accumulatedSteps = 0
        for ((level, stepsRequired) in stepGoals) {
            accumulatedSteps += stepsRequired
            if (stepsTaken < accumulatedSteps) {
                return level
            }
        }
        return ActivityLevel.Running // Return the last level if all are completed
    }

    fun isLevelCompleted(stepsTaken: Int, level: ActivityLevel): Boolean {
        return stepsTaken >= getStepsRequiredForPreviousLevels(level) + getStepsRequiredForLevel(level)
    }
}

