package com.qingqiu.openagent.service.impl;


import static com.qingqiu.openagent.constant.Common.REGISTER_CAPTCHA_TTL;
import static com.qingqiu.openagent.constant.Common.USER_REGISTER_CAPTCHA_KEY;
import static com.qingqiu.openagent.util.RegularUtil.checkMail;
import static com.qingqiu.openagent.util.RegularUtil.checkPassword;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingqiu.openagent.enums.BizExceptionEnum;
import com.qingqiu.openagent.enums.UserType;
import com.qingqiu.openagent.exception.BizException;
import com.qingqiu.openagent.mapper.UserMapper;
import com.qingqiu.openagent.model.dto.UserLoginDTO;
import com.qingqiu.openagent.model.dto.UserRegisterDTO;
import com.qingqiu.openagent.model.dto.UserRegisterMailDTO;
import com.qingqiu.openagent.model.entity.User;
import com.qingqiu.openagent.model.vo.LoginUserVO;
import com.qingqiu.openagent.service.UserService;
import com.qingqiu.openagent.util.MailUtil;
import com.qingqiu.openagent.util.UserContext;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * @author Qing Qiu
 * @description
 * @createDate
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

  @Resource
  private JavaMailSenderImpl mailSender;

  @Resource
  private StringRedisTemplate stringRedisTemplate;

  @Value("${spring.mail.username}")
  private String fromEmail;

  @Resource
  UserMapper userMapper;

  /**
   * @return
   */
  @Override
  public LoginUserVO login(UserLoginDTO userLogin) {
    //1.判断邮箱和密码是否为空,邮箱格式校验，密码长度校验
    String loginUserMail = userLogin.getMail();
    String loginPassword = userLogin.getPassword();
    if (StringUtils.isAnyBlank(loginUserMail, loginPassword) || checkMail(loginUserMail)
        || checkPassword(loginPassword)) {
      throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(), BizExceptionEnum.PARAMS_ERROR.getMessage());
    }

    //2.根据邮箱从数据库查询用户
    LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(User::getMail, loginUserMail);
    User queryUser = userMapper.selectOne(queryWrapper);
    if (queryUser == null) {
      throw new BizException(BizExceptionEnum.NOT_REGISTER_ERROR.getCode(), BizExceptionEnum.NOT_REGISTER_ERROR.getMessage());
    }
    if (queryUser.getStatus() == 1) {
      throw new BizException(BizExceptionEnum.FORBIDDEN_ERROR.getCode(), BizExceptionEnum.FORBIDDEN_ERROR.getMessage());
    }

    //3.核对密码
      if (!encrypt(loginPassword).equals(queryUser.getPassword())) {
        throw new BizException(BizExceptionEnum.LOGIN_PARAMS_ERROR.getCode(), BizExceptionEnum.LOGIN_PARAMS_ERROR.getMessage());
      }

    return loginSuccess(queryUser);
  }

  /**
   * 传入参数，使用参数本身作为盐值 进行加密
   *
   * @param str
   * @return
   */
  public String encrypt(String str) {
    return DigestUtils.md5DigestAsHex((str + str).getBytes());
  }

  /**
   * @param user
   */
  private LoginUserVO loginSuccess(User user) {
    UserContext.saveUser(user.getId());
    // sa-token 会自动往响应写 Set-Cookie（cookie 名 = token-name = "authorization"）
    StpUtil.login(user.getId());
    LoginUserVO loginUserVO = new LoginUserVO();
    loginUserVO.setUserId(user.getId());
    loginUserVO.setUserName(user.getUserName());
    loginUserVO.setMail(user.getMail());
    loginUserVO.setRole(user.getRole());
    return loginUserVO;
  }

  @Override
  public Boolean sendRegisterCode(UserRegisterMailDTO userRegister) {
    String mail = userRegister.getMail();
    //检查该邮箱是否已注册
    checkMailIsRegistered(mail);
    //发送验证码
    log.info("尝试发送邮箱验证码给用户：{}进行注册操作", mail);
    String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));

    // 先落 Redis 再发邮件：避免 Redis 异常时邮件已发出导致用户重复请求收到两封验证码
    try {
      stringRedisTemplate.opsForValue()
          .set(USER_REGISTER_CAPTCHA_KEY + mail, code, REGISTER_CAPTCHA_TTL, TimeUnit.MINUTES);
    } catch (RuntimeException e) {
      log.error("写入Redis验证码失败, mail={}", mail, e);
      throw new BizException(BizExceptionEnum.SYSTEM_BUSY.getCode(), "验证码服务暂时不可用，请稍后重试");
    }

    MailUtil mailUtil = new MailUtil(mailSender, fromEmail);
    mailUtil.sendCode(mail, code);
    return Boolean.TRUE;
  }

  /**
   * @return
   */
  @Override
  public LoginUserVO register(UserRegisterDTO userRegister) {
    String mail = userRegister.getMail();
    String password = userRegister.getPassword();
    String rePassword = userRegister.getRePassword();
    String registerCode = userRegister.getCode();

    //检查注册参数是否为空
    if (StringUtils.isAnyBlank(mail, password, rePassword, registerCode)) {
      throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(), BizExceptionEnum.PARAMS_ERROR.getMessage());
    }

    // 检查参数是否合法
    if (!password.equals(rePassword) || checkMail(mail) || checkPassword(password)) {
      throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(),  BizExceptionEnum.PARAMS_ERROR.getMessage());
    }

    //检查邮箱是否被注册
    checkMailIsRegistered(mail);

    //从redis获取验证码
    String rightCode;
    try {
      rightCode = stringRedisTemplate.opsForValue().get(USER_REGISTER_CAPTCHA_KEY + mail);
    } catch (RuntimeException e) {
      log.error("读取Redis验证码失败, mail={}", mail, e);
      throw new BizException(BizExceptionEnum.SYSTEM_BUSY.getCode(), "验证码服务暂时不可用，请稍后重试");
    }

    //检查验证码是否存在
    if (StringUtils.isBlank(rightCode)) {
      throw new BizException(BizExceptionEnum.CAPTCHA_ERROR.getCode(),  BizExceptionEnum.CAPTCHA_ERROR.getMessage());
    }

    //核验验证码是否正确
    if (!rightCode.equals(registerCode)) {
      throw new BizException(BizExceptionEnum.CAPTCHA_ERROR.getCode(),   BizExceptionEnum.CAPTCHA_ERROR.getMessage());
    }
    //4.密码盐值加密，写入数据库，注册成功
    String userName = mail.substring(0, mail.indexOf('@'));
    User user = User.builder()
      .userName(userName)
      .password(encrypt(password))
      .mail(mail)
      .role(UserType.USER.getCode())
      .build();
    int insert = userMapper.insert(user);
    if (insert <= 0) {
      throw new BizException(BizExceptionEnum.REGISTER_ERROR.getCode(),
          BizExceptionEnum.REGISTER_ERROR.getMessage());
    }

    return loginSuccess(user);
  }

  private void checkMailIsRegistered(String mail) {
    //发送验证码时已检查该邮箱是否注册，这里再检查一次
    LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(User::getMail, mail);
    User user = userMapper.selectOne(queryWrapper);
    if (Objects.nonNull(user)) {
      throw new BizException(
          BizExceptionEnum.ACCOUNT_EXISTED_ERROR.getCode(), BizExceptionEnum.ACCOUNT_EXISTED_ERROR.getMessage());
    }
  }

  /**
   * @return
   */
  @Override
  public Boolean logout(HttpServletRequest httpServletRequest) {
    try {
      StpUtil.logout();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
//    //获取请求头中的token，这是用户登录时生成的uuid
//    String token = httpServletRequest.getHeader(AUTHORIZATION);
//    if (StringUtils.isBlank(token)) {
//      throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的token");
//    }
//    String tokenKey = Common.LOGIN_TOKEN_KEY + token;
//    return stringRedisTemplate.delete(tokenKey);
    return Boolean.TRUE;
  }


  @Override
  public LoginUserVO getLoginUser() {
    Long userId = UserContext.getUser();
    if (userId == null) {
      throw new BizException(BizExceptionEnum.NOT_LOGIN_ERROR.getCode(), BizExceptionEnum.NOT_LOGIN_ERROR.getMessage());
    }
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new BizException(BizExceptionEnum.NOT_FOUND_ERROR.getCode(), BizExceptionEnum.NOT_FOUND_ERROR.getMessage());
    }
    LoginUserVO loginUserVO = new LoginUserVO();
    loginUserVO.setUserId(user.getId());
    loginUserVO.setRole(user.getRole());
    loginUserVO.setUserName(user.getUserName());
    loginUserVO.setMail(user.getMail());
    return loginUserVO;
  }
}




