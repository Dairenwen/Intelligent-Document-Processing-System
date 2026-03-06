#!/usr/bin/env python3
"""
数据库连接诊断脚本
"""

import sys
import socket
import time

db_config = {
    'host': '124.222.53.34',
    'port': 3306,
    'database': 'docai',
    'user': 'drw',
    'password': 'dairenwen1092'
}

print("=" * 60)
print("  数据库连接诊断工具")
print("=" * 60)
print()

# 1. 检查网络连接
print("1️⃣  检查网络连接...")
try:
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(5)
    result = sock.connect_ex((db_config['host'], db_config['port']))
    sock.close()
    
    if result == 0:
        print(f"   ✅ 可以连接到 {db_config['host']}:{db_config['port']}")
    else:
        print(f"   ❌ 无法连接到 {db_config['host']}:{db_config['port']}")
        print(f"      错误代码: {result}")
        print("      可能原因:")
        print("      - MySQL 服务器未运行")
        print("      - 防火墙阻止了连接")
        print("      - IP 地址或端口错误")
except Exception as e:
    print(f"   ❌ 连接失败: {e}")

print()

# 2. 尝试连接数据库
print("2️⃣  尝试连接 MySQL 数据库...")
try:
    import mysql.connector
    
    connection = mysql.connector.connect(
        host=db_config['host'],
        port=db_config['port'],
        user=db_config['user'],
        password=db_config['password'],
        database=db_config['database'],
        connection_timeout=10
    )
    
    print(f"   ✅ MySQL 数据库连接成功")
    
    # 3. 检查表
    cursor = connection.cursor()
    cursor.execute("SHOW TABLES")
    tables = cursor.fetchall()
    
    print(f"   📋 数据库表数量: {len(tables)}")
    if tables:
        for (table_name,) in tables:
            print(f"      - {table_name}")
    
    cursor.close()
    connection.close()
    
except mysql.connector.Error as e:
    print(f"   ❌ MySQL 连接失败: {e}")
    error_code = e.errno if hasattr(e, 'errno') else None
    
    if error_code == 1045:
        print("      原因: 用户名或密码错误")
    elif error_code == 1049:
        print("      原因: 数据库 'docai' 不存在")
    elif error_code == 2003 or error_code == 2006:
        print("      原因: 无法连接到服务器")
    else:
        print(f"      错误代码: {error_code}")
        
except Exception as e:
    print(f"   ❌ 发生异常: {e}")
    print("      请检查是否安装了 mysql-connector-python")
    print("      运行: pip install mysql-connector-python")

print()

# 4. 显示配置信息
print("3️⃣  当前数据库配置:")
print(f"   Host: {db_config['host']}")
print(f"   Port: {db_config['port']}")
print(f"   Database: {db_config['database']}")
print(f"   Username: {db_config['user']}")
print(f"   Password: {db_config['password']}")

print()
print("=" * 60)
