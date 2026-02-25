# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

secKillBot 是一个 Java 项目，目前处于初始化阶段，尚无实现代码。项目托管于 GitHub：https://github.com/xdu-d/secKillBot.git

## 技术栈

- **语言**: Java 8 (JDK 1.8)
- **构建工具**: Maven 3.6.3
- **IDE**: IntelliJ IDEA

## 构建与运行

项目尚未创建 `pom.xml`，初始化 Maven 项目时应创建标准目录结构：

```
src/main/java/
src/main/resources/
src/test/java/
src/test/resources/
pom.xml
```

Maven 本地仓库配置于 `C:\Users\10355\Program Data\maven-local-repository`，settings 文件位于 `C:\Users\10355\.m2\settings.xml`。

一旦 `pom.xml` 存在，常用命令：

```bash
mvn clean compile        # 编译
mvn clean package        # 打包
mvn test                 # 运行所有测试
mvn test -Dtest=ClassName # 运行单个测试类
mvn clean package -DskipTests # 跳过测试打包
```

## 权限配置

`.claude/settings.local.json` 中已配置 Claude Code 权限，当前仅允许 `cd` 命令执行。如需运行其他命令，需更新该文件中的 `permissions.allow` 列表。
