package com.thinkuldeep.sdui.client.renderer

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinkuldeep.sdui.client.model.UiComponent
import com.thinkuldeep.sdui.client.viewmodel.LandingViewModel
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun Render(component: UiComponent, viewModel: LandingViewModel) {

    when (component) {

        is UiComponent.Column -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                component.children.forEach {
                    Render(it, viewModel)
                }
            }
        }

        is UiComponent.Row -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                component.children.forEach {
                    Render(it, viewModel)
                }
            }
        }

        is UiComponent.Text -> {
            Text(
                text = component.value,
                fontSize = when (component.size) {
                    "large" -> 24.sp
                    "medium" -> 18.sp
                    else -> 14.sp
                },
                fontWeight = when (component.weight) {
                    "bold" -> FontWeight.Bold
                    "medium" -> FontWeight.Medium
                    else -> FontWeight.Normal
                },
                modifier = Modifier.padding(8.dp)
            )
        }

        is UiComponent.Image -> {
            KamelImage(
                resource = asyncPainterResource(component.url),
                contentDescription = null,
                modifier = Modifier
                    .padding(8.dp)
                    .then(
                        if (component.width != null && component.height != null)
                            Modifier.size(component.width.dp, component.height.dp)
                        else Modifier
                    )
            )
        }

        is UiComponent.Button -> {
            Button(
                onClick = {
                    viewModel.dispatch(component.action, component.id)
                },
                modifier = Modifier.padding(8.dp)
            ) {
                Text(component.value)
            }
        }

        is UiComponent.FeaturedItems -> {
            Render(component.button, viewModel)

            val firstChild = component.children.firstOrNull()

            firstChild?.let {
                Render(it, viewModel)
            }
        }
    }
}