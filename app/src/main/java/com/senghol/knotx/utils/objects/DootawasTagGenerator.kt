package com.senghol.knotx.utils.objects

object DootawasTagGenerator {
        private val CHAR_POOL: List<Char> =
            ('A'..'Z') + ('a'..'z') + ('0'..'9')

        private const val LENGTH = 8

        private val random = java.security.SecureRandom()

        fun generate(): String {
            val result = StringBuilder(LENGTH)

            repeat(LENGTH) {
                result.append(CHAR_POOL[random.nextInt(CHAR_POOL.size)])
            }

            return result.toString()
        }
    }