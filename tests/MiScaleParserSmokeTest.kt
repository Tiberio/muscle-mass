import br.com.meutreino.devices.MiScaleParser

fun main(){
    fun frame(flags:Int,weight:Int=14000):ByteArray = ByteArray(13).also{ b ->
        b[0]=flags.toByte(); b[1]=(flags shr 8).toByte()
        b[2]=0xea.toByte(); b[3]=0x07
        b[4]=10; b[5]=19; b[11]=weight.toByte(); b[12]=(weight shr 8).toByte()
    }
    check(MiScaleParser.parse(frame(0x0440))?.kilograms==70.0)
    check(MiScaleParser.parse(frame(0x0040))==null)
    check(MiScaleParser.parse(frame(0x0540))==null)
    check(MiScaleParser.parse(frame(0x04c0))==null)
    check(MiScaleParser.parse(byteArrayOf(1,2))==null)
    check(MiScaleParser.parse(frame(0x0440,0))==null)
    println("6 testes BLE Mi Scale aprovados")
}
