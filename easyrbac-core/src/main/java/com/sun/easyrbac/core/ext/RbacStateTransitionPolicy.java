package com.sun.easyrbac.core.ext;

/**
 * 状态流转权限扩展点：判断从 fromState 到 toState 是否允许（如工单「待审核」→「已通过」仅审核员可操作）。
 * 框架不提供默认实现，用户实现此接口后由业务在状态变更前调用。
 *
 * @author SUNRUI
 */
public interface RbacStateTransitionPolicy {

    /**
     * 是否允许从 fromState 变更为 toState（可由 roleCode 或 userId 等判断）。
     *
     * @param fromState 原状态
     * @param toState   目标状态
     * @param roleCode  当前角色编码（或由业务传入 userId 再查角色）
     * @return true 允许，false 拒绝
     */
    boolean allowedTransition(String fromState, String toState, String roleCode);
}
