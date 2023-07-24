package com.example.checklists

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

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
    var items: MutableList<ListItem>
)

/*Value defining what percentage of the screen height the banner and task item entries should take up*/
const val BannerPercent = 0.1
/*Value defining what percentage of the screen width/height the settings should take up*/
const val SettingsPercent = 0.5
/*Values defining end of item separator and end of item file*/
const val EndItem = "ENDITEMSEP"
const val EndItems = "ENDOFITEMS"

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun MainScreen() {
    var selectedItemIndex by remember { mutableStateOf(-1) }
    var selectedListItemIndex by remember { mutableStateOf(-1)}
    var inList by remember{mutableStateOf(false)}
    var activeItems = mutableListOf<GenericItem>()
    var path by remember{mutableStateOf("All items")}
    var items = mutableListOf<GenericItem>()
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
    val settingsSaved= loadSettings(context)
    if (settingsSaved.isNotEmpty()) {
        autoDelListTime = settingsSaved[0]
        autoDelListSelect = settingsSaved[1]
        moveCompleteItemsSelect = settingsSaved[2]
    }
    Log.i("SETTINGS","Settings now: $autoDelListTime, $autoDelListSelect, $moveCompleteItemsSelect")
    Column(
        Modifier
            .background(Color.LightGray)
            .fillMaxHeight()) {
        Row(
            Modifier
                .background(Color.Blue)
                .size(width = screenWidth.dp, height = bannerHeight.dp)
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
            Text(
                text = "Checklists",
                modifier = Modifier.weight(1f),
                color = Color.Cyan
            )
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
        )
        Column(Modifier.verticalScroll(rememberScrollState())) {
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
                    activeItems[selectedItemIndex].items.forEach {
                    drawItems(
                        activeItems[selectedItemIndex],
                        screenWidth,
                        bannerHeight,
                        onItemSelect = { listItemSelected ->
                            selectedListItemIndex = listItemSelected
                        },
                        color,
                        index,
                        inList
                    )
                    index++
                    color = if (color == Color.LightGray) Color.Gray else Color.LightGray
                }
                }
            } else {
            items.forEach {
                if(it.parent == path.substringAfterLast('/')) {
                    drawItems(
                        it,
                        screenWidth,
                        bannerHeight,
                        onItemSelect = { itemSelected -> selectedItemIndex = itemSelected },
                        color, index, inList
                    )
                    index++
                    color = if (color == Color.LightGray) Color.Gray else Color.LightGray
                }
            }
            }
        }
    }
    if (selectedListItemIndex > -1) {
        Log.d("SELECT", "Selected listItem index: $selectedListItemIndex")
        activeItems[selectedItemIndex].items[selectedListItemIndex].completed += 1
        saveItems(context, items, path, activeItems)
        selectedListItemIndex = -1
    }
    if (settingsOpen) {
        settingsScreen(settingsWidth, settingsHeight, context, onClose = {settingsOpen = false}, onDelTimeChange = {newTime -> autoDelListTime = newTime},
            onDelListSelectChange = {newDelListSelect -> autoDelListSelect = newDelListSelect}, onMoveCompleteItemsSelectChange = {newMoveCompSelect -> moveCompleteItemsSelect = newMoveCompSelect},
            autoDelListTime, autoDelListSelect, moveCompleteItemsSelect)
    }
    if (createOpen) {
        createScreen(settingsWidth, settingsHeight, context, items, onClose = {createOpen = false}, path, activeItems, inList, selectedItemIndex)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun createScreen(settingsWidth: Double, settingsHeight: Double, context: Context, items: MutableList<GenericItem>, onClose: () -> Unit, path: String, activeItems: MutableList<GenericItem>, inList: Boolean, selectedItemIndex: Int) {
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
                    onClick = { createNewItem(items, newItemName, newItemSelectedType, context, path, activeItems, inList, selectedItemIndex, maxComplete.toInt()) }
                ) {
                    Text("Create")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun settingsScreen(settingsWidth: Double, settingsHeight: Double, context: Context, onClose: () -> Unit, onDelTimeChange: (String) -> Unit,
                   onDelListSelectChange: (String) -> Unit, onMoveCompleteItemsSelectChange: (String) -> Unit, defDelTime: String, defDelListSelect: String,
                   defMoveCompleteItemsSelect: String){
    var autoDelListTime by rememberSaveable { mutableStateOf(defDelTime) }
    val delUnits = arrayOf("Minutes", "Hours", "Days")
    var autoDelListExpanded by remember { mutableStateOf(false) }
    var autoDelListSelect by remember { mutableStateOf(defDelListSelect) }
    val moveCompleteItems = arrayOf("Bottom", "Top", "Don't Move", "Delete Them")
    var moveCompleteItemsExpanded by remember { mutableStateOf(false) }
    var moveCompleteItemsSelect by remember { mutableStateOf(defMoveCompleteItemsSelect) }
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
                        saveSettings(context, autoDelListTime, autoDelListSelect, moveCompleteItemsSelect)
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
                                    saveSettings(context, autoDelListTime, autoDelListSelect, moveCompleteItemsSelect)
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
                                    saveSettings(context, autoDelListTime, autoDelListSelect, moveCompleteItemsSelect)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun drawItemIcon(item: GenericItem, index: Int, inList: Boolean) {
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

@Composable
fun drawItems(
    item: GenericItem,
    screenWidth: Int,
    bannerHeight: Double,
    onItemSelect: (Int) -> Unit,
    color: Color,
    index: Int,
    inList: Boolean
) {
    var realColor = color
    if(inList && item.items[index].completed >= item.items[index].max) realColor = Color.Green
    if(!inList && isCompletedList(item)) realColor = Color.Green
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier =
        Modifier
            .background(realColor)
            .size(width = screenWidth.dp, height = bannerHeight.dp)
            .clickable { onItemSelect(index) }
    ) {
        drawItemIcon(item, index, inList)
    }
}

private fun rebuildActiveItems(items: MutableList<GenericItem>, activeItems: MutableList<GenericItem>, path: String) {
    var parent: String
    var parentItem = GenericItem("null", 0, "null", "null", mutableListOf<ListItem>())
    items.forEach {
        if(it.name.equals(path.substringAfterLast('/'))) {
            parentItem = it
        }
    }
    parent = parentItem.parent
    items.forEach {
        if(it.parent == parent) activeItems.add(it)
    }
    Log.i("REBUILD", "Rebuilt activeItems: $activeItems")
}

private fun createNewItem(itemsList: MutableList<GenericItem>, named: String, typed: String, context: Context, path: String, activeItems: MutableList<GenericItem>, inList: Boolean, selectedItemIndex: Int, max: Int) {
    if(inList) {
        activeItems[selectedItemIndex].items.add(ListItem(name = named, activeItems[selectedItemIndex].items.lastIndex + 1, 0, max))
        saveItems(context, itemsList, path, activeItems)
        Toast.makeText(context, "Added task: $named to ${activeItems[selectedItemIndex].name}", Toast.LENGTH_SHORT).show()
    } else{
        itemsList.add(GenericItem(
            name = named,
            position = itemsList.lastIndex + 1,
            parent = "All items",
            type = typed.lowercase(),
            items = mutableListOf()
        ))
        saveItems(context, itemsList, path, activeItems)
        Toast.makeText(context, "Added item: $named to All items", Toast.LENGTH_SHORT).show()
    }
}

private fun saveItems(context: Context, itemsList: MutableList<GenericItem>, path: String, activeItems: MutableList<GenericItem>) {
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
    loadItems(context, itemsList, path, activeItems)
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
            while (line != null && line != EndItems) {
                var lineNum = 0
                var loadingItem = GenericItem("null", 0, "All items", "checklist", mutableListOf())
                while(line != null && line != EndItem && line != EndItems) {
                    var innerLineNum = 0
                    var loadingListItem = ListItem("null", 0, 0, 0)
                    when (lineNum) {
                        0 -> loadingItem.name = line.substringBeforeLast('\n')
                        1 -> loadingItem.position = line.toInt()
                        2 -> loadingItem.parent = line.substringBeforeLast('\n')
                        3 -> loadingItem.type = line.substringBeforeLast('\n')
                        else -> {
                            while(line != null && line != EndItem && line != EndItems) {
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

private fun saveSettings(context: Context, autoDelListTime: String, autoDelListSelect: String, moveCompleteItemsSelect: String) {
    try {
        val fileName = "settings.txt"
        val file = File(context.filesDir, fileName)
        val writer = BufferedWriter(FileWriter(file))
        writer.write(autoDelListTime)
        writer.newLine()
        writer.write(autoDelListSelect)
        writer.newLine()
        writer.write(moveCompleteItemsSelect)
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