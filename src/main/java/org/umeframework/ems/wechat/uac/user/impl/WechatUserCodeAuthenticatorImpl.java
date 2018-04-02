package org.umeframework.ems.wechat.uac.user.impl;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.collections4.map.HashedMap;
import org.umeframework.dora.bean.BeanTransformer;
import org.umeframework.dora.bean.BeanUtil;
import org.umeframework.dora.exception.ApplicationException;
import org.umeframework.dora.util.CodecUtil;
import org.umeframework.dora.util.StringUtil;
import org.umeframework.uac.entity.UmeUserDto;
import org.umeframework.uac.user.impl.BaseAuthenticator;
import org.umeframework.ems.wechat.uac.entity.UmeWechatUserDto;
import org.umeframework.ems.wechat.uac.message.MessageConst;
import org.umeframework.wechat.service.WechatCommonService;
import org.umeframework.wechat.service.dto.WechatUserDto;
import org.umeframework.wechat.service.dto.WechatWebAccTokenDto;

/**
 * WeChat用户鉴权方式实现类<br>
 * WeChat鉴权的场景下该类由框架回调，回调时传入accessCode（由微信服务器传递）作为登录ID，无需密码<br>
 * 
 * @author mayue
 */
public abstract class WechatUserCodeAuthenticatorImpl extends BaseAuthenticator<Map<String, Object>> implements MessageConst {
	/**
	 * 微信通用服务接口实例。
	 */
	@Resource(name = "wechatCommonService")
	private WechatCommonService wechatCommonService;
	/**
	 * UmeUserDto对象中微信openid对应属性的名称。
	 */
	private String openidPropertyName = "wechatOpenid";

	/*
	 * (non-Javadoc)
	 *
	 * @see org.umeframework.dora.service.com.UserAuthenticator#doAuthentication(java.lang.String, java.lang.String)
	 */
	@Override
	public Map<String, Object> findActualUser(String accessCode, String optionPasspord, String... options) {
		if (StringUtil.isEmpty(accessCode)) {
			throw new ApplicationException(UME_WECHAT_UAC_MSG_001);
		}
		// 使用微信服务器返回的accessToken和openid，查询微信用户基本属性信息
		WechatWebAccTokenDto wechatWebAccToken = wechatCommonService.getWechatWebAccTokenDTO(accessCode);
		WechatUserDto wechatUser = wechatCommonService.getWechatUserData(wechatWebAccToken.getOpenid(), wechatWebAccToken.getAccess_token());
		String openid = wechatUser.getOpenid();

		UmeWechatUserDto param = new UmeWechatUserDto();
		param.setWechatOpenid(openid);
		List<UmeWechatUserDto> existUmeWechatUser = getDao().queryForObjectList(UmeWechatUserDto.SQLID.SEARCH, param, UmeWechatUserDto.class);

		UmeWechatUserDto umeWechatUser = null;
		UmeUserDto umeUser = null;
		if (existUmeWechatUser.size() == 0) {
			// 对应用户不存在的时候，创建新用户信息
			// 首先，插入UME微信用户记录
			umeWechatUser = this.makeUmeWechatUser(wechatUser);
			if (getDao().update(UmeWechatUserDto.SQLID.INSERT, umeWechatUser) != 1) {
				throw new ApplicationException(UME_WECHAT_UAC_MSG_002, new Object[] { openid });
			}
			// 然后，插入UME基础用户记录
			umeUser = this.makeUmeUser(umeWechatUser);
			if (getDao().update(UmeUserDto.SQLID.INSERT, umeUser) != 1) {
				throw new ApplicationException(UME_WECHAT_UAC_MSG_002, new Object[] { openid });
			}
		} else if (existUmeWechatUser.size() == 1) {
			umeWechatUser = existUmeWechatUser.get(0);
			umeUser = getDao().queryForObject(UmeUserDto.SQLID.FIND, umeWechatUser.getUid(), UmeUserDto.class);
			if (umeUser == null || (umeUser.getUserStatus() != null && umeUser.getUserStatus() != 1)) {
				// 用户状态信息为不可用设定状态
				throw new ApplicationException(UME_WECHAT_UAC_MSG_003, new Object[] { umeUser.getUserStatus() });
			}
			// 更新UME微信用户记录
			if (getDao().update(UmeWechatUserDto.SQLID.UPDATE, umeWechatUser) != 1) {
				throw new ApplicationException(UME_WECHAT_UAC_MSG_002, new Object[] { wechatUser.getOpenid() });
			}
		} else {
			// 用户信息异常：状态不一致
			throw new ApplicationException(UME_WECHAT_UAC_MSG_004, new Object[] { wechatUser.getOpenid() });
		}
		// 整合UmeWechatUser和UmeUser的属性后作为User对象返回。
		Map<String, Object> user = new HashedMap<String, Object>();
		user = BeanUtil.beanToMap(umeUser);
		user.putAll(BeanUtil.beanToMap(umeWechatUser));
		return user;
	}

	/**
	 * @return the openidPropertyName
	 */
	public String getOpenidPropertyName() {
		return openidPropertyName;
	}

	/**
	 * @param openidPropertyName
	 *            the openidPropertyName to set
	 */
	public void setOpenidPropertyName(String openidPropertyName) {
		this.openidPropertyName = openidPropertyName;
	}

	/**
	 * makeUmeWechatUser<br>
	 *
	 * @param wechatUser
	 */
	public UmeWechatUserDto makeUmeWechatUser(WechatUserDto wechatUser) {
		UmeWechatUserDto umeWechatUser = new UmeWechatUserDto();
		// 记录当前系统时间
		Timestamp currentTime = getCurrentTimestamp();
		BeanTransformer.populateObject(umeWechatUser, wechatUser);
		// 生成新用户UID
		String uid = CodecUtil.encodeMD5Hex(wechatUser.getOpenid());
		umeWechatUser.setUid(uid);
		// umeUser.setUserStatus(1);
		umeWechatUser.setCreateAuthor("system");
		umeWechatUser.setCreateDatetime(currentTime);
		umeWechatUser.setUpdateAuthor("system");
		umeWechatUser.setUpdateDatetime(currentTime);
		// 更新用户表中微信相关的最新信息
		umeWechatUser.setWechatOpenid(wechatUser.getOpenid());
		umeWechatUser.setWechatCity(wechatUser.getCity());
		umeWechatUser.setWechatCountry(wechatUser.getCountry());
		umeWechatUser.setWechatHeadimgurl(wechatUser.getHeadimgurl());
		umeWechatUser.setWechatLanguage(wechatUser.getLanguage());
		String wechatUserNickname = wechatUser.getNickname() == null ? "" : CodecUtil.encodeAsUTF8((wechatUser.getNickname()));
		umeWechatUser.setWechatNickname(wechatUserNickname);
		if (wechatUser.getPrivilege() != null && wechatUser.getPrivilege().length > 0) {
			StringBuilder tagidStr = new StringBuilder();
			for (String e : wechatUser.getPrivilege()) {
				tagidStr.append(e);
				tagidStr.append(" | ");
			}
			umeWechatUser.setWechatPrivilege(tagidStr.toString());
		}
		umeWechatUser.setWechatProvince(wechatUser.getProvince());
		umeWechatUser.setWechatSex(wechatUser.getSex());
		umeWechatUser.setWechatUnionid(wechatUser.getUnionid());
		umeWechatUser.setWechatSubscribe(wechatUser.getSubscribe());
		Timestamp wechatUserSubscribeTime = new Timestamp(wechatUser.getSubscribe_time() * 1000);
		umeWechatUser.setWechatSubscribeTime(wechatUserSubscribeTime);
		umeWechatUser.setWechatGroupid(wechatUser.getGroupid());
		if (wechatUser.getTagid_list() != null && wechatUser.getTagid_list().length > 0) {
			StringBuilder tagidStr = new StringBuilder();
			for (int e : wechatUser.getTagid_list()) {
				tagidStr.append(e);
				tagidStr.append(" | ");
			}
			umeWechatUser.setWechatTagidList(tagidStr.toString());
		}
		return umeWechatUser;
	}

	/**
	 * makeUmeUser
	 * 
	 * @param umeWechatUser
	 * @return
	 */
	public UmeUserDto makeUmeUser(UmeWechatUserDto umeWechatUser) {
		UmeUserDto umeUser = new UmeUserDto();
		umeUser.setUserId(umeWechatUser.getUid());
		umeUser.setUserDesc("微信用户" + umeWechatUser.getWechatOpenid());
		umeUser.setUserStatus(1);
		umeUser.setCreateAuthor(umeWechatUser.getCreateAuthor());
		umeUser.setCreateDatetime(umeWechatUser.getCreateDatetime());
		umeUser.setUpdateAuthor(umeWechatUser.getUpdateAuthor());
		umeUser.setUpdateDatetime(umeWechatUser.getUpdateDatetime());
		return umeUser;
	}

}
