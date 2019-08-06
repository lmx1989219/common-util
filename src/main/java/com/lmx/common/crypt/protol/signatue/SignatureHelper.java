package com.lmx.common.crypt.protol.signatue;

import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.PublicKeySign;
import com.google.crypto.tink.PublicKeyVerify;
import com.google.crypto.tink.config.TinkConfig;
import com.google.crypto.tink.signature.PublicKeySignFactory;
import com.google.crypto.tink.signature.PublicKeyVerifyFactory;
import com.google.crypto.tink.signature.SignatureKeyTemplates;

import java.security.GeneralSecurityException;

/**
 * 数字签名提供签名数据和签名验证的功能。它保证签名数据的真实性和完整性，而不是保密性。
 * <p>
 * 数字签名的功能在Tink中表示为一对原语：PublicKeySign用于数据签名，PublicKeyVerify用于签名验证。这些原语的实现对于自适应选择的消息攻击是安全的。
 * <p>
 * Created by Administrator on 2018/12/8.
 */
public class SignatureHelper {
    /**
     * 签名密钥处理器
     */
    static KeysetHandle privateKeysetHandle;

    static {
        try {
            TinkConfig.register();
            // 1. Generate the private key material.
            privateKeysetHandle = KeysetHandle.generateNew(
                    SignatureKeyTemplates.ECDSA_P256);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    /**
     * 数字签名
     *
     * @param data
     * @return
     * @throws GeneralSecurityException
     */
    public static byte[] signature(byte[] data) throws GeneralSecurityException {
        // 2. Get the primitive.
        PublicKeySign signer = PublicKeySignFactory.getPrimitive(privateKeysetHandle);
        // 3. Use the primitive to sign.
        return signer.sign(data);
    }

    /**
     * 验签
     *
     * @param signature
     * @param originData
     * @throws GeneralSecurityException
     */
    public static void verify(byte[] signature, byte[] originData) throws GeneralSecurityException {
        // 1. Obtain a handle for the public key material.
        KeysetHandle publicKeysetHandle = privateKeysetHandle.getPublicKeysetHandle();
        // 2. Get the primitive.
        PublicKeyVerify verifier = PublicKeyVerifyFactory.getPrimitive(publicKeysetHandle);
        // 4. Use the primitive to verify.
        verifier.verify(signature, originData);
    }

    public static void main(String[] args) {
        byte[] origin = "hello,lmx".getBytes();
        try {
            byte[] signBytes = SignatureHelper.signature(origin);
            System.out.println(new String(signBytes));
            SignatureHelper.verify(signBytes, origin);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }
}
