package com.wuweibi.bullet.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.qq.connect.QQConnectException;
import com.qq.connect.api.qzone.UserInfo;
import com.qq.connect.javabeans.qzone.UserInfoBean;
import com.wuweibi.bullet.annotation.JwtUser;
import com.wuweibi.bullet.conn.CoonPool;
import com.wuweibi.bullet.domain.ResultMessage;
import com.wuweibi.bullet.domain.domain.session.Session;
import com.wuweibi.bullet.domain.message.MessageResult;
import com.wuweibi.bullet.entity.User;
import com.wuweibi.bullet.entity.api.Result;
import com.wuweibi.bullet.exception.type.AuthErrorType;
import com.wuweibi.bullet.oauth2.service.AuthenticationService;
import com.wuweibi.bullet.service.UserService;
import com.wuweibi.bullet.utils.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.provider.token.ConsumerTokenServices;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * 
 * @author marker
 * @version 1.0
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

	@Autowired
    private UserService userService;

	
	@InitBinder  
	public void initBinder(WebDataBinder binder) {  
	    // 添加一个日期类型编辑器，也就是需要日期类型的时候，怎么把字符串转化为日期类型  
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");  
		dateFormat.setLenient(false);  
		binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));  
		
//		binder.setValidator(new UserValidator()); //添加一个spring自带的validator
	}




    @Autowired
    private CoonPool pool;
	
	/**
	 * 获取登录的用户信息
	 */
	@RequestMapping(value="/login/info", method=RequestMethod.GET) 
	public Result loginInfo(  @JwtUser Session session ){

		if(session.isNotLogin()){
			return Result.fail(AuthErrorType.INVALID_LOGIN);
		}

		Long userId = session.getUserId();

		// 验证邮箱正确性
		User user = userService.getById(userId);
		user.setPassword(null);

		JSONObject result = (JSONObject)JSON.toJSON(user);

		result.put("connNums", pool.count());

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		result.put("loginTime", sdf.format(user.getLoginTime()));
		result.put("balance", StringUtil.roundHalfUp(user.getBalance()));


		return Result.success(result);
	}
	
	
	
	
	@RequestMapping(value="/internet",method=RequestMethod.POST) 
	public ResultMessage register(HttpServletRequest request, @RequestParam("from") String from){
		HttpSession session = request.getSession(false);
		if(session != null){
			User user = new User();
			String accessToken = (String) session.getAttribute("demo_access_token");
			String openID = (String) session.getAttribute("demo_openid"); 
			if("qq".equals(from)){
	            UserInfo qzoneUserInfo = new UserInfo(accessToken, openID);
	            UserInfoBean userInfoBean;
				try {
					userInfoBean = qzoneUserInfo.getUserInfo();
					if (userInfoBean.getRet() == 0) {
//						user.setName(userInfoBean.getNickname());

//						user.setOpenId(openID);
						return new ResultMessage(true, user);
		            }else{
		            	return new ResultMessage(false, "很抱歉，我们没能正确获取到您的信息，原因是： " + userInfoBean.getMsg());
		            }
				} catch (QQConnectException e) {
					e.printStackTrace();
				} 
			}
			 
		}else{
			new ResultMessage(false,"操作失败 session invalid");
		}
		 
		
		return new ResultMessage(false,"操作失败");
	}



	@Resource()
	ConsumerTokenServices consumerTokenServices;


	/**
	 * 注销操作
	 */
	@RequestMapping(value="/loginout", method=RequestMethod.POST)
	public Result loginout(HttpServletRequest request){
		String authentication = request.getHeader(HttpHeaders.AUTHORIZATION);
		String tokenValue = StringUtils.substring(authentication, AuthenticationService.BEARER_BEGIN_INDEX);
		if(consumerTokenServices.revokeToken(tokenValue)){
			return Result.success();
		}else{
			return Result.fail(AuthErrorType.INVALID_REQUEST);
		}
	}



    /**
     * 修改密码
     */
    @RequestMapping(value="/changepass", method=RequestMethod.POST)
    public MessageResult changepass(@RequestParam String pass,
                                    @RequestParam String code,
                                HttpServletRequest request){
        // 根据code查询用户信息
        return userService.changePass4Code(code, pass);
    }
	
}
