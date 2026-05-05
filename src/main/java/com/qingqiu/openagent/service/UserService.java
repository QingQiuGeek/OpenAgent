package com.qingqiu.openagent.service;

import com.qingqiu.openagent.model.dto.UserLoginDTO;
import com.qingqiu.openagent.model.dto.UserRegisterDTO;
import com.qingqiu.openagent.model.dto.UserRegisterMailDTO;
import com.qingqiu.openagent.model.vo.LoginUserVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author 懒大王Smile
 * @description 针对表【user(用户表)】的数据库操作Service
 * @createDate 2024-09-12 22:19:13
 */
public interface UserService {

  LoginUserVO login(UserLoginDTO userLogin);

  LoginUserVO register(UserRegisterDTO userRegister);

  Boolean logout(HttpServletRequest httpServletRequest);

  LoginUserVO getLoginUser();

  Boolean sendRegisterCode(UserRegisterMailDTO userRegisterMail);

}
