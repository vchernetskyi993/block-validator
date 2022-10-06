package com.example

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigInteger
import java.security.MessageDigest
import java.util.HexFormat
import kotlin.system.exitProcess


fun main(args: Array<String>) = BlockValidator().main(args)

private val json = Json {
    prettyPrint = true
}

private val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }
}

class BlockValidator : CliktCommand(help = "Validate Bitcoin block hash.") {
    private val block by argument()

    override fun run() = runBlocking {
        echo("Validating block $block...")

        echo("\nFetching block data as JSON...")

        val blockData = client.get("https://blockchain.info/block-height/$block")
            .body<BlockList>().blocks.first()

        echo("Fetched: ${json.encodeToString(blockData)}")

        echo("\nConverting all header fields to hex...")

        val blockDataHex = blockData.toHex()

        echo("Block data hex: ${json.encodeToString(blockDataHex)}")

        echo("\nConverting all header fields to little endian...")

        val blockDataLittleEndian = blockDataHex.toLittleEndian()

        echo("Block data little endian: ${json.encodeToString(blockDataLittleEndian)}")

        echo("\nHashing...")

        val digest = MessageDigest.getInstance("SHA-256")
        val hash0 = digest.digest(blockDataLittleEndian.serialize())
        echo("First hash: ${hash0.toHexString()}")

        val hash1 = digest.digest(hash0)
        echo("Second hash: ${hash1.toHexString()}")

        val hash1LittleEndian = hash1.toHexString().toLittleEndian()
        echo("Second hash little endian (hash of the $block block): $hash1LittleEndian")

        if (blockData.hash == hash1LittleEndian) {
            echo("Header hash is equal to block hash")
        } else {
            echo("Header hash is not equal to block hash ($hash1LittleEndian != ${blockData.hash})")
            exitProcess(1)
        }

        echo("\nCalculating target value from block's nBits...")

        val target = targetFromNBits(blockDataHex.nBits)
        echo("Target as hex: ${target.toString(16)}")

        val hash1AsInt = hash1LittleEndian.toBigInteger(16)
        if (hash1AsInt < target) {
            echo("Header hash value is smaller than target")
        } else {
            echo("Header hash value is not smaller than target ($hash1AsInt >= $target)")
            exitProcess(1)
        }
    }
}

@Serializable
data class BlockList(
    val blocks: List<BlockData>,
)

@Serializable
data class BlockData(
    val hash: String,
    @SerialName("ver")
    val version: Long,
    @SerialName("prev_block")
    val previousBlock: String,
    @SerialName("mrkl_root")
    val merkleRoot: String,
    val time: Long,
    @SerialName("bits")
    val nBits: Long,
    val nonce: Long,
) {
    fun toHex() = BlockDataHex(
        version = version.toString(16),
        previousBlock = previousBlock,
        merkleRoot = merkleRoot,
        time = time.toString(16),
        nBits = nBits.toString(16),
        nonce = nonce.toString(16),
    )
}

@Serializable
data class BlockDataHex(
    @SerialName("ver")
    val version: String,
    @SerialName("prev_block")
    val previousBlock: String,
    @SerialName("mrkl_root")
    val merkleRoot: String,
    val time: String,
    @SerialName("bits")
    val nBits: String,
    val nonce: String,
) {
    fun toLittleEndian() = BlockDataHex(
        version = version.toLittleEndian(),
        previousBlock = previousBlock.toLittleEndian(),
        merkleRoot = merkleRoot.toLittleEndian(),
        time = time.toLittleEndian(),
        nBits = nBits.toLittleEndian(),
        nonce = nonce.toLittleEndian(),
    )

    fun serialize(): ByteArray = sequenceOf(version, previousBlock, merkleRoot, time, nBits, nonce)
        .map { it.toHexBytes() }
        .reduce { acc, bytes -> acc + bytes }
}

private fun String.toLittleEndian(): String =
    chunked(2).reversed().joinToString("")

private fun String.toHexBytes(): ByteArray =
    HexFormat.of().parseHex(this)

private fun ByteArray.toHexString(): String =
    HexFormat.of().formatHex(this)

private fun targetFromNBits(nBits: String): BigInteger {
    val power = 256.toBigInteger().pow(nBits.take(2).toInt(16) - 3)
    val base = nBits.drop(2).toBigInteger(16)
    return base * power
}
