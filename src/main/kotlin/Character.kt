//interface CharacterFactory {
//    fun createRandomCharacter(): Character
//}
//
//class EmperorFactory : CharacterFactory {
//    private val characterList = mutableListOf<Character>()
//
//    init {
//        characterList.add(Character.createCharacter("Liu Bei"))
//        characterList.add(Character.createCharacter("Cao Cao"))
//        characterList.add(Character.createCharacter("Sun Quan"))
//    }
//
//    override fun createRandomCharacter(): Character {
//        if (characterList.isEmpty()) {
//            throw IllegalStateException("No more characters")
//        }
//
//        val randomSelectIndex = (0 until characterList.size).random()
//        val selectedCharacter = characterList[randomSelectIndex]
//        characterList.removeAt(randomSelectIndex)
//        return selectedCharacter
//    }
//}
//
//class RegularFactory : CharacterFactory {
//    private val characterList = mutableListOf<Character>()
//
//    init {
//        characterList.add(Character.createCharacter("Kwan Yue"))
//        characterList.add(Character.createCharacter("Chow Yue"))
//        characterList.add(Character.createCharacter("Diao Chan"))
//    }
//
//    override fun createRandomCharacter(): Character {
//        if (characterList.isEmpty()) {
//            throw IllegalStateException("No more characters")
//        }
//
//        val randomSelectIndex = (0 until characterList.size).random()
//        val selectedCharacter = characterList[randomSelectIndex]
//        characterList.removeAt(randomSelectIndex)
//        return selectedCharacter
//    }
//}
//
//interface Card {
//    fun getEmoji(): String
//}
//
//abstract class Character : Card {
//    open var health: Int = 3
//    open var name: String = ""
//
//    open fun drawingPhase() {
//        println("Drawing 2 cards")
//    }
//
//    open fun discardPhase() {
//        println("My health point is now $health. Discard cards to match my current health point.")
//    }
//
//    companion object {
//        fun createCharacter(name: String): Character {
//            return when (name) {
//                "Kwan Yue" -> KwanYue()
//                "Chow Yue" -> ChowYue()
//                "Diao Chan" -> DiaoChan()
//                "Liu Bei" -> LiuBei()
//                "Cao Cao" -> CaoCao()
//                "Sun Quan" -> SunQuan()
//                else -> throw IllegalArgumentException("Unknown character")
//            }
//        }
//    }
//}
//
//object Singleton {
//    val characterList = mutableListOf<Character>()
//    private val emperorFactory = EmperorFactory()
//    private val regularFactory = RegularFactory()
//
//    init {
//        for (i in 0 until 2) {
//            characterList.add(emperorFactory.createRandomCharacter())
//        }
//        for (i in 0 until 3) {
//            characterList.add(regularFactory.createRandomCharacter())
//        }
//    }
//}
//
//class KwanYue : Character() {
//    override var health: Int = 4
//    override var name: String = "Kwan Yue"
//
//    override fun discardPhase() {
//        println("My health point is now 4. Discard cards to match my current health point.")
//    }
//
//    override fun getEmoji(): String {
//        return "\uD83C\uDF1A"
//    }
//}
//
//class ChowYue : Character() {
//    override var name: String = "Chow Yue"
//
//    override fun drawingPhase() {
//        println("I'm handsome, so I can draw 3 cards")
//    }
//
//    override fun getEmoji(): String {
//        return "\uD83D\uDCAA"
//    }
//}
//
//class DiaoChan : Character() {
//    override var name: String = "Diao Chan"
//
//    override fun discardPhase() {
//        super.discardPhase()
//        println("I can draw one more card because the moon allows me to do so.")
//    }
//
//    override fun getEmoji(): String {
//        return "\uD83C\uDF1A"
//    }
//}
//
//class LiuBei : Character() {
//    override var health: Int = 5
//    override var name: String = "Liu Bei"
//
//    override fun getEmoji(): String {
//        return "\uD83C\uDF1A"
//    }
//}
//
//class CaoCao : Character() {
//    override var health: Int = 5
//    override var name: String = "Cao Cao"
//
//    override fun getEmoji(): String {
//        return "\uD83C\uDF1A"
//    }
//}
//
//class SunQuan : Character() {
//    override var health: Int = 5
//    override var name: String = "Sun Quan"
//
//    override fun getEmoji(): String {
//        return "\uD83C\uDF1A"
//    }
//}
//
//fun main() {
//    val characterList = Singleton.characterList
//    characterList.forEach {
//        println("${it.name}'s turn:")
//        it.drawingPhase()
//        it.discardPhase()
//        println(it.getEmoji())
//    }
//}
