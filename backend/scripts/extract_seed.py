#!/usr/bin/env python3
"""把 DatabaseInitializer.java 里的种子数组转成 data.sql。

一次性脚本：数据库从 H2 迁到 MySQL 时用来搬运种子数据，
避免手工转录 228 行中文数据出错。
"""
import re
import sys
from pathlib import Path

SRC = Path(__file__).resolve().parents[1] / "src/main/java/com/example/wms/config/DatabaseInitializer.java"
OUT = Path(__file__).resolve().parents[1] / "src/main/resources/data.sql"


def strip_comments(body: str) -> str:
    """去掉数组字面量里的 // 行注释，注释里含有逗号和大括号会干扰解析。"""
    return "\n".join(line.split("//")[0] for line in body.splitlines())


def parse_rows(body: str):
    """从 Java 二维数组字面量里取出每一行的字面量列表（只取最内层的 {}）。"""
    body = strip_comments(body)
    rows, i, n = [], 0, len(body)
    while i < n:
        if body[i] != "{":
            i += 1
            continue
        i += 1
        vals, buf, in_str, esc = [], "", False, False
        while i < n:
            c = body[i]
            if in_str:
                if esc:
                    buf += c
                    esc = False
                elif c == "\\":
                    esc = True
                elif c == '"':
                    in_str = False
                else:
                    buf += c
            elif c == '"':
                in_str = True
            elif c == ",":
                vals.append(buf.strip())
                buf = ""
            elif c == "}":
                vals.append(buf.strip())
                break
            else:
                buf += c
            i += 1
        i += 1
        if any(v for v in vals):
            rows.append(vals)
    return rows


def lit(v: str) -> str:
    """把 Java 字面量转成 SQL 字面量。"""
    v = v.strip()
    if v.endswith("L"):          # 2001L
        return v[:-1]
    if re.fullmatch(r"-?\d+(\.\d+)?", v):
        return v
    if v.startswith('"') and v.endswith('"'):
        v = v[1:-1]
    return "'" + v.replace("\\'", "''").replace("'", "''") + "'"


def extract_array(text: str, var: str) -> str:
    """取出 `Object[][] <var> = { ... };` 里最外层大括号之间的内容（不含外层括号）。"""
    m = re.search(r"Object\[\]\[\]\s+" + var + r"\s*=\s*\{", text)
    if not m:
        sys.exit(f"找不到种子数组: {var}")
    start = m.end() - 1
    depth, i, n = 0, start, len(text)
    while i < n:
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return text[start + 1:i]   # 丢掉最外层的一对大括号
        i += 1
    sys.exit(f"种子数组未闭合: {var}")


def main():
    text = SRC.read_text(encoding="utf-8")

    specs = [
        ("mats", "materials", "id,code,name,spec,unit,package_qty,category"),
        ("sups", "suppliers", "id,code,name,contact_name,contact_phone,address,province,city"),
        ("custs", "customers", "id,code,name,contact_name,contact_phone,address,province,city"),
        ("whs", "warehouses", "id,code,name,address,city,type"),
        ("locs", "locations", "id,warehouse_id,code,name,area,shelf,layer"),
    ]

    out = [
        "-- 种子数据：由 backend/scripts/extract_seed.py 从原 DatabaseInitializer.java 生成。",
        "-- 使用 INSERT IGNORE，重复启动不会产生重复数据（依赖各表 UNIQUE 约束）。",
        "",
    ]
    total = 0
    for var, table, cols in specs:
        rows = parse_rows(extract_array(text, var))
        out.append(f"-- {table}: {len(rows)} 行")
        for r in rows:
            vals = ", ".join(lit(v) for v in r)
            out.append(f"INSERT IGNORE INTO {table} ({cols}) VALUES ({vals});")
        out.append("")
        total += len(rows)

    users = [("admin", "123456", "系统管理员", "admin"),
             ("zhangsan", "123456", "张三", "operator"),
             ("lisi", "123456", "李四", "operator")]
    out.append(f"-- users: {len(users)} 行")
    for u in users:
        vals = ", ".join("'" + v + "'" for v in u)
        out.append(f"INSERT IGNORE INTO users (username,password,nickname,role) VALUES ({vals});")
    out.append("")
    total += len(users)

    OUT.write_text("\n".join(out), encoding="utf-8")
    print(f"已写出 {OUT}，共 {total} 行种子数据")


if __name__ == "__main__":
    main()
