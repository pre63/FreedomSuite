package org.freedomsuite.calendar.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun EventFormFields(
    title: String,
    onTitleChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    startEpochMs: Long,
    onStartChange: (Long) -> Unit,
    endEpochMs: Long,
    onEndChange: (Long) -> Unit,
) {
    val context = LocalContext.current
    val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        label = { Text("Title") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    OutlinedTextField(
        value = location,
        onValueChange = onLocationChange,
        label = { Text("Location") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        singleLine = true,
    )
    OutlinedTextField(
        value = description,
        onValueChange = onDescriptionChange,
        label = { Text("Description") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        minLines = 4,
    )
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text("Start")
        OutlinedButton(
            onClick = {
                val cal = Calendar.getInstance().apply { timeInMillis = startEpochMs }
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        val updated = Calendar.getInstance().apply {
                            timeInMillis = startEpochMs
                            set(year, month, day)
                        }
                        onStartChange(updated.timeInMillis)
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH),
                ).show()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(formatter.format(Date(startEpochMs)))
        }
        OutlinedButton(
            onClick = {
                val cal = Calendar.getInstance().apply { timeInMillis = startEpochMs }
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val updated = Calendar.getInstance().apply {
                            timeInMillis = startEpochMs
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onStartChange(updated.timeInMillis)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true,
                ).show()
            },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        ) {
            Text("Change start time")
        }
    }
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text("End")
        OutlinedButton(
            onClick = {
                val cal = Calendar.getInstance().apply { timeInMillis = endEpochMs }
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        val updated = Calendar.getInstance().apply {
                            timeInMillis = endEpochMs
                            set(year, month, day)
                        }
                        onEndChange(updated.timeInMillis)
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH),
                ).show()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(formatter.format(Date(endEpochMs)))
        }
        OutlinedButton(
            onClick = {
                val cal = Calendar.getInstance().apply { timeInMillis = endEpochMs }
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val updated = Calendar.getInstance().apply {
                            timeInMillis = endEpochMs
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onEndChange(updated.timeInMillis)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true,
                ).show()
            },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        ) {
            Text("Change end time")
        }
    }
}
