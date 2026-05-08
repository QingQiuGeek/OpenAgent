package com.qingqiu.openagent.constant;

/**
 * @author: qingqiugeek
 * @date: 2026/5/12 21:40
 * @description: Common
 */

public interface Common {

  String SA_TOKEN_USER_ROLE = "sa-token:role:";

  //限制可上传的图片大小
  int IMG_SIZE_LIMIT = 1;

  //限制可上传的图片大小单位
  String IMG_SIZE_UNIT = "M";

  //注册验证码有效期（min）
  Long REGISTER_CAPTCHA_TTL = 120L;

  //注册验证码 Redis key 前缀
  String USER_REGISTER_CAPTCHA_KEY = "openagent:register:captcha:";

  //文件保存目录
  String FILE_SAVE_DIR = System.getProperty("user.dir") + "/tmp";

  /**
   * 阿里云OSS存储的默认目录前缀
   */
  String OSS_UPLOAD_PREFIX = "upload";

}
