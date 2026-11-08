package com.example.ui.screens

import com.example.R
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalendarAccount
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.PrimaryExpressive
import com.example.ui.theme.SecondaryExpressive

@Composable
fun CalendarFilterScreen(
    calendars: List<CalendarAccount>,
    selectedCalendarIds: Set<Long>,
    hasCalendarPermission: Boolean,
    onRequestPermission: () -> Unit,
    onToggleCalendar: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .padding(bottom = 16.dp)
            .testTag("calendar_filter_screen")
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.calendars_synced, calendars.size), color = PrimaryExpressive, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(
                text = stringResource(R.string.calendars),
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.calendars_intro),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = PlusJakartaSans,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )
        }

        // Permission Card (Only show if permission is missing)
        if (!hasCalendarPermission) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("permission_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(PrimaryExpressive.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = PrimaryExpressive,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.calendar_access),
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.calendar_access_desc),
                                fontFamily = PlusJakartaSans,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryExpressive),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("grant_permission_button")
                        ) {
                            Text(stringResource(R.string.allow), fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // Calendar selection controls
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.selected_calendars, selectedCalendarIds.size), fontSize = 11.sp)
                    androidx.compose.material3.FilledTonalButton(
                        onClick = onSelectAll,
                        modifier = Modifier.weight(1f).testTag("select_all_calendars_button"),
                        shape = RoundedCornerShape(50),
                        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = PrimaryExpressive),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text(stringResource(R.string.select_all), fontFamily = PlusJakartaSans, maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                    androidx.compose.material3.FilledTonalButton(
                        onClick = onDeselectAll,
                        modifier = Modifier.weight(1f).testTag("clear_calendars_button"),
                        shape = RoundedCornerShape(50),
                        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = PrimaryExpressive),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text(stringResource(R.string.clear_selection), fontFamily = PlusJakartaSans, maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // List of calendars
        itemsIndexed(calendars, key = { _, cal -> cal.id }) { index, cal ->
            val isChecked = selectedCalendarIds.contains(cal.id)
            val calColor = Color(if (cal.color != 0) cal.color else 0xFFE040FB.toInt())

            Card(
                modifier = Modifier
                    .fillMaxWidth()

                    .toggleable(value = isChecked, role = androidx.compose.ui.semantics.Role.Checkbox, onValueChange = { onToggleCalendar(cal.id) })
                    .testTag("calendar_item_${cal.id}"),
                shape = RoundedCornerShape(topStart = if (index == 0) 22.dp else 0.dp, topEnd = if (index == 0) 22.dp else 0.dp, bottomStart = if (index == calendars.lastIndex) 22.dp else 0.dp, bottomEnd = if (index == calendars.lastIndex) 22.dp else 0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(calColor)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cal.displayName,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = cal.accountName,
                            fontFamily = PlusJakartaSans,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier.padding(start = 10.dp).size(22.dp).clip(CircleShape)
                            .background(if (isChecked) calColor else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked) Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
