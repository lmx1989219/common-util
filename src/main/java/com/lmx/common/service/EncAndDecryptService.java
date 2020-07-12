package com.lmx.common.service;

/**
 * 加解密接口，可自己灵活扩展
 * Created by lucas on 2020/7/9.
 */
public interface EncAndDecryptService {

  String decrypt(String secretStr);

  String encrypt(String originStr);
}
