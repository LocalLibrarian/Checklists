package com.travisshirley.checklists

import android.content.ClipData
import android.content.ClipboardManager
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
import com.example.checklists.R
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
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
/*Values defining temp path strings to avoid errors when moving items or forcing screen redraws*/
const val TempPath = "path_FIX"
const val TempPath2 = "path_FIX2"
/*Values defining font sizes*/
val titleFontSize = 22.sp
val subTitleFontSize = 20.sp

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

/*Main init func, calls most others and draws the main screen and its layout*/
@Composable
@Preview
fun MainScreen() {
    var selectedItemIndex by remember { mutableStateOf(-1) }
    var selectedListItemIndex by remember { mutableStateOf(-1)}
    var inList by remember{mutableStateOf(false)}
    val activeItems = mutableListOf<GenericItem>()
    var path by remember{mutableStateOf("All items")}
    val items = mutableListOf<GenericItem>()
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
    var completedColor = Color.Green
    val settingsSaved= loadSettings(context)
    var choseTutorial by remember{ mutableStateOf(false) }
    var firstTime by remember{ mutableStateOf(true) }
    if (settingsSaved.isNotEmpty()) {
        autoDelListTime = settingsSaved[0]
        autoDelListSelect = settingsSaved[1]
        moveCompleteItemsSelect = settingsSaved[2]
        completedColor = Color(red = settingsSaved[3].toFloat(), green = settingsSaved[4].toFloat(), blue = settingsSaved[5].toFloat())
    }
    Log.i("INIT", "Completed full init of global vars")
    Log.i("SETTINGS","Settings now: $autoDelListTime, $autoDelListSelect, $moveCompleteItemsSelect, $completedColor")
    Column(modifier = Modifier
        .background(Color.LightGray)
        .fillMaxSize()) {
        Row( /*Beginning of header drawing*/
            Modifier
                .background(Color.Blue)
                .size(width = screenWidth.dp, height = bannerHeight.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Image(
                painter = painterResource(R.drawable.settingsicon),
                contentDescription = "Settings Icon",
                modifier = Modifier
                    .size((bannerHeight / 4).dp)
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
                    .size((bannerHeight / 4).dp)
                    .clickable(enabled = true, onClick = { createOpen = true })
            )
        } /*End of header drawing*/
        Row { /*Drawing path*/
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
        Column( /*Everything else draws from here. Keeps header above everything that way*/
            Modifier
                .verticalScroll(rememberScrollState())
                .background(Color.LightGray)
                .fillMaxSize()) {
            if((loadItems(context, items, path, activeItems) && firstTime) || choseTutorial) {
                TutorialScreen(settingsWidth, settingsHeight, onClose = {
                    choseTutorial = false
                    firstTime = false})
            }
            items.sortBy{it.position}
            var color = Color.LightGray
            var index = 0
            if(selectedItemIndex > -1) { /*Handling clicking something in a folder*/
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
            } else { /*Drawing everything with current path as parent*/
                val toDelete = mutableListOf<GenericItem>()
                items.forEach {
                    if(it.parent == path.substringAfterLast('/')) {
                        var timeUnit = 0L
                        when (autoDelListSelect) {
                            "Minutes" -> {timeUnit = OneMin
                            }
                            "Hours" -> {timeUnit = OneHour
                            }
                            "Days" -> {timeUnit = OneDay
                            }
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
    if (selectedListItemIndex > -1) { /*Handling clicking something in a list*/
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
            items,
            openTutorial = {choseTutorial = true}
        )
    }
    if (createOpen) {
        CreateScreen(settingsWidth, settingsHeight, context, items, onClose = {createOpen = false}, path, activeItems, inList, selectedItemIndex, moveCompleteItemsSelect, onRedraw = {path += "/$TempPath2"})
    }
    /*Forcing screen redraw*/
    if(path.substringAfterLast('/') == TempPath) path = path.substringBeforeLast('/')
}

/*Writes the path clickables onto the screen. Clicking one sets the path to it*/
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

/*Draws the Add new... button screen*/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(settingsWidth: Double, settingsHeight: Double, context: Context, items: MutableList<GenericItem>, onClose: () -> Unit, path: String, activeItems: MutableList<GenericItem>, inList: Boolean, selectedItemIndex: Int, moveCompleteItemsSelect: String, onRedraw: () -> Unit) {
    var newItemName by rememberSaveable { mutableStateOf("New Item") }
    var maxComplete by rememberSaveable { mutableStateOf("1") }
    val itemTypes = arrayOf("Checklist", "Folder")
    var newItemExpanded by remember { mutableStateOf(false) }
    var newItemSelectedType by remember { mutableStateOf(itemTypes[0]) }
    var importText by remember{ mutableStateOf("Import Data...") }
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
                        .size(width = 30.dp, height = 30.dp)
                )
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Text(text = "Create New",
                    fontSize = subTitleFontSize
                )
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
                    onClick = { createNewItem(items, newItemName, newItemSelectedType, context, path, activeItems, inList, selectedItemIndex, maxComplete.toInt(), moveCompleteItemsSelect, onRedraw = {onRedraw()}) }
                ) {
                    Text("Create")
                }
                if(!inList) {
                    Button(onClick = { importItems(importText, items, activeItems, path, context) }) {
                        Text(text = "Import")
                    }
                    TextField(
                        value = importText,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = false,
                        onValueChange = {
                            importText = it
                        }
                    )
                }
            }
        }
    }
}

/*Imports items from string, basically just edited loadItems func*/
private fun importItems(toImport: String, items: MutableList<GenericItem>, activeItems: MutableList<GenericItem>, path: String, context: Context) {
    val lines = toImport.split('\n')
    var i = 0
    var line = lines[i]
    var updated = false
    while (i < lines.size && line != EndItems) {
        while (line != EndItems) {
            var lineNum = 0
            val loadingItem = GenericItem("null", 0, "All items", "checklist", 0L, mutableListOf())
            while (line != EndItem && line != EndItems) {
                var innerLineNum = 0
                var loadingListItem = ListItem("null", 0, 0, 0)
                when (lineNum) {
                    0 -> loadingItem.name = line.substringBeforeLast('\n')
                    1 -> loadingItem.position = activeItems.lastIndex + 1
                    2 -> loadingItem.parent = line.substringBeforeLast('\n')
                    3 -> loadingItem.type = line.substringBeforeLast('\n')
                    4 -> loadingItem.timeCompleted = line.toLong()
                    else -> {
                        while (line != EndItem && line != EndItems) {
                            when (innerLineNum) {
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
                            i++
                            line = lines[i]
                        }
                    }
                }
                lineNum++
                if (line != EndItems && loadingItem.items.isEmpty()) {
                    i++
                    line = lines[i]
                }
            }
            if (loadingItem.name != "null") {
                items.add(loadingItem)
                Log.i("IMPORT", "Imported item: $loadingItem")
                updated = true
                if (loadingItem.parent == path.substringAfterLast('/')) {
                    activeItems.add(loadingItem)
                }
            }
            if (line != EndItems) {
                i++
                line = lines[i]
            }
        }
    }
    if(updated) saveItems(context, items)
}

/*Draws Settings button screen*/
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
    itemsList: MutableList<GenericItem>,
    openTutorial: () -> Unit
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
                        .size(width = 30.dp, height = 30.dp)
                )
            }
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {
                Text(text = "Settings",
                    fontSize = subTitleFontSize
                )
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
                    onClose()
                    openTutorial()
                }) {
                    Text(text = "Open Tutorial")
                }
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

/*Child of DrawItems. Draws icon for GenericItem or complete/max for ListItems and writes name of any item*/
@Composable
fun DrawItemIcon(item: GenericItem, index: Int, inList: Boolean) {
    if(inList) {
        Text(text = "${item.items[index].completed} / ${item.items[index].max}")
    }
    else if(item.type == "checklist") {
        Image(
            painter = painterResource(R.drawable.listicon),
            contentDescription = "List Icon",
            Modifier
                .padding(10.dp)
                .size(30.dp)
        )
    } else{
        Image(
            painter = painterResource(R.drawable.foldericon),
            contentDescription = "Folder Icon",
            Modifier
                .padding(10.dp)
                .size(30.dp)
        )
    }
    Text(text = if(!inList) item.name else item.items[index].name,
        Modifier.padding(10.dp))
}

/*Returns true if passed list has all tasks fully completed, false otherwise*/
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

/*Copies data for an item to clipboard so it can be sent to someone else and pasted into an import.
Functions similarly to saveItems*/
fun exportItems(activeItems: MutableList<GenericItem>, index: Int, context: Context) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    var copy = activeItems[index].name + '\n' + activeItems[index].position.toString() + '\n' + activeItems[index].parent + '\n' +
            activeItems[index].type + '\n' + activeItems[index].timeCompleted.toString()
    activeItems[index].items.forEach {
        copy += '\n' + it.name + '\n' + it.position.toString() + '\n' + it.completed.toString() + '\n' + it.max.toString()
    }
    copy += '\n' + EndItem
    copy += '\n' + EndItems
    val clip = ClipData.newPlainText("test", copy)
    clipboardManager.setPrimaryClip(clip)
}

/*Draws either GenericItems or ListItems depending on inList value. Draws 1 item at a time, must be called multiple times to draw all.
Also handles item dragging/movement and rearranging and calls func to export items*/
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
        Box(modifier = Modifier.width(20.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier =
        Modifier
            .background(realColor)
            .size(width = screenWidth.dp - 40.dp, height = bannerHeight.dp)
            .combinedClickable(
                onClick = { onItemSelect(index) },
                onLongClick = { editVisible = true })
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = { /*Drag handling*/
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
        Box(modifier = Modifier.width((screenWidth - 155).dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start) {
                DrawItemIcon(item, index, inList)
            }
        }
        if (editVisible) {
            ShowEditButton(
                onClick = { editOpen = true },
                on2ndClick = { editVisible = false },
                bannerHeight,
                activeItems,
                index,
                context)
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

/*Draws Edit..., Hide and Share buttons on items*/
@Composable
fun ShowEditButton(onClick: () -> Unit, on2ndClick: () -> Unit, bannerHeight: Double, activeItems: MutableList<GenericItem>, index: Int, context: Context) {
    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { onClick() }) {
                Text(text = "Edit...")
            }
            Button(onClick = { on2ndClick() }) {
                Text(text = "Hide")
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxHeight()) {
            Image(
                painter = painterResource(R.drawable.shareicon),
                contentDescription = "Share Item",
                modifier = Modifier
                    .size((bannerHeight / 2.5).dp)
                    .clickable(enabled = true, onClick = { exportItems(activeItems, index, context) })
            )
        }
    }
}

/*Draws the tutorial screen and handles its pages changing*/
@Composable
fun TutorialScreen(settingsWidth: Double, settingsHeight: Double, onClose: () -> Unit) {
    var pageNum by remember{ mutableStateOf(0) }
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
                        .size(width = 30.dp, height = 30.dp)
                )
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Text(text = "Tutorial",
                    fontSize = subTitleFontSize
                )
                Box(
                    modifier = Modifier
                        .size(width = settingsWidth.dp, height = 5.dp)
                        .background(Color(204, 204, 204, 255))
                )
                Column(modifier = Modifier
                    .verticalScroll(rememberScrollState())) {
                    var subTitle = "null"
                    var tutText = "null"
                    when (pageNum) {
                        0 -> {
                            subTitle = "Welcome to Checklists!"
                            tutText =
                                ("It looks like this is your first time using the app. Continue reading to learn how to use the app, or, if you prefer, close this tutorial "
                                        + "by tapping on the \"X\" icon in the top-right corner. You can navigate the pages of this tutorial by tapping on the \"Next Page\" and \"Previous Page\" buttons on the bottom.\n\n" +
                                        "You can also re-open this tutorial at any time via the \"Settings\" menu that is always on the top-left of the screen.\n\n" +
                                        "We will begin on the next page by going over the main screen behind this Tutorial box.")
                        }

                        1 -> {
                            subTitle = "Main Screen"
                            tutText =
                                ("The Main Screen is where you do almost everything in Checklists. From this screen, you can tap on lists and folders to " +
                                        "open them and view their contents.\n\nTo go back from a folder or list, tap on where you would like to go to on the text " +
                                        "that says \"All items/example folder/list 1\". For example, to go back to the \"example folder\", tap on \"example folder\", " +
                                        "and the same for \"All items\".\n\nWhen in a list, you simply tap on tasks to add 1 to their completion counter. Once they " +
                                        "reach their max completion, they are marked as complete by having a different colored background and possibly changing where they " +
                                        "are in the list, depending on your settings. Lists with all tasks completed also color differently and will delete after some " +
                                        "time, depending on settings.\n\nYou can also tap and hold on tasks/lists/folders to edit them after tapping \"Edit...\". This " +
                                        "allows you to change their name, what folder/list they belong to, and current completion/max completeness of tasks, as " +
                                        "well as delete items.\n\nNOTE: when typing in many text boxes in this app, you may have to tap and hold to select all " +
                                        "text/numbers in the box, then start typing in order to properly enter what information you want.\n\nLastly, you can also " +
                                        "drag lists and tasks to re-order them on screen.")
                        }

                        2 -> {
                            subTitle = "Settings"
                            tutText = ("In the Settings menu, accessed via the button in the top-left, you can change how various things in the app work. "
                                    + "The \"Auto-delete completed lists after\" setting " +
                                    "controls how long to wait until completed lists are automatically deleted. You can type how many units " +
                                    "of time you want to wait, and then select which unit (Minutes, Hours, or Days) to wait in that time for.\n\nYou can also " +
                                    "choose where to move completed list items to. You can either automatically move them to the Top or Bottom of the list, " +
                                    "or choose not to move them at all. This also affects where newly-created items are placed in the list.\n\nYou may also " +
                                    "change what color completed items change to via RGB values with a preview of the color on the Settings screen.\n\nThis " +
                                    "tutorial can be re-opened on the Settings screen as well.\n\nLastly, you may delete all lists, folders, and tasks with the " +
                                    "\"Wipe All Data\" button.")
                        }

                        3 -> {
                            subTitle = "Add new..."
                            tutText = ("The Add New screen, accessed via the button in the top-right, is where you create new lists, folders, and " +
                                    "tasks/list items. When this button is tapped on and you are not in a list, you can name and create a new list or folder " +
                                    "in the current folder (or add it to \"All items\").\n\nWhen in a list, you name the task you are adding and define its " +
                                    "maximum completeness, which is how many times it can be tapped on before being marked as complete.\n\nClick create when " +
                                    "done in the Add New screen to create the new item. You can create multiple items without closing the screen, but they " +
                                    "need to have unique names across all lists/folders for lists and folders, and unique names in the current list for " +
                                    "tasks.")
                        }

                        4 -> {
                            subTitle = "End"
                            tutText = ("That should cover everything! Remember, if you need to access the tutorial again at any point, you can do so from the " +
                                    "Settings menu.")
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = subTitle)
                    }
                    Text(text = tutText)
                    Column(
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (pageNum > 0) {
                                Button(onClick = { pageNum-- }) {
                                    Text(text = "Previous Page")
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (pageNum < 4) {
                                    Button(onClick = { pageNum++ }) {
                                        Text(text = "Next Page")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/*Draws the Edit... button screen*/
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
                .size(width = settingsWidth.dp, height = (settingsHeight * 0.75).dp)
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
                        .size(width = 30.dp, height = 30.dp)
                )
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Text(text = "Edit Item",
                    fontSize = subTitleFontSize
                )
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
                                                val illegalNames = mutableListOf<String>()
                                                items[i].items.forEach { illegalNames.add(it.name) }
                                                if(item.items[index].name in illegalNames) {
                                                    Toast.makeText(context, "Names must be unique. That name is already in use in the targeted list.", Toast.LENGTH_LONG).show()
                                                    break
                                                }
                                                else {
                                                    for (j in index until item.items.size) {
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
                                                            item.items[index].position =
                                                                items[i].items.size
                                                            items[i].items.add(item.items[index])
                                                        }
                                                    }
                                                    item.items.removeAt(index)
                                                    items[i].items.sortBy { it.position }
                                                    break
                                                }
                                            }
                                            onClose()
                                        }
                                        parentExpanded = false
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

/*Deletes completed lists if enough time has passed according to settings*/
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

/*Iterate over current list and perform actions on completed tasks according to settings. Do same for handling completed lists*/
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
            }
        }
        activeItems[selectedItemIndex].items.sortBy{it.position}
        onRedraw()
    }
    if(isCompletedList(activeItems[selectedItemIndex])) {
        activeItems[selectedItemIndex].timeCompleted = System.currentTimeMillis()
    }
    saveItems(context, itemsList)
}

/*Rebuilds activeItems list by getting parent of current end of path. For use in lists after updates, mainly*/
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

/*Creates a new item, can be GenericItem or ListItem depending on inList value*/
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
    moveCompleteItemsSelect: String,
    onRedraw: () -> Unit
) {
    val illegalNames = mutableListOf<String>() /*List built of other names that would cause issues if duplicated*/
    if(inList) {
        activeItems[selectedItemIndex].items.forEach { illegalNames.add(it.name) }
        if(named in illegalNames) {
            Toast.makeText(context, "Names must be unique. That name is already in use.", Toast.LENGTH_LONG).show()
        }
        else {
            if (moveCompleteItemsSelect == "Bottom") {
                activeItems[selectedItemIndex].items.forEach {
                    it.position += 1
                }
                activeItems[selectedItemIndex].items.add(ListItem(name = named, 0, 0, max))
                activeItems[selectedItemIndex].items.sortBy { it.position }
            } else activeItems[selectedItemIndex].items.add(
                ListItem(
                    name = named,
                    activeItems[selectedItemIndex].items.lastIndex + 1,
                    0,
                    max
                )
            )
            saveItems(context, itemsList)
            Toast.makeText(
                context,
                "Added task: $named to ${activeItems[selectedItemIndex].name}",
                Toast.LENGTH_SHORT
            ).show()
            onRedraw()
        }
    } else{
        itemsList.forEach { illegalNames.add(it.name) }
        if(named in illegalNames) {
            Toast.makeText(context, "Names must be unique. That name is already in use.", Toast.LENGTH_LONG).show()
        }
        else {
            itemsList.add(
                GenericItem(
                    name = named,
                    position = activeItems.lastIndex + 1,
                    parent = path.substringAfterLast('/'),
                    type = typed.lowercase(),
                    timeCompleted = 0L,
                    items = mutableListOf()
                )
            )
            saveItems(context, itemsList)
            Toast.makeText(
                context,
                "Added item: $named to ${path.substringAfterLast('/')}",
                Toast.LENGTH_SHORT
            ).show()
            rebuildActiveItems(itemsList, activeItems, path)
        }
    }
}

/*Saves itemsList to file. Called whenever change occurs so user does not lose data on app close*/
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
        Log.w("SAVE", "Could not save items")
    }
}

/*Loads items from file. Should only be called at beginning of program when items is empty in memory*/
private fun loadItems(context: Context, items: MutableList<GenericItem>, path: String, activeItems: MutableList<GenericItem>): Boolean {
    try {
        val fileName = "items.txt"
        val file = File(context.filesDir, fileName)
        val reader = BufferedReader(FileReader(file))
        if (!file.exists()) {
            Log.w("LOAD", "No saved items found, could be first app use?")
            reader.close()
            return true
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
            return false
        }
    } catch (e: IOException) {
        e.printStackTrace()
        Log.w("LOAD", "No saved items found, could be first app use?")
        return true
    }
}

/*Saves settings data to file. Called when settings values change in settings screen*/
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
        Log.w("SAVE", "Could not save settings")
    }
}

/*Loads settings data from file. If returns nothing, settings get set to default values. Should only be called at beginning of program*/
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
        Log.w("LOAD", "No saved settings found")
        emptyList()
    }
}