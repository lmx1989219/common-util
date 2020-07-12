package com.lmx.common.service.impl;

import com.google.common.base.Charsets;
import com.lmx.common.service.EncAndDecryptService;
import com.lmx.common.util.AesUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by lucas on 2020/7/9.
 */
@Slf4j
public class DefaultAesServiceImpl implements EncAndDecryptService {
    private String aesKey = "14LbKUwKowbSgARK6/uSpw==";

    public DefaultAesServiceImpl(String aesKey) {
        this.aesKey = aesKey;
    }

    public DefaultAesServiceImpl() {
    }

    @Override
    public String decrypt(String secretStr) {
        try {
            return AesUtil.decrypt(secretStr, aesKey, Charsets.UTF_8.toString());
        } catch (Exception e) {
            log.error("decrypt error", e);
            return null;
        }
    }

    @Override
    public String encrypt(String originStr) {
        try {
            return AesUtil.encrypt(originStr, aesKey, Charsets.UTF_8.toString());
        } catch (Exception e) {
            log.error("encrypt error", e);
            return null;
        }
    }
}
