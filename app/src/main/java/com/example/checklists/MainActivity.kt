package com.example.checklists

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.lang.IllegalArgumentException
import java.lang.NumberFormatException
import kotlin.math.roundToInt

/*ListItem is how items in lists are stored in memory*/
data class ListItem(
    var name: String,
    var position: Int,
    var completed: Int,
    var max: Int
)

/*GenericItem can be folder or list, folders just have no items value*/
data class GenericItem(
    var name: String,
    var position: Int,
    var parent: String,
    var type: String,
    var timeCompleted: Long,
    var items: MutableList<ListItem>
)

/*Value defining what percentage of the screen height the banner and task item entries should take up*/
const val BannerPercent = 0.1
/*Value defining what percentage of the screen width/height the settings should take up*/
const val SettingsPercent = 0.75
/*Values defining end of item separator and end of item file*/
const val EndItem = "ENDITEMSEP"
const val EndItems = "ENDOFITEMS"
/*Value defining time units in milliseconds for auto-deleting lists*/
const val OneMin = 60 * 1000L
const val OneHour = 60 * OneMin
const val OneDay = 24 * OneHour
/*Value defining temp path values to avoid errors when moving items*/
const val TempPath = "path_FIX"
const val TempPath2 = "path_FIX2"

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }
}

@Composable
@Preview
fun MainScreen() {
    var selectedItemIndex by remember { mutableStateOf(-1) }
    var selectedListItemIndex by remember { mutableStateOf(-1)}
    var inList by remember{mutableStateOf(false)}
    val activeItems = mutableListOf<GenericItem>()
    var path by remember{mutableStateOf("All items")}
    val items = mutableListOf<GenericItem>()
    Log.i("INIT", "Reset/cleared items")
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp
    val screenWidth = configuration.screenWidthDp
    val settingsWidth = screenWidth * SettingsPercent
    val settingsHeight = screenHeight * SettingsPercent
    val bannerHeight = (screenHeight * BannerPercent)
    var settingsOpen by remember{mutableStateOf(false)}
    var createOpen by remember{mutableStateOf(false)}
    var autoDelListTime = "60"
    var autoDelListSelect = "Minutes"
    var moveCompleteItemsSelect = "Bottom"
    val context = LocalContext.current
    val titleFontSize = 22.sp
    var completedColor = Color.Green
    val settingsSaved= loadSettings(context)
    if (settingsSaved.isNotEmpty()) {
        autoDelListTime = settingsSaved[0]
        autoDelListSelect = settingsSaved[1]
        moveCompleteItemsSelect = settingsSaved[2]
        completedColor = Color(red = settingsSaved[3].toFloat(), green = settingsSaved[4].toFloat(), blue = settingsSaved[5].toFloat())
    }
    Log.i("SETTINGS","Settings now: $autoDelListTime, $autoDelListSelect, $moveCompleteItemsSelect, $completedColor")
    Column(modifier = Modifier.background(Color.LightGray).fillMaxSize()) {
        Row(
            Modifier
                .background(Color.Blue)
                .size(width = screenWidth.dp, height = bannerHeight.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Image(
                painter = painterResource(R.drawable.settingsicon),
                contentDescription = "Settings Icon",
                modifier = Modifier
                    .height((bannerHeight / 4).dp)
                    .clickable(enabled = true, onClick = { settingsOpen = true })
            )
            ClickableText(
                onClick = { settingsOpen = true },
                modifier = Modifier.weight(1f),
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = Color.Gray,
                        )
                    ) { append("Settings") }
                }
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier
                .fillMaxHeight()
                .weight(4f)) {
                Text(
                    text = "Checklists",
                    color = Color.Cyan,
                    fontSize = titleFontSize
                )
            }
            ClickableText(
                onClick = { createOpen = true },
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = Color.Gray,
                        )
                    ) { append("Add new...") }
                }
            )
            Image(
                painter = painterResource(R.drawable.addicon),
                contentDescription = "Add Item Icon",
                modifier = Modifier
                    .height((bannerHeight / 4).dp)
                    .clickable(enabled = true, onClick = { createOpen = true })
            )
        }
        Row {/*
            ClickableText(
                onClick = {
                    path = path.substringBeforeLast('/')
                    selectedItemIndex = -1
                    inList = false
                },
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = Color.Black,
                        )
                    ) { append(path) }
                }
            )*/
            val splitPath = path.split('/')
            var buildPath = ""
            splitPath.forEach {
                buildPath += "$it/"
                DrawPath(path = buildPath.substringBeforeLast('/'), onUse = {
                    newPath -> path = newPath
                    selectedItemIndex = -1
                    inList = false
                })
                Text(text = "/")
            }
        }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .background(Color.LightGray)
                .fillMaxSize()) {
            loadItems(context, items, path, activeItems)
            items.sortBy{it.position}
            var color = Color.LightGray
            var index = 0
            if(selectedItemIndex > -1) {
                Log.i("SELECT", "Selected item index: $selectedItemIndex")
                if(!inList) {
                    path += "/${activeItems[selectedItemIndex].name}"
                    inList = activeItems[selectedItemIndex].type == "checklist"
                    if(!inList) {selectedItemIndex = -1}
                } else{
                    rebuildActiveItems(items, activeItems, path)
                    if(path.substringAfterLast('/') != TempPath && path.substringAfterLast('/') != TempPath2) repeat(
                        activeItems[selectedItemIndex].items.size
                    ) {
                        DrawItems(
                            activeItems[selectedItemIndex],
                            screenWidth,
                            bannerHeight,
                            onItemSelect = { listItemSelected ->
                                selectedListItemIndex = listItemSelected
                            },
                            color,
                            index,
                            inList,
                            completedColor,
                            settingsWidth,
                            settingsHeight,
                            context,
                            items,
                            activeItems,
                            onRedraw = {
                                path += "/$TempPath"
                            },
                            moveCompleteItemsSelect
                        )
                        index++
                        color = if (color == Color.LightGray) Color.Gray else Color.LightGray
                    }
                    if(path.substringAfterLast('/') == TempPath2) path = path.substringBeforeLast('/')
                }
            } else {
                val toDelete = mutableListOf<GenericItem>()
                items.forEach {
                    if(it.parent == path.substringAfterLast('/')) {
                        var timeUnit = 0L
                        when (autoDelListSelect) {
                            "Minutes" -> {timeUnit = OneMin}
                            "Hours" -> {timeUnit = OneHour}
                            "Days" -> {timeUnit = OneDay}
                        }
                        if(it.timeCompleted > 0L && System.currentTimeMillis() - it.timeCompleted >= autoDelListTime.toLong() * timeUnit) {
                            toDelete.add(it)
                        } else {
                            DrawItems(
                                it,
                                screenWidth,
                                bannerHeight,
                                onItemSelect = { itemSelected -> selectedItemIndex = itemSelected },
                                color,
                                index,
                                inList,
                                completedColor,
                                settingsWidth,
                                settingsHeight,
                                context,
                                items,
                                activeItems,
                                onRedraw = {
                                    path += "/$TempPath"
                                },
                                moveCompleteItemsSelect
                            )
                            index++
                            color = if (color == Color.LightGray) Color.Gray else Color.LightGray
                        }
                    }
                }
                deleteCompleted(items, toDelete, context)
            }
        }
    }
    if (selectedListItemIndex > -1) {
        Log.d("SELECT", "Selected listItem index: $selectedListItemIndex")
        activeItems[selectedItemIndex].items[selectedListItemIndex].completed += 1
        handleCompletedItems(
            moveCompleteItemsSelect,
            activeItems,
            selectedItemIndex,
            items,
            context,
            selectedListItemIndex
        ) { path += "/$TempPath2" }
        selectedListItemIndex = -1
    }
    if (settingsOpen) {
        SettingsScreen(
            settingsWidth,
            settingsHeight,
            context,
            onClose = {settingsOpen = false},
            onDelTimeChange = { newTime -> autoDelListTime = newTime},
            onDelListSelectChange = {newDelListSelect -> autoDelListSelect = newDelListSelect},
            onMoveCompleteItemsSelectChange = { newMoveCompSelect -> moveCompleteItemsSelect = newMoveCompSelect},
            autoDelListTime,
            autoDelListSelect,
            moveCompleteItemsSelect,
            completedColor,
            items
        )
    }
    if (createOpen) {
        CreateScreen(settingsWidth, settingsHeight, context, items, onClose = {createOpen = false}, path, activeItems, inList, selectedItemIndex, moveCompleteItemsSelect)
    }
    if(path.substringAfterLast('/') == TempPath) path = path.substringBeforeLast('/')
}

@Composable
fun DrawPath(path: String, onUse: (String) -> Unit) {
    ClickableText(
        onClick = {
            onUse(path)
        },
        text = buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    color = Color.Black,
                )
            ) { append(path.substringAfterLast('/')) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(settingsWidth: Double, settingsHeight: Double, context: Context, items: MutableList<GenericItem>, onClose: () -> Unit, path: String, activeItems: MutableList<GenericItem>, inList: Boolean, selectedItemIndex: Int, moveCompleteItemsSelect: String) {
    var newItemName by rememberSaveable { mutableStateOf("New Item") }
    var maxComplete by rememberSaveable { mutableStateOf("1") }
    val itemTypes = arrayOf("Checklist", "Folder")
    var newItemExpanded by remember { mutableStateOf(false) }
    var newItemSelectedType by remember { mutableStateOf(itemTypes[0]) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(width = settingsWidth.dp, height = settingsHeight.dp)
                .background(Color(216, 216, 216, 255))
        ) {
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxSize()
            ) {
                Image(
                    painter = painterResource(R.drawable.closeicon),
                    contentDescription = "Close Create New Icon",
                    modifier = Modifier
                        .clickable(enabled = true, onClick = { onClose() })
                )
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Text(text = "Create New")
                Box(
                    modifier = Modifier
                        .size(width = settingsWidth.dp, height = 5.dp)
                        .background(Color(204, 204, 204, 255))
                )
                Text(text = "Item Name:")
                TextField(
                    value = newItemName,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    onValueChange = {
                        newItemName = it
                    }
                )
                if(inList) {
                    Text(text = "Max Completable:")
                    TextField(
                        value = maxComplete,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        onValueChange = {
                            maxComplete = it
                        }
                    )
                    Text(text = "\"Max Completable\" means how many times you can tap on the task before it is completed." +
                            " For example, a task with Max Completable of 3 can be tapped on 3 times before it is marked as totally complete.")
                } else {
                    Text(text = "Item Type:")
                    ExposedDropdownMenuBox(
                        expanded = newItemExpanded,
                        onExpandedChange = {
                            newItemExpanded = !newItemExpanded
                        }
                    ) {
                        TextField(
                            value = newItemSelectedType,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = newItemExpanded) },
                            modifier = Modifier.menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = newItemExpanded,
                            onDismissRequest = { newItemExpanded = false }
                        ) {
                            itemTypes.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(text = item) },
                                    onClick = {
                                        newItemSelectedType = item
                                        newItemExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = { createNewItem(items, newItemName, newItemSelectedType, context, path, activeItems, inList, selectedItemIndex, maxComplete.toInt(), moveCompleteItemsSelect) }
                ) {
                    Text("Create")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsWidth: Double,
    settingsHeight: Double,
    context: Context,
    onClose: () -> Unit,
    onDelTimeChange: (String) -> Unit,
    onDelListSelectChange: (String) -> Unit,
    onMoveCompleteItemsSelectChange: (String) -> Unit,
    defDelTime: String,
    defDelListSelect: String,
    defMoveCompleteItemsSelect: String,
    defCompletedColor: Color,
    itemsList: MutableList<GenericItem>
){
    var autoDelListTime by rememberSaveable { mutableStateOf(defDelTime) }
    val delUnits = arrayOf("Minutes", "Hours", "Days")
    var autoDelListExpanded by remember { mutableStateOf(false) }
    var autoDelListSelect by remember { mutableStateOf(defDelListSelect) }
    val moveCompleteItems = arrayOf("Bottom", "Top", "Don't Move")
    var moveCompleteItemsExpanded by remember { mutableStateOf(false) }
    var moveCompleteItemsSelect by remember { mutableStateOf(defMoveCompleteItemsSelect) }
    var completedColor by remember{ mutableStateOf(defCompletedColor) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(width = settingsWidth.dp, height = settingsHeight.dp)
                .background(Color(216, 216, 216, 255))
        ) {
            Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.Top ,modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(R.drawable.closeicon),
                    contentDescription = "Close Settings Icon",
                    modifier = Modifier
                        .clickable(enabled = true, onClick = { onClose() })
                )
            }
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {
                Text(text = "Settings")
                Box(
                    modifier = Modifier
                        .size(width = settingsWidth.dp, height = 5.dp)
                        .background(Color(204, 204, 204, 255))
                )
                Text(text = "Auto-delete completed lists after:")
                TextField(
                    value = autoDelListTime,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    onValueChange = {
                        autoDelListTime = it
                        onDelTimeChange(autoDelListTime)
                        saveSettings(context, autoDelListTime, autoDelListSelect, moveCompleteItemsSelect, completedColor)
                    }
                )
                ExposedDropdownMenuBox(
                    expanded = autoDelListExpanded,
                    onExpandedChange = {
                        autoDelListExpanded = !autoDelListExpanded
                    }
                ) {
                    TextField(
                        value = autoDelListSelect,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = autoDelListExpanded) },
                        modifier = Modifier.menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = autoDelListExpanded,
                        onDismissRequest = { autoDelListExpanded = false }
                    ) {
                        delUnits.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(text = item) },
                                onClick = {
                                    autoDelListSelect = item
                                    onDelListSelectChange(autoDelListSelect)
                                    autoDelListExpanded = false
                                    saveSettings(context, autoDelListTime, autoDelListSelect, moveCompleteItemsSelect, completedColor)
                                }
                            )
                        }
                    }
                }
                Text(text = "Move completed list items to:")
                ExposedDropdownMenuBox(
                    expanded = moveCompleteItemsExpanded,
                    onExpandedChange = {
                        moveCompleteItemsExpanded = !moveCompleteItemsExpanded
                    }
                ) {
                    TextField(
                        value = moveCompleteItemsSelect,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = moveCompleteItemsExpanded) },
                        modifier = Modifier.menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = moveCompleteItemsExpanded,
                        onDismissRequest = { moveCompleteItemsExpanded = false }
                    ) {
                        moveCompleteItems.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(text = item) },
                                onClick = {
                                    moveCompleteItemsSelect = item
                                    onMoveCompleteItemsSelectChange(moveCompleteItemsSelect)
                                    moveCompleteItemsExpanded = false
                                    saveSettings(context, autoDelListTime, autoDelListSelect, moveCompleteItemsSelect, completedColor)
                                }
                            )
                        }
                    }
                }
                Row(Modifier.background(completedColor)) {
                    Text(text = "Red, Green, Blue of completed items:")
                }
                TextField(
                    value = completedColor.red.toString(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    onValueChange = {
                        try {
                            completedColor = Color(red = it.toFloat(), green = completedColor.green, blue = completedColor.blue)
                            saveSettings(context, autoDelListTime, autoDelListSelect, moveCompleteItemsSelect, completedColor)
                        }
                        catch (e: IllegalArgumentException) {
                            Toast.makeText(context, "RGB values must be between 0.0 and 1.0", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                TextField(
                    value = completedColor.green.toString(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    onValueChange = {
                        try {
                            completedColor = Color(
                                green = it.toFloat(),
                                red = completedColor.red,
                                blue = completedColor.blue
                            )
                            saveSettings(
                                context,
                                autoDelListTime,
                                autoDelListSelect,
                                moveCompleteItemsSelect,
                                completedColor
                            )
                        }
                        catch (e: IllegalArgumentException) {
                            Toast.makeText(context, "RGB values must be between 0.0 and 1.0", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                TextField(
                    value = completedColor.blue.toString(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    onValueChange = {
                        try {
                            completedColor = Color(
                                blue = it.toFloat(),
                                green = completedColor.green,
                                red = completedColor.red
                            )
                            saveSettings(
                                context,
                                autoDelListTime,
                                autoDelListSelect,
                                moveCompleteItemsSelect,
                                completedColor
                            )
                        }
                        catch (e: IllegalArgumentException) {
                            Toast.makeText(context, "RGB values must be between 0.0 and 1.0", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                Button(onClick = {
                    itemsList.clear()
                    saveItems(context, itemsList)
                }) {
                    Text(text = "Wipe All Data")
                }
            }
        }
    }
}

@Composable
fun DrawItemIcon(item: GenericItem, index: Int, inList: Boolean) {
    if(inList) {
        Text(text = "${item.items[index].completed} / ${item.items[index].max}")
    }
    else if(item.type == "checklist") {
        Image(
            painter = painterResource(R.drawable.listicon),
            contentDescription = "List Icon"
        )
    } else{
        Image(
            painter = painterResource(R.drawable.foldericon),
            contentDescription = "Folder Icon"
        )
    }
    Text(text = if(!inList) item.name else item.items[index].name,
        Modifier.padding(16.dp))
}

fun isCompletedList(list: GenericItem): Boolean {
    var retVal = true
    list.items.forEach {
        if(it.completed < it.max) {
            retVal = false
            return@forEach
        }
    }
    retVal = if(list.items.isEmpty()) false else retVal
    return retVal
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrawItems(
    item: GenericItem,
    screenWidth: Int,
    bannerHeight: Double,
    onItemSelect: (Int) -> Unit,
    color: Color,
    index: Int,
    inList: Boolean,
    completedColor: Color,
    settingsWidth: Double,
    settingsHeight: Double,
    context: Context,
    items: MutableList<GenericItem>,
    activeItems: MutableList<GenericItem>,
    onRedraw: () -> Unit,
    moveCompleteItemsSelect: String
) {
    var editOpen by remember { mutableStateOf(false) }
    var editVisible by remember { mutableStateOf(false) }
    var realColor = color
    if (inList && item.items[index].completed >= item.items[index].max) realColor = completedColor
    if (!inList && isCompletedList(item)) realColor = completedColor
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var zInd by remember { mutableStateOf(0f) }
    Row(modifier = Modifier
        .width(screenWidth.dp)
        .zIndex(zInd)) {
        Box(modifier = Modifier.width(10.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier =
        Modifier
            .background(realColor)
            .size(width = screenWidth.dp - 20.dp, height = bannerHeight.dp)
            .combinedClickable(
                onClick = { onItemSelect(index) },
                onLongClick = { editVisible = true })
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = {
                    zInd = 1f
                    Log.i("DRAG", "Began dragging index: $index")
                }, onDrag = { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }, onDragEnd = {
                    zInd = 0f
                    var newRowPosition = (offsetY / (bannerHeight * 3.8) + index).roundToInt()
                    Log.i("DRAG", "Ended dragging at new (unrounded) index: $newRowPosition")
                    if (!inList) {
                        newRowPosition = newRowPosition.coerceIn(0, activeItems.size - 1)
                        if (newRowPosition < item.position) {
                            for (i in newRowPosition until item.position) {
                                activeItems[i].position += 1
                            }
                        } else if (newRowPosition > item.position) {
                            for (i in item.position + 1 until newRowPosition + 1) {
                                activeItems[i].position -= 1
                            }
                        }
                        item.position = newRowPosition
                        items.sortBy { it.position }
                    } else {
                        newRowPosition = newRowPosition.coerceIn(0, item.items.size - 1)
                        if (newRowPosition < item.items[index].position) {
                            for (i in newRowPosition until item.items[index].position) {
                                item.items[i].position += 1
                            }
                        } else if (newRowPosition > item.items[index].position) {
                            for (i in item.items[index].position + 1 until newRowPosition + 1) {
                                item.items[i].position -= 1
                            }
                        }
                        item.items[index].position = newRowPosition
                        item.items.sortBy { it.position }
                    }
                    saveItems(context, items)
                    offsetX = 0f
                    offsetY = 0f
                    onRedraw()
                })
            }
    ) {
        DrawItemIcon(item, index, inList)
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (editVisible) {
                    ShowEditButton(
                        onClick = { editOpen = true },
                        on2ndClick = { editVisible = false })
                }
            }
        }
    }
}
    if(editOpen) {
        editVisible = false
        EditScreen(settingsWidth, settingsHeight, onClose = {
            editOpen = false
            saveItems(context, items)
            onRedraw()
        }, inList, item, index, items, context, moveCompleteItemsSelect)
    }
}

@Composable
fun ShowEditButton(onClick: () -> Unit, on2ndClick: () -> Unit) {
    Button(onClick = { onClick() }) {
        Text(text = "Edit...")
    }
    Button(onClick = { on2ndClick() }) {
        Text(text = "Hide")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(settingsWidth: Double, settingsHeight: Double, onClose: () -> Unit, inList: Boolean, item: GenericItem, index: Int, items: MutableList<GenericItem>, context: Context,
               moveCompleteItemsSelect: String) {
    var newName by remember{ mutableStateOf("") }
    var newCurrent by remember{ mutableStateOf("") }
    var newMax by remember{ mutableStateOf("") }
    val possibleParents = mutableListOf<String>()
    var parentExpanded by remember{ mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(width = settingsWidth.dp, height = settingsHeight.dp)
                .background(Color(216, 216, 216, 255))
        ) {
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxSize()
            ) {
                Image(
                    painter = painterResource(R.drawable.closeicon),
                    contentDescription = "Close Edit Icon",
                    modifier = Modifier
                        .clickable(enabled = true, onClick = { onClose() })
                )
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Text(text = "Edit Item")
                Box(
                    modifier = Modifier
                        .size(width = settingsWidth.dp, height = 5.dp)
                        .background(Color(204, 204, 204, 255))
                )
                Text(text = "Item Name:")
                if(!inList) {
                    newName = item.name
                    TextField(
                        value = newName,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true,
                        onValueChange = {
                            items.forEach { thing ->
                                if(thing.parent == item.name) thing.parent = it
                            }
                            item.name = it
                            newName = it
                        }
                    )
                    possibleParents.add("All items")
                    items.forEach {
                        if(it.type == "folder") possibleParents.add(it.name)
                    }
                    Text(text = "Item Parent/Folder:")
                    ExposedDropdownMenuBox(
                        expanded = parentExpanded,
                        onExpandedChange = {
                            parentExpanded = !parentExpanded
                        }
                    ) {
                        TextField(
                            value = item.parent,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = parentExpanded) },
                            modifier = Modifier.menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = parentExpanded,
                            onDismissRequest = { parentExpanded = false }
                        ) {
                            possibleParents.forEach { par ->
                                DropdownMenuItem(
                                    text = { Text(text = par) },
                                    onClick = {
                                        item.parent = par
                                        parentExpanded = false
                                        onClose()
                                    }
                                )
                            }
                        }
                    }
                } else {
                    newName = item.items[index].name
                    TextField(
                        value = newName,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true,
                        onValueChange = {
                            item.items[index].name = it
                            newName = it
                        }
                    )
                    items.forEach {
                        if(it.type == "checklist") possibleParents.add(it.name)
                    }
                    Text(text = "List This Belongs To:")
                    ExposedDropdownMenuBox(
                        expanded = parentExpanded,
                        onExpandedChange = {
                            parentExpanded = !parentExpanded
                        }
                    ) {
                        TextField(
                            value = item.name,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = parentExpanded) },
                            modifier = Modifier.menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = parentExpanded,
                            onDismissRequest = { parentExpanded = false }
                        ) {
                            possibleParents.forEach { par ->
                                DropdownMenuItem(
                                    text = { Text(text = par) },
                                    onClick = {
                                        for(i in 0 until items.size) {
                                            if(items[i].name == par) {
                                                for(j in index until item.items.size) {
                                                    item.items[j].position -= 1
                                                }
                                                when (moveCompleteItemsSelect) {
                                                    "Bottom" -> {
                                                        item.items[index].position = 0
                                                        items[i].items.forEach {
                                                            it.position += 1
                                                        }
                                                        items[i].items.add(item.items[index])
                                                    }
                                                    "Top" -> {
                                                        item.items[index].position = items[i].items.size
                                                        items[i].items.add(item.items[index])}
                                                }
                                                item.items.removeAt(index)
                                                items[i].items.sortBy { it.position }
                                                break
                                            }
                                        }
                                        parentExpanded = false
                                        onClose()
                                    }
                                )
                            }
                        }
                    }
                    Text(text = "Task Current Completeness:")
                    newCurrent = item.items[index].completed.toString()
                    TextField(
                        value = newCurrent,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        onValueChange = {
                            try {
                                item.items[index].completed = it.toInt()
                                newCurrent = it
                                if (item.items[index].completed < item.items[index].max) item.timeCompleted =
                                    0L
                            } catch (e: NumberFormatException) {
                                Toast.makeText(context, "Values must be at least 0", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    Text(text = "Task Max Completable")
                    newMax = item.items[index].max.toString()
                    TextField(
                        value = newMax,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        onValueChange = {
                            try {
                                item.items[index].max = it.toInt()
                                newMax = it
                                if (item.items[index].completed < item.items[index].max) item.timeCompleted =
                                    0L
                            } catch (e: NumberFormatException) {
                                Toast.makeText(context, "Values must be at least 0", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
                Button(onClick = {
                     if(inList) {
                         for (i in index + 1 until item.items.size) {
                             item.items[i].position -= 1
                         }
                         item.items.removeAt(index)
                     }
                    else {
                         for (i in index + 1 until items.size) {
                             items[i].position -= 1
                         }
                         items.removeAt(index)
                    }
                }) {
                    Text(text = "Delete Item")
                }
            }
        }
    }
}

private fun deleteCompleted(
    itemsList: MutableList<GenericItem>,
    toDelete: MutableList<GenericItem>,
    context: Context
) {
    Log.i("DELETE", "toDelete list = $toDelete")
    while(toDelete.isNotEmpty()) {
        for (i in itemsList.indexOf(toDelete[0]) + 1 until itemsList.size) {
            itemsList[i].position -= 1
        }
        Log.i("DELETE", "Deleted item: ${toDelete[0]} at index: ${itemsList.indexOf(toDelete[0])}")
        itemsList.removeAt(itemsList.indexOf(toDelete[0]))
        toDelete.removeAt(0)
        saveItems(context, itemsList)
    }
}

/*Iterate over current checklist and perform actions on completed tasks according to settings. Do same for handling completed checklists*/
private fun handleCompletedItems(
    moveCompleteItemsSelect: String,
    activeItems: MutableList<GenericItem>,
    selectedItemIndex: Int,
    itemsList: MutableList<GenericItem>,
    context: Context,
    i: Int,
    onRedraw: () -> Unit
) {
    if(activeItems[selectedItemIndex].items[i].completed >= activeItems[selectedItemIndex].items[i].max) {
        when (moveCompleteItemsSelect) {
            "Bottom" -> {
                activeItems[selectedItemIndex].items[i].position = activeItems[selectedItemIndex].items.size - 1
                for (j in i + 1 until activeItems[selectedItemIndex].items.size) {
                    activeItems[selectedItemIndex].items[j].position -= 1
                }
            }
            "Top" -> {
                for (j in 0 until i) {
                    activeItems[selectedItemIndex].items[j].position += 1
                }
                activeItems[selectedItemIndex].items[i].position = 0
            } /*legacy code*/
            "Delete Them" -> {
                for (j in i + 1 until activeItems[selectedItemIndex].items.size) {
                    activeItems[selectedItemIndex].items[j].position -= 1
                }
                activeItems[selectedItemIndex].items.removeAt(i)
            } /*end of legacy*/
        }
        activeItems[selectedItemIndex].items.sortBy{it.position}
        onRedraw()
    }
    if(isCompletedList(activeItems[selectedItemIndex])) {
        activeItems[selectedItemIndex].timeCompleted = System.currentTimeMillis()
    }
    saveItems(context, itemsList)
}

private fun rebuildActiveItems(items: MutableList<GenericItem>, activeItems: MutableList<GenericItem>, path: String) {
    activeItems.clear()
    val parent: String
    var parentItem = GenericItem("null", 0, "null", "null", 0L, mutableListOf())
    items.forEach {
        if(it.name == path.substringAfterLast('/')) {
            parentItem = it
        }
    }
    parent = if(path.substringAfterLast('/') == "All items") "All items" else parentItem.parent
    items.forEach {
        if(it.parent == parent) activeItems.add(it)
    }
    Log.i("REBUILD", "Rebuilt activeItems: $activeItems")
}

private fun createNewItem(
    itemsList: MutableList<GenericItem>,
    named: String,
    typed: String,
    context: Context,
    path: String,
    activeItems: MutableList<GenericItem>,
    inList: Boolean,
    selectedItemIndex: Int,
    max: Int,
    moveCompleteItemsSelect: String
) {
    if(inList) {
        if(moveCompleteItemsSelect == "Bottom") {
            activeItems[selectedItemIndex].items.forEach {
                it.position += 1
            }
            activeItems[selectedItemIndex].items.add(ListItem(name = named, 0, 0, max))
            activeItems[selectedItemIndex].items.sortBy {it.position}
        } else activeItems[selectedItemIndex].items.add(ListItem(name = named, activeItems[selectedItemIndex].items.lastIndex + 1, 0, max))
        saveItems(context, itemsList)
        Toast.makeText(context, "Added task: $named to ${activeItems[selectedItemIndex].name}", Toast.LENGTH_SHORT).show()
    } else{
        itemsList.add(GenericItem(
            name = named,
            position = activeItems.lastIndex + 1,
            parent = path.substringAfterLast('/'),
            type = typed.lowercase(),
            timeCompleted = 0L,
            items = mutableListOf()
        ))
        saveItems(context, itemsList)
        Toast.makeText(context, "Added item: $named to ${path.substringAfterLast('/')}", Toast.LENGTH_SHORT).show()
        rebuildActiveItems(itemsList, activeItems, path)
    }
}

private fun saveItems(
    context: Context,
    itemsList: MutableList<GenericItem>
) {
    try {
        val fileName = "items.txt"
        val file = File(context.filesDir, fileName)
        val writer = BufferedWriter(FileWriter(file))
        itemsList.forEach { generic ->
            writer.write(generic.name)
            writer.newLine()
            writer.write(generic.position.toString())
            writer.newLine()
            writer.write(generic.parent)
            writer.newLine()
            writer.write(generic.type)
            writer.newLine()
            writer.write(generic.timeCompleted.toString())
            generic.items.forEach {
                writer.newLine()
                writer.write(it.name)
                writer.newLine()
                writer.write(it.position.toString())
                writer.newLine()
                writer.write(it.completed.toString())
                writer.newLine()
                writer.write(it.max.toString())
            }
            writer.newLine()
            writer.write(EndItem)
            writer.newLine()
            Log.i("SAVE", "Saved: $generic")
        }
        writer.write(EndItems)
        writer.close()
    } catch (e: IOException) {
        e.printStackTrace()
        Toast.makeText(context, "Error saving items!", Toast.LENGTH_SHORT).show()
    }
}

private fun loadItems(context: Context, items: MutableList<GenericItem>, path: String, activeItems: MutableList<GenericItem>) {
    try {
        val fileName = "items.txt"
        val file = File(context.filesDir, fileName)
        val reader = BufferedReader(FileReader(file))
        if (!file.exists()) {
            Log.w("LOAD", "Could not find items.txt file")
            reader.close()
        } else {
            var line: String
            line = reader.readLine()
            while (line != EndItems) {
                var lineNum = 0
                val loadingItem = GenericItem("null", 0, "All items", "checklist", 0L, mutableListOf())
                while(line != EndItem && line != EndItems) {
                    var innerLineNum = 0
                    var loadingListItem = ListItem("null", 0, 0, 0)
                    when (lineNum) {
                        0 -> loadingItem.name = line.substringBeforeLast('\n')
                        1 -> loadingItem.position = line.toInt()
                        2 -> loadingItem.parent = line.substringBeforeLast('\n')
                        3 -> loadingItem.type = line.substringBeforeLast('\n')
                        4 -> loadingItem.timeCompleted = line.toLong()
                        else -> {
                            while(line != EndItem && line != EndItems) {
                                when(innerLineNum) {
                                    0 -> loadingListItem.name = line.substringBeforeLast('\n')
                                    1 -> loadingListItem.position = line.toInt()
                                    2 -> loadingListItem.completed = line.toInt()
                                    3 -> {
                                        loadingListItem.max = line.toInt()
                                        innerLineNum = -1
                                        loadingItem.items.add(loadingListItem)
                                        loadingListItem = ListItem("null", 0, 0, 0)
                                    }
                                }
                                innerLineNum++
                                line = reader.readLine()
                            }
                        }
                    }
                    lineNum++
                    if(line != EndItems && loadingItem.items.isEmpty()) line = reader.readLine()
                }
                if(loadingItem.name != "null") {
                    items.add(loadingItem)
                    Log.i("LOAD", "Loaded item: $loadingItem")
                    if(loadingItem.parent == path.substringAfterLast('/')) {
                        activeItems.add(loadingItem)
                    }
                }
                if(line != EndItems) line = reader.readLine()
            }
            reader.close()
        }
    } catch (e: IOException) {
        e.printStackTrace()
        Toast.makeText(context, "Error loading items!", Toast.LENGTH_SHORT).show()
    }
}

private fun saveSettings(context: Context, autoDelListTime: String, autoDelListSelect: String, moveCompleteItemsSelect: String, completedColor: Color) {
    try {
        val fileName = "settings.txt"
        val file = File(context.filesDir, fileName)
        val writer = BufferedWriter(FileWriter(file))
        writer.write(autoDelListTime)
        writer.newLine()
        writer.write(autoDelListSelect)
        writer.newLine()
        writer.write(moveCompleteItemsSelect)
        writer.newLine()
        writer.write(completedColor.red.toString())
        writer.newLine()
        writer.write(completedColor.green.toString())
        writer.newLine()
        writer.write(completedColor.blue.toString())
        writer.close()
    } catch (e: IOException) {
        e.printStackTrace()
        Toast.makeText(context, "Error saving settings!", Toast.LENGTH_SHORT).show()
    }
}

private fun loadSettings(context: Context): List<String> {
    return try {
        val fileName = "settings.txt"
        val file = File(context.filesDir, fileName)
        if (!file.exists()) {
            emptyList()
        } else {
            file.readLines()
        }
    } catch (e: IOException) {
        e.printStackTrace()
        Toast.makeText(context, "Error loading settings!", Toast.LENGTH_SHORT).show()
        emptyList()
    }
}