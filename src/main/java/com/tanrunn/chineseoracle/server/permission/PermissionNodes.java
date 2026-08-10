package com.tanrunn.chineseoracle.server.permission;

/**
 * Permission node constants (design document section 12.1).
 */
public final class PermissionNodes {
    public static final String CMD_SELF = "chinese_oracle.command.self";
    public static final String CMD_OTHERS = "chinese_oracle.command.others";
    public static final String CMD_SET = "chinese_oracle.command.set";
    public static final String CMD_REROLL = "chinese_oracle.command.reroll";
    public static final String CMD_RELOAD = "chinese_oracle.command.reload";
    public static final String BYPASS_PENALTY = "chinese_oracle.bypass.penalty";
    public static final String REROLL_UNLIMITED = "chinese_oracle.reroll.unlimited";

    private PermissionNodes() {
    }
}
