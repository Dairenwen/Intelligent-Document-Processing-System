#!/usr/bin/env python3
"""
DocAI 数据库验证脚本
检查数据库是否正确初始化
"""

import sys
import mysql.connector

db_config = {
    'host': '124.222.53.34',
    'port': 3306,
    'user': 'drw',
    'password': 'dairenwen1092',
    'database': 'docai'
}

print("=" * 50)
print("  DocAI 数据库验证")
print("=" * 50)
print()

try:
    # 连接到数据库
    connection = mysql.connector.connect(**db_config)
    cursor = connection.cursor()
    
    print("✅ 数据库连接成功\n")
    
    # 查询表
    cursor.execute("""
        SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES 
        WHERE TABLE_SCHEMA = 'docai'
    """)
    tables = cursor.fetchall()
    
    if not tables:
        print("❌ 未找到任何表")
        sys.exit(1)
    
    print("📋 数据库表:")
    for (table_name,) in tables:
        print(f"   ✓ {table_name}")
    
    print()
    
    # 检查 users 表
    print("检查 users 表:")
    cursor.execute("""
        SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE 
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = 'docai' AND TABLE_NAME = 'users'
    """)
    columns = cursor.fetchall()
    for col_name, col_type, nullable in columns:
        null_info = "NULL" if nullable == 'YES' else "NOT NULL"
        print(f"   - {col_name}: {col_type} ({null_info})")
    
    print()
    
    # 检查 documents 表
    print("检查 documents 表:")
    cursor.execute("""
        SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE 
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = 'docai' AND TABLE_NAME = 'documents'
    """)
    columns = cursor.fetchall()
    for col_name, col_type, nullable in columns:
        null_info = "NULL" if nullable == 'YES' else "NOT NULL"
        print(f"   - {col_name}: {col_type} ({null_info})")
    
    print()
    
    # 查询数据行数
    cursor.execute("SELECT COUNT(*) FROM users")
    user_count = cursor.fetchone()[0]
    
    cursor.execute("SELECT COUNT(*) FROM documents")
    doc_count = cursor.fetchone()[0]
    
    print("📊 数据统计:")
    print(f"   users 表: {user_count} 条记录")
    print(f"   documents 表: {doc_count} 条记录")
    
    print()
    print("=" * 50)
    print("  ✅ 数据库验证完成！")
    print("=" * 50)
    
    cursor.close()
    connection.close()

except Exception as e:
    print(f"❌ 验证失败: {e}")
    sys.exit(1)
