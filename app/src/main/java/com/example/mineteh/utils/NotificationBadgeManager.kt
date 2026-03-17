package com.example.mineteh.utils

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.example.mineteh.R
import com.example.mineteh.viewmodel.NotificationsViewModel

object NotificationBadgeManager {
    
    fun setupBadge(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        viewModel: NotificationsViewModel,
        badgeView: TextView?
    ) {
        badgeView?.let { badge ->
            viewModel.unreadCount.observe(lifecycleOwner, Observer { count ->
                updateBadge(badge, count)
            })
        }
    }
    
    private fun updateBadge(badgeView: TextView, count: Int) {
        if (count > 0) {
            badgeView.visibility = View.VISIBLE
            badgeView.text = if (count > 99) "99+" else count.toString()
        } else {
            badgeView.visibility = View.GONE
        }
    }
    
    fun updateBadgeDirectly(badgeView: TextView?, count: Int) {
        badgeView?.let { badge ->
            updateBadge(badge, count)
        }
    }
}