package com.senghol.knotx.utils.objects

import com.senghol.knotx.BuildConfig
import com.senghol.knotx.data.database.Sandesh
import com.senghol.knotx.utils.enums.SandeshStithi
import java.util.UUID

object SandeshTestData {

    private val messages = listOf(
        """{"paath":"Hii","link":false,"diD":0}""",
        """{"paath":"Hey! What's up?","link":false,"diD":0}""",
        """{"paath":"Nothing much, just got home.","link":false,"diD":0}""",
        """{"paath":"Oh nice 😂","link":false,"diD":0}""",
        """{"paath":"What are you doing?","link":false,"diD":0}""",
        """{"paath":"Just working on something.","link":false,"diD":0}""",
        """{"paath":"Still working? Take a break sometimes 😂","link":false,"diD":0}""",
        """{"paath":"Haha yeah, almost done.","link":false,"diD":0}""",
        """{"paath":"By the way, did you check the photos I sent yesterday?","link":false,"diD":0}""",
        """{"paath":"Yes! They look amazing.","link":false,"diD":0}""",
        """{"paath":"I especially liked the last one.","link":false,"diD":0}""",
        """{"paath":"That was my favourite too!","link":false,"diD":0}""",
        """{"paath":"We should plan something this weekend.","link":false,"diD":0}""",
        """{"paath":"Sure, I'm free on Saturday.","link":false,"diD":0}""",
        """{"paath":"Let's decide tomorrow then.","link":false,"diD":0}""",
        """{"paath":"Sounds good 👍","link":false,"diD":0}""",
        """{"paath":"Also, don't forget to bring the documents.","link":false,"diD":0}""",
        """{"paath":"Oh right! Thanks for reminding me.","link":false,"diD":0}""",
        """{"paath":"I'll keep them ready.","link":false,"diD":0}""",
        """{"paath":"Perfect.","link":false,"diD":0}""",
        """{"paath":"How was your day today?","link":false,"diD":0}""",
        """{"paath":"Pretty good actually. Had a lot of work in the morning, but things got better later.","link":false,"diD":0}""",
        """{"paath":"That's good to hear!","link":false,"diD":0}""",
        """{"paath":"What about yours?","link":false,"diD":0}""",
        """{"paath":"Busy as usual 😂","link":false,"diD":0}""",
        """{"paath":"You really need to relax sometimes.","link":false,"diD":0}""",
        """{"paath":"I know, I know 😅","link":false,"diD":0}""",
        """{"paath":"Okay, talk to you later!","link":false,"diD":0}""",
        """{"paath":"Take care 👋","link":false,"diD":0}""",
        """{"paath":"Aur bata, office ka kya scene hai abhi?","link":false,"diD":0}""",
        """{"paath":"Bas wahi daily ka kaam chal raha hai, kuch naya nahi 😂","link":false,"diD":0}""",
        """{"paath":"Yaar kabhi kabhi lagta hai pura din laptop ke saamne hi nikal jata hai.","link":false,"diD":0}""",
        """{"paath":"Hahaha same bhai, weekend ka intezaar rehta hai bas.","link":false,"diD":0}""",
        """{"paath":"Waise kal market chalna hai kya? Kuch samaan lena tha.","link":false,"diD":0}""",
        """{"paath":"Haan chal sakte hain, mujhe bhi thoda kaam hai udhar.","link":false,"diD":0}""",
        """{"paath":"Kitne baje tak free ho jayega?","link":false,"diD":0}""",
        """{"paath":"Around 4 ya 5 baje tak ho jaunga.","link":false,"diD":0}""",
        """{"paath":"Theek hai fir mujhe message kar dena.","link":false,"diD":0}""",
        """{"paath":"Haan pakka, bhoolunga nahi.","link":false,"diD":0}""",
        """{"paath":"Aur bhai ghar pe sab badhiya?","link":false,"diD":0}""",
        """{"paath":"Haan sab mast hain, mummy aaj kuch special bana rahi hain.","link":false,"diD":0}""",
        """{"paath":"Achha kya ban raha hai? 😂","link":false,"diD":0}""",
        """{"paath":"Paneer aur parathe, abhi se bhook lag rahi hai.","link":false,"diD":0}""",
        """{"paath":"Bhai mere liye bhi rakhwa dena 😂","link":false,"diD":0}""",
        """{"paath":"Tu bas ghar aa ja, khila denge.","link":false,"diD":0}""",
        """{"paath":"Challenge accepted 😂","link":false,"diD":0}""",
        """{"paath":"Waise ek kaam tha tujhse.","link":false,"diD":0}""",
        """{"paath":"Haan bol, kya hua?","link":false,"diD":0}""",
        """{"paath":"Mujhe woh file chahiye thi jo tune last week share ki thi.","link":false,"diD":0}""",
        """{"paath":"Ek minute, check karta hoon.","link":false,"diD":0}""",
        """{"paath":"Mil gayi kya?","link":false,"diD":0}""",
        """{"paath":"Haan mil gayi, abhi forward karta hoon.","link":false,"diD":0}""",
        """{"paath":"Thanks bhai, kaafi help ho jayegi.","link":false,"diD":0}""",
        """{"paath":"Arey koi na, isme thanks ki kya baat hai.","link":false,"diD":0}""",
        """{"paath":"Bhai kabhi kabhi purane din yaad aate hain.","link":false,"diD":0}""",
        """{"paath":"Sach mein, tab kitna free time hota tha apne paas.","link":false,"diD":0}""",
        """{"paath":"Ab toh milne ka plan banao toh bhi 10 baar schedule dekhna padta hai 😂","link":false,"diD":0}""",
        """{"paath":"Hahaha bilkul sahi kaha.","link":false,"diD":0}""",
        """{"paath":"Is baar milenge toh araam se baithenge, koi jaldi nahi.","link":false,"diD":0}""",
        """{"paath":"Done bhai, iss baar proper plan banate hain.","link":false,"diD":0}""",
        """{"paath":"Chal ab main thoda busy ho raha hoon.","link":false,"diD":0}""",
        """{"paath":"Haan koi baat nahi, baad mein baat karte hain.","link":false,"diD":0}""",
        """{"paath":"Aur haan, ghar pahunch ke bata dena.","link":false,"diD":0}""",
        """{"paath":"Haan bhai, pahunchte hi message kar dunga.","link":false,"diD":0}""",
        """{"paath":"Take care yaar ❤️","link":false,"diD":0}""",
        """{"paath":"Tu bhi bhai, milte hain jaldi.","link":false,"diD":0}""",
        """{"paath":"Oye ek last baat 😂","link":false,"diD":0}""",
        """{"paath":"Ab kya hua bhai? 😂","link":false,"diD":0}""",
        """{"paath":"Kal late mat hona bas.","link":false,"diD":0}""",
        """{"paath":"Haan haan, iss baar time pe aaunga.","link":false,"diD":0}""",
        """{"paath":"Dekhte hain, tera record toh kharab hai 😂","link":false,"diD":0}""",
        """{"paath":"Abe ek baar late kya ho gaya, yaad hi rakh liya tune.","link":false,"diD":0}""",
        """{"paath":"Ek baar nahi bhai, har baar 😂","link":false,"diD":0}""",
        """{"paath":"Achha chal ab zyada mat bol.","link":false,"diD":0}""",
        """{"paath":"Hahaha okay boss 😎","link":false,"diD":0}"""
    )

    private val senderPattern = listOf(
        true, true, false, false, false,
        true, true, true, false, false,
        true, false, false, false, true,
        true, false, true, true, false,
        false, true, true, false, false,
        true, false, true, true, false,
        true, false, true, false, true,
        false, false, true, false, true,
        false, true, false, true, false,
        true, false, true, false, true,
        false, true, false, true, false,
        true, false, true, false, true,
        false, true, false, true, false,
        true, false, true, false, true,
        false, true, false, true, false,
        true, false, true, false, true
    )

    private const val TEST_USERNAME = "+918766256951"

    fun generate(
        userName: String,
        count: Int = 100
    ): List<Sandesh> {
        if (!BuildConfig.DEBUG || userName != TEST_USERNAME) return emptyList()

        val now = System.currentTimeMillis()

        return List(count) { index ->
            val messageIndex = index % messages.size

            Sandesh(
                sandeshId = UUID.randomUUID().toString(),
                userNumber = TEST_USERNAME,
                samagri = messages[messageIndex],
                isSelf = senderPattern[messageIndex],
                time = now - ((count - index) * 60_000L),
                sandeshType = 0,
                sandeshStithi = SandeshStithi.DHRIST
            )
        }
    }
}