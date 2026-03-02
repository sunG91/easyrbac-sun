package com.sun.easyrbac.core.domain;

/**
 * 接口/权限领域模型（对应接口表一行）。
 * path 为必配字段，用于权限校验与角色-接口关联。
 *
 * @author SUNRUI
 */
public class RbacApi {

    private Long id;
    /** 接口路径标识，必配 */
    private String path;
    private String method;
    private String controllerName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getControllerName() {
        return controllerName;
    }

    public void setControllerName(String controllerName) {
        this.controllerName = controllerName;
    }
}
