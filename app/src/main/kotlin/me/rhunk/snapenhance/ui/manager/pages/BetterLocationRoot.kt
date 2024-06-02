package me.rhunk.snapenhance.ui.manager.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.bridge.location.FriendLocation
import me.rhunk.snapenhance.bridge.location.LocationCoordinates
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableStateList
import me.rhunk.snapenhance.common.ui.rememberAsyncUpdateDispatcher
import me.rhunk.snapenhance.common.util.snap.BitmojiSelfie
import me.rhunk.snapenhance.storage.addOrUpdateLocationCoordinate
import me.rhunk.snapenhance.storage.getLocationCoordinates
import me.rhunk.snapenhance.storage.removeLocationCoordinate
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.util.AlertDialogs
import me.rhunk.snapenhance.ui.util.DialogProperties
import me.rhunk.snapenhance.ui.util.coil.BitmojiImage
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class BetterLocationRoot : Routes.Route() {
    private val alertDialogs by lazy { AlertDialogs(context.translation) }

    @Composable
    private fun FriendLocationItem(
        friendLocation: FriendLocation,
        dismiss: () -> Unit
    ) {
        ElevatedCard(onClick = {
            context.config.root.global.betterLocation.coordinates.setAny(friendLocation.latitude to friendLocation.longitude)
            dismiss()
        }, modifier = Modifier.padding(4.dp)) {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BitmojiImage(
                    context = context,
                    url = BitmojiSelfie.getBitmojiSelfie(
                        friendLocation.bitmojiSelfieId,
                        friendLocation.bitmojiId,
                        BitmojiSelfie.BitmojiSelfieType.NEW_THREE_D
                    ),
                    size = 48,
                    modifier = Modifier.padding(6.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(friendLocation.displayName?.let { "$it (${friendLocation.username})" }
                        ?: friendLocation.username, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = buildString {
                            append(friendLocation.localityPieces.joinToString(", "))
                            append("\n")
                            append("Lat: ${friendLocation.latitude.toFloat()}, Lng: ${friendLocation.longitude.toFloat()}")
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Light,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }

    @Composable
    private fun FriendLocationsDialogs(
        friendsLocation: List<FriendLocation>,
        dismiss: () -> Unit
    ) {
        var search by remember { mutableStateOf("") }
        val filteredFriendsLocation = rememberAsyncMutableStateList(defaultValue = friendsLocation, keys = arrayOf(search)) {
            search.takeIf { it.isNotBlank() }?.let {
                friendsLocation.filter {
                    it.displayName?.contains(search, ignoreCase = true) == true || it.username.contains(search, ignoreCase = true)
                }
            }  ?: friendsLocation
        }

        ElevatedCard(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.padding(top = 32.dp, bottom = 32.dp)
        ) {
            Text(
                translation["teleport_to_friend_title"],
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                value = search,
                onValueChange = { search = it },
                label = { Text(translation["search_bar"]) }
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                item {
                    if (friendsLocation.isEmpty()) {
                        Text(
                            translation["no_friends_map"],
                            fontSize = 16.sp,
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Light
                        )
                    } else if (filteredFriendsLocation.isEmpty()) {
                        Text(
                            translation["no_friends_found"],
                            fontSize = 16.sp,
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Light
                        )
                    }
                }
                items(filteredFriendsLocation) { friendLocation ->
                    FriendLocationItem(friendLocation, dismiss)
                }
            }
        }
    }

    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val coordinatesProperty = remember {
            context.config.root.global.betterLocation.getPropertyPair("coordinates")
        }

        val updateDispatcher = rememberAsyncUpdateDispatcher()
        val savedCoordinates = rememberAsyncMutableStateList(
            defaultValue = listOf(),
            updateDispatcher = updateDispatcher
        ) {
            context.database.getLocationCoordinates()
        }
        var showMap by remember { mutableStateOf(false) }
        var addSavedCoordinateDialog by remember { mutableStateOf(false) }
        var showTeleportDialog by remember { mutableStateOf(false) }

        val marker = remember { mutableStateOf<Marker?>(null) }
        val mapView = remember { mutableStateOf<MapView?>(null) }
        var spoofedCoordinates by remember(showTeleportDialog, showMap) { mutableStateOf(coordinatesProperty.value.get() as? Pair<*, *>) }

        fun addSavedCoordinate(id: Int?, locationCoordinates: LocationCoordinates) {
            context.coroutineScope.launch {
                context.database.addOrUpdateLocationCoordinate(id, locationCoordinates)
                updateDispatcher.dispatch()
            }
        }

        if (showTeleportDialog) {
            me.rhunk.snapenhance.ui.util.Dialog(
                properties = DialogProperties(usePlatformDefaultWidth = false),
                onDismissRequest = { showTeleportDialog = false },
                content = {
                    FriendLocationsDialogs(remember { context.locationManager.friendsLocation }) {
                        showTeleportDialog = false
                        context.coroutineScope.launch {
                            context.config.writeConfig()
                        }
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Text(
                translation.format(
                    "spoofed_coordinates_title",
                    "latitude" to ((spoofedCoordinates?.first as? Double)?.toFloat() ?: "0.0").toString(),
                    "longitude" to ((spoofedCoordinates?.second as? Double)?.toFloat() ?: "0.0").toString()
                ),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )

            if (addSavedCoordinateDialog) {
                var savedName by remember { mutableStateOf("") }
                me.rhunk.snapenhance.ui.util.Dialog(
                    onDismissRequest = { addSavedCoordinateDialog = false },
                    content = {
                        alertDialogs.DefaultDialogCard {
                            val focusRequester = remember { FocusRequester() }
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(translation["save_coordinates_dialog_title"], fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    modifier = Modifier
                                        .focusRequester(focusRequester)
                                        .onGloballyPositioned {
                                            focusRequester.requestFocus()
                                        },
                                    value = savedName,
                                    onValueChange = { savedName = it },
                                    label = { Text(translation["saved_name_dialog_hint"]) }
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            addSavedCoordinateDialog = false
                                            addSavedCoordinate(null, LocationCoordinates().apply {
                                                this.name = savedName
                                                this.latitude = marker.value?.position?.latitude as Double
                                                this.longitude = marker.value?.position?.longitude as Double
                                            })
                                        },
                                        enabled = savedName.isNotBlank()
                                    ) {
                                        Text(translation["save_dialog_button"])
                                    }
                                }
                            }
                        }
                    }
                )
            }

            if (showMap) {
                me.rhunk.snapenhance.ui.util.Dialog(
                    onDismissRequest = { showMap = false },
                    content = {
                        alertDialogs.ChooseLocationDialog(property = coordinatesProperty, marker, mapView, saveCoordinates = {
                            addSavedCoordinateDialog = true
                        }) {
                            showMap = false
                            context.config.writeConfig()
                        }
                    }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = { showMap = true }) {
                            Text(translation["choose_location_button"])
                        }
                        Button(onClick = { showTeleportDialog = true }) {
                            Text(translation["teleport_to_friend_button"])
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = translation["spoof_location_toggle"])
                        Spacer(modifier = Modifier.weight(1f))
                        var isSpoofing by remember { mutableStateOf(context.config.root.global.betterLocation.spoofLocation.get()) }
                        Switch(
                            checked = isSpoofing,
                            onCheckedChange = {
                                isSpoofing = it
                                context.config.root.global.betterLocation.spoofLocation.set(it)
                            }
                        )
                    }
                }
                item {
                    Text(
                        translation["saved_coordinates_title"],
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
                item {
                    if (savedCoordinates.isEmpty()) {
                        Text(
                            translation["no_saved_coordinates_hint"],
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 20.dp),
                            fontWeight = FontWeight.Light
                        )
                    }
                }
                items(savedCoordinates) { coordinates ->
                    var showDeleteDialog by remember { mutableStateOf(false) }

                    if (showDeleteDialog) {
                        me.rhunk.snapenhance.ui.util.Dialog(
                            onDismissRequest = { showDeleteDialog = false },
                            content = {
                                alertDialogs.ConfirmDialog(
                                    title = translation["delete_dialog_title"],
                                    message = translation["delete_dialog_message"],
                                    onConfirm = {
                                        showDeleteDialog = false
                                        context.coroutineScope.launch {
                                            context.database.removeLocationCoordinate(coordinates.id)
                                            updateDispatcher.dispatch()
                                        }
                                    },
                                    onDismiss = { showDeleteDialog = false }
                                )
                            }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${coordinates.name} (${coordinates.latitude.toFloat()}, ${coordinates.longitude.toFloat()})",
                            fontWeight = if (spoofedCoordinates == coordinates.latitude to coordinates.longitude) FontWeight.Bold else FontWeight.Light,
                            modifier = Modifier
                                .padding(8.dp)
                                .weight(1f)
                                .clickable {
                                    spoofedCoordinates =
                                        coordinates.latitude to coordinates.longitude
                                    coordinatesProperty.value.setAny(spoofedCoordinates)
                                    context.coroutineScope.launch {
                                        context.config.writeConfig()
                                    }
                                    GeoPoint(coordinates.latitude, coordinates.longitude).also {
                                        marker.value?.position = it
                                        mapView.value?.controller?.apply {
                                            animateTo(it)
                                            setZoom(16.0)
                                        }
                                    }
                                },
                            fontSize = 16.sp
                        )
                        FilledIconButton(onClick = {
                            showDeleteDialog = true
                        }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }
}