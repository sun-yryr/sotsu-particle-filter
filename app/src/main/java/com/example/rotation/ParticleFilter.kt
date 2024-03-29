package com.example.rotation

import android.util.Log
import kotlin.math.*
import kotlin.random.Random

class ParticleFilter(particle_count: Int, alpha: Int, sigma: Int) {
    private var x_resampled = generate_random_particles_accelerate(particle_count)     // パーティクルの集合
    private var likelihoods_normed = DoubleArray(particle_count)                             // 尤度(indexはx_resampledに遵守)
    private var alpha = alpha                                                          // パラメータ(平均)?
    private var sigma = sigma                                                          // パラメータ(分散)?
    private val particle_count = particle_count                                        // パーティクルの数

    /**
     * 引数で与えたパーティクルの数だけ3次元パーティクルをランダムに生成する(一様分布)
     * https://qiita.com/clomie/items/e5dd35dcfcba082b2a7f
     * @param n_particle [Int] 生成するパーティクルの数
     * @return Array<DoubleArray> 生成したパーティクル。FloatArrayのサイズは3で上からx,y,z
     */
    private fun generate_random_particles(n_particle: Int): Array<DoubleArray> {
        var particle = Array(n_particle, {DoubleArray(6)})
        for (i in 0..n_particle-1) {
            val rdm = Random.nextInt((2 * 1e8 + 1).toInt())
            var rdm_double = rdm.toDouble() / 1e8   // 0 ... 2のランダムdouble
            rdm_double -= 1.0                               // -1 ... 1のランダムdouble
            val phi = Math.toRadians(Random.nextInt(360 + 1).toDouble())    // 0 ~ 360度のランダム -> ラジアン
            val x = (sqrt(1 - rdm_double.pow(2)) * Math.cos(phi))
            val y = (sqrt(1 - rdm_double.pow(2)) * Math.sin(phi))
            val z = rdm_double
            particle[i][0] = x
            particle[i][1] = y
            particle[i][2] = z
        }
        return particle
    }

    /**
     * 引数で与えたパーティクルの数だけ6次元パーティクルをランダムに生成する(一様分布)
     * https://qiita.com/clomie/items/e5dd35dcfcba082b2a7f
     * @param n_particle [Int] 生成するパーティクルの数
     * @return Array<DoubleArray> 生成したパーティクル。FloatArrayのサイズは6で上からx,y,z(端末の加速度, パーティクルの位置),x,y,z(パーティクルの速度)
     */
    private fun generate_random_particles_accelerate(n_particle: Int): Array<DoubleArray> {
        var particles = Array(n_particle, {DoubleArray(6)})
        for (i in 0..n_particle-1) {
            for (p in particles[i].indices) {
                val rdm = Random.nextInt((2 * 1e8 + 1).toInt())
                var rdm_double = rdm.toDouble() / 1e8   // 0 ... 2のランダムdouble
                rdm_double -= 1.0                               // -1 ... 1のランダムdouble
                particles[i][p] = rdm_double
            }
        }
        return particles
    }

    /**
     * 引数で与えた秒数分だけ各パーティクルの位置を移動させる
     * @param particle [Array FloatArray] 移動するパーティクルの集合
     * @param seconds [Int] 移動させる秒数
     * @return Array<DoubleArray> 移動したパーティクルの集合
     */
    private fun move_particles(particle: Array<DoubleArray>, seconds: Int = 1): Array<DoubleArray> {
        var x = Array(particle_count, {DoubleArray(6)})
        for (i in particle.indices) {
            for (j in 0..2) {
                x[i][j] = particle[i][j] - particle[i][j+3] * seconds
                x[i][j+3] = particle[i][j+3]
            }
        }
        return x
    }


    /**
     * 6次元ガウス分布を使って尤度の計算をする。正規化済み
     * @param particles Array<DoubleArray> 尤度を計算するパーティクルの集合
     * @param measured_point [DoubleArray] 測定点
     * @return [DoubleArray] 尤度
     */
    private fun calcurate_likelihood_accelerate(particles: Array<DoubleArray>, measured_point: DoubleArray, param: Double = 1.0): DoubleArray {
        var gauss = {x: Double, y: Double, z: Double, dx: Double, dy: Double, dz: Double, s: Double ->
            val left = 1.0/(s.pow(6) * (2 * PI).pow(3))
            val x2 = (measured_point[0] - x).pow(2)
            val y2 = (measured_point[1] - y).pow(2)
            val z2 = (measured_point[2] - z).pow(2)
            val dx2 = (measured_point[3] - dx).pow(2)
            val dy2 = (measured_point[4] - dy).pow(2)
            val dz2 = (measured_point[5] - dz).pow(2)
            val s2 = s.pow(2)
            (left * exp(-((x2 + y2 + z2 + dx2 + dy2 + dz2) / (2.0 * s2))))
        }
        var likelihood = DoubleArray(particle_count)
        var sum = 0.0
        for (i in particles.indices) {
            val tmp_particle = particles[i]
            likelihood[i] = gauss(tmp_particle[0], tmp_particle[1], tmp_particle[2], tmp_particle[3], tmp_particle[4], tmp_particle[5], param)
            sum += likelihood[i]
        }
        for (i in particles.indices) {
            likelihood[i] /= sum
        }
        return likelihood
    }


    /**
     * 3次元ガウス分布を使って尤度の計算をする。正規化済み
     * @param particles Array<DoubleArray> 尤度を計算するパーティクルの集合
     * @param measured_point [DoubleArray] 測定点
     * @return [DoubleArray] 尤度
     */
    private fun calcurate_likelihood(particles: Array<DoubleArray>, measured_point: DoubleArray, param: Double = 1.0): DoubleArray {
        var gauss = {x: Double, y: Double, z: Double, s: Double ->
            val left = 1.0/((sqrt(2 * PI)).pow(3))
            val x2 = (measured_point[0] - x).pow(2)
            val y2 = (measured_point[1] - y).pow(2)
            val z2 = (measured_point[2] - z).pow(2)
            val s2 = s.pow(2)
             (left * exp(-((x2 + y2 + z2) / (2.0 * s2))))
        }
        var likelihood = DoubleArray(particle_count)
        var sum = 0.0
        for (i in particles.indices) {
            val tmp_particle = particles[i]
            likelihood[i] = gauss(tmp_particle[0], tmp_particle[1], tmp_particle[2], param)
            sum += likelihood[i]
        }
        for (i in particles.indices) {
            likelihood[i] /= sum
        }
        return likelihood
    }

    /**
     * 尤度の合成をする。（足し算して正規化）
     * @param likelihood [DoubleArray] 尤度。個数分作る
     * @return [DoubleArray] 合成後の尤度
     */
    private fun synthesize_likelihood(likelihood_1: DoubleArray, likelihood_2: DoubleArray): DoubleArray {
        var sum = 0.0;
        var normalization_likelihood = DoubleArray(particle_count)
        // likelihood == 尤度[particlu_count]
        for (i in likelihood_1.indices) {
            sum += likelihood_1[i]
            normalization_likelihood[i] += likelihood_1[i]
            sum += likelihood_2[i]
            normalization_likelihood[i] += likelihood_2[i]
        }
        for (i in normalization_likelihood.indices) {
            normalization_likelihood[i] /= sum
        }
        return normalization_likelihood
    }

    /**
     * リサンプリングする。
     * @param x [DoubleArray] パーティクルたち
     * @param likelihood [DoubleArray] 尤度
     */
    private fun resampling(x: Array<DoubleArray>, likelihood: DoubleArray) {
        // 累積和
        var cumulative_sum = DoubleArray(likelihood.size)
        cumulative_sum[0] = likelihood[0]
        for (i in 1..(particle_count-1)) {
            cumulative_sum[i] = cumulative_sum[i-1] + likelihood[i]
        }
        var max_likelihood = 0.0
        /* [0, 1/partclue_count) の乱数を出す */
        var rdm = Random.nextDouble()
        rdm /= particle_count.toDouble()
        for (i in x_resampled.indices) {
            // 累積[index] < rdm <= 累積[index+1]となるindexを探したい
            val index = find_index_from_cumulative_sum(cumulative_sum, rdm)
            // 代入している
            // いい感じのディープコピーの方法を探す
            for (p in x[index].indices) {
                x_resampled[i][p] = x[index][p]
            }
            rdm += 1.0/particle_count.toDouble()
        }
    }

    /**
     * システムノイズ
     * コンストラクタで与えた平均と分散をここで使う。ノイズがいい感じに付与されるようにしたい。
     */
    private fun systemNoise() {
        for (i in x_resampled.indices) {
            for (j in x_resampled[i].indices) {
                val rdm = java.util.Random()
                val noise = rdm.nextGaussian() / 100.0
                x_resampled[i][j] += noise
            }
        }
    }


    /**
     * 尤度っぽい計算をする。これはユークリッド距離が一番近いものを返す
     * @param particles [Array FloatArray] 尤度を計算するパーティクルの集合
     * @param measured_point [FloatArray] 測定点
     * @return particle [FloatArray] パーティクル
     */
    private fun find_nearest_particle(particles: Array<DoubleArray>, measured_point: DoubleArray): DoubleArray {
        var distance = 1000000F
        var return_particle = DoubleArray(6)
        for (particle in particles) {
            if (distance > uclidean_distance(particle, measured_point)) {
                return_particle = particle.clone()
            }
        }
        return return_particle
    }

    /* --------------------------------------------------サブ関数----------------------------------------------- */

    private fun uclidean_distance(a: DoubleArray, b: DoubleArray): Double {
        var g = 0.0
        for (i in 0..2) {
            g += (a[i]-b[i]).pow(2)
        }
        return sqrt(g)
    }

    private fun Uniform(mu: Double = 0.0, sigma: Double = 1.0): Double {
        var rdm = com.example.rotation.Random()
        return rdm.nextDouble()
    }

    private fun normal(mu: Double = 0.0, sigma: Double = 1.0): Double {
        val z = sqrt(-2.0 * log(Uniform(), kotlin.math.E)) * sin(2.0 * PI * Uniform())
        return mu + z * sigma
    }

    private fun toDouble(input: FloatArray): DoubleArray {
        var output = DoubleArray(input.size)
        for (i in input.indices) {
            output[i] = input[i].toDouble()
        }
        return output
    }

    /**
     * 尤度が一番大きい index を返す
     */
    private fun find_max_index(likelihood: DoubleArray): Int {
        var index = 0
        for(i in likelihood.indices) {
            if (likelihood[i] > likelihood[index]) {
                index = i
            }
        }
        return index
    }

    private fun find_index_from_cumulative_sum(cumulative_sum: DoubleArray, rdm: Double): Int {
        for (i in cumulative_sum.indices) {
            if (cumulative_sum[i] > rdm) {
                return i
            }
        }
        return cumulative_sum.size-1
    }

    /* ----------------------------------------------------------------------------------------------------- */

    /**
     * パーティクルフィルタを実行する
     * @param input1 [FloatArray] 観測した世界座標軸加速度1
     * @param input2 [FloatArray] 観測した世界座標軸加速度2
     * @return [FloatArray] パーティクルフィルタで出た世界座標軸加速度
     */
    public fun run(input1: FloatArray, input2: FloatArray): List<Float> {
        /* 測定データを Double に変換する */
        val input1 = toDouble(input1)
        val input2 = toDouble(input2)
        /* resampled にシステムノイズを足して x を生成する */
        /* パーティクルの速度分だけ移動させる */
        var x = move_particles(x_resampled)
        /* 尤度の計算をして，likelihood に保存しておく */
        var likelihood_1 = calcurate_likelihood_accelerate(x, input1)
        var likelihood_2 = calcurate_likelihood_accelerate(x, input2)
        likelihoods_normed = synthesize_likelihood(likelihood_1, likelihood_2)
        /* リサンプリングして，x_resampled に保存しておく */
        resampling(x, likelihoods_normed)
        systemNoise()
        val index = find_max_index(likelihoods_normed)
        val output = x[index].map { it.toFloat() }
        return output
    }

    /**
     * デバッグ用
     */
    public fun DEBUG_particleData(): Array<DoubleArray> {
        return x_resampled
    }
    public fun DEBUG_likelihood(): DoubleArray {
        return likelihoods_normed
    }
}
