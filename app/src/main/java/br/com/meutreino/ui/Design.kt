package br.com.meutreino.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val Midnight=Color(0xFF071729)
val Panel=Color(0xFF102840)
val PanelLight=Color(0xFF15395F)
val Blue=Color(0xFF1685FF)
val LightBlue=Color(0xFF75BCFF)
val Muted=Color(0xFFACC5DE)

@Composable fun MeuTreinoTheme(content: @Composable ()->Unit) {
    MaterialTheme(colorScheme=darkColorScheme(
        primary=Blue,onPrimary=Color.White, secondary=LightBlue,
        background=Midnight,onBackground=Color.White,
        surface=Panel,onSurface=Color.White, surfaceVariant=PanelLight,
        outline=Color(0xFF34506B)
    ), typography=Typography(),content=content)
}
@Composable fun Pane(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){
    Column(modifier=modifier.fillMaxWidth().background(Panel,RoundedCornerShape(16.dp)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
}
@Composable fun Title(text:String){Text(text,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,color=Color.White)}
@Composable fun Subtext(text:String){Text(text,style=MaterialTheme.typography.bodySmall,color=Muted)}
@Composable fun PrimaryButton(text:String,enabled:Boolean=true,onClick:()->Unit){
    Button(onClick=onClick,modifier=Modifier.fillMaxWidth().heightIn(min=50.dp),enabled=enabled,shape=RoundedCornerShape(12.dp)) { Text(text) }
}
@Composable fun OutlineButton(text:String,onClick:()->Unit){
    OutlinedButton(onClick=onClick,modifier=Modifier.fillMaxWidth().heightIn(min=46.dp),shape=RoundedCornerShape(12.dp)) {Text(text)}
}
@Composable fun Choice(text:String,selected:Boolean,onClick:()->Unit,modifier:Modifier=Modifier){
    OutlinedButton(onClick=onClick,modifier=modifier.heightIn(min=46.dp),shape=RoundedCornerShape(10.dp),
        border=BorderStroke(1.dp,if(selected)Blue else Color(0xFF35516E)),
        colors=ButtonDefaults.outlinedButtonColors(containerColor=if(selected)PanelLight else Midnight,contentColor=Color.White)){
        Text(if(selected)"✓ $text" else text,maxLines=2)
    }
}
@Composable fun SpacerSmall(){Spacer(Modifier.height(4.dp))}
