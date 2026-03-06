#!/usr/bin/env python3
"""
DocAI 数据库初始化脚本
连接到远程 MySQL 数据库并执行 init.sql
"""

import sys
import subprocess

# 首先尝试安装 mysql-connector-python
print("📦 安装 MySQL Python 驱动...")
result = subprocess.run(
    [sys.executable, "-m", "pip", "install", "mysql-connector-python", "-q"],
    capture_output=True
)

if result.returncode != 0:
    print("⚠️  mysql-connector-python 安装失败，尝试使用 PyMySQL...")
    result = subprocess.run(
        [sys.executable, "-m", "pip", "install", "pymysql", "-q"],
        capture_output=True
    )
    if result.returncode != 0:
        print("❌ 无法安装任何 MySQL 驱动")
        sys.exit(1)
    use_pymysql = True
else:
    use_pymysql = False

print("✅ MySQL 驱动安装成功\n")

# 数据库配置
db_config = {
    'host': '124.222.53.34',
    'port': 3306,
    'user': 'drw',
    'password': 'dairenwen1092',
}

# 读取 init.sql 文件
try:
    with open('docai/docai/src/main/resources/init.sql', 'r', encoding='utf-8') as f:
        sql_script = f.read()
except FileNotFoundError:
    print("❌ 无法找到 init.sql 文件")
    print("请确保从项目根目录运行此脚本")
    sys.exit(1)

print("=" * 50)
print("  DocAI 数据库初始化")
print("=" * 50)
print(f"\n🔗 连接信息:")
print(f"   主机: {db_config['host']}:{db_config['port']}")
print(f"   用户: {db_config['user']}")
print(f"   数据库: docai")
print()

try:
    if use_pymysql:
        print("使用 PyMySQL 驱动...")
        import pymysql
        
        # 先连接到 MySQL 服务器（不指定数据库）
        connection = pymysql.connect(
            host=db_config['host'],
            port=db_config['port'],
            user=db_config['user'],
            password=db_config['password'],
            autocommit=False,
            charset='utf8mb4'
        )
    else:
        print("使用 mysql-connector-python 驱动...")
        import mysql.connector
        
        connection = mysql.connector.connect(
            host=db_config['host'],
            port=db_config['port'],
            user=db_config['user'],
            password=db_config['password'],
            autocommit=False
        )
    
    print("✅ 数据库连接成功\n")
    
    cursor = connection.cursor()
    
    # 分割 SQL 语句并执行
    print("📝 执行初始化脚本...")
    statements = [s.strip() for s in sql_script.split(';') if s.strip()]
    
    for i, statement in enumerate(statements, 1):
        if statement.upper().startswith('USE '):
            print(f"   [{i}/{len(statements)}] 选择数据库...")
        elif statement.upper().startswith('CREATE DATABASE'):
            print(f"   [{i}/{len(statements)}] 创建数据库...")
        elif statement.upper().startswith('CREATE TABLE'):
            # 提取表名
            table_name = statement[statement.find('TABLE')+6:].split()[1]
            print(f"   [{i}/{len(statements)}] 创建表: {table_name}")
        else:
            print(f"   [{i}/{len(statements)}] 执行 SQL...")
        
        try:
            cursor.execute(statement)
        except Exception as e:
            print(f"⚠️  执行 '{statement[:50]}...' 时出错: {e}")
            # 有些操作会失败（如 CREATE TABLE IF NOT EXISTS），继续执行
            if "already exists" not in str(e).lower():
                connection.rollback()
                raise
    
    # 提交事务
    connection.commit()
    
    cursor.close()
    connection.close()
    
    print("\n✅ 数据库初始化成功！")
    print("\n" + "=" * 50)
    print("  后续步骤:")
    print("=" * 50)
    print("1. 验证数据库:")
    print("   python verify_db.py")
    print("\n2. 启动后端:")
    print("   cd docai/docai")
    print("   mvn spring-boot:run")
    print("\n3. 访问系统:")
    print("   http://localhost:8080")
    print("=" * 50)

except Exception as e:
    print(f"\n❌ 初始化失败: {e}")
    print("\n可能的原因:")
    print("  1. 数据库服务器未运行或无法连接")
    print("  2. 用户名或密码错误")
    print("  3. 网络防火墙阻止连接")
    sys.exit(1)
