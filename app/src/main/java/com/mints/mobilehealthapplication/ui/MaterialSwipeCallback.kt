package com.mints.mobilehealthapplication.ui

import android.content.Context
import android.graphics.Canvas
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.mints.mobilehealthapplication.R

class MaterialSwipeCallback(
    private val context: Context,
    private val swipeLeftAction: SwipeAction,
    private val swipeRightAction: SwipeAction,
    private val onSwipeLeft: (Int, () -> Unit) -> Unit = { _, _ -> },
    private val onSwipeRight: (Int, () -> Unit) -> Unit = { _, _ -> }
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    data class SwipeAction(
        @DrawableRes val iconRes: Int,
        @ColorRes val backgroundColorRes: Int,
        val label: String
    )

    private var isSwipeInProgress = false
    private val iconSize = context.resources.getDimensionPixelSize(R.dimen.material_touch_target_size)
    private val background = MaterialShapeDrawable().apply {
        shapeAppearanceModel = ShapeAppearanceModel.Builder()
            .setAllCornerSizes(context.resources.getDimension(R.dimen.material_corner_radius))
            .build()
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return !isSwipeInProgress
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        if (isSwipeInProgress) {
            return
        }
        isSwipeInProgress = true
        val position = viewHolder.adapterPosition
        when (direction) {
            ItemTouchHelper.LEFT -> handleSwipeAction(swipeLeftAction, position)
            ItemTouchHelper.RIGHT -> handleSwipeAction(swipeRightAction, position)
        }
    }

    private fun handleSwipeAction(action: SwipeAction, position: Int) {
        when (action) {
            swipeLeftAction -> onSwipeLeft(position) { isSwipeInProgress = false }
            swipeRightAction -> onSwipeRight(position) { isSwipeInProgress = false }
        }
    }

    override fun onChildDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val elevation = if (isCurrentlyActive) {
            context.resources.getDimension(R.dimen.m3_elevation_level2)
        } else {
            0f
        }

        when {
            dX > 0 -> drawSwipeRight(canvas, itemView, dX, swipeRightAction, elevation)
            dX < 0 -> drawSwipeLeft(canvas, itemView, dX, swipeLeftAction, elevation)
        }

        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun drawSwipeRight(
        canvas: Canvas,
        itemView: View,
        dX: Float,
        action: SwipeAction,
        elevation: Float
    ) {
        background.apply {
            setTint(ContextCompat.getColor(context, action.backgroundColorRes))
            this.elevation = elevation
            setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
            draw(canvas)
        }

        ContextCompat.getDrawable(context, action.iconRes)?.let { icon ->
            val iconMargin = (itemView.height - iconSize) / 2
            val iconTop = itemView.top + iconMargin
            val iconLeft = itemView.left + iconMargin
            icon.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
            icon.draw(canvas)
        }
    }

    private fun drawSwipeLeft(
        canvas: Canvas,
        itemView: View,
        dX: Float,
        action: SwipeAction,
        elevation: Float
    ) {
        background.apply {
            setTint(ContextCompat.getColor(context, action.backgroundColorRes))
            this.elevation = elevation
            setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
            draw(canvas)
        }

        ContextCompat.getDrawable(context, action.iconRes)?.let { icon ->
            val iconMargin = (itemView.height - iconSize) / 2
            val iconTop = itemView.top + iconMargin
            val iconRight = itemView.right - iconMargin
            icon.setBounds(iconRight - iconSize, iconTop, iconRight, iconTop + iconSize)
            icon.draw(canvas)
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        viewHolder.itemView.translationX = 0f
    }
}