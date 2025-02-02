package com.boke.filter.gitee;


import com.boke.model.dto.UserDetailsDTO;
import org.springframework.security.authentication.AbstractAuthenticationToken;

public class GiteeAuthentication extends AbstractAuthenticationToken {

  private String code;
  private UserDetailsDTO currentUser;

  public GiteeAuthentication() {
    super(null); // 权限，用不上，直接null
  }

  @Override
  public Object getCredentials() {
    return isAuthenticated() ? null : code;
  }

  @Override
  public Object getPrincipal() {
    return isAuthenticated() ? currentUser : null;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public UserDetailsDTO getCurrentUser() {
    return currentUser;
  }

  public void setCurrentUser(UserDetailsDTO currentUser) {
    this.currentUser = currentUser;
  }
}