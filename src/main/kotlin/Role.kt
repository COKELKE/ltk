import java.util.*
import kotlin.math.abs
import kotlin.math.max

interface Role {
    val roleTitle: String
    fun getEnemy(): String
}

class Monarch : Role, Publisher {
    override val roleTitle = "Monarch"
    private var subscribers = mutableListOf<Subscriber>()

    override fun getEnemy(): String {
        return "attack Rebel, then Traitors."
    }

    override fun subscribe(subscriber: Subscriber) {
        subscribers.add(subscriber)
    }

    override fun notifySubscribers(dodged: Boolean, hp: Int, numOfCards: Int) {
        subscribers.forEach {
            it.update(dodged, hp, numOfCards)
        }
    }

    override fun removeSubscriber(subscriber: Subscriber) {
        subscribers.remove(subscriber)
    }
}

class Minister : Role, Subscriber {
    override val roleTitle = "Minister"
    private var dangerLevel: Float = 1.0f

    override fun getEnemy(): String {
        return "attack Rebel, then Traitors."
    }

    override fun update(dodged: Boolean, hp: Int, numOfCards: Int) {
        dangerLevel += if (hp >= 5) {
            1.0f
        } else if (hp >= 3) {
            2.0f
        } else {
            3.0f
        }
        dangerLevel += if (numOfCards >= 5) {
            1.0f
        } else if (numOfCards >= 3) {
            2.0f
        } else {
            3.0f
        }
        println("$roleTitle estimates the danger level as $dangerLevel.")
    }
}

class Rebel : Role {
    override val roleTitle = "Rebel"

    override fun getEnemy(): String {
        return "attack Monarch."
    }
}

class Traitors : Role, Subscriber {
    override val roleTitle = "Traitors"
    private var dangerLevel: Float = 1.0f

    override fun getEnemy(): String {
        return "attack Rebel, then Monarch."
    }

    override fun update(dodged: Boolean, hp: Int, numOfCards: Int) {
        dangerLevel += if (hp >= 5) {
            3.0f
        } else if (hp >= 3) {
            2.0f
        } else {
            1.0f
        }
        dangerLevel += if (numOfCards >= 5) {
            3.0f
        } else if (numOfCards >= 3) {
            2.0f
        } else {
            1.0f
        }
        println("$roleTitle estimates the danger level as $dangerLevel.")
    }
}

abstract class Hero(val role: Role) : Role by role {
    open var name: String = ""
    open var maxHP: Int = 3
    open var hp: Int = 3
    open var shouldAbandon = false
    open var canSkipDiscard = false
    open val commands = mutableListOf<Command>()
    open var isAlive: Boolean = true
    open var canAttackAgain: Boolean = true
    open lateinit var strategy: Strategy
    private var gameJustStart = true
    open var numOfUseSleightOfHand = 0 // Prevent using Sleight of Hand too many times can make the game unable to stop
    open var hasAbandonAlready = false

    private var mount: Mount? = null
    private var rangeModifier: Int = 0

    open var cardList = mutableListOf<Card>()

    // Please do not directly modify this value to prevent cardList and numOfCards from being out of sync,
    // use updateCardNumber() instead. Thanks, by SamLam140330，
    var numOfCards: Int = 4
        private set

    open fun inRangeOf(target: Hero, range: AttackRange): Boolean {
        return calculateDistance(target) <= range.distance
    }

    open fun calculateDistance(target: Hero): Int {
        // Get the current hero's location
        val x1 = 2
        val y1 = 3

        // Get the target hero's location
        val x2 = 4
        val y2 = 5

        // Calculate the distance between two hero and return the distance value
        val distanceX = abs(x1 - x2)
        val distanceY = abs(y1 - y2)
        return max(distanceX, distanceY)//need To set
    }

    open fun templateMethod() {
        if (gameJustStart) {
            for (i in 1..4) {
                cardList.add(CardFactory.createRandomCard())
            }
            gameJustStart = false
        }

        if (isAlive) {
            println("$name's turn:")
            executeCommand()
            if (!shouldAbandon) {
                drawCards()
                playCards()
            }
            if (!canSkipDiscard) {
                discardCards()
            }
            shouldAbandon = false
            canSkipDiscard = false
        }
    }

    open fun drawCards() {
        println("Drawing 2 cards.")
        for (i in 1..2) {
            cardList.add(CardFactory.createRandomCard())
        }
        updateCardNumber()
        println("$name now has $numOfCards cards.")
    }

    open fun discardCards() {
        val cardAndHpDifferent: Int = numOfCards - hp
        if (cardAndHpDifferent > 0) {
            println("Current HP is $hp, now have $numOfCards cards.")
            strategy.selectCardToDiscard()
            updateCardNumber()
        } else {
            println("Current HP is $hp, number of cards is $numOfCards. No need to discard cards.")
        }
        canAttackAgain = true
        hasAbandonAlready = false
        numOfUseSleightOfHand = 0
    }

    open fun playCards() {
        while (numOfCards > 0 && strategy.playNextCard()) {
            strategy.playNextCard()
        }
    }

    open fun peach() {
        for (i in cardList) {
            if (i is PeachCard) {
                cardList.remove(i)
                updateCardNumber()
                break
            }
        }
        hp += 1
        println("$name spent 1 Peach card to save 1 hp, current hp is $hp.")
    }

    open fun oathOfPeachGarden() {
        for (i in cardList) {
            if (i is OathOfPeachGardenCard) {
                cardList.remove(i)
                updateCardNumber()
                for (j in characterList) {
                    if (hp < maxHP && j.isAlive) {
                        hp += 1
                        println("$name use Oath of Peach Garden, ${j.name} gain 1 HP, current hp is ${j.hp}.")
                    }
                }
                break
            }
        }
    }

    open fun sleightOfHand() {
        for (i in cardList) {
            if (i is SleightOfHandCard) {
                numOfUseSleightOfHand++
                println("You used Sleight of Hand and draw 2 card.")
                cardList.remove(i)
                for (j in 1..2) {
                    cardList.add(CardFactory.createRandomCard())
                }
                updateCardNumber()
                println("$name now has $numOfCards cards.")
                break
            }
        }
    }

    open fun acedia() {
        for (i in cardList) {
            if (i is AcediaCard) {
                cardList.remove(i)
                updateCardNumber()
                checkTargetPlayer(role)
                break
            }
        }
    }

    private fun checkTargetPlayer(role: Role) {
        when (role) {
            is Monarch -> {
                for (i in characterList) {
                    if ((i.role is Rebel || i.role is Traitors) && !i.hasAbandonAlready) {
                        println("$name used Acedia card.")
                        i.setCommand(Abandon(i))
                        i.hasAbandonAlready = true
                        break
                    }
                }
            }

            is Minister -> {
                for (i in characterList) {
                    if (i.role is Rebel || i.role is Traitors && !i.hasAbandonAlready) {
                        println("$name used Acedia card.")
                        i.setCommand(Abandon(i))
                        i.hasAbandonAlready = true
                        break
                    }
                }
            }

            is Rebel -> {
                for (i in characterList) {
                    if (i.role is Monarch && !i.hasAbandonAlready) {
                        println("$name used Acedia card.")
                        i.setCommand(Abandon(i))
                        i.hasAbandonAlready = true
                        break
                    }
                }
            }

            is Traitors -> {
                for (i in characterList) {
                    if ((i.role is Rebel || i.role is Monarch) && !i.hasAbandonAlready) {
                        println("$name used Acedia card.")
                        i.setCommand(Abandon(i))
                        i.hasAbandonAlready = true
                        break
                    }
                }
            }
        }
    }

    open fun zhugeCrossbowCard() {
        for (i in cardList) {
            if (i is ZhugeCrossbowCard) {
                cardList.remove(i)
                updateCardNumber()
                for (j in characterList) {
                    if (inRangeOf(j, AttackRange.NEAR) && j.isAlive) {
                        j.beingAttacked()
                        break
                    }
                }
                break
            }
        }
        canAttackAgain = true
    }

    //Four Mounts
    fun mountRedHare() {
        println("You get a mount: Red Hare!")
        for (i in cardList) {
            if (i is RedHareCard) {     //RedHareCard, DaYuanCard, HuaLiuCard, TheShadowCard
                cardList.remove(i)
                updateCardNumber()
                break
            }
        }
        mount = Mount.RED_HARE
        rangeModifier = -1
        canAttackAgain = false
    }

    fun mountDaYuan() {
        println("You get a mount: Da Yuan!")
        for (i in cardList) {
            if (i is DaYuanCard) {
                cardList.remove(i)
                updateCardNumber()
                break
            }
        }
        mount = Mount.DA_YUAN
        rangeModifier = -1
        canAttackAgain = false
    }

    fun mountHuaLiu() {
        println("You get a mount: Hua Liu!")
        for (i in cardList) {
            if (i is HuaLiuCard) {
                cardList.remove(i)
                updateCardNumber()
                break
            }
        }
        mount = Mount.HUA_LIU
        rangeModifier = 1
        canAttackAgain = false
    }

    fun mountTheShadow() {
        println("You get a mount: The Shadow!")
        for (i in cardList) {
            if (i is TheShadowCard) {
                cardList.remove(i)
                updateCardNumber()
                break
            }
        }
        mount = Mount.THE_SHADOW
        rangeModifier = 1
        canAttackAgain = false
    }

    open fun attack() {
        println("$name is a $roleTitle, spent 1 card to ${getEnemy()}")
        for (i in cardList) {
            if (i is AttackCard) {
                cardList.remove(i)
                updateCardNumber()
                break
            }
        }

        for (i in 0 until characterList.size) {
            if (characterList[i].isAlive) {
                if (this.role.roleTitle == Monarch().roleTitle) {
                    if (characterList[i].role.roleTitle == Rebel().roleTitle || characterList[i].role.roleTitle == Traitors().roleTitle) {
                        characterList[i].beingAttacked()
                        break
                    }
                }

                if (this.role.roleTitle == Minister().roleTitle) {
                    if (characterList[i].role.roleTitle == Rebel().roleTitle || characterList[i].role.roleTitle == Traitors().roleTitle) {
                        characterList[i].beingAttacked()
                        break
                    }
                }

                if (this.role.roleTitle == Traitors().roleTitle) {
                    var containsEverything = false

                    for (j in characterList) {
                        if (j.role.roleTitle == Minister().roleTitle || j.role.roleTitle == Rebel().roleTitle || j.role.roleTitle == Traitors().roleTitle)
                            containsEverything = true
                    }
                    if (containsEverything) {
                        if ((characterList[i].role.roleTitle == Rebel().roleTitle || characterList[i].role.roleTitle == Traitors().roleTitle || characterList[i].role.roleTitle == Minister().roleTitle) && characterList[i] != this) {
                            characterList[i].beingAttacked()
                            break
                        }
                    } else {
                        if (characterList[i].role.roleTitle == Monarch().roleTitle) {
                            characterList[i].beingAttacked()
                            break
                        }
                    }
                }

                if (this.role.roleTitle == Rebel().roleTitle) {
                    if (characterList[i].role.roleTitle == Traitors().roleTitle || characterList[i].role.roleTitle == Monarch().roleTitle) {
                        characterList[i].beingAttacked()
                        break
                    }
                }
            }
            canAttackAgain = false
        }
    }

    open fun beingAttacked() {
        println("$name got attacked.")
        if (!dodgeAttack()) {
            hp -= 1
            println("$name is unable to dodge attack, current hp is $hp.")
        } else {
            println("$name dodged attack, current hp is $hp.")
        }

        if (role is Monarch) {
            (this as MonarchHero).notifySubscribers(false, hp, numOfCards)
        }

        if (hp <= 2) {
            strategy.state = UnhealthyState(this)
        } else if (hp >= 3) {
            strategy.state = HealthyState(this)
        }

        // On the brink of death, check if other players will help
        if (hp == 0) {
            for (i in 0 until characterList.size) {
                if (role is Monarch && characterList[i].role is Minister && characterList[i].numOfCards > 1) {
                    val ableToHelp = characterList[i].strategy.state.playHealCard()
                    if (ableToHelp) {
                        this.hp += 1
                        println("${this.name} has been healed by ${characterList[i].name}, current hp is ${this.hp}")
                        break
                    }
                }

                if (role is Minister && (characterList[i].role is Monarch || characterList[i].role is Minister) && characterList[i].numOfCards > 1) {
                    val ableToHelp = characterList[i].strategy.state.playHealCard()
                    if (ableToHelp) {
                        this.hp += 1
                        println("${this.name} has been healed by ${characterList[i].name}, current hp is ${this.hp}")
                        break
                    }
                }

                if (role is Rebel && characterList[i].role is Rebel && characterList[i].numOfCards > 1) {
                    val ableToHelp = characterList[i].strategy.state.playHealCard()
                    if (ableToHelp) {
                        this.hp += 1
                        println("${this.name} has been healed by ${characterList[i].name}, current hp is ${this.hp}")
                        break
                    }
                }

                if (role is Traitors && characterList[i].role is Traitors && characterList[i].numOfCards > 1) {
                    val ableToHelp = characterList[i].strategy.state.playHealCard()
                    if (ableToHelp) {
                        this.hp += 1
                        println("${this.name} has been healed by ${characterList[i].name}, current hp is ${this.hp}")
                        break
                    }
                }
            }
        }

        if (hp <= 0) {
            isAlive = false
            if (role is Subscriber && MonarchFactory.createdMonarchHero is Publisher) {
                MonarchFactory.createdMonarchHero.removeSubscriber(role)
            }
            println("$name has died.")
        }
    }

    open fun dodgeAttack(): Boolean {
        for (i in cardList) {
            if (i is DodgeCard) {
                cardList.remove(i)
                updateCardNumber()
                println("Dodge card played to cancel the attack.")
                println("$name dodged attack, current hp is $hp.")
                return true
            }
        }
        return false
    }

    open fun helpUsingAttack(): Boolean {
        for (i in cardList) {
            if (i is AttackCard) {
                cardList.remove(i)
                updateCardNumber()
                println("Attack card played.")
                return true
            }
        }
        return false
    }

    open fun helpUsingPeach(): Boolean {
        for (i in cardList) {
            if (i is PeachCard) {
                cardList.remove(i)
                updateCardNumber()
                println("Peach card played to heal.")
                return true
            }
        }
        return false
    }

    open fun shouldHealHimself(): Boolean {
        if (hp < maxHP) {
            return true
        }
        return false
    }

    open fun heal() {
        for (i in cardList) {
            if (i is PeachCard) {
                hp += 1
                println("$name is healed, current hp is $hp.")
                break
            }
        }
    }

    open fun setCommand(command: Command) {
        commands.add(command)
    }

    open fun executeCommand() {
        while (commands.isNotEmpty()) {
            commands.removeFirst().execute()
        }
    }

    open fun updateCardNumber() {
        numOfCards = cardList.size
    }

    @JvmName("setStrategyForHero") // Prevent same JVM signature error
    fun setStrategy(strategy: Strategy) {
        this.strategy = strategy
    }
}

abstract class MonarchHero(role: Monarch) : Hero(role), Publisher by role {
    override var maxHP = 5
    override var hp = 5
}

abstract class WarriorHero(role: Role) : Hero(role) {
    override var maxHP = 4
    override var hp = 4
}

abstract class AdvisorHero(role: Role) : Hero(role) {
    override var maxHP = 3
    override var hp = 3
}

interface Strategy {
    var state: State

    fun playNextCard(): Boolean
    fun selectCardToDiscard()
    fun changeState(state: State)
}

open class BasicStrategy(theHero: Hero) : Strategy {
    var hero = theHero
    override var state: State = HealthyState(hero)

    override fun playNextCard(): Boolean {
        for (card in hero.cardList) {
            if (card is AttackCard && hero.canAttackAgain) {
                hero.attack()
                return true
            }

            if (card is PeachCard && hero.shouldHealHimself()) {
                hero.peach()
                return true
            }

            if (card is OathOfPeachGardenCard && hero.shouldHealHimself()) {
                hero.oathOfPeachGarden()
                return true
            }

            if (card is SleightOfHandCard && hero.numOfUseSleightOfHand < 3) {
                hero.sleightOfHand()
                return true
            }

            if (card is AcediaCard) {
                hero.acedia()
                return true
            }

            if (card is ZhugeCrossbowCard && hero.canAttackAgain) {
                hero.zhugeCrossbowCard()
                return true
            }

            if (card is RedHareCard && hero.canAttackAgain) {
                hero.mountRedHare()
                return true
            }

            if (card is DaYuanCard && hero.canAttackAgain) {
                hero.mountDaYuan()
                return true
            }

            if (card is HuaLiuCard && hero.canAttackAgain) {
                hero.mountHuaLiu()
                return true
            }

            if (card is TheShadowCard && hero.canAttackAgain) {
                hero.mountTheShadow()
                return true
            }
        }
        return false
    }

    override fun selectCardToDiscard() {
        println("Selecting a card to discard...")
        state.recommendCardToDiscard()
        hero.discardCards()
    }

    override fun changeState(state: State) {
        this.state = state
    }
}

interface Handler {
    fun setNext(handler: Handler)
    fun handle(): Boolean
}

abstract class ShuHero(role: Role) : Hero(role), Handler {
    private var nextHandler: Handler? = null

    override fun setNext(handler: Handler) {
        nextHandler = handler
    }

    override fun handle(): Boolean {
        return if (isAlive) {
            if (numOfCards > 0 && roleTitle != "Rebel" && roleTitle != "Traitors") {
                for (i in cardList) {
                    if (i is AttackCard) {
                        cardList.remove(i)
                        updateCardNumber()
                        println("$name spent 1 card to help his/her lord to use attack card.")
                        break
                    }
                }
                true
            } else {
                println("$name doesn't want to help")
                nextHandler?.handle() ?: false
            }
        } else {
            nextHandler?.handle() ?: false
        }
    }
}

abstract class WuHero(role: Role) : Hero(role), Handler {
    private var nextHandler: Handler? = null

    override fun setNext(handler: Handler) {
        nextHandler = handler
    }

    override fun handle(): Boolean {
        return if (isAlive) {
            if (numOfCards > 0 && roleTitle != "Rebel" && roleTitle != "Traitors") {
                for (i in cardList) {
                    if (i is PeachCard) {
                        cardList.remove(i)
                        updateCardNumber()
                        println("$name spent 1 card to help his/her lord to use peach card.")
                        break
                    }
                }
                true
            } else {
                println("$name doesn't want to help")
                nextHandler?.handle() ?: false
            }
        } else {
            nextHandler?.handle() ?: false
        }
    }
}

abstract class WeiHero(role: Role) : Hero(role), Handler {
    private var nextHandler: Handler? = null

    override fun setNext(handler: Handler) {
        nextHandler = handler
    }

    override fun handle(): Boolean {
        return if (isAlive) {
            if (numOfCards > 0 && roleTitle != "Rebel") {
                for (i in cardList) {
                    if (i is DodgeCard) {
                        cardList.remove(i)
                        updateCardNumber()
                        println("$name spent 1 card to help his/her lord to dodge.")
                        break
                    }
                }
                true
            } else {
                println("$name doesn't want to help")
                nextHandler?.handle() ?: false
            }
        } else {
            nextHandler?.handle() ?: false
        }
    }
}

// Shu
class LiuBei(role: Monarch) : MonarchHero(role) { // Done: Rouse
    override var name = "Liu Bei"
    var nextHandler: Handler? = null

    override fun helpUsingAttack(): Boolean {
        return if (nextHandler?.handle() == true) {
            true
        } else {
            println("No one can help lord to use attack.")
            false
        }
    }
}

class GuanYu { // Done: Saint Warrior
    val name = "Guan Yu"
    fun getAttackString() = "Power 💪!!"
}

class GuanYuAdapter(role: Role) : ShuHero(role) {
    private val guanYu = GuanYu()
    override var name = guanYu.name
    override var maxHP = 4
    override var hp = 4

    override fun attack() {
        println(guanYu.getAttackString())
        super.attack()
    }
}

class GuanYuStrategyAdapter(hero: Hero) : BasicStrategy(hero) {
    override fun playNextCard(): Boolean {
        for (card in hero.cardList) {
            if (card.isRedCard()) {
                hero.cardList.remove(card)
                hero.updateCardNumber()
                hero.attack()
                return true
            }
        }
        super.playNextCard()
        return false
    }

    override fun selectCardToDiscard() {
        println("I prefer red cards.")
        super.selectCardToDiscard()
    }
}

class ZhangFei(role: Role) : ShuHero(role) { // Done: Roar
    override var name = "Zhang Fei"
    override var maxHP = 4
    override var hp = 4

    override fun attack() {
        super.attack()
        canAttackAgain = true
    }
}

class ZhugeLiang(role: Role) : ShuHero(role) {
    override var name = "Zhuge Liang"
    override var maxHP = 3
    override var hp = 3
}

class ZhangYun(role: Role) : ShuHero(role) { // Done: Dragon Courage
    override var name = "Zhang Yun"
    override var maxHP = 4
    override var hp = 4

    override fun dodgeAttack(): Boolean {
        for (i in cardList) {
            if (i is DodgeCard || i is AttackCard) {
                cardList.remove(i)
                updateCardNumber()
                println("Dodged the attack.")
                println("$name dodged attack, current hp is $hp.")
                return true
            }
        }
        return false
    }
}

class ZhangYunStrategyAdapter(hero: Hero) : BasicStrategy(hero) {
    override fun playNextCard(): Boolean {
        for (card in hero.cardList) {
            if (card is DodgeCard && hero.canAttackAgain) {
                hero.cardList.remove(card)
                hero.updateCardNumber()
                hero.attack()
                return true
            }
        }
        super.playNextCard()
        return false
    }
}

class MaChao(role: Role) : ShuHero(role) {
    override var name = "Ma Chao"
    override var maxHP = 4
    override var hp = 4
}

class HuangYueYing(role: Role) : ShuHero(role) {
    override var name = "Huang Yue Ying"
    override var maxHP = 3
    override var hp = 3
}

// Wu
class SunQuan(role: Monarch) : MonarchHero(role) { // Done Balance of Power, Rescue
    override var name = "Sun Quan"
    var nextHandler: Handler? = null

    override fun playCards() {
        if (hp <= 2) {
            var shouldUseBalanceOfPower = false
            for (i in cardList) {
                if (i !is PeachCard && i !is DodgeCard) {
                    shouldUseBalanceOfPower = true
                    break
                }
            }
            if (shouldUseBalanceOfPower) {
                println("I use Balance of Power")
                val numOfCard = cardList.size
                for (i in 1..numOfCard) {
                    cardList.removeAt(0)
                }
                for (i in 1..numOfCard) {
                    cardList.add(CardFactory.createRandomCard())
                }
            }
        }
        super.playCards()
    }

    override fun helpUsingPeach(): Boolean {
        return if (nextHandler?.handle() == true) {
            true
        } else {
            println("No one can help lord to peach.")
            false
        }
    }
}

class GanNing(role: Role) : WuHero(role) {
    override var name = "Gan Ning"
    override var maxHP = 4
    override var hp = 4
}

class LuMeng(role: Role) : WuHero(role) { // Done: Self Restraint
    override var name = "Lu Meng"
    override var maxHP = 4
    override var hp = 4

    override fun attack() {
        super.attack()
        canSkipDiscard = true
    }
}

class HuangGai(role: Role) : WuHero(role) {
    override var name = "Huang Gai"
    override var maxHP = 4
    override var hp = 4
}

class ZhouYu(role: Role) : WuHero(role) {
    override var name = "Zhou Yu"
    override var maxHP = 3
    override var hp = 3

    override fun drawCards() {
        println("I'm handsome, so I can draw 3 cards.")
        for (i in 1..3) {
            cardList.add(CardFactory.createRandomCard())
        }
        updateCardNumber()
        println("$name now has $numOfCards cards.")
    }
}

class DaQiao(role: Role) : WuHero(role) { // Done: National Beauty
    override var name = "Da Qiao"
    override var maxHP = 3
    override var hp = 3
}

class DaQiaoStrategyAdapter(hero: Hero) : BasicStrategy(hero) {
    private var hasUsedAbandonCard = false

    override fun playNextCard(): Boolean {
        return if (hero.canAttackAgain) {
            if (hero.role is Rebel && !hasUsedAbandonCard) {
                for (i in hero.cardList) {
                    if (i.suit == Suit.Diamond) {
                        MonarchFactory.createdMonarchHero.setCommand(Abandon(MonarchFactory.createdMonarchHero))
                        hero.cardList.remove(i)
                        hero.updateCardNumber()
                        hasUsedAbandonCard = true
                        break
                    }
                }
            }
            super.playNextCard()
            hero.canAttackAgain = false
            true
        } else {
            false
        }
    }
}

class LuXun(role: Role) : WuHero(role) {
    override var name = "Lu Xun"
    override var maxHP = 3
    override var hp = 3
}

class SunShangXiang(role: Role) : WuHero(role) {
    override var name = "Sun Shang Xiang"
    override var maxHP = 3
    override var hp = 3
}

// Wai
class CaoCao(role: Monarch) : MonarchHero(role) { // Done: Escort
    override var name = "Cao Cao"
    var nextHandler: Handler? = null

    override fun dodgeAttack(): Boolean {
        return if (nextHandler?.handle() == true) {
            true
        } else {
            println("No one can help lord to dodge.")
            false
        }
    }
}

class SimaYi(role: Role) : WeiHero(role) {
    override var maxHP = 3
    override var hp = 3
    override var name = "Sima Yi"
}

class XiahouDun(role: Role) : WeiHero(role) {
    override var maxHP = 4
    override var hp = 4
    override var name = "Xiahou Dun"
}

class ZhangLiao(role: Role) : WeiHero(role) {
    override var maxHP = 4
    override var hp = 4
    override var name = "Zhang Liao"
}

class XuChu(role: Role) : WeiHero(role) {
    override var maxHP = 4
    override var hp = 4
    override var name = "Xu Chu"
}

class GuoJia(role: Role) : WeiHero(role) {
    override var maxHP = 3
    override var hp = 3
    override var name = "Guo Jia"
}

class ZhenJi(role: Role) : WeiHero(role) {
    override var maxHP = 3
    override var hp = 3
    override var name = "Zhen Ji"
}

// Kingdomless
class HuaTuo(role: Role) : AdvisorHero(role) {
    override var name = "Hua Tuo"
}

class LuBu(role: Role) : WarriorHero(role) {
    override var maxHP = 5
    override var hp = 5
    override var name = "Lu Bu"
}

class DiaoChan(role: Role) : AdvisorHero(role) { // Done: Eclipse
    override var name = "Diao Chan"

    override fun discardCards() {
        super.discardCards()
        if (numOfCards == hp) {
            if (cardList.size < 1) {
                cardList.add(CardFactory.createRandomCard())
                cardList.add(CardFactory.createRandomCard())
                println("I can draw 2 more cards")
            } else {
                cardList.add(CardFactory.createRandomCard())
                println("I can draw 1 more card")
            }
            updateCardNumber()
            println("Now I have $numOfCards cards.")
        }
    }
}

interface Command {
    fun execute()
}

class Abandon(private var hero: Hero) : Command {
    init {
        println("${hero.name} being placed the Abandon card.")
    }

    override fun execute() {
        val card = CardFactory.createRandomCard()
        hero.shouldAbandon = card.suit != Suit.Heart

        if (hero.shouldAbandon) {
            println("${hero.name} round got abandoned.")
        } else {
            println("Abandon card voided.")
        }
    }
}

enum class Suit {
    Club, Diamond, Heart, Spades
}

enum class AttackRange(val distance: Int) {
    NEAR(1),
    MIDDLE(2),
    FAR(3),
    ANY(0),
    REMOTE(4),
    ULTRA(5)
}

enum class Mount(val mount: String, val description: String) {
    RED_HARE("Red Hare", "Your range with other heroes is reduced by 1"),
    DA_YUAN("Da Yuan", "Your range with other heroes is reduced by 1"),
    HUA_LIU("Hua Liu", "Your range with other heroes is increased by 1"),
    THE_SHADOW("The Shadow", "Your range with other heroes is increased by 1")
}

interface Card {
    val number: Int
    val suit: Suit
    //val mount: Mount

    fun createCard(): Card
    fun isRedCard(): Boolean
}

object CardFactory {
    private val usedCardList = mutableListOf<Card>()

    fun createRandomCard(): Card {
        val randomNumber = (1..13).random() // A, 2 - 10, J, Q, K
        val randomSuit = Suit.values().random()
        val generatedCard = when (val randomCardType = (1..11).random()) {
            1 -> AttackCard(randomNumber, randomSuit)
            2 -> DodgeCard(randomNumber, randomSuit)
            3 -> PeachCard(randomNumber, randomSuit)
            4 -> OathOfPeachGardenCard(randomNumber, randomSuit)
            5 -> SleightOfHandCard(randomNumber, randomSuit)
            6 -> AcediaCard(randomNumber, randomSuit)
            7 -> ZhugeCrossbowCard(randomNumber, randomSuit)
            8 -> RedHareCard(randomNumber, randomSuit)
            9 -> DaYuanCard(randomNumber, randomSuit)
            10 -> HuaLiuCard(randomNumber, randomSuit)
            11 -> TheShadowCard(randomNumber, randomSuit)
            else -> throw IllegalArgumentException("Invalid random number: $randomCardType")
        }
        return if (usedCardList.contains(generatedCard)) {
            createRandomCard()
        } else {
            usedCardList.add(generatedCard)
            generatedCard
        }
    }
}

abstract class Cards(override val number: Int, override val suit: Suit) : Card {
    abstract override fun createCard(): Cards

    override fun isRedCard(): Boolean {
        return suit == Suit.Diamond || suit == Suit.Heart
    }
}

// Basic Cards
class AttackCard(number: Int, suit: Suit) : Cards(number, suit) {
    override fun createCard(): Cards {
        return AttackCard(number, suit)
    }
}

class DodgeCard(number: Int, suit: Suit) : Cards(number, suit) {
    override fun createCard(): Cards {
        return DodgeCard(number, suit)
    }
}

class PeachCard(number: Int, suit: Suit) : Cards(number, suit) {
    override fun createCard(): Cards {
        return PeachCard(number, suit)
    }
}

// Tactics cards
class OathOfPeachGardenCard(number: Int, suit: Suit) : Cards(number, suit) {
    override fun createCard(): Cards {
        return OathOfPeachGardenCard(number, suit)
    }
}

class SleightOfHandCard(number: Int, suit: Suit) : Cards(number, suit) {
    override fun createCard(): Cards {
        return SleightOfHandCard(number, suit)
    }
}

// Delay tactics cards
class AcediaCard(number: Int, suit: Suit) : Cards(number, suit) {
    override fun createCard(): Cards {
        return AcediaCard(number, suit)
    }
}

//Equipment cards: Weapons
class ZhugeCrossbowCard(number: Int, suit: Suit) : Cards(number, suit) {
    override fun createCard(): Cards {
        return ZhugeCrossbowCard(number, suit)
    }
}

//Equipment cards: Mounts
class RedHareCard(number: Int, suit: Suit) : Cards(number, suit) {
    override fun createCard(): Cards {
        return RedHareCard(number, suit)
    }
}

class DaYuanCard(number: Int, suit: Suit) : Cards(number, suit) {
    override fun createCard(): Cards {
        return DaYuanCard(number, suit)
    }
}

class HuaLiuCard(number: Int, suit: Suit) : Cards(number, suit) {
    override fun createCard(): Cards {
        return HuaLiuCard(number, suit)
    }
}

class TheShadowCard(number: Int, suit: Suit) : Cards(number, suit) {
    override fun createCard(): Cards {
        return TheShadowCard(number, suit)
    }
}

interface Subscriber {
    fun update(dodged: Boolean, hp: Int, numOfCards: Int)
}

interface Publisher {
    fun subscribe(subscriber: Subscriber)
    fun notifySubscribers(dodged: Boolean, hp: Int, numOfCards: Int)
    fun removeSubscriber(subscriber: Subscriber)
}

interface State {
    fun playHealCard(): Boolean
    fun recommendCardToDiscard()
}

class HealthyState(private var hero: Hero) : State {
    override fun playHealCard(): Boolean {
        for (i in hero.cardList) {
            if (i is PeachCard) {
                hero.cardList.remove(i)
                hero.updateCardNumber()
                return true
            }
        }
        return false
    }

    override fun recommendCardToDiscard() {
        println("Healthy, keep attack card instead of dodge card.")
        val dodgeCard = hero.cardList.find { it is DodgeCard }
        if (dodgeCard != null) {
            hero.cardList.remove(dodgeCard)
        } else {
            val random = (1 until hero.cardList.size).random()
            hero.cardList.removeAt(random)
        }
        hero.updateCardNumber()
    }
}

class UnhealthyState(private var hero: Hero) : State {
    override fun playHealCard(): Boolean {
        for (i in hero.cardList) {
            if (i is PeachCard) {
                hero.cardList.remove(i)
                hero.updateCardNumber()
                return true
            }
        }
        return false
    }

    override fun recommendCardToDiscard() {
        println("Not Healthy, keep dodge card instead of attack card.")
        val attackCard = hero.cardList.find { it is AttackCard }
        if (attackCard != null) {
            hero.cardList.remove(attackCard)
        } else {
            val random = (1 until hero.cardList.size).random()
            hero.cardList.removeAt(random)
        }
        hero.updateCardNumber()
    }
}

val characterList = mutableListOf<Hero>()
var numOfPlayer = 0

fun main() {
    numOfPlayer = inputNumberOfPlayer()
    println() // For easy to read
    characterListInitiationPhase()
    mainPhase()
}

fun inputNumberOfPlayer(): Int {
    print("Please enter the total number of players: ")
    val reader = Scanner(System.`in`)
    val numberOfPlayer: Int
    try {
        numberOfPlayer = reader.nextInt()
    } catch (e: InputMismatchException) {
        println("Please enter a digit and without decimal point!")
        return inputNumberOfPlayer()
    }
    return if (numberOfPlayer < 3 || numberOfPlayer > 10) {
        println("Please enter a number between 3 and 10!")
        inputNumberOfPlayer()
    } else {
        numberOfPlayer
    }
}

fun characterListInitiationPhase() {
    characterList.add(MonarchFactory.createdMonarchHero)
    for (i in 0 until (numOfPlayer - 1)) {
        characterList.add(NonMonarchFactory.createRandomHero())
    }
}

interface GameObjectFactory {
    fun getRandomRole(): Role
    fun createRandomHero(): Hero
}

object MonarchFactory : GameObjectFactory {
    val createdMonarchHero = createRandomHero()

    override fun getRandomRole(): Monarch {
        return Monarch()
    }

    override fun createRandomHero(): Hero {
        val createdHero = when (val random = (1..3).random()) {
            1 -> LiuBei(getRandomRole())
            2 -> SunQuan(getRandomRole())
            3 -> CaoCao(getRandomRole())
            else -> throw IllegalArgumentException("Invalid random number: $random")
        }
        createdHero.setStrategy(BasicStrategy(createdHero))
        return createdHero
    }
}

object NonMonarchFactory : GameObjectFactory {
    private val usedCharacterList = mutableListOf<Int>()
    private var numOfMinisterAllow = 0
    private var numOfRebelAllow = 0
    private var numOfTraitorAllow = 0

    init {
        when (numOfPlayer) {
            3 -> {
                numOfMinisterAllow = 0
                numOfRebelAllow = 1
                numOfTraitorAllow = 1
            }

            4 -> {
                numOfMinisterAllow = 1
                numOfRebelAllow = 1
                numOfTraitorAllow = 1
            }

            5 -> {
                numOfMinisterAllow = 1
                numOfRebelAllow = 2
                numOfTraitorAllow = 1
            }

            6 -> {
                numOfMinisterAllow = 1
                numOfRebelAllow = 2
                numOfTraitorAllow = 2
            }

            7 -> {
                numOfMinisterAllow = 2
                numOfRebelAllow = 3
                numOfTraitorAllow = 1
            }

            8 -> {
                numOfMinisterAllow = 2
                numOfRebelAllow = 3
                numOfTraitorAllow = 2
            }

            9 -> {
                numOfMinisterAllow = 3
                numOfRebelAllow = 4
                numOfTraitorAllow = 1
            }

            10 -> {
                numOfMinisterAllow = 3
                numOfRebelAllow = 4
                numOfTraitorAllow = 2
            }

            else -> throw IllegalArgumentException("Invalid number")
        }
    }

    override fun getRandomRole(): Role {
        return when ((1..3).random()) {
            1 -> if (numOfMinisterAllow > 0) {
                numOfMinisterAllow--
                Minister()
            } else {
                getRandomRole()
            }

            2 -> if (numOfRebelAllow > 0) {
                numOfRebelAllow--
                Rebel()
            } else {
                getRandomRole()
            }

            3 -> if (numOfTraitorAllow > 0) {
                numOfTraitorAllow--
                Traitors()
            } else {
                getRandomRole()
            }

            else -> throw IllegalArgumentException("Invalid random number")
        }
    }

    override fun createRandomHero(): Hero {
        val random = (1..22).random()
        if (random !in usedCharacterList) {
            usedCharacterList.add(random)
            val createdHero = when (random) {
                1 -> GuanYuAdapter(getRandomRole())
                2 -> ZhangFei(getRandomRole())
                3 -> ZhugeLiang(getRandomRole())
                4 -> ZhangYun(getRandomRole())
                5 -> MaChao(getRandomRole())
                6 -> HuangYueYing(getRandomRole())
                7 -> GanNing(getRandomRole())
                8 -> LuMeng(getRandomRole())
                9 -> HuangGai(getRandomRole())
                10 -> ZhouYu(getRandomRole())
                11 -> DaQiao(getRandomRole())
                12 -> LuXun(getRandomRole())
                13 -> SunShangXiang(getRandomRole())
                14 -> SimaYi(getRandomRole())
                15 -> XiahouDun(getRandomRole())
                16 -> ZhangLiao(getRandomRole())
                17 -> XuChu(getRandomRole())
                18 -> GuoJia(getRandomRole())
                19 -> ZhenJi(getRandomRole())
                20 -> HuaTuo(getRandomRole())
                21 -> LuBu(getRandomRole())
                22 -> DiaoChan(getRandomRole())
                else -> throw IllegalArgumentException("Invalid random number: $random")
            }
            if (createdHero is Handler && MonarchFactory.createdMonarchHero is CaoCao) {
                val currentHandler = MonarchFactory.createdMonarchHero.nextHandler
                if (currentHandler == null) {
                    MonarchFactory.createdMonarchHero.nextHandler = createdHero
                } else {
                    currentHandler.setNext(createdHero)
                }
            }
            if (createdHero is Handler && MonarchFactory.createdMonarchHero is SunQuan) {
                val currentHandler = MonarchFactory.createdMonarchHero.nextHandler
                if (currentHandler == null) {
                    MonarchFactory.createdMonarchHero.nextHandler = createdHero
                } else {
                    currentHandler.setNext(createdHero)
                }
            }
            if (createdHero is Handler && MonarchFactory.createdMonarchHero is LiuBei) {
                val currentHandler = MonarchFactory.createdMonarchHero.nextHandler
                if (currentHandler == null) {
                    MonarchFactory.createdMonarchHero.nextHandler = createdHero
                } else {
                    currentHandler.setNext(createdHero)
                }
            }
            when (createdHero) {
                is GuanYuAdapter -> {
                    createdHero.setStrategy(GuanYuStrategyAdapter(createdHero))
                }

                is DaQiao -> {
                    createdHero.setStrategy(DaQiaoStrategyAdapter(createdHero))
                }

                is ZhangYun -> {
                    createdHero.setStrategy(ZhangYunStrategyAdapter(createdHero))
                }

                else -> {
                    createdHero.setStrategy(BasicStrategy(createdHero))
                }
            }
            return createdHero
        } else {
            return createRandomHero()
        }
    }
}

fun mainPhase() {
    var index = 0

    while (true) {
        if (checkWinCondition()) {
            break
        }
        if (characterList[index].isAlive) {
            characterList[index].templateMethod()
            println() // For easy to read
        }
        index += 1
        if (index > characterList.size - 1) {
            index = 0
        }
    }
}

fun checkWinCondition(): Boolean {
    var containsMonarch = false
    var containsMinster = false
    var containsRebel = false
    var containsTraitor = false

    for (i in 0 until characterList.size) {
        if (characterList[i].role is Minister && characterList[i].isAlive) {
            containsMinster = true
        }
        if (characterList[i].role is Monarch && characterList[i].isAlive) {
            containsMonarch = true
        }
        if (characterList[i].role is Rebel && characterList[i].isAlive) {
            containsRebel = true
        }
        if (characterList[i].role is Traitors && characterList[i].isAlive) {
            containsTraitor = true
        }
    }

    if (containsMonarch && containsMinster && !containsRebel && !containsTraitor) {
        monarchMinisterVictory()
        return true
    }
    if (containsMonarch && !containsMinster && !containsRebel && !containsTraitor) {
        monarchVictory()
        return true
    }
    if (!containsMonarch && !containsMinster && !containsRebel && containsTraitor) {
        traitorsVictory()
        return true
    }
    if (!containsMonarch && containsRebel) {
        rebelVictory()
        return true
    }

    return false
}

fun monarchMinisterVictory() {
    println("All Rebels and Traitors has died, Monarch and Minister Wins!")
}

fun monarchVictory() {
    println("All Rebels and Traitors has died, Monarch Wins!")
}

fun rebelVictory() {
    println("The Monarch has died, Rebel Wins!")
}

fun traitorsVictory() {
    println("All other players has died, Traitors Wins!")
}
