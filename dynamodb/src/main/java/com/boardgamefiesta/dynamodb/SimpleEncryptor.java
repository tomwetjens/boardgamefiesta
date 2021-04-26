package com.boardgamefiesta.dynamodb;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

final class SimpleEncryptor {

    private static final String CIPHER_ALGORITHM = "RC4";
    private static final String SECRET = "secret";

    public byte[] encrypt(byte[] input) {
        try {
            return getCipher(Cipher.ENCRYPT_MODE).doFinal(input);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new UnsupportedOperationException("Failed to encrypt", e);
        }
    }

    public byte[] decrypt(byte[] input) {
        try {
            return getCipher(Cipher.DECRYPT_MODE).doFinal(input);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new UnsupportedOperationException("Failed to decrypt", e);
        }
    }

    private Cipher getCipher(int opmode) {
        var secretKeySpec = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), CIPHER_ALGORITHM);

        try {
            var cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(opmode, secretKeySpec);

            return cipher;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            throw new UnsupportedOperationException("Failed to initialize cipher", e);
        }
    }
}
