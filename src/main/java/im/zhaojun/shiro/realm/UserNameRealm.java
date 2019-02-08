package im.zhaojun.shiro.realm;

import im.zhaojun.mapper.UserMapper;
import im.zhaojun.model.User;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Set;

@Component
public class UserNameRealm extends AuthorizingRealm {

    private static final Logger log = LoggerFactory.getLogger(UserNameRealm.class);

    @Resource
    private UserMapper userMapper;

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        log.info("从数据库获取权限信息");
        User user = (User) principals.getPrimaryPrincipal();

        String username = user.getUsername();

        Set<String> roles = userMapper.selectRoleNameByUserName(username);
        Set<String> perms = userMapper.selectPermsByUserName(username);

        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        authorizationInfo.setRoles(roles);
        authorizationInfo.setStringPermissions(perms);
        return authorizationInfo;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        log.info("从数据库获取认证信息");
        String username = (String) token.getPrincipal();
        User user = userMapper.selectOneByUserName(username);
        if (user == null) {
            throw new UnknownAccountException();
        }
        // 如果账号被锁定, 则抛出异常, (超级管理员除外)
        if ("0".equals(user.getStatus()) && "admin".equals(username) == false) {
            throw new LockedAccountException();
        }
        return new SimpleAuthenticationInfo(user, user.getPassword(), ByteSource.Util.bytes(user.getSalt()), super.getName());
    }

    public void clearAuthorizationCache(){
        this.clearCachedAuthorizationInfo(SecurityUtils.getSubject().getPrincipals());
    }


    /**
     * 超级管理员用户所有权限
     */
    @Override
    public boolean isPermitted(PrincipalCollection principals, String permission) {
        User user = (User) principals.getPrimaryPrincipal();
        return "admin".equals(user.getUsername()) || super.isPermitted(principals, permission);
    }

    /**
     * 超级管理员用户所有角色
     */
    @Override
    public boolean hasRole(PrincipalCollection principals, String roleIdentifier) {
        User user = (User) principals.getPrimaryPrincipal();
        return "admin".equals(user.getUsername()) || super.hasRole(principals, roleIdentifier);
    }
}
