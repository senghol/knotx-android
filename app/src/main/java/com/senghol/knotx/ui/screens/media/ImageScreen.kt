package com.senghol.knotx.ui.screens.media
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.ExperimentalFoundationApi
import com.senghol.knotx.data.database.Sandesh
import com.senghol.knotx.utils.enums.SandeshType

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageScreen(
    sandeshList: List<Sandesh>
) {
    // Filter for IMAGE type
    val imageSandeshList = sandeshList.filter { it.sandeshType == SandeshType.IMAGE.value }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 128.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(imageSandeshList) { sandesh ->
            Card(
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.elevatedCardElevation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                val imageUri = sandesh.samagri // Assuming samagri holds image URL or path
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
