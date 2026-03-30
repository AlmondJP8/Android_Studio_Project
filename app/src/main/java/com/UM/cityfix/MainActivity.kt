package com.UM.cityfix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.UM.cityfix.components.AppNav
import com.UM.cityfix.components.Header
import com.UM.cityfix.components.Logo
import com.UM.cityfix.components.MainBG
import com.UM.cityfix.components.button
import com.UM.cityfix.components.contentText
import com.UM.cityfix.components.tutorialBox
import com.UM.cityfix.ui.theme.CityFixTheme
import com.UM.cityfix.ui.theme.appName
import com.UM.cityfix.ui.theme.buttonText
import com.UM.cityfix.ui.theme.conttext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CityFixTheme (darkTheme = false){
                val navController = rememberNavController()
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav(navController = navController)
                }
            }
        }
    }
}
data class TutorialStep(val text: String, val imageRes: Int)

@Composable
fun Greeting(navController: NavHostController? = null, modifier: Modifier = Modifier) {
    val steps = listOf(
        TutorialStep("Create an Account", R.drawable.pic_tutor1),
        TutorialStep("Login to the App", R.drawable.pic_tutor2),
        TutorialStep("Complete the Form & Submit", R.drawable.pic_tutor3),
    )

    //the entire phone
    Column(Modifier.MainBG()) {
        //header
        Row(Modifier.Header().fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(id = R.drawable.pic_logo), contentDescription = "Logo", Modifier.Logo(),)
            Text("CITYFIX", style = appName)

            Spacer( Modifier.weight(1f))

            Text("Login", Modifier.button().clickable{navController?.navigate("login")}, style = buttonText)
            Text("Signup", Modifier.button().clickable{navController?.navigate("signup")}, style = buttonText)
        }
        //tutorial
        LazyColumn(modifier = Modifier.padding(5.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(steps.size) { index ->
                val currentStep = steps[index] // Get the whole object
                Column(modifier = Modifier.tutorialBox(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = currentStep.imageRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Text(
                        text = currentStep.text,
                        style = conttext,
                        modifier = Modifier.contentText()
                    )
                }
            }
        }

    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CityFixTheme {
        Greeting()
    }
}