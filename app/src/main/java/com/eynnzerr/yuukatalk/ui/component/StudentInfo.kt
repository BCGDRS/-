package com.eynnzerr.yuukatalk.ui.component

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.eynnzerr.yuukatalk.data.model.Character
import com.eynnzerr.yuukatalk.R

@Composable
fun StudentInfo(
    student: Character,
    modifier: Modifier = Modifier,
    onPickAvatar: () -> Unit = {},
) {
    var expand by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    Column {
        Row(
            modifier = modifier
                .padding(16.dp)
                .fillMaxWidth()
                .clickable {
                    expand = !expand
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(student.getAvatarPaths(context)[selectedIndex])
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .size(64.dp, 64.dp)
                    .clip(MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = student.school,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            SchoolLogo(school = student.school)
        }

        AnimatedVisibility(
            visible = expand,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            LazyRow (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                itemsIndexed(
                    items = student.getAvatarPaths(context),
                    key = { index, _ -> index }
                ) { index, path ->
                    StudentAvatar(
                        url = path,
                        withBorder = index == selectedIndex,
                        isSelected = index == selectedIndex,
                        size = 48.dp,
                        onClick = {
                            student.currentAvatar = path
                            selectedIndex = index
                            onPickAvatar()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SchoolLogo(school: String) {
    val painter = when (school) {
        School.ABYDOS -> painterResource(id = R.drawable.school_logo_abydos)
        School.GEHENNA -> painterResource(id = R.drawable.school_logo_gehenna)
        School.HYAKKIYAKO -> painterResource(id = R.drawable.school_logo_hyakkiyako)
        School.MILLENNIUM -> painterResource(id = R.drawable.school_logo_millennium)
        School.RED_WINTER -> painterResource(id = R.drawable.school_logo_redwinter)
        School.SHAN_HAI_JING -> painterResource(id = R.drawable.school_logo_shanhaijing)
        School.SRT -> painterResource(id = R.drawable.school_logo_srt)
        School.TRINITY -> painterResource(id = R.drawable.school_logo_trinity)
        School.VALKYRIE -> painterResource(id = R.drawable.school_logo_valkyrie)
        School.ARIUS -> painterResource(id = R.drawable.student_logo_arius)
        else -> painterResource(id = R.drawable.school_logo_federal)
    }

    Image(
        painter = painter,
        contentDescription = "school logo",
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(56.dp)
    )
}

@Preview(
    name = "StudentInfo",
    showBackground = true
)
@Composable
fun StudentInfoPreview() {
    val student = Character(
        name = "优香",
        nameRoma = "Yuuka",
        school = "Millennium",
        avatarPath = "millennium/yuuka/avatar",
        emojiPath = "millennium/yuuka/emoji",
    )

    StudentInfo(student = student) {
        Log.d(TAG, "StudentInfoPreview: student avatar path: ${student.currentAvatar}")
    }
}

object School {
    const val ABYDOS = "Abydos"
    const val FEDERAL = "Federal"
    const val GEHENNA = "Gehenna"
    const val HYAKKIYAKO = "Hyakkiyako"
    const val MILLENNIUM = "Millennium"
    const val RED_WINTER = "Red Winter"
    const val SHAN_HAI_JING = "Shan Hai Jing"
    const val SRT = "SRT"
    const val TRINITY = "Trinity"
    const val VALKYRIE = "Valkyrie"
    const val ARIUS = "Arius"
}

private const val TAG = "StudentInfo"