package com.senghol.knotx.ui.components

// TODO: adjust the package above to wherever your other story UI files live

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.senghol.knotx.R
import com.senghol.knotx.data.database.StoryEntity
import com.senghol.knotx.utils.data.UserStoryGroup

/**
 * One row in the Stories tab list — an avatar (gradient ring = has unseen stories,
 * plain gray ring = all seen) plus a title/subtitle. Tapping it is wired up by
 * the caller (HomeScreen) since what "tap" should do depends on whether this is
 * the "Your Story" row or a contact's row.
 */
@Composable
fun StoryRow(
    title: String,
    subtitle: String,
    hasUnseenRing: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .border(
                    width = 2.dp,
                    brush = if (hasUnseenRing) {
                        Brush.sweepGradient(listOf(Color(0xFFFF512F), Color(0xFFDD2476), Color(0xFFFF512F)))
                    } else {
                        Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.3f)))
                    },
                    shape = CircleShape
                )
                .padding(3.dp)
                .clip(CircleShape)
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun StoriesEmptyMessage() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.no_stories_yet),
            color = Color.Gray
        )
    }
}
/** Simple relative-time label – "Just now", "5m ago", "2h ago", "1d ago". */
fun timeAgo(epochMs: Long): String {
    val diffMinutes = (System.currentTimeMillis() - epochMs) / 60_000
    return when {
        diffMinutes < 1 -> "Just now"
        diffMinutes < 60 -> "${diffMinutes}m ago"
        diffMinutes < 1_440 -> "${diffMinutes / 60}h ago"
        else -> "${diffMinutes / 1_440}d ago"
    }
}


@Composable
fun StoriesTabContent(
    myStories: List<StoryEntity>,
    feedGroups: List<UserStoryGroup>,
    onAddStoryClick: () -> Unit,
    onMyStoryClick: () -> Unit,
    onUserGroupClick: (UserStoryGroup) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.my_status_title),
            style = MaterialTheme.typography.labelLarge,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val hasMyStories = myStories.isNotEmpty()
        val mySubtitle = if (hasMyStories) {
            stringResource(R.string.tap_to_view_time, timeAgo(myStories.last().createdAt))
        } else {
            stringResource(R.string.tap_to_add_status_update)
        }

        StoryRow(
            title = stringResource(R.string.my_story_title),
            subtitle = mySubtitle,
            hasUnseenRing = hasMyStories,
            onClick = {
                if (hasMyStories) onMyStoryClick() else onAddStoryClick()
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = Color.Gray.copy(alpha = 0.2f)
        )

        Text(
            text = stringResource(R.string.recent_updates_title),
            style = MaterialTheme.typography.labelLarge,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (feedGroups.isEmpty()) {
            StoriesEmptyMessage()
        } else {
            feedGroups.forEach { group ->
                val latestStoryTime = group.stories.maxOfOrNull { it.createdAt } ?: 0L

                StoryRow(
                    title = group.userName,
                    subtitle = timeAgo(latestStoryTime),
                    hasUnseenRing = group.hasUnseenStories,
                    onClick = { onUserGroupClick(group) }
                )
            }
        }
    }
}