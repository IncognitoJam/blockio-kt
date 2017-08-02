package io.block.api

import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.DERSequenceGenerator
import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.crypto.InvalidCipherTextException
import org.bouncycastle.crypto.PBEParametersGenerator
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.crypto.signers.HMacDSAKCalculator
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.math.BigInteger
import java.nio.charset.Charset
import java.util.*

object SigningUtils {

    fun signWithdrawalRequest(request: WithdrawSignRequest, secretPin: String) {
        val privateKey = getPrivateKeyFromPassphrase(
                Base64.getDecoder().decode(request.encryptedPassphrase.passphrase),
                pinToKey(secretPin)
        )

        val generatedPubKey = derivePublicKey(privateKey)
        for (input in request.inputs) {
            input.signers
                    .filter { Arrays.equals(generatedPubKey, fromHex(it.signerPubKey)) }
                    .forEach { it.signedData = signData(input.dataToSign, privateKey) }
        }
    }

    /**
     * Step (0) to (3): Converts secret PIN to AES key
     * @param pin Secret PIN
     * *
     * @return AES key for next steps
     */
    internal fun pinToKey(pin: String): ByteArray {
        val iterations = 1024
        val pinBytes = PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(pin.toCharArray())
        val salt = PBEParametersGenerator.PKCS5PasswordToUTF8Bytes("".toCharArray())

        var generator = PKCS5S2ParametersGenerator(SHA256Digest())
        generator.init(pinBytes, salt, iterations)
        var params = generator.generateDerivedParameters(128) as KeyParameter

        val intResult = PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(toHex(params.getKey()).toCharArray())

        generator = PKCS5S2ParametersGenerator(SHA256Digest())
        generator.init(intResult, salt, iterations)
        params = generator.generateDerivedParameters(256) as KeyParameter

        return params.key
    }

    /**
     * Steps (4) to (7): Decrypt passphrase with secret key
     * @param pass passphrase from withdrawal request
     * *
     * @param key secret key
     * *
     * @return decrypted passphrase for next steps
     * *
     * @throws BlockIOException
     */
    @Throws(BlockIOException::class)
    internal fun decryptPassphrase(pass: ByteArray, key: ByteArray): ByteArray {
        val aes = PaddedBufferedBlockCipher(AESEngine())
        val aesKey = KeyParameter(key)
        aes.init(false, aesKey)
        try {
            return cipherData(aes, pass)
        } catch (e: InvalidCipherTextException) {
            throw BlockIOException("Unexpected error while signing transaction. Please file an issue report.")
        }
    }

    /**
     * Wrapper for getting a private key from an encrypted passphrase
     * and encryption key combination
     * @param encryptedPassphrase the encrypted passphrase
     * *
     * @param key the secret key
     * *
     * @return private key
     * *
     * @throws BlockIOException
     */
    @Throws(BlockIOException::class)
    internal fun getPrivateKeyFromPassphrase(encryptedPassphrase: ByteArray, key: ByteArray): ByteArray {
        val passphrase = decryptPassphrase(encryptedPassphrase, key)
        try {
            return getPrivKey(fromHex(passphrase.toString(Charset.forName("UTF-8"))))
        } catch (e: UnsupportedEncodingException) {
            throw BlockIOException("Your system does not seem to support UTF-8 encoding! Aborting signing process.")
        }

    }

    /**
     * Used only for testing: encrypt secret passphrase
     * @param plain plain passphrase
     * *
     * @param key secret key
     * *
     * @return encrypted passphrase
     * *
     * @throws BlockIOException
     */
    @Throws(BlockIOException::class)
    internal fun encryptPassphrase(plain: String, key: ByteArray): ByteArray {
        val aes = PaddedBufferedBlockCipher(AESEngine())
        val aesKey = KeyParameter(key)
        aes.init(true, aesKey)
        try {
            return cipherData(aes, plain.toByteArray(charset("UTF-8")))
        } catch (e: InvalidCipherTextException) {
            throw BlockIOException("Unexpected error while signing transaction. Please file an issue report.")
        } catch (e: UnsupportedEncodingException) {
            throw BlockIOException("Your system does not seem to support UTF-8 encoding! Aborting signing process.")
        }

    }

    @Throws(InvalidCipherTextException::class)
    internal fun cipherData(cipher: PaddedBufferedBlockCipher, data: ByteArray): ByteArray {
        val minSize = cipher.getOutputSize(data.size)
        val outBuf = ByteArray(minSize)
        val length1 = cipher.processBytes(data, 0, data.size, outBuf, 0)
        val length2 = cipher.doFinal(outBuf, length1)
        val actualLength = length1 + length2
        val result = ByteArray(actualLength)
        System.arraycopy(outBuf, 0, result, 0, result.size)
        return result
    }

    /**
     * Step (8): Get privkey from decrypted passphrase
     * @param passphrase
     * *
     * @return
     */
    internal fun getPrivKey(passphrase: ByteArray): ByteArray {
        val digest = SHA256Digest()
        val privBytes = ByteArray(digest.digestSize)
        digest.update(passphrase, 0, passphrase.size)
        digest.doFinal(privBytes, 0)
        return privBytes
    }

    /**
     * Step (8) to (11): Derive pubkey from passphrase
     * @param privBytes
     * *
     * @return
     * *
     * @throws BlockIOException
     */
    @Throws(BlockIOException::class)
    internal fun derivePublicKey(privBytes: ByteArray): ByteArray {
        val params = SECNamedCurves.getByName("secp256k1")
        val ecParams = ECDomainParameters(params.curve, params.g, params.n, params.h)
        val priv = BigInteger(1, privBytes)
        val pubBytes = ecParams.g.multiply(priv).getEncoded(true)

        return pubBytes
    }


    @Throws(BlockIOException::class)
    internal fun signData(input: String, key: ByteArray): String {
        val signer = ECDSASigner(HMacDSAKCalculator(SHA256Digest()))
        val params = SECNamedCurves.getByName("secp256k1")
        val ecParams = ECDomainParameters(params.curve, params.g, params.n, params.h)
        val priv = BigInteger(1, key)
        val privKey = ECPrivateKeyParameters(priv, ecParams)

        signer.init(true, privKey)
        val sigs = signer.generateSignature(fromHex(input))

        val r = sigs[0]
        var s = sigs[1]

        // BIP62: "S must be less than or equal to half of the Group Order N"
        val overTwo = params.n.shiftRight(1)
        if (s.compareTo(overTwo) == 1) {
            s = params.n.subtract(s)
        }

        try {
            val bos = ByteArrayOutputStream()
            val seq = DERSequenceGenerator(bos)
            seq.addObject(ASN1Integer(r))
            seq.addObject(ASN1Integer(s))
            seq.close()
            return toHex(bos.toByteArray())
        } catch (e: IOException) {
            throw BlockIOException("That should never happen... File an issue report.")  // Cannot happen.
        }

    }

    /**
     * Convert byte array to a hex string representation
     * @param array input bytes
     * *
     * @return hex string
     */
    internal fun toHex(array: ByteArray): String {
        val bi = BigInteger(1, array)
        val hex = bi.toString(16)
        val paddingLength = array.size * 2 - hex.length
        if (paddingLength > 0) {
            return String.format("%0" + paddingLength + "d", 0) + hex
        } else {
            return hex
        }
    }

    /**
     * Converts a hex string representation of bytes into a byte array
     * @param s hex string
     * *
     * @return byte array
     */
    internal fun fromHex(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

}