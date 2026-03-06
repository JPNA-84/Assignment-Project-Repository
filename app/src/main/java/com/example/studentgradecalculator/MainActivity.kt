package com.example.studentgradecalculator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class MainActivity : AppCompatActivity() {

    private val courseViews = mutableListOf<CourseView>()
    private lateinit var coursesContainer: LinearLayout
    private lateinit var studentNameInput: TextInputEditText
    private lateinit var calculationModeSpinner: Spinner
    private lateinit var resultCard: MaterialCardView
    private lateinit var resultName: TextView
    private lateinit var resultAverage: TextView
    private lateinit var resultLetter: TextView
    private lateinit var resultStatus: TextView
    private lateinit var resultCoursesList: LinearLayout

    data class CourseView(
        val container: View,
        val nameInput: TextInputEditText,
        val gradeInput: TextInputEditText,
        val unitsInput: TextInputEditText,
        val unitsLayout: TextInputLayout
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        coursesContainer = findViewById(R.id.coursesContainer)
        studentNameInput = findViewById(R.id.studentNameInput)
        calculationModeSpinner = findViewById(R.id.calculationModeSpinner)
        resultCard = findViewById(R.id.resultCard)
        resultName = findViewById(R.id.resultName)
        resultAverage = findViewById(R.id.resultAverage)
        resultLetter = findViewById(R.id.resultLetter)
        resultStatus = findViewById(R.id.resultStatus)
        resultCoursesList = findViewById(R.id.resultCoursesList)

        setupSpinner()
        setupButtons()

        // Add 3 course fields by default
        repeat(3) { addCourseField() }
    }

    private fun setupSpinner() {
        val modes = listOf("Simple Average", "Weighted Average (with Units)")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        calculationModeSpinner.adapter = adapter

        calculationModeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val isWeighted = pos == 1
                courseViews.forEach { it.unitsLayout.visibility = if (isWeighted) View.VISIBLE else View.GONE }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupButtons() {
        findViewById<MaterialButton>(R.id.addCourseBtn).setOnClickListener { addCourseField() }
        findViewById<MaterialButton>(R.id.calculateBtn).setOnClickListener { calculateGrades() }
        findViewById<MaterialButton>(R.id.clearBtn).setOnClickListener { clearAll() }
    }

    private fun addCourseField() {
        val inflater = LayoutInflater.from(this)
        val courseView = inflater.inflate(R.layout.item_course, coursesContainer, false)

        val nameInput = courseView.findViewById<TextInputEditText>(R.id.courseNameInput)
        val gradeInput = courseView.findViewById<TextInputEditText>(R.id.courseGradeInput)
        val unitsInput = courseView.findViewById<TextInputEditText>(R.id.courseUnitsInput)
        val unitsLayout = courseView.findViewById<TextInputLayout>(R.id.courseUnitsLayout)
        val removeBtn = courseView.findViewById<ImageButton>(R.id.removeCourseBtn)

        // Set course number label
        val label = courseView.findViewById<TextView>(R.id.courseLabel)
        label.text = "Course ${courseViews.size + 1}"

        val isWeighted = calculationModeSpinner.selectedItemPosition == 1
        unitsLayout.visibility = if (isWeighted) View.VISIBLE else View.GONE

        val cv = CourseView(courseView, nameInput, gradeInput, unitsInput, unitsLayout)
        courseViews.add(cv)
        coursesContainer.addView(courseView)

        removeBtn.setOnClickListener {
            if (courseViews.size > 1) {
                courseViews.remove(cv)
                coursesContainer.removeView(courseView)
                updateCourseLabels()
            } else {
                Toast.makeText(this, "At least one course is required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateCourseLabels() {
        courseViews.forEachIndexed { index, cv ->
            cv.container.findViewById<TextView>(R.id.courseLabel).text = "Course ${index + 1}"
        }
    }

    private fun calculateGrades() {
        val studentName = studentNameInput.text.toString().trim()
        if (studentName.isEmpty()) {
            studentNameInput.error = "Please enter student name"
            return
        }

        val isWeighted = calculationModeSpinner.selectedItemPosition == 1
        val courses = mutableListOf<Triple<String, Double, Double>>() // name, grade, units

        for (cv in courseViews) {
            val name = cv.nameInput.text.toString().trim()
            val gradeStr = cv.gradeInput.text.toString().trim()

            if (name.isEmpty() || gradeStr.isEmpty()) continue

            val grade = gradeStr.toDoubleOrNull()
            if (grade == null || grade < 0 || grade > 100) {
                Toast.makeText(this, "Grade for '$name' must be between 0 and 100", Toast.LENGTH_SHORT).show()
                return
            }

            val units = if (isWeighted) {
                val u = cv.unitsInput.text.toString().trim().toDoubleOrNull()
                if (u == null || u <= 0) {
                    Toast.makeText(this, "Please enter valid units for '$name'", Toast.LENGTH_SHORT).show()
                    return
                }
                u
            } else 1.0

            courses.add(Triple(name, grade, units))
        }

        if (courses.isEmpty()) {
            Toast.makeText(this, "Please fill in at least one course", Toast.LENGTH_SHORT).show()
            return
        }

        val average = if (isWeighted) {
            val totalWeighted = courses.sumOf { it.second * it.third }
            val totalUnits = courses.sumOf { it.third }
            totalWeighted / totalUnits
        } else {
            courses.map { it.second }.average()
        }

        showResult(studentName, average, courses, isWeighted)
    }

    private fun showResult(name: String, average: Double, courses: List<Triple<String, Double, Double>>, isWeighted: Boolean) {
        val letterGrade = getLetterGrade(average)
        val passed = average >= 60.0

        resultName.text = name
        resultAverage.text = String.format("%.2f%%", average)
        resultLetter.text = letterGrade

        resultStatus.text = if (passed) "✓ PASSED" else "✗ FAILED"
        resultStatus.setTextColor(
            ContextCompat.getColor(this, if (passed) R.color.pass_green else R.color.fail_red)
        )

        val gradeColor = getGradeColor(average)
        resultLetter.setTextColor(ContextCompat.getColor(this, gradeColor))
        resultAverage.setTextColor(ContextCompat.getColor(this, gradeColor))

        resultCoursesList.removeAllViews()
        courses.forEach { (courseName, grade, units) ->
            val row = TextView(this).apply {
                val unitStr = if (isWeighted) " (${units.toInt()} units)" else ""
                text = "• $courseName$unitStr: ${String.format("%.1f", grade)}% — ${getLetterGrade(grade)}"
                textSize = 14f
                setPadding(0, 6, 0, 6)
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
            resultCoursesList.addView(row)
        }

        resultCard.visibility = View.VISIBLE
        resultCard.animate().alpha(1f).translationY(0f).setDuration(400).start()
    }

    private fun getLetterGrade(grade: Double): String = when {
        grade >= 97 -> "A+"
        grade >= 93 -> "A"
        grade >= 90 -> "A-"
        grade >= 87 -> "B+"
        grade >= 83 -> "B"
        grade >= 80 -> "B-"
        grade >= 77 -> "C+"
        grade >= 73 -> "C"
        grade >= 70 -> "C-"
        grade >= 67 -> "D+"
        grade >= 63 -> "D"
        grade >= 60 -> "D-"
        else -> "F"
    }

    private fun getGradeColor(grade: Double): Int = when {
        grade >= 90 -> R.color.grade_a
        grade >= 80 -> R.color.grade_b
        grade >= 70 -> R.color.grade_c
        grade >= 60 -> R.color.grade_d
        else -> R.color.fail_red
    }

    private fun clearAll() {
        studentNameInput.text?.clear()
        courseViews.forEach {
            it.nameInput.text?.clear()
            it.gradeInput.text?.clear()
            it.unitsInput.text?.clear()
        }
        resultCard.visibility = View.GONE
    }
}
