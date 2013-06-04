package com.mitsugaru.KarmicShare.permissions;

public enum PermissionNode
{
	KARMA(".karma"), KARMA_OTHER(".karma.other"), GIVE(".give"), TAKE(".take"), INFO(
			".info"), CHEST(".chest"), SIGN(".sign"), GROUP_CREATE(
			".group.create"), GROUP_ADD(".group.add"), GROUP_REMOVE(
			".group.remove"), GROUP_LEAVE(".group.leave"), IGNORE_KARMA(
			".ignore.karma"), IGNORE_GROUP(".ignore.group"), COMMANDS_GIVE(
			".commands.give"), COMMANDS_TAKE(".commands.take"), COMMANDS_LIST(
			".commands.list"), COMMANDS_OPEN(".commands.open"), COMMANDS_VALUE(
			".commands.value"), ADMIN_ADD(".admin.add"), ADMIN_CLEANUP(
			".admin.cleanup"), ADMIN_RESET(".admin.reset"), ADMIN_SET(
			".admin.set"), ADMIN_DRAIN(".admin.drain"), ADMIN_RELOAD(
			".admin.reload"), ADMIN_GROUP_CREATE(".admin.group.create"), ADMIN_GROUP_DELETE(
			".admin.group.delete"), ADMIN_GROUP_ADD(".admin.group.add"), ADMIN_GROUP_REMOVE(
			".admin.group.remove");

	private static final String prefix = "KarmicShare";
	private String node;

	private PermissionNode(String node)
	{
		this.node = prefix + node;
	}

	public String getNode()
	{
		return node;
	}
}
