package com.example.rotation


class Random : java.util.Random {

    private var generator: RandomIntGenerator? = null

    @JvmOverloads
    constructor(seed: Long = System.currentTimeMillis()) : super(seed) {
        setSeed(seed)
    }

    constructor(seedArray: IntArray) {
        init(seedArray)
    }

    @Synchronized
    override fun setSeed(seed: Long) {
        val seedArray = IntArray(2)
        seedArray[0] = (seed and -0x1).toInt()
        seedArray[1] = seed.ushr(32).toInt()

        if (seedArray[1] == 0) {
            init(seedArray[0])
        } else {
            init(seedArray)
        }
    }

    override fun next(bits: Int): Int {
        return generator!!.nextInt().ushr(32 - bits)
    }

    @Synchronized
    fun setSeed(seedArray: IntArray) {
        init(seedArray)
    }

    private fun init(seed: Int) {
        if (generator == null) {
            generator = RandomIntGenerator(seed.toLong())
        } else {
            generator!!.init(seed.toLong())
        }
    }

    private fun init(seedArray: IntArray) {
        if (generator == null) {
            generator = RandomIntGenerator(seedArray)
        } else {
            generator!!.init(seedArray)
        }
    }

    companion object {

        private val serialVersionUID = 2479573230251354962L
    }
}


class RandomIntGenerator {

    private val mt = IntArray(N)

    private var mtIndex = N + 1

    /**
     * seedでRandomIntGeneratorのインスタンスを作る
     *
     * @param seed シード
     */
    constructor(seed: Long) {
        init(seed)
    }

    /**
     * 配列seedArrayでRandomIntGeneratorのインスタンスを作る
     *
     * @param seed シード配列
     */
    constructor(seedArray: IntArray) {
        init(seedArray)
    }

    /**
     * 一様分布の int 型の擬似乱数を返す。
     *
     * @return 一様分布の int 型の次の擬似乱数値
     */
    fun nextInt(): Int {
        var nextInt = generateInt()

        // 精度を上げる為の調律
        nextInt = temper(nextInt)

        return nextInt
    }

    /**
     * seedでMersenne Twister配列を初期化する
     *
     * @param seed シード
     */
    fun init(seed: Long) {

        val longMt = createLongMt(seed)

        setMt(longMt)
    }

    /**
     * 配列seedArrayでMersenne Twister配列を初期化する
     *
     * @param seedArray シード配列
     */
    fun init(seedArray: IntArray) {
        val longMt = createLongMt(19650218)

        val max = if (N > seedArray.size) N else seedArray.size
        run {
            var i = 1
            var j = 0
            var counter = 0
            while (counter < max) {
                if (N <= i) {
                    longMt[0] = longMt[N - 1]
                    i = 1
                }
                if (seedArray.size <= j) {
                    j = 0
                }
                longMt[i] = longMt[i] xor (longMt[i - 1] xor longMt[i - 1].ushr(30)) * 1664525
                longMt[i] += (seedArray[j] + j).toLong()
                longMt[i] = longMt[i] and 0xffffffffL
                i++
                j++
                counter++
            }
        }

        val initial = max % (N - 1) + 1
        var i = initial
        var counter = 0
        while (counter < N - 1) {
            if (N <= i) {
                longMt[0] = longMt[N - 1]
                i = 1
            }
            longMt[i] = longMt[i] xor (longMt[i - 1] xor longMt[i - 1].ushr(30)) * 1566083941
            longMt[i] -= i.toLong()
            longMt[i] = longMt[i] and 0xffffffffL
            i++
            counter++
        }

        longMt[0] = 0x80000000L

        setMt(longMt)
    }

    /**
     * Mersenne Twisterアルゴリズムで、一様分布の int 型の擬似乱数を返す。
     *
     * @return
     */
    @Synchronized
    private fun generateInt(): Int {
        twist()

        val ret = mt[mtIndex]
        mtIndex++

        return ret
    }

    /**
     * 配列をtwistする
     *
     */
    private fun twist() {
        val BIT_MATRIX = intArrayOf(0x0, -0x66f74f21)
        val UPPER_MASK = -0x80000000
        val LOWER_MASK = 0x7fffffff

        if (mtIndex < N) {
            return
        }

        if (mtIndex > N) {
            init(5489)
        }

        for (i in 0 until N) {
            val x = mt[i] and UPPER_MASK or (mt[(i + 1) % N] and LOWER_MASK)
            mt[i] = mt[(i + M) % N] xor x.ushr(1) xor BIT_MATRIX[x and 0x1]
        }

        mtIndex = 0
    }

    /**
     * long型の配列を、ビット配列としてintに変換して、インスタンス変数mtにセットする
     *
     * @param longMt 配列
     */
    @Synchronized
    private fun setMt(longMt: LongArray) {
        for (i in 0 until N) {
            mt[i] = toInt(longMt[i])
        }

        mtIndex = N
    }

    companion object {
        private val N = 624
        private val M = 397

        /**
         * 調律する
         *
         * @param num 調律するint
         * @return 調律されたint
         */
        private fun temper(num: Int): Int {
            var num = num

            num = num xor num.ushr(11)
            num = num xor (num shl 7 and -0x62d3a980)
            num = num xor (num shl 15 and -0x103a0000)
            num = num xor num.ushr(18)

            return num
        }

        /**
         * 2^32 - 1以下のlongを、右32ビット列だけintに変換する
         *
         * @param num 2^32 - 1以下のlong
         * @return 変換したint
         */
        private fun toInt(num: Long): Int {
            return (if (num > Integer.MAX_VALUE) num - 0x100000000L else num).toInt()
        }

        /**
         * seedから、初期化したmtをlongで作る
         *
         * @param seed シード
         * @return 初期化したmt
         */
        private fun createLongMt(seed: Long): LongArray {
            val longMt = LongArray(N)

            longMt[0] = seed and 0xffffffffL

            for (i in 1 until N) {
                longMt[i] = longMt[i - 1]

                longMt[i] = longMt[i] ushr 30
                longMt[i] = longMt[i] xor longMt[i - 1]
                longMt[i] *= 0x6C078965L
                longMt[i] += i.toLong()
                longMt[i] = longMt[i] and 0xffffffffL
            }

            return longMt
        }
    }
}