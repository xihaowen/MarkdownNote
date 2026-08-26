package com.mdnote.markdownnote.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.widget.EditText

/**
 * 原生编辑控件：长按后以首次按下的精确字符偏移作为选区锚点。
 */
internal class PreciseSelectionEditText(context: Context) : EditText(context) {

    private var exactSelectionInProgress = false
    private var exactSelectionAnchor = 0

    // 仅在拖动结束时回传精准选区和工具栏位置，避免长文本拖动期间重复复制大字符串并触发重组。
    var onExactSelectionCompleted: ((ExactDragToolbarRequest?) -> Unit)? = null

    // 软键盘在行首退格时交给编辑器合并行，保留原有的分行行为。
    var onBackspaceAtStart: ((String) -> Unit)? = null

    private val longPressDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(event: MotionEvent): Boolean = true

            override fun onLongPress(event: MotionEvent) {
                exactSelectionInProgress = true
                // 长按选择期间禁止 LazyColumn 截获 MOVE，保持前后连续拖动。
                parent?.requestDisallowInterceptTouchEvent(true)
                exactSelectionAnchor = offsetAt(event)
                applyExactSelection(event)
            }
        }
    )

    init {
        // 禁用 TextView 默认的词级长按选择，避免它抢占精确字符锚点。
        isLongClickable = false
    }

    override fun performLongClick(): Boolean {
        // 即使设备实现绕过 isLongClickable，也不执行默认词级扩选。
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    // 普通点击完整交给 EditText；本类消费的分支是长按拖动选区，不属于 click 手势。
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            // 开始新的文本操作时关闭上一次的 Compose 工具栏。
            onExactSelectionCompleted?.invoke(null)
        }
        longPressDetector.onTouchEvent(event)
        if (!exactSelectionInProgress) {
            return super.onTouchEvent(event)
        }

        return when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                applyExactSelection(event)
                true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                exactSelectionInProgress = false
                // 选择完成或取消后恢复父容器的正常滚动能力。
                parent?.requestDisallowInterceptTouchEvent(false)
                if (event.actionMasked == MotionEvent.ACTION_UP) {
                    val finalSelection = exactDragSelection(selectionStart, selectionEnd, text.length)
                    // 松手时只请求既有 Compose 工具栏，避免原生 ActionMode 覆盖 AI 工具栏。
                    onExactSelectionCompleted?.invoke(
                        completedDragToolbarRequest(finalSelection, selectionToolbarBounds(finalSelection))
                    )
                }
                true
            }

            else -> true
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DEL && consumeBackspaceAtStart()) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
    // EditText 获焦并调用 showSoftInput 时，InputMethodManager 经
    // onCreateInputConnection -> hasEditorInFocusSearchDirection -> focusSearch
    // 向上冒泡到 AndroidComposeView，进而让 LazyColumn 越界搜索并实例化文档全部行
    // （含图片），矩阵运算卡死主线程导致 ANR。此 EditText 是行内唯一输入控件，
    // 跨行焦点由 Compose editState 管理，不依赖 Android 焦点遍历，因此直接返回自身。
    override fun focusSearch(direction: Int): View {
        return this
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val inputConnection = super.onCreateInputConnection(outAttrs) ?: return null
        return object : InputConnectionWrapper(inputConnection, false) {
            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                if (beforeLength > 0 && consumeBackspaceAtStart()) {
                    return true
                }
                return super.deleteSurroundingText(beforeLength, afterLength)
            }

            override fun sendKeyEvent(event: KeyEvent): Boolean {
                if (event.action == KeyEvent.ACTION_DOWN &&
                    event.keyCode == KeyEvent.KEYCODE_DEL &&
                    consumeBackspaceAtStart()
                ) {
                    return true
                }
                return super.sendKeyEvent(event)
            }
        }
    }

    private fun applyExactSelection(event: MotionEvent) {
        val selection = exactDragSelection(exactSelectionAnchor, offsetAt(event), text.length)
        setSelection(selection.anchor, selection.edge)
    }

    private fun offsetAt(event: MotionEvent): Int =
        getOffsetForPosition(event.x, event.y).coerceIn(0, text.length)

    private fun consumeBackspaceAtStart(): Boolean {
        if (selectionStart != 0 || selectionEnd != 0) return false
        onBackspaceAtStart?.invoke(text.toString())
        return true
    }

    private fun selectionToolbarBounds(selection: ExactDragSelection): ExactSelectionToolbarBounds {
        val location = IntArray(2)
        getLocationOnScreen(location)
        val textLayout = layout
        if (textLayout == null) {
            return ExactSelectionToolbarBounds(
                left = location[0].toFloat(),
                top = location[1].toFloat(),
                right = (location[0] + width).toFloat(),
                bottom = (location[1] + height).toFloat()
            )
        }

        val start = minOf(selection.anchor, selection.edge)
        val end = maxOf(selection.anchor, selection.edge)
        val startLine = textLayout.getLineForOffset(start)
        val endLine = textLayout.getLineForOffset(end)
        val contentLeft = location[0] + totalPaddingLeft - scrollX
        val contentTop = location[1] + totalPaddingTop - scrollY
        // 使用原生文本布局的屏幕坐标，让 Compose 工具栏对准精准选区。
        return ExactSelectionToolbarBounds(
            left = contentLeft + if (startLine == endLine) {
                minOf(textLayout.getPrimaryHorizontal(start), textLayout.getPrimaryHorizontal(end))
            } else {
                textLayout.getLineLeft(startLine)
            },
            top = (contentTop + textLayout.getLineTop(startLine)).toFloat(),
            right = contentLeft + if (startLine == endLine) {
                maxOf(textLayout.getPrimaryHorizontal(start), textLayout.getPrimaryHorizontal(end))
            } else {
                textLayout.getLineRight(endLine)
            },
            bottom = (contentTop + textLayout.getLineBottom(endLine)).toFloat()
        )
    }
}

/** 原生 Selection.setSelection 所需的锚点和边界，方向不能被归一化。 */
internal data class ExactDragSelection(
    val anchor: Int,
    val edge: Int
)

/** 精准选区在屏幕全局坐标中的工具栏锚点范围。 */
internal data class ExactSelectionToolbarBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

/** 精准拖动完成后交给 Compose 工具栏的选区和锚点。 */
internal data class ExactDragToolbarRequest(
    val selection: ExactDragSelection,
    val bounds: ExactSelectionToolbarBounds
)

// 坐标转字符偏移后始终保留按下顺序，并防止文本更新后的旧坐标越界。
internal fun exactDragSelection(anchor: Int, current: Int, textLength: Int): ExactDragSelection {
    val safeLength = textLength.coerceAtLeast(0)
    return ExactDragSelection(
        anchor = anchor.coerceIn(0, safeLength),
        edge = current.coerceIn(0, safeLength)
    )
}

// 空选区不显示工具栏，非空选区保留拖动方向并交给 Compose 的既有 AI 工具栏。
internal fun completedDragToolbarRequest(
    selection: ExactDragSelection,
    bounds: ExactSelectionToolbarBounds
): ExactDragToolbarRequest? =
    if (selection.anchor == selection.edge) null else ExactDragToolbarRequest(selection, bounds)

// 最终选区文本只在手势完成后生成，正反向选择都按实际范围截取。
internal fun completedDragSelectionText(text: String, selection: ExactDragSelection): String {
    val safeSelection = exactDragSelection(selection.anchor, selection.edge, text.length)
    return text.substring(
        startIndex = minOf(safeSelection.anchor, safeSelection.edge),
        endIndex = maxOf(safeSelection.anchor, safeSelection.edge)
    )
}
