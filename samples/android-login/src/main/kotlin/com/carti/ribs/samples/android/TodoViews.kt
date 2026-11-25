package com.carti.ribs.samples.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** 목록 한 줄에 그릴 값. 공유 모듈의 `Todo`를 순수 뷰에 들이지 않으려고 둔다(ADR-0011). */
data class TodoRow(val id: Long, val title: String, val isDone: Boolean)

/** 할 일 목록의 순수 뷰. */
@Composable
fun TodoListView(rows: List<TodoRow>, onTapRow: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            TextButton(onClick = { onTapRow(row.id) }, modifier = Modifier.testTag(Tags.todoRow(row.id))) {
                Text((if (row.isDone) "✓ " else "") + row.title)
            }
        }
    }
}

/** 할 일 상세의 순수 뷰. */
@Composable
fun TodoDetailView(title: String, isDone: Boolean, onBack: () -> Unit, onToggleDone: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onBack, modifier = Modifier.testTag(Tags.BACK_BUTTON)) { Text("← 목록") }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag(Tags.DETAIL_TITLE))
        Button(onClick = onToggleDone, modifier = Modifier.testTag(Tags.TOGGLE_DONE_BUTTON)) {
            Text(if (isDone) "완료 취소" else "완료로 표시")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TodoListViewPreview() = TodoListView(
    rows = listOf(TodoRow(1, "장보기", isDone = false), TodoRow(2, "운동", isDone = true)),
    onTapRow = {},
)

@Preview(showBackground = true)
@Composable
private fun TodoDetailViewPreview() = TodoDetailView(title = "장보기", isDone = false, onBack = {}, onToggleDone = {})
