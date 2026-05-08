package com.qingqiu.openagent.service;

import com.qingqiu.openagent.model.dto.UserLoginDTO;
import com.qingqiu.openagent.model.dto.UserRegisterDTO;
import com.qingqiu.openagent.model.dto.UserRegisterMailDTO;
import com.qingqiu.openagent.model.vo.LoginUserVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author: qingqiugeek
 * @date: 2026/5/4 20:15
 * @description: User service
 */
public interface UserService {

  LoginUserVO login(UserLoginDTO userLogin);

  LoginUserVO register(UserRegisterDTO userRegister);

  Boolean logout(HttpServletRequest httpServletRequest);

  LoginUserVO getLoginUser();

  Boolean sendRegisterCode(UserRegisterMailDTO userRegisterMail);

}
